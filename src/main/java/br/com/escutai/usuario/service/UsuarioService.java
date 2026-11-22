package br.com.escutai.usuario.service;

import br.com.escutai.shared.exception.AutenticacaoException;
import br.com.escutai.shared.exception.UsuarioJaCadastradoException;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.repository.UsuarioRepository;

import java.util.List;

public class UsuarioService {

    private UsuarioRepository repository;
    private Usuario usuarioLogado;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
        this.usuarioLogado = null;
    }

    public Usuario cadastrar(String nome, String username, String email, String senha) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("O username é obrigatório.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        if (senha == null || senha.trim().isEmpty()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }

        if (repository.existePorEmail(email)) {
            throw new UsuarioJaCadastradoException("Já existe um usuário cadastrado com o e-mail '" + email + "'.");
        }
        if (repository.existePorUsername(username)) {
            throw new UsuarioJaCadastradoException("O username '" + username + "' já está em uso.");
        }

        Usuario novoUsuario = new Usuario(nome.trim(), username.trim(), email.trim(), senha);
        repository.salvar(novoUsuario);
        return novoUsuario;
    }

    public Usuario login(String emailOuUsername, String senha) {
        if (emailOuUsername == null || emailOuUsername.trim().isEmpty() || senha == null || senha.isEmpty()) {
            throw new AutenticacaoException("Informe o e-mail/username e a senha para entrar.");
        }

        Usuario usuario = repository.buscarPorEmailOuUsername(emailOuUsername);
        if (usuario == null || !usuario.verificarSenha(senha)) {
            throw new AutenticacaoException("E-mail/username ou senha inválidos.");
        }

        this.usuarioLogado = usuario;
        return usuario;
    }

    public void logout() {
        this.usuarioLogado = null;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public boolean estaLogado() {
        return usuarioLogado != null;
    }

    public List<Usuario> listarUsuarios() {
        return repository.listarTodos();
    }
}