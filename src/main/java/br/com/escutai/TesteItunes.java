
package br.com.escutai;

import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.catalogo.service.ItunesService;

import javax.swing.*;
import java.util.ArrayList;

public class TesteItunes {

    public static void main(String[] args) {

        ItunesService servico = new ItunesService();

        try {

            ArrayList<Musica> musicas = servico.buscarMusicas("Lady Gaga");

            if (musicas.isEmpty()) {
                System.out.println("Nenhuma música encontrada.");
                return;
            }

            Musica musica = musicas.get(0);

            System.out.println("Música: " + musica.getTitulo());
            System.out.println("Artista: " + musica.getArtista());
            System.out.println("Capa: " + musica.getImagemUrl());

            SwingUtilities.invokeLater(() -> {

                JFrame janela = new JFrame("Teste de capa - ESCUTAÍ");

                try {
                    java.awt.image.BufferedImage capa = javax.imageio.ImageIO.read(
                            new java.net.URL(musica.getImagemUrl()));

                    JLabel imagem = new JLabel(new ImageIcon(capa));
                    janela.add(imagem);

                } catch (Exception erro) {
                    erro.printStackTrace();
                }

                janela.setSize(350, 380);
                janela.setLocationRelativeTo(null);
                janela.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                janela.setVisible(true);

            });

        } catch (Exception e) {

            System.out.println("Erro ao consultar o iTunes:");
            e.printStackTrace();

        }
    }
}
