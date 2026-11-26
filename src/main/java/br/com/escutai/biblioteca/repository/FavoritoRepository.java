package br.com.escutai.biblioteca.repository;

import br.com.escutai.biblioteca.model.Favorito;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.persistence.RepositorioJson;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class FavoritoRepository {

    private final List<Favorito> favoritos;
    private final RepositorioJson repositorioJson;

    public FavoritoRepository() {
        this(new RepositorioJson());
    }

    public FavoritoRepository(RepositorioJson repositorioJson) {
        this.repositorioJson = repositorioJson;
        this.favoritos = repositorioJson.carregarFavoritos(repositorioJson.carregarUsuarios());
    }

    public FavoritoRepository(RepositorioJson repositorioJson, UsuarioRepository usuarioRepository) {
        this.repositorioJson = repositorioJson;
        List<Usuario> usuarios = (usuarioRepository != null)
                ? usuarioRepository.listarTodos()
                : repositorioJson.carregarUsuarios();
        this.favoritos = repositorioJson.carregarFavoritos(usuarios);
    }

    public synchronized void salvar(Favorito favorito) {
        if (favorito == null) {
            return;
        }
        if (!existe(favorito.getUsuario(), favorito.getConteudo())) {
            favoritos.add(favorito);
            salvarDados();
        }
    }

    public synchronized boolean remover(Usuario usuario, ConteudoMusical conteudoMusical) {
        if (usuario == null || conteudoMusical == null) {
            return false;
        }
        boolean removido = favoritos.removeIf(f ->
                f.getUsuario() != null
                        && f.getConteudo() != null
                        && f.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())
                        && f.getConteudo().getId() == conteudoMusical.getId()
        );
        if (removido) {
            salvarDados();
        }
        return removido;
    }

    public synchronized boolean existe(Usuario usuario, ConteudoMusical conteudoMusical) {
        if (usuario == null || conteudoMusical == null) {
            return false;
        }
        for (Favorito f : favoritos) {
            if (f.getUsuario() != null
                    && f.getConteudo() != null
                    && f.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())
                    && f.getConteudo().getId() == conteudoMusical.getId()) {
                return true;
            }
        }
        return false;
    }

    public synchronized List<ConteudoMusical> listarFavoritosPorUsuario(Usuario usuario) {
        List<ConteudoMusical> listaFavoritos = new ArrayList<>();
        if (usuario == null) {
            return listaFavoritos;
        }
        for (Favorito f : favoritos) {
            if (f.getUsuario() != null
                    && f.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())) {
                listaFavoritos.add(f.getConteudo());
            }
        }
        return listaFavoritos;
    }

    private void salvarDados() {
        repositorioJson.salvarFavoritos(favoritos);
    }
}