package br.com.escutai.biblioteca.model;

import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.usuario.model.Usuario;

import java.time.LocalDateTime;
import java.util.Objects;

public class Favorito {

    private Usuario usuario;
    private ConteudoMusical conteudo;
    private LocalDateTime dataFavorito;

    public Favorito(Usuario usuario, ConteudoMusical conteudo) {
        this(usuario, conteudo, LocalDateTime.now());
    }

    public Favorito(Usuario usuario, ConteudoMusical conteudo, LocalDateTime dataFavorito) {
        if (usuario == null || conteudo == null) {
            throw new IllegalArgumentException("Usuário e conteúdo não podem ser nulos");
        }
        this.usuario = usuario;
        this.conteudo = conteudo;
        this.dataFavorito = (dataFavorito != null) ? dataFavorito : LocalDateTime.now();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public ConteudoMusical getConteudo() {
        return conteudo;
    }

    public LocalDateTime getDataFavorito() {
        return dataFavorito;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Favorito favorito = (Favorito) obj;
        String userA = usuario.getUsername() != null ? usuario.getUsername().toLowerCase() : "";
        String userB = favorito.usuario.getUsername() != null ? favorito.usuario.getUsername().toLowerCase() : "";
        return conteudo.getId() == favorito.conteudo.getId()
                && Objects.equals(userA, userB);
    }

    @Override
    public int hashCode() {
        String user = usuario.getUsername() != null ? usuario.getUsername().toLowerCase() : "";
        return Objects.hash(user, conteudo.getId());
    }
}