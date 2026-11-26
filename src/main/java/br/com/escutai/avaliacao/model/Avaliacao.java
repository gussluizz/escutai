package br.com.escutai.avaliacao.model;

import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.exception.NotaInvalidaException;
import br.com.escutai.usuario.model.Usuario;

import java.time.LocalDate;

public class Avaliacao {

    private int nota;
    private String comentario;
    private LocalDate data;
    private Usuario usuario;
    private ConteudoMusical conteudo;

    public Avaliacao(int nota, String comentario, LocalDate data, Usuario usuario, ConteudoMusical conteudo) throws NotaInvalidaException {
        setNota(nota);
        this.comentario = comentario;
        this.data = (data != null) ? data : LocalDate.now();
        this.usuario = usuario;
        this.conteudo = conteudo;
    }

    public Avaliacao(Usuario usuario, ConteudoMusical conteudo, int nota, String comentario, LocalDate data) throws NotaInvalidaException {
        this(nota, comentario, data, usuario, conteudo);
    }

    public Avaliacao(Usuario usuario, ConteudoMusical conteudo, int nota, String comentario) throws NotaInvalidaException {
        this(nota, comentario, LocalDate.now(), usuario, conteudo);
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