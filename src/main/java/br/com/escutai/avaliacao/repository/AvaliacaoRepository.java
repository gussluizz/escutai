package br.com.escutai.avaliacao.repository;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.persistence.RepositorioJson;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

public class AvaliacaoRepository {

    private List<Avaliacao> avaliacoes;
    private RepositorioJson repositorioJson;

    public AvaliacaoRepository() {
        this(new RepositorioJson(), new ArrayList<Usuario>());
    }

    public AvaliacaoRepository(RepositorioJson repositorioJson, UsuarioRepository usuarioRepository) {
        this(repositorioJson, usuarioRepository.listarTodos());
    }

    private AvaliacaoRepository(RepositorioJson repositorioJson, List<Usuario> usuarios) {
        this.repositorioJson = repositorioJson;
        this.avaliacoes = repositorioJson.carregarAvaliacoes(usuarios);
    }

    public void salvar(Avaliacao avaliacao) {
        avaliacoes.add(avaliacao);
        salvarDados();
    }

    public boolean remover(Avaliacao avaliacao) {
        if (avaliacao == null) {
            return false;
        }
        boolean removida = avaliacoes.remove(avaliacao);
        if (removida) {
            salvarDados();
        }
        return removida;
    }

    public void atualizarDados() {
        salvarDados();
    }

    private void salvarDados() {
        repositorioJson.salvarAvaliacoes(avaliacoes);
    }

    public List<Avaliacao> listarTodas() {
        return new ArrayList<>(avaliacoes);
    }

    public boolean contem(Avaliacao avaliacao) {
        return avaliacoes.contains(avaliacao);
    }

    public Avaliacao buscarPorUsuarioEConteudo(Usuario usuario, ConteudoMusical conteudo) {
        if (usuario == null || conteudo == null) {
            return null;
        }
        for (Avaliacao a : avaliacoes) {
            if (a.getUsuario().getUsername().equals(usuario.getUsername())
                    && a.getConteudo().getId() == conteudo.getId()) {
                return a;
            }
        }
        return null;
    }

    public List<Avaliacao> listarPorConteudo(ConteudoMusical conteudo) {
        List<Avaliacao> resultado = new ArrayList<>();
        if (conteudo == null) {
            return resultado;
        }
        for (Avaliacao a : avaliacoes) {
            if (a.getConteudo().getId() == conteudo.getId()) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    public List<Avaliacao> listarPorUsuario(Usuario usuario) {
        List<Avaliacao> resultado = new ArrayList<>();
        if (usuario == null) {
            return resultado;
        }
        for (Avaliacao a : avaliacoes) {
            if (a.getUsuario().getUsername().equals(usuario.getUsername())) {
                resultado.add(a);
            }
        }
        return resultado;
    }
}
