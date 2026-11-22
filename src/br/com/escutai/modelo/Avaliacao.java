package br.com.escutai.modelo;

public class Avaliacao {
    private String id;
    private String usuarioEmail;
    private String conteudoTitulo;
    private String tipoConteudo;
    private int nota;
    private String comentario;

    public Avaliacao(String id, String usuarioEmail, String conteudoTitulo, String tipoConteudo, int nota, String comentario) {
        this.id = id;
        this.usuarioEmail = usuarioEmail;
        this.conteudoTitulo = conteudoTitulo;
        this.tipoConteudo = tipoConteudo;
        this.nota = nota;
        this.comentario = comentario;
    }

    public String getId() {
        return id;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public String getConteudoTitulo() {
        return conteudoTitulo;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }

    public int getNota() {
        return nota;
    }

    public String getComentario() {
        return comentario;
    }
}