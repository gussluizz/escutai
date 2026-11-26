package br.com.escutai.catalogo.model;

import java.util.ArrayList;

public class Album extends ConteudoMusical {

    private ArrayList<Musica> musicas;

    public Album(long id, String titulo, String artista, String imagemUrl, String linkSpotify, String linkAppleMusic) 
    {

        super(id, titulo, artista, imagemUrl, linkSpotify, linkAppleMusic);

        this.musicas = new ArrayList<>();

    }

    public void adicionarMusica(Musica musica) 
    {
        musicas.add(musica);
    }

    public ArrayList<Musica> getMusicas() 
    {
        return musicas;
    }

}