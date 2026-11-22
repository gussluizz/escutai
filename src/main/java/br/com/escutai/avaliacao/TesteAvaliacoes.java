package br.com.escutai.avaliacao;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.repository.AvaliacaoRepository;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.avaliacao.view.AvaliacaoView;
import br.com.escutai.catalogo.model.Album;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.usuario.model.Usuario;

public class TesteAvaliacoes {
    public static void main(String[] args) {
        AvaliacaoRepository repository = new AvaliacaoRepository();
        AvaliacaoService service = new AvaliacaoService(repository);
        AvaliacaoView view = new AvaliacaoView(service);

        Usuario ana = new Usuario("Ana Silva", "anasilva", "ana@email.com", "123456");
        Usuario joao = new Usuario("João Souza", "joaosouza", "joao@email.com", "654321");

        Musica musica = new Musica(1L, "Tempo Perdido", "Legião Urbana", "img.png", "sp", "am", 302L);
        Album album = new Album(10L, "Dois", "Legião Urbana", "album.png", "sp", "am");

        // 1. Publicar avaliações (1 a 5 estrelas + comentário)
        Avaliacao avAna = view.publicar(ana, musica, 5, "Clássico absoluto!");
        Avaliacao avJoao = view.publicar(joao, musica, 4, "Muito boa, mas longa.");
        view.publicar(ana, album, 5, "Melhor álbum da banda.");

        // 2. Testar nota inválida (deve barrar e mostrar aviso)
        view.publicar(joao, album, 6, "Nota fora do limite");

        // 3. Exibir avaliações e média da música
        view.exibirAvaliacoes(musica);

        // 4. Visualizar apenas avaliações de outros usuários (visão da Ana)
        view.exibirAvaliacoesDeOutros(musica, ana);

        // 5. Editar própria avaliação e tentar editar a do colega
        view.editar(ana, avJoao, 1, "Tentando sabotar"); // Deve bloquear
        view.editar(ana, avAna, 3, "Mudei de ideia depois de ouvir de novo.");
        view.exibirAvaliacoes(musica);

        // 6. Excluir própria avaliação e checar nova média
        view.excluir(ana, avAna);
        view.exibirAvaliacoes(musica);
    }
}