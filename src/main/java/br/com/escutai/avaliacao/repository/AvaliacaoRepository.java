package br.com.escutai.avaliacao.repository;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.persistence.RepositorioJson;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class AvaliacaoRepository {

    private final List<Avaliacao> avaliacoes;
    private final RepositorioJson repositorioJson;

    public AvaliacaoRepository() {
        this(new RepositorioJson());
    }

    public AvaliacaoRepository(RepositorioJson repositorioJson) {
        this.repositorioJson = repositorioJson;
        this.avaliacoes = repositorioJson.carregarAvaliacoes(repositorioJson.carregarUsuarios());
    }

    public AvaliacaoRepository(RepositorioJson repositorioJson, UsuarioRepository usuarioRepository) {
        this.repositorioJson = repositorioJson;
        List<Usuario> usuarios = (usuarioRepository != null)
                ? usuarioRepository.listarTodos()
                : repositorioJson.carregarUsuarios();
        this.avaliacoes = repositorioJson.carregarAvaliacoes(usuarios);
    }

    public synchronized void salvar(Avaliacao avaliacao) {
        if (avaliacao != null) {
            avaliacoes.add(avaliacao);
            salvarDados();
        }
    }

    public synchronized boolean remover(Avaliacao avaliacao) {
        if (avaliacao == null) {
            return false;
        }
        boolean removida = avaliacoes.removeIf(a ->
                a == avaliacao || (
                        a.getUsuario() != null
                                && avaliacao.getUsuario() != null
                                && a.getConteudo() != null
                                && avaliacao.getConteudo() != null
                                && a.getUsuario().getUsername().equalsIgnoreCase(avaliacao.getUsuario().getUsername())
                                && a.getConteudo().getId() == avaliacao.getConteudo().getId()
                )
        );
        if (removida) {
            salvarDados();
        }
        return removida;
    }

    public synchronized void atualizarDados() {
        salvarDados();
    }

    private void salvarDados() {
        repositorioJson.salvarAvaliacoes(avaliacoes);
    }

    public synchronized List<Avaliacao> listarTodas() {
        return new ArrayList<>(avaliacoes);
    }

    public synchronized boolean contem(Avaliacao avaliacao) {
        return avaliacoes.contains(avaliacao);
    }

    public synchronized Avaliacao buscarPorUsuarioEConteudo(Usuario usuario, ConteudoMusical conteudo) {
        if (usuario == null || conteudo == null) {
            return null;
        }
        for (Avaliacao a : avaliacoes) {
            if (a.getUsuario() != null
                    && a.getConteudo() != null
                    && a.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())
                    && a.getConteudo().getId() == conteudo.getId()) {
                return a;
            }
        }
        return null;
    }

    public synchronized List<Avaliacao> listarPorConteudo(ConteudoMusical conteudo) {
        List<Avaliacao> resultado = new ArrayList<>();
        if (conteudo == null) {
            return resultado;
        }
        for (Avaliacao a : avaliacoes) {
            if (a.getConteudo() != null && a.getConteudo().getId() == conteudo.getId()) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    public synchronized List<Avaliacao> listarPorUsuario(Usuario usuario) {
        List<Avaliacao> resultado = new ArrayList<>();
        if (usuario == null) {
            return resultado;
        }
        for (Avaliacao a : avaliacoes) {
            if (a.getUsuario() != null
                    && a.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())) {
                resultado.add(a);
            }
        }
        return resultado;
    }
}