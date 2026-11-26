
package br.com.escutai.catalogo.service;

import br.com.escutai.catalogo.model.Musica;

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
import java.util.ArrayList;

public class ItunesService {

        private HttpClient cliente;

        public ItunesService() {
                cliente = HttpClient.newHttpClient();
        }

        public ArrayList<Musica> buscarMusicas(String termo)
                        throws IOException, InterruptedException {

                String busca = URLEncoder.encode(termo, StandardCharsets.UTF_8);

                String endereco = "https://itunes.apple.com/search?term="
                                + busca + "&entity=song&limit=20";

                HttpRequest requisicao = HttpRequest.newBuilder()
                                .uri(URI.create(endereco))
                                .GET()
                                .build();

                HttpResponse<String> resposta = cliente.send(
                                requisicao,
                                HttpResponse.BodyHandlers.ofString());

                if (resposta.statusCode() != 200) {
                        throw new IOException("Erro ao consultar a API do iTunes.");
                }

                JsonObject resultado = JsonParser
                                .parseString(resposta.body())
                                .getAsJsonObject();

                JsonArray resultados = resultado.getAsJsonArray("results");

                ArrayList<Musica> musicas = new ArrayList<>();

                for (int i = 0; i < resultados.size(); i++) {

                        JsonObject item = resultados.get(i).getAsJsonObject();

                        long id = item.get("trackId").getAsLong();
                        String titulo = item.get("trackName").getAsString();
                        String artista = item.get("artistName").getAsString();

                        String imagemUrl = item.has("artworkUrl100")
                                        ? item.get("artworkUrl100").getAsString()
                                                        .replace("100x100bb", "600x600bb")
                                        : null;
                        String linkAppleMusic = item.has("trackViewUrl")
                                        ? item.get("trackViewUrl").getAsString()
                                        : null;

                        long duracao = item.has("trackTimeMillis")
                                        ? item.get("trackTimeMillis").getAsLong()
                                        : 0;

                        Musica musica = new Musica(
                                        id, titulo, artista, imagemUrl,
                                        null, linkAppleMusic, duracao);

                        musicas.add(musica);
                }

                return musicas;
        }
}
