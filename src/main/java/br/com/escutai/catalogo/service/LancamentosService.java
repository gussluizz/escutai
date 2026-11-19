package br.com.escutai.catalogo.service;

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
import java.util.Comparator;

/**
 * Busca os lançamentos para a tela inicial.
 *
 * Usa o feed público da Apple (rss.marketingtools.apple.com), que não precisa de
 * chave. Esse feed só oferece o ranking "mais tocados"; então pegamos o ranking
 * do Brasil, ficamos só com os álbuns lançados recentemente e ordenamos do mais
 * novo para o mais antigo. Se o Brasil falhar, tenta os EUA.
 *
 * Classe separada do ItunesService de propósito, para não interferir na busca.
 */
public class LancamentosService {

    private static final String[] PAISES = { "br", "us" };
    private static final int DIAS_RECENTES = 120;
    private static final int MINIMO_ITENS = 6;

    private final HttpClient cliente = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Um álbum lançado, com os dados que a interface precisa. */
    public static class Lancamento {
        private final long id;
        private final String titulo;
        private final String artista;
        private final String imagemUrl;
        private final String linkAppleMusic;
        private final LocalDate data;

        public Lancamento(long id, String titulo, String artista, String imagemUrl,
                String linkAppleMusic, LocalDate data) {
            this.id = id;
            this.titulo = titulo;
            this.artista = artista;
            this.imagemUrl = imagemUrl;
            this.linkAppleMusic = linkAppleMusic;
            this.data = data;
        }

        public long getId() {
            return id;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getArtista() {
            return artista;
        }

        public String getImagemUrl() {
            return imagemUrl;
        }

        public String getLinkAppleMusic() {
            return linkAppleMusic;
        }

        public LocalDate getData() {
            return data;
        }
    }

    public ArrayList<Lancamento> buscarLancamentos(int quantidade)
            throws IOException, InterruptedException {

        IOException ultimoErro = null;

        for (String pais : PAISES) {
            try {
                ArrayList<Lancamento> lista = consultar(pais, quantidade);
                if (!lista.isEmpty()) {
                    return lista;
                }
            } catch (IOException e) {
                ultimoErro = e;
            }
        }

        if (ultimoErro != null) {
            throw ultimoErro;
        }
        return new ArrayList<>();
    }

    private ArrayList<Lancamento> consultar(String pais, int quantidade)
            throws IOException, InterruptedException {

        String endereco = "https://rss.marketingtools.apple.com/api/v2/"
                + pais + "/music/most-played/100/albums.json";

        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create(endereco))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resposta = cliente.send(
                requisicao,
                HttpResponse.BodyHandlers.ofString());

        if (resposta.statusCode() != 200) {
            throw new IOException("Erro ao consultar os lançamentos (HTTP "
                    + resposta.statusCode() + ").");
        }

        JsonObject raiz = JsonParser.parseString(resposta.body()).getAsJsonObject();
        JsonArray resultados = raiz.getAsJsonObject("feed").getAsJsonArray("results");

        LocalDate hoje = LocalDate.now();
        ArrayList<Lancamento> todos = new ArrayList<>();

        for (int i = 0; i < resultados.size(); i++) {
            JsonObject item = resultados.get(i).getAsJsonObject();

            String id = texto(item, "id");
            String titulo = texto(item, "name");
            String artista = texto(item, "artistName");
            String data = texto(item, "releaseDate");

            if (id == null || titulo == null || artista == null || data == null) {
                continue;
            }

            LocalDate lancamento;
            long idNumerico;
            try {
                lancamento = LocalDate.parse(data.substring(0, 10));
                idNumerico = Long.parseLong(id);
            } catch (Exception e) {
                continue;
            }

            if (lancamento.isAfter(hoje)) {
                continue; // ainda não saiu
            }

            String imagem = texto(item, "artworkUrl100");
            if (imagem != null) {
                imagem = imagem.replace("100x100bb", "600x600bb");
            }

            todos.add(new Lancamento(idNumerico, titulo, artista, imagem,
                    texto(item, "url"), lancamento));
        }

        todos.sort(Comparator.comparing(Lancamento::getData).reversed());

        LocalDate limite = hoje.minusDays(DIAS_RECENTES);
        ArrayList<Lancamento> recentes = new ArrayList<>();
        for (Lancamento l : todos) {
            if (!l.getData().isBefore(limite)) {
                recentes.add(l);
            }
        }

        // se saiu pouca coisa recente, completa com os mais novos disponíveis
        if (recentes.size() < MINIMO_ITENS) {
            recentes = new ArrayList<>(todos.subList(0, Math.min(MINIMO_ITENS, todos.size())));
        }

        if (recentes.size() > quantidade) {
            recentes = new ArrayList<>(recentes.subList(0, quantidade));
        }
        return recentes;
    }

    private static String texto(JsonObject item, String chave) {
        JsonElement e = item.get(chave);
        if (e == null || e.isJsonNull()) {
            return null;
        }
        return e.getAsString();
    }
}