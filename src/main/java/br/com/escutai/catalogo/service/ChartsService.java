package br.com.escutai.catalogo.service;

import br.com.escutai.catalogo.service.LancamentosService.Lancamento;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Rankings de músicas para a tela inicial, usando o feed público da Apple
 * (rss.marketingtools.apple.com), que não precisa de chave.
 *
 * - Top Brasil: o ranking "mais tocadas" do Brasil.
 * - Top Mundo: o feed da Apple é por país e não tem um ranking global pronto,
 *   então combinamos o ranking de vários países grandes: cada música ganha
 *   pontos pela posição que ocupa em cada país (1º lugar = 50 pontos, 2º = 49...)
 *   e a lista final é ordenada pela soma.
 *
 * Classe separada do ItunesService de propósito, para não interferir na busca.
 */
public class ChartsService {

    private static final String[] PAISES_MUNDO = {
            "us", "gb", "br", "mx", "de", "fr", "es", "it", "ca", "au" };
    private static final int PROFUNDIDADE = 50;

    private final HttpClient cliente = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ArrayList<Lancamento> topBrasil(int quantidade)
            throws IOException, InterruptedException {
        ArrayList<Lancamento> lista = consultar("br");
        return new ArrayList<>(lista.subList(0, Math.min(quantidade, lista.size())));
    }

    public ArrayList<Lancamento> topMundo(int quantidade)
            throws IOException, InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            List<Callable<ArrayList<Lancamento>>> tarefas = new ArrayList<>();
            for (String pais : PAISES_MUNDO) {
                tarefas.add(() -> consultar(pais));
            }

            Map<Long, Integer> pontos = new HashMap<>();
            Map<Long, Lancamento> dados = new LinkedHashMap<>();
            int paisesOk = 0;

            for (Future<ArrayList<Lancamento>> futuro : pool.invokeAll(tarefas)) {
                try {
                    ArrayList<Lancamento> ranking = futuro.get();
                    paisesOk++;
                    for (int i = 0; i < ranking.size(); i++) {
                        Lancamento l = ranking.get(i);
                        pontos.merge(l.getId(), PROFUNDIDADE - i, Integer::sum);
                        dados.putIfAbsent(l.getId(), l);
                    }
                } catch (ExecutionException e) {
                    // um país que falhou não derruba o ranking dos outros
                }
            }

            if (paisesOk == 0) {
                throw new IOException("Não foi possível consultar o ranking de nenhum país.");
            }

            ArrayList<Lancamento> ordenada = new ArrayList<>(dados.values());
            ordenada.sort((a, b) -> Integer.compare(pontos.get(b.getId()), pontos.get(a.getId())));

            return new ArrayList<>(ordenada.subList(0, Math.min(quantidade, ordenada.size())));
        } finally {
            pool.shutdown();
        }
    }

    /** Ranking de músicas de um país, na ordem do feed (1º lugar primeiro). */
    private ArrayList<Lancamento> consultar(String pais) throws IOException, InterruptedException {

        String endereco = "https://rss.marketingtools.apple.com/api/v2/"
                + pais + "/music/most-played/" + PROFUNDIDADE + "/songs.json";

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(endereco))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resposta = cliente.send(
                requisicao,
                HttpResponse.BodyHandlers.ofString());

        if (resposta.statusCode() != 200) {
            throw new IOException("Erro ao consultar o ranking (" + pais + ", HTTP "
                    + resposta.statusCode() + ").");
        }

        JsonObject raiz = JsonParser.parseString(resposta.body()).getAsJsonObject();
        JsonArray resultados = raiz.getAsJsonObject("feed").getAsJsonArray("results");

        ArrayList<Lancamento> lista = new ArrayList<>();

        for (int i = 0; i < resultados.size(); i++) {
            JsonObject item = resultados.get(i).getAsJsonObject();

            String id = texto(item, "id");
            String titulo = texto(item, "name");
            String artista = texto(item, "artistName");

            if (id == null || titulo == null || artista == null) {
                continue;
            }

            long idNumerico;
            try {
                idNumerico = Long.parseLong(id);
            } catch (NumberFormatException e) {
                continue;
            }

            LocalDate data;
            try {
                data = LocalDate.parse(texto(item, "releaseDate").substring(0, 10));
            } catch (Exception e) {
                data = LocalDate.now();
            }

            String imagem = texto(item, "artworkUrl100");
            if (imagem != null) {
                imagem = imagem.replace("100x100bb", "600x600bb");
            }

            lista.add(new Lancamento(idNumerico, titulo, artista, imagem,
                    texto(item, "url"), data));
        }

        return lista;
    }

    private static String texto(JsonObject item, String chave) {
        JsonElement e = item.get(chave);
        if (e == null || e.isJsonNull()) {
            return null;
        }
        return e.getAsString();
    }
}