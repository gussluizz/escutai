package br.com.escutai.biblioteca.service;

import br.com.escutai.biblioteca.model.Favorito;
import br.com.escutai.biblioteca.repository.FavoritoRepository;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.exception.ConteudoJaFavoritadoException;
import br.com.escutai.usuario.model.Usuario;

import java.util.List;

public class FavoritoService {

    private final FavoritoRepository repository;

    public FavoritoService() {
        this(new FavoritoRepository());
    }

    public FavoritoService(FavoritoRepository repository) {
        this.repository = repository;
    }

    public void adicionarNaLista(Usuario usuario, ConteudoMusical conteudoMusical) {
        if (usuario == null || conteudoMusical == null) {
            throw new IllegalArgumentException("Usuário e conteúdo devem ser informados");
        }
        if (repository.existe(usuario, conteudoMusical)) {
            throw new ConteudoJaFavoritadoException(
                    "O conteúdo '" + conteudoMusical.getTitulo() + "' já está na lista de favoritos do usuário '" + usuario.getUsername() + "'."
            );
        }
        Favorito novoFavorito = new Favorito(usuario, conteudoMusical);
        repository.salvar(novoFavorito);
    }

    public boolean removerDaLista(Usuario usuario, ConteudoMusical conteudo) {
        if (usuario == null || conteudo == null) {
            return false;
        }
        return repository.remover(usuario, conteudo);
    }

    public List<ConteudoMusical> listarFavoritos(Usuario usuario) {
        return repository.listarFavoritosPorUsuario(usuario);
    }

    public boolean isFavorito(Usuario usuario, ConteudoMusical conteudo) {
        return repository.existe(usuario, conteudo);
    }
}