
package br.com.escutai.usuario.model;

public class Usuario {

    private String nome;
    private String email;
    private String senha;
    private String username;

    public Usuario(String nome, String username, String email, String senha) {
        this.nome = nome;
        this.username = username;
        this.email = email;
        this.senha = senha;
    }

    public String getNome() {
        return nome;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public boolean verificarSenha(String senhaInformada) {
        if (senhaInformada == null) {
            return false;
        }

        return this.senha.equals(senhaInformada);
    }
}
