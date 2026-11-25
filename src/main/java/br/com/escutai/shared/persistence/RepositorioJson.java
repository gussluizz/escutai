package br.com.escutai.shared.persistence;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.biblioteca.model.Favorito;
import br.com.escutai.catalogo.model.Album;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.usuario.model.Usuario;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * Responsável por salvar e carregar os dados do ESCUTAÍ em arquivos JSON.
 * Os arquivos são criados na pasta "dados", na raiz do projeto.
 */
public class RepositorioJson {

    private static final Path PASTA_DADOS = Paths.get("dados");
    private static final Path ARQUIVO_USUARIOS = PASTA_DADOS.resolve("usuarios.json");
    private static final Path ARQUIVO_AVALIACOES = PASTA_DADOS.resolve("avaliacoes.json");
    private static final Path ARQUIVO_FAVORITOS = PASTA_DADOS.resolve("favoritos.json");

    private final Gson gson;

    public RepositorioJson() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public List<Usuario> carregarUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();

        if (!Files.exists(ARQUIVO_USUARIOS)) {
            return usuarios;
        }

        try {
            String json = Files.readString(ARQUIVO_USUARIOS, StandardCharsets.UTF_8);
            Type tipoLista = TypeToken.getParameterized(List.class, UsuarioDados.class).getType();
            List<UsuarioDados> dados = gson.fromJson(json, tipoLista);

            if (dados != null) {
                for (UsuarioDados dado : dados) {
                    usuarios.add(new Usuario(dado.nome, dado.username, dado.email, dado.senha));
                }
            }
        } catch (IOException erro) {
            throw new RuntimeException("Não foi possível carregar os usuários.", erro);
        }

        return usuarios;
    }

    public void salvarUsuarios(List<Usuario> usuarios) {
        List<UsuarioDados> dados = new ArrayList<>();

        for (Usuario usuario : usuarios) {
            UsuarioDados dado = new UsuarioDados();
            dado.nome = usuario.getNome();
            dado.username = usuario.getUsername();
            dado.email = usuario.getEmail();
            dado.senha = usuario.getSenha();
            dados.add(dado);
        }

        salvarArquivo(ARQUIVO_USUARIOS, gson.toJson(dados));
    }

    public List<Avaliacao> carregarAvaliacoes(List<Usuario> usuarios) {
        List<Avaliacao> avaliacoes = new ArrayList<>();

        if (!Files.exists(ARQUIVO_AVALIACOES)) {
            return avaliacoes;
        }

        try {
            String json = Files.readString(ARQUIVO_AVALIACOES, StandardCharsets.UTF_8);
            Type tipoLista = TypeToken.getParameterized(List.class, AvaliacaoDados.class).getType();
            List<AvaliacaoDados> dados = gson.fromJson(json, tipoLista);

            if (dados != null) {
                for (AvaliacaoDados dado : dados) {
                    if (dado == null || dado.username == null) {
                        continue;
                    }

                    Usuario usuario = buscarUsuario(usuarios, dado.username);
                    if (usuario == null) {
                        usuario = new Usuario(dado.username, dado.username, "", "");
                    }

                    ConteudoMusical conteudo = reconstruirConteudo(
                            dado.tipoConteudo,
                            dado.conteudoId,
                            dado.titulo,
                            dado.artista,
                            dado.imagemUrl,
                            dado.linkSpotify,
                            dado.linkAppleMusic,
                            dado.duracao
                    );

                    LocalDate data = (dado.data != null && !dado.data.isBlank())
                            ? LocalDate.parse(dado.data)
                            : LocalDate.now();

                    Avaliacao avaliacao = new Avaliacao(
                            dado.nota,
                            dado.comentario,
                            data,
                            usuario,
                            conteudo
                    );

                    conteudo.adicionarAvaliacao(avaliacao);
                    avaliacoes.add(avaliacao);
                }
            }
        } catch (IOException erro) {
            throw new RuntimeException("Não foi possível carregar as avaliações.", erro);
        } catch (Exception erro) {
            throw new RuntimeException("Os dados das avaliações estão inválidos.", erro);
        }

        return avaliacoes;
    }

    public void salvarAvaliacoes(List<Avaliacao> avaliacoes) {
        List<AvaliacaoDados> dados = new ArrayList<>();

        for (Avaliacao avaliacao : avaliacoes) {
            ConteudoMusical conteudo = avaliacao.getConteudo();
            AvaliacaoDados dado = new AvaliacaoDados();

            dado.username = avaliacao.getUsuario().getUsername();
            dado.conteudoId = conteudo.getId();
            dado.tipoConteudo = (conteudo instanceof Album) ? "ALBUM" : "MUSICA";
            dado.titulo = conteudo.getTitulo();
            dado.artista = conteudo.getArtista();
            dado.imagemUrl = conteudo.getImagemUrl();
            dado.linkSpotify = conteudo.getLinkSpotify();
            dado.linkAppleMusic = conteudo.getLinkAppleMusic();
            dado.duracao = conteudo instanceof Musica ? ((Musica) conteudo).getDuracao() : 0;
            dado.nota = avaliacao.getNota();
            dado.comentario = avaliacao.getComentario();
            dado.data = (avaliacao.getData() != null ? avaliacao.getData() : LocalDate.now()).toString();

            dados.add(dado);
        }

        salvarArquivo(ARQUIVO_AVALIACOES, gson.toJson(dados));
    }

    public List<Favorito> carregarFavoritos(List<Usuario> usuarios) {
        List<Favorito> favoritos = new ArrayList<>();

        if (!Files.exists(ARQUIVO_FAVORITOS)) {
            return favoritos;
        }

        try {
            String json = Files.readString(ARQUIVO_FAVORITOS, StandardCharsets.UTF_8);
            Type tipoLista = TypeToken.getParameterized(List.class, FavoritoDados.class).getType();
            List<FavoritoDados> dados = gson.fromJson(json, tipoLista);

            if (dados != null) {
                for (FavoritoDados dado : dados) {
                    if (dado == null || dado.username == null) {
                        continue;
                    }

                    Usuario usuario = buscarUsuario(usuarios, dado.username);
                    if (usuario == null) {
                        usuario = new Usuario(dado.username, dado.username, "", "");
                    }

                    ConteudoMusical conteudo = reconstruirConteudo(
                            dado.tipoConteudo,
                            dado.conteudoId,
                            dado.titulo,
                            dado.artista,
                            dado.imagemUrl,
                            dado.linkSpotify,
                            dado.linkAppleMusic,
                            dado.duracao
                    );

                    LocalDateTime dataFavorito = (dado.dataFavorito != null && !dado.dataFavorito.isBlank())
                            ? LocalDateTime.parse(dado.dataFavorito)
                            : LocalDateTime.now();

                    favoritos.add(new Favorito(usuario, conteudo, dataFavorito));
                }
            }
        } catch (IOException erro) {
            throw new RuntimeException("Não foi possível carregar os favoritos.", erro);
        } catch (Exception erro) {
            throw new RuntimeException("Os dados dos favoritos estão inválidos.", erro);
        }

        return favoritos;
    }

    public void salvarFavoritos(List<Favorito> favoritos) {
        List<FavoritoDados> dados = new ArrayList<>();

        for (Favorito favorito : favoritos) {
            ConteudoMusical conteudo = favorito.getConteudo();
            FavoritoDados dado = new FavoritoDados();

            dado.username = favorito.getUsuario().getUsername();
            dado.conteudoId = conteudo.getId();
            dado.tipoConteudo = (conteudo instanceof Album) ? "ALBUM" : "MUSICA";
            dado.titulo = conteudo.getTitulo();
            dado.artista = conteudo.getArtista();
            dado.imagemUrl = conteudo.getImagemUrl();
            dado.linkSpotify = conteudo.getLinkSpotify();
            dado.linkAppleMusic = conteudo.getLinkAppleMusic();
            dado.duracao = conteudo instanceof Musica ? ((Musica) conteudo).getDuracao() : 0;
            dado.dataFavorito = (favorito.getDataFavorito() != null
                    ? favorito.getDataFavorito()
                    : LocalDateTime.now()).toString();

            dados.add(dado);
        }

        salvarArquivo(ARQUIVO_FAVORITOS, gson.toJson(dados));
    }

    private ConteudoMusical reconstruirConteudo(String tipoConteudo, long id, String titulo, String artista,
                                                String imagemUrl, String linkSpotify, String linkAppleMusic, long duracao) {
        if ("ALBUM".equalsIgnoreCase(tipoConteudo)) {
            return new Album(id, titulo, artista, imagemUrl, linkSpotify, linkAppleMusic);
        }
        return new Musica(id, titulo, artista, imagemUrl, linkSpotify, linkAppleMusic, duracao);
    }

    private Usuario buscarUsuario(List<Usuario> usuarios, String username) {
        if (usuarios == null || username == null) {
            return null;
        }
        for (Usuario usuario : usuarios) {
            if (usuario.getUsername() != null && usuario.getUsername().equalsIgnoreCase(username.trim())) {
                return usuario;
            }
        }
        return null;
    }

    private void salvarArquivo(Path arquivo, String conteudo) {
        try {
            Files.createDirectories(PASTA_DADOS);
            Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new RuntimeException("Não foi possível salvar os dados em JSON.", erro);
        }
    }

    private static class UsuarioDados {
        String nome;
        String username;
        String email;
        String senha;
    }

    private static class AvaliacaoDados {
        String username;
        long conteudoId;
        String tipoConteudo;
        String titulo;
        String artista;
        String imagemUrl;
        String linkSpotify;
        String linkAppleMusic;
        long duracao;
        int nota;
        String comentario;
        String data;
    }

    private static class FavoritoDados {
        String username;
        long conteudoId;
        String tipoConteudo;
        String titulo;
        String artista;
        String imagemUrl;
        String linkSpotify;
        String linkAppleMusic;
        long duracao;
        String dataFavorito;
    }
}