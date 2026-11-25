package br.com.escutai.usuario.repository;

import br.com.escutai.shared.persistence.RepositorioJson;
import br.com.escutai.usuario.model.Usuario;

import java.util.ArrayList;
import java.util.List;

public class UsuarioRepository {

    private List<Usuario> usuarios;
    private RepositorioJson repositorioJson;

    public UsuarioRepository() {
        this(new RepositorioJson());
    }

    public UsuarioRepository(RepositorioJson repositorioJson) {
        this.repositorioJson = repositorioJson;
        this.usuarios = repositorioJson.carregarUsuarios();
    }

    public void salvar(Usuario usuario) {
        usuarios.add(usuario);
        repositorioJson.salvarUsuarios(usuarios);
    }

    public boolean existePorEmail(String email) {
        if (email == null) {
            return false;
        }
        for (Usuario u : usuarios) {
            if (u.getEmail().equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean existePorUsername(String username) {
        if (username == null) {
            return false;
        }
        for (Usuario u : usuarios) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                return true;
            }
        }
        return false;
    }

    public Usuario buscarPorEmailOuUsername(String login) {
        if (login == null) {
            return null;
        }
        String loginLimpo = login.trim();
        for (Usuario u : usuarios) {
            if (u.getEmail().equalsIgnoreCase(loginLimpo)
                    || u.getUsername().equalsIgnoreCase(loginLimpo)) {
                return u;
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() {
        return new ArrayList<>(usuarios);
    }
}
