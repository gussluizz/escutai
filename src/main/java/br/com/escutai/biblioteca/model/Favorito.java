package br.com.escutai.biblioteca.model;

import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.usuario.model.Usuario;
import java.time.LocalDateTime;
import java.util.Objects;

public class Favorito {

    private Usuario usuario;
    private ConteudoMusical conteudo;
    private LocalDateTime  dataFavorito;

    public Favorito(Usuario usuario, ConteudoMusical conteudo) {
        if (usuario == null || conteudo == null) {
            throw new IllegalArgumentException("Usuário e conteúdo não podem ser nulos");
        }
        this.usuario = usuario;
        this.conteudo = conteudo;
        this.dataFavorito = LocalDateTime.now();
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
        return conteudo.getId() == favorito.conteudo.getId()
                && Objects.equals(usuario.getUsername(), favorito.usuario.getUsername())
                && Objects.equals(conteudo.getClass(), favorito.conteudo.getClass());
    }
    @Override
    public int hashCode() {
        return Objects.hash(usuario.getUsername(), conteudo.getId(), conteudo.getClass());
    }

}