package br.com.escutai.avaliacao.model;

import java.time.LocalDate;

import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.exception.NotaInvalidaException;
import br.com.escutai.usuario.model.Usuario;

public class Avaliacao {

    private int nota;
    private String comentario;
    private LocalDate data;
    private Usuario usuario;
    private ConteudoMusical conteudo;

    public Avaliacao(int nota, String comentario, LocalDate data, Usuario usuario, ConteudoMusical conteudo) throws NotaInvalidaException {
        setNota(nota);
        this.comentario = comentario;
        this.data = data;
        this.usuario = usuario;
        this.conteudo = conteudo;
    }

    public int getNota() {
        return nota;
    }

    public void setNota(int nota) throws NotaInvalidaException {
        if (nota < 1 || nota > 5) {
            throw new NotaInvalidaException("A nota deve ser entre 1 e 5.");
        }
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public ConteudoMusical getConteudo() {
        return conteudo;
    }
}