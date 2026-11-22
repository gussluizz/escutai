package br.com.escutai.usuario.view;

import br.com.escutai.shared.exception.AutenticacaoException;
import br.com.escutai.shared.exception.UsuarioJaCadastradoException;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.service.UsuarioService;

import java.util.List;

public class UsuarioView {

    private UsuarioService usuarioService;

    public UsuarioView(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public Usuario cadastrar(String nome, String username, String email, String senha) {
        try {
            Usuario usuario = usuarioService.cadastrar(nome, username, email, senha);
            System.out.println("Usuário @" + usuario.getUsername() + " cadastrado com sucesso!");
            return usuario;
        } catch (IllegalArgumentException | UsuarioJaCadastradoException e) {
            System.out.println("Erro ao cadastrar: " + e.getMessage());
            return null;
        }
    }

    public Usuario login(String emailOuUsername, String senha) {
        try {
            Usuario usuario = usuarioService.login(emailOuUsername, senha);
            System.out.println("Login realizado! Bem-vindo(a), " + usuario.getNome() + " (@" + usuario.getUsername() + ").");
            return usuario;
        } catch (AutenticacaoException e) {
            System.out.println("Erro no login: " + e.getMessage());
            return null;
        }
    }

    public void logout() {
        if (!usuarioService.estaLogado()) {
            System.out.println("Nenhum usuário está logado no momento.");
            return;
        }
        String username = usuarioService.getUsuarioLogado().getUsername();
        usuarioService.logout();
        System.out.println("Logout realizado para @" + username + ".");
    }

    public void exibirUsuariosCadastrados() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        System.out.println("\n=== Usuários Cadastrados ===");

        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
            return;
        }

        for (Usuario u : usuarios) {
            System.out.println("- " + u.getNome() + " (@" + u.getUsername() + ") | " + u.getEmail());
        }
    }
}