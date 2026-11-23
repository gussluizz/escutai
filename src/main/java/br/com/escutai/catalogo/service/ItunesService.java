package br.com.escutai.catalogo.service;

import br.com.escutai.catalogo.model.Album;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.shared.exception.BuscaSemResultadosException;
import br.com.escutai.shared.exception.ErroComunicacaoApiException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;

/**
 * Serviço responsável por integrar o catálogo do ESCUTAÍ com a iTunes Search API.
 * Realiza buscas de músicas e álbuns, converte o JSON retornado em objetos do modelo
 * e trata falhas de rede ou buscas sem resultados através de exceções customizadas.
 */
public class ItunesService {

    // Cliente HTTP nativo do Java (java.net.http) encapsulado na classe
    private final HttpClient cliente;

    public ItunesService() {
        // Configura um tempo limite (timeout) de 10 segundos para não travar a interface
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Método mantido para compatibilidade com outras telas do sistema
     * que já utilizavam a assinatura original de busca de músicas.
     */
    public ArrayList<Musica> buscarMusicas(String termo)
            throws IOException, InterruptedException {
        try {
            return executarBuscaMusicas(termo);
        } catch (ErroComunicacaoApiException e) {
            throw new IOException(e.getMessage(), e);
        } catch (BuscaSemResultadosException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Busca músicas pelo nome na API do iTunes (entity=song).
     *
     * @param termo Nome da música ou artista pesquisado.
     * @return Lista de objetos Musica preenchidos com título, artista, capa, duração e links.
     * @throws BuscaSemResultadosException Se o termo estiver vazio ou nada for encontrado.
     * @throws ErroComunicacaoApiException Se houver falha de conexão ou erro na API.
     */
    public ArrayList<Musica> executarBuscaMusicas(String termo)
            throws BuscaSemResultadosException, ErroComunicacaoApiException {
        validarTermo(termo);

        // Consulta a API filtrando apenas por faixas musicais ("song")
        JsonArray resultados = consultarApi(termo, "song");
        ArrayList<Musica> musicas = new ArrayList<>();

        for (int i = 0; i < resultados.size(); i++) {
            JsonObject item = resultados.get(i).getAsJsonObject();

            // Ignora registros incompletos que não possuam ID ou nome da faixa
            if (!item.has("trackId") || !item.has("trackName")) {
                continue;
            }

            long id = item.get("trackId").getAsLong();
            String titulo = item.get("trackName").getAsString();
            String artista = item.has("artistName") ? item.get("artistName").getAsString() : "Artista desconhecido";

            // Aumenta a resolução da capa de 100x100 para 300x300 pixels para melhor exibição no Swing
            String imagemUrl = item.has("artworkUrl100")
                    ? item.get("artworkUrl100").getAsString().replace("100x100bb", "300x300bb")
                    : null;

            String linkAppleMusic = item.has("trackViewUrl")
                    ? item.get("trackViewUrl").getAsString()
                    : null;

            // Como a API do iTunes não fornece link direto do Spotify, geramos um link de busca direta
            String linkSpotify = gerarLinkBuscaSpotify(titulo, artista);

            // Duração da faixa em milissegundos (será formatada para mm:ss na interface)
            long duracao = item.has("trackTimeMillis")
                    ? item.get("trackTimeMillis").getAsLong()
                    : 0;

            musicas.add(new Musica(id, titulo, artista, imagemUrl, linkSpotify, linkAppleMusic, duracao));
        }

        // Lança exceção customizada caso a lista final esteja vazia
        if (musicas.isEmpty()) {
            throw new BuscaSemResultadosException("Nenhuma música encontrada para: \"" + termo + "\".");
        }

        return musicas;
    }

    /**
     * Busca álbuns pelo nome na API do iTunes (entity=album).
     *
     * @param termo Nome do álbum ou artista pesquisado.
     * @return Lista de objetos Album preenchidos.
     * @throws BuscaSemResultadosException Se o termo estiver vazio ou nenhum álbum for encontrado.
     * @throws ErroComunicacaoApiException Se houver falha de conexão ou erro na API.
     */
    public ArrayList<Album> executarBuscaAlbuns(String termo)
            throws BuscaSemResultadosException, ErroComunicacaoApiException {
        validarTermo(termo);

        // Consulta a API filtrando apenas por álbuns ("album")
        JsonArray resultados = consultarApi(termo, "album");
        ArrayList<Album> albuns = new ArrayList<>();

        for (int i = 0; i < resultados.size(); i++) {
            JsonObject item = resultados.get(i).getAsJsonObject();

            // Na API do iTunes, os campos de álbum usam o prefixo "collection"
            if (!item.has("collectionId") || !item.has("collectionName")) {
                continue;
            }

            long id = item.get("collectionId").getAsLong();
            String titulo = item.get("collectionName").getAsString();
            String artista = item.has("artistName") ? item.get("artistName").getAsString() : "Artista desconhecido";

            String imagemUrl = item.has("artworkUrl100")
                    ? item.get("artworkUrl100").getAsString().replace("100x100bb", "300x300bb")
                    : null;

            String linkAppleMusic = item.has("collectionViewUrl")
                    ? item.get("collectionViewUrl").getAsString()
                    : null;

            String linkSpotify = gerarLinkBuscaSpotify(titulo, artista);

            albuns.add(new Album(id, titulo, artista, imagemUrl, linkSpotify, linkAppleMusic));
        }

        if (albuns.isEmpty()) {
            throw new BuscaSemResultadosException("Nenhum álbum encontrado para: \"" + termo + "\".");
        }

        return albuns;
    }

    /**
     * Demonstra Polimorfismo: retorna uma lista única da superclasse ConteudoMusical
     * contendo tanto instâncias de Musica quanto de Album para a busca geral.
     */
    public ArrayList<ConteudoMusical> buscarTodos(String termo)
            throws BuscaSemResultadosException, ErroComunicacaoApiException {
        ArrayList<ConteudoMusical> todos = new ArrayList<>();
        BuscaSemResultadosException erroVazio = null;

        try {
            todos.addAll(executarBuscaMusicas(termo));
        } catch (BuscaSemResultadosException e) {
            erroVazio = e;
        }

        try {
            todos.addAll(executarBuscaAlbuns(termo));
        } catch (BuscaSemResultadosException e) {
            // Só repassa a exceção se tanto a busca de músicas quanto a de álbuns falharem em achar algo
            if (todos.isEmpty()) {
                throw (erroVazio != null) ? erroVazio : e;
            }
        }

        return todos;
    }

    /**
     * Valida se o usuário digitou algum texto antes de gastar uma requisição de rede.
     */
    private void validarTermo(String termo) throws BuscaSemResultadosException {
        if (termo == null || termo.trim().isEmpty()) {
            throw new BuscaSemResultadosException("Digite o nome de uma música ou álbum para buscar.");
        }
    }

    /**
     * Método privado auxiliar (encapsulamento) que monta a URL, envia a requisição HTTP GET
     * e converte o corpo da resposta usando a biblioteca Gson.
     */
    private JsonArray consultarApi(String termo, String entidade) throws ErroComunicacaoApiException {
        try {
            // Codifica espaços e acentos para o formato seguro de URL (UTF-8)
            String busca = URLEncoder.encode(termo.trim(), StandardCharsets.UTF_8);
            String endereco = "https://itunes.apple.com/search?term=" + busca
                    + "&entity=" + entidade + "&country=BR&limit=20";

            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(endereco))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());

            // Verifica se o servidor da Apple retornou sucesso (HTTP 200 OK)
            if (resposta.statusCode() != 200) {
                throw new ErroComunicacaoApiException(
                        "A API do iTunes respondeu com código HTTP " + resposta.statusCode() + ".");
            }

            JsonObject json = JsonParser.parseString(resposta.body()).getAsJsonObject();
            return json.getAsJsonArray("results");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ErroComunicacaoApiException(
                    "Falha de conexão ao consultar a API do iTunes. Verifique sua internet.", e);
        } catch (Exception e) {
            throw new ErroComunicacaoApiException("Erro inesperado ao processar os dados da API.", e);
        }
    }

    /**
     * Gera uma URL de pesquisa direta no Spotify para complementar o link do Apple Music.
     */
    private String gerarLinkBuscaSpotify(String titulo, String artista) {
        String query = URLEncoder.encode(titulo + " " + artista, StandardCharsets.UTF_8);
        return "https://open.spotify.com/search/" + query;
    }
}