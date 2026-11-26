
package br.com.escutai.catalogo.model;

public class Musica extends ConteudoMusical {

    private long duracao;

    public Musica(long id, String titulo, String artista, String imagemUrl, String linkSpotify, String linkAppleMusic, long duracao)
     {
        super(id, titulo, artista, imagemUrl, linkSpotify, linkAppleMusic);

        this.duracao = duracao;
    }

    public long getDuracao() {
        return duracao;
    }

}
