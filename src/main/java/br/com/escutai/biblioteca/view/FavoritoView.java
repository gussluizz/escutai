package br.com.escutai.biblioteca.view;

import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.catalogo.model.Album;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.shared.exception.ConteudoJaFavoritadoException;
import br.com.escutai.usuario.model.Usuario;

import java.util.List;

public class FavoritoView {
    private FavoritoService favoritoService;

    public FavoritoView(FavoritoService favoritoService){
        this.favoritoService = favoritoService;
    }
    public void adicionar(Usuario usuario, ConteudoMusical conteudoMusical){
        try{
            favoritoService.adicionarNaLista(usuario, conteudoMusical);
            System.out.println("\"" +conteudoMusical.getTitulo() + "\" adicionado aos favoritos");
        } catch (ConteudoJaFavoritadoException e){
            System.out.println("Aviso: "+ e.getMessage());
        }
    }
    public void remover(Usuario usuario, ConteudoMusical conteudoMusical){
        boolean removido = favoritoService.removerDaLista(usuario, conteudoMusical);
        if(removido){
            System.out.println("\"" + conteudoMusical.getTitulo() + "\"removido dos favoritos");
        } else{
            System.out.println("\"" + conteudoMusical.getTitulo() + "\" não estva nos favoritos");
        }
    }

    public void exibirFavoritos(Usuario usuario){
        List<ConteudoMusical> favoritos = favoritoService.listarFavoritos(usuario);
        System.out.println("\n===Favoritos de " + usuario.getNome() + " (@" + usuario.getUsername() + ") ===");

        if(favoritos.isEmpty()){
            System.out.println("Nenhum conteúdo favorito encontrado.");
            return;
        }

        for(ConteudoMusical item : favoritos){
            if(item instanceof Musica m){
                System.out.println("[Música] " + m.getTitulo() + " - " + m.getArtista() + " (" + m.getDuracao() + "s)");
            } else if (item instanceof Album a) {
                System.out.println("[Álbum] " + a.getTitulo() + " - " + a.getArtista() + " (" + a.getMusicas().size() + " faixas)" );
            } else{
                System.out.println("[Item]   " + item.getTitulo() + " - " + item.getArtista());
            }
        }
    }
}
