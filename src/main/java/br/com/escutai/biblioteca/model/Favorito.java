package br.com.escutai.biblioteca.model;

import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.usuario.model.Usuario;

public class Favorito {

    private Usuario usuario;
    private ConteudoMusical conteudo;

    public Favorito(Usuario usuario, ConteudoMusical conteudo) 
    {
        this.usuario = usuario;
        this.conteudo = conteudo;
    }

    public Usuario getUsuario() 
    {
        return usuario;
    }

    public ConteudoMusical getConteudo() 
    {
        return conteudo;
    }

}
