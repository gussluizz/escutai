package br.com.escutai.biblioteca;

import br.com.escutai.biblioteca.repository.FavoritoRepository;
import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.biblioteca.view.FavoritoView;
import br.com.escutai.catalogo.model.Album;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.usuario.model.Usuario;

public class TesteFavoritos {
    public static void main(String[] args) {
        FavoritoRepository repository = new FavoritoRepository();
        FavoritoService service = new FavoritoService(repository);
        FavoritoView view = new FavoritoView(service);

        Usuario usuario = new Usuario("Ana Silva", "anasilva", "ana@email.com", "123456");
        Musica musica = new Musica(1L, "Tempo Perdido", "Legião Urbana", "img.png", "sp", "am", 302L);
        Album album = new Album(10L, "Dois", "Legião Urbana", "album.png", "sp", "am");

        // 1. Adicionar música e álbum
        view.adicionar(usuario, musica);
        view.adicionar(usuario, album);

        // 2. Tentar adicionar conteúdo repetido (deve bloquear e exibir aviso)
        view.adicionar(usuario, musica);

        // 3. Visualizar lista de favoritos
        view.exibirFavoritos(usuario);

        // 4. Remover música e visualizar novamente
        view.remover(usuario, musica);
        view.exibirFavoritos(usuario);
    }
}