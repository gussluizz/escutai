package br.com.escutai.avaliacao.repository;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.usuario.model.Usuario;

import java.util.ArrayList;
import java.util.List;

public class AvaliacaoRepository {

    private List<Avaliacao> avaliacoes;

    public AvaliacaoRepository() {
        this.avaliacoes = new ArrayList<>();
    }

    public void salvar(Avaliacao avaliacao) {
        avaliacoes.add(avaliacao);
    }

    public boolean remover(Avaliacao avaliacao) {
        if (avaliacao == null) {
            return false;
        }
        return avaliacoes.remove(avaliacao);
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