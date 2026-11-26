package br.com.escutai.shared.exception;

public class UsuarioJaCadastradoException extends RuntimeException {
    public UsuarioJaCadastradoException(String mensagem) {
        super(mensagem);
    }
}