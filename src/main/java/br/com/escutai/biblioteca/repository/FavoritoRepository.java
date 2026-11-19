package br.com.escutai.biblioteca.repository;

import br.com.escutai.biblioteca.model.Favorito;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.usuario.model.Usuario;
import java.util.ArrayList;
import java.util.List;

public class FavoritoRepository{
    private List<Favorito> favoritos;

    public FavoritoRepository() {
        this.favoritos = new ArrayList<>();
    }
    public void salvar(Favorito favorito){
        favoritos.add(favorito);
    }
    public boolean remover(Usuario usuario, ConteudoMusical conteudoMusical){
        if(usuario == null || conteudoMusical == null){
            return false;
        }
        Favorito alvo = new Favorito(usuario, conteudoMusical);
        return favoritos.remove(alvo);
    }
    public boolean existe(Usuario usuario, ConteudoMusical conteudoMusical){
        if(usuario == null || conteudoMusical == null){
            return false;
        }
        Favorito alvo = new Favorito(usuario, conteudoMusical);
        return favoritos.contains(alvo);
    }

    public List<ConteudoMusical> listarFavoritosPorUsuario(Usuario usuario){
        List<ConteudoMusical> listaFavoritos = new ArrayList<>();
        if(usuario == null){
            return listaFavoritos;
        }
        for(Favorito f : favoritos){
            if(f.getUsuario().getUsername().equals(usuario.getUsername())){
                listaFavoritos.add(f.getConteudo());
            }
        }
        return listaFavoritos;
    }
}