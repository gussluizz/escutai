package br.com.escutai.avaliacao.service;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.repository.AvaliacaoRepository;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.exception.NotaInvalidaException;
import br.com.escutai.usuario.model.Usuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AvaliacaoService {

    private AvaliacaoRepository repository;

    public AvaliacaoService(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    public Avaliacao publicarAvaliacao(Usuario usuario, ConteudoMusical conteudo, int nota, String comentario) throws NotaInvalidaException {
        if (usuario == null || conteudo == null) {
            throw new IllegalArgumentException("Usuário e conteúdo devem ser informados.");
        }
        if (repository.buscarPorUsuarioEConteudo(usuario, conteudo) != null) {
            throw new IllegalStateException("Você já avaliou este conteúdo. Edite sua avaliação existente.");
        }

        Avaliacao novaAvaliacao = new Avaliacao(nota, comentario, LocalDate.now(), usuario, conteudo);
        repository.salvar(novaAvaliacao);
        conteudo.adicionarAvaliacao(novaAvaliacao);
        return novaAvaliacao;
    }

    public List<Avaliacao> listarAvaliacoes(ConteudoMusical conteudo) {
        return repository.listarPorConteudo(conteudo);
    }

    public List<Avaliacao> listarAvaliacoesDeOutrosUsuarios(ConteudoMusical conteudo, Usuario usuarioLogado) {
        List<Avaliacao> todas = repository.listarPorConteudo(conteudo);
        List<Avaliacao> deOutros = new ArrayList<>();

        for (Avaliacao a : todas) {
            if (usuarioLogado == null || !a.getUsuario().getUsername().equals(usuarioLogado.getUsername())) {
                deOutros.add(a);
            }
        }
        return deOutros;
    }

    public void editarAvaliacao(Usuario usuarioLogado, Avaliacao avaliacao, int novaNota, String novoComentario) throws NotaInvalidaException {
        if (usuarioLogado == null || avaliacao == null) {
            throw new IllegalArgumentException("Usuário e avaliação devem ser informados.");
        }
        if (!avaliacao.getUsuario().getUsername().equals(usuarioLogado.getUsername())) {
            throw new SecurityException("Você só pode editar suas próprias avaliações.");
        }

        avaliacao.setNota(novaNota);
        avaliacao.setComentario(novoComentario);
        avaliacao.setData(LocalDate.now());
    }

    public boolean excluirAvaliacao(Usuario usuarioLogado, Avaliacao avaliacao) {
        if (usuarioLogado == null || avaliacao == null) {
            return false;
        }
        if (!avaliacao.getUsuario().getUsername().equals(usuarioLogado.getUsername())) {
            throw new SecurityException("Você só pode excluir suas próprias avaliações.");
        }

        avaliacao.getConteudo().getAvaliacoes().remove(avaliacao);
        return repository.remover(avaliacao);
    }

    public double calcularMediaNotas(ConteudoMusical conteudo) {
        List<Avaliacao> lista = repository.listarPorConteudo(conteudo);
        if (lista.isEmpty()) {
            return 0.0;
        }

        int soma = 0;
        for (Avaliacao a : lista) {
            soma += a.getNota();
        }
        return (double) soma / lista.size();
    }
}