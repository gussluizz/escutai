package br.com.escutai.avaliacao.view;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.exception.NotaInvalidaException;
import br.com.escutai.usuario.model.Usuario;

import java.util.List;

public class AvaliacaoView {

    private AvaliacaoService avaliacaoService;

    public AvaliacaoView(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    public Avaliacao publicar(Usuario usuario, ConteudoMusical conteudo, int nota, String comentario) {
        try {
            Avaliacao avaliacao = avaliacaoService.publicarAvaliacao(usuario, conteudo, nota, comentario);
            System.out.println("Avaliação publicada para \"" + conteudo.getTitulo() + "\" com nota " + nota + "★!");
            return avaliacao;
        } catch (NotaInvalidaException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("Aviso: " + e.getMessage());
            return null;
        }
    }

    public void editar(Usuario usuarioLogado, Avaliacao avaliacao, int novaNota, String novoComentario) {
        try {
            avaliacaoService.editarAvaliacao(usuarioLogado, avaliacao, novaNota, novoComentario);
            System.out.println("Avaliação de \"" + avaliacao.getConteudo().getTitulo() + "\" atualizada para " + novaNota + "★!");
        } catch (NotaInvalidaException | IllegalArgumentException | SecurityException e) {
            System.out.println("Aviso: " + e.getMessage());
        }
    }

    public void excluir(Usuario usuarioLogado, Avaliacao avaliacao) {
        try {
            boolean removida = avaliacaoService.excluirAvaliacao(usuarioLogado, avaliacao);
            if (removida) {
                System.out.println("Avaliação removida com sucesso!");
            } else {
                System.out.println("Avaliação não encontrada.");
            }
        } catch (SecurityException e) {
            System.out.println("Aviso: " + e.getMessage());
        }
    }

    public void exibirAvaliacoes(ConteudoMusical conteudo) {
        List<Avaliacao> avaliacoes = avaliacaoService.listarAvaliacoes(conteudo);
        double media = avaliacaoService.calcularMediaNotas(conteudo);

        System.out.printf("\n=== Avaliações de \"%s\" (Média: %.1f★) ===\n", conteudo.getTitulo(), media);

        if (avaliacoes.isEmpty()) {
            System.out.println("Nenhuma avaliação registrada ainda.");
            return;
        }

        for (Avaliacao a : avaliacoes) {
            System.out.println("@" + a.getUsuario().getUsername() + " (" + a.getData() + ") - "
                    + a.getNota() + "★: \"" + a.getComentario() + "\"");
        }
    }

    public void exibirAvaliacoesDeOutros(ConteudoMusical conteudo, Usuario usuarioLogado) {
        List<Avaliacao> deOutros = avaliacaoService.listarAvaliacoesDeOutrosUsuarios(conteudo, usuarioLogado);

        System.out.println("\n=== Avaliações de outros usuários para \"" + conteudo.getTitulo() + "\" ===");

        if (deOutros.isEmpty()) {
            System.out.println("Nenhum outro usuário avaliou este conteúdo ainda.");
            return;
        }

        for (Avaliacao a : deOutros) {
            System.out.println("@" + a.getUsuario().getUsername() + " (" + a.getData() + ") - "
                    + a.getNota() + "★: \"" + a.getComentario() + "\"");
        }
    }
}