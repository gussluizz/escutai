package br.com.escutai.catalogo.model;
import br.com.escutai.avaliacao.model.Avaliavel;
import java.util.ArrayList;
import br.com.escutai.avaliacao.model.Avaliacao;

public abstract class ConteudoMusical implements Avaliavel {

    private long id;
    private String titulo;
    private String artista;
    private String imagemUrl;
    private String linkSpotify;
    private String linkAppleMusic;
    private ArrayList<Avaliacao> avaliacoes;

    public ConteudoMusical(long id, String titulo, String artista, String imagemUrl, String linkSpotify, String linkAppleMusic) 
    {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.imagemUrl = imagemUrl;
        this.linkSpotify = linkSpotify;
        this.linkAppleMusic = linkAppleMusic;
        this.avaliacoes = new ArrayList<>();
    }

    @Override
    public void adicionarAvaliacao(Avaliacao avaliacao) 
        {
            avaliacoes.add(avaliacao);
        }

    public long getId() 
    {
        return id;
    }

    public String getTitulo() 
    {
        return titulo;
    }

    public String getArtista() 
    {
        return artista;
    }

    public String getImagemUrl() 
    {
        return imagemUrl;
    }

    public String getLinkSpotify() 
    {
        return linkSpotify;
    }

    public String getLinkAppleMusic() 
    {
        return linkAppleMusic;
    }

    public ArrayList<Avaliacao> getAvaliacoes() 
    {
        return avaliacoes;
    }

}