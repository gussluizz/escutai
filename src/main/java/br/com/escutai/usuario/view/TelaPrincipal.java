
package br.com.escutai.view;

import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.catalogo.service.ItunesService;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.service.UsuarioService;
import br.com.escutai.avaliacao.service.AvaliacaoService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class TelaPrincipal extends JFrame {

    private ItunesService itunesService;

    private JTextField campoBusca;
    private JPanel painelResultados;

    private Usuario usuario;
    private UsuarioService usuarioService;
    private AvaliacaoService avaliacaoService;

    private CardLayout cardLayout;
    private JPanel painelCards;

    public TelaPrincipal(
            Usuario usuario,
            UsuarioService usuarioService,
            AvaliacaoService avaliacaoService
    ) {

        this.usuario = usuario;
        this.usuarioService = usuarioService;
        this.avaliacaoService = avaliacaoService;

        itunesService = new ItunesService();

        setTitle("ESCUTAÍ");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        painelCards = new JPanel(cardLayout);

        painelCards.add(criarPainelPesquisa(), "pesquisa");

        add(painelCards);

        cardLayout.show(painelCards, "pesquisa");
    }

    private JPanel criarPainelPesquisa() {

        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));

        painelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JLabel titulo = new JLabel("ESCUTAÍ", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));

        campoBusca = new JTextField();

        JButton botaoBuscar = new JButton("Buscar");
        JButton botaoVoltar = new JButton("Voltar ao login");

        JPanel painelBusca = new JPanel(new BorderLayout(5, 5));

        painelBusca.add(campoBusca, BorderLayout.CENTER);
        painelBusca.add(botaoBuscar, BorderLayout.EAST);

        painelResultados = new JPanel();

        painelResultados.setLayout(
                new BoxLayout(painelResultados, BoxLayout.Y_AXIS)
        );

        JScrollPane rolagem = new JScrollPane(painelResultados);

        JPanel painelTopo = new JPanel(new BorderLayout(10, 10));

        painelTopo.add(titulo, BorderLayout.NORTH);
        painelTopo.add(painelBusca, BorderLayout.CENTER);
        painelTopo.add(botaoVoltar, BorderLayout.SOUTH);

        painelPrincipal.add(painelTopo, BorderLayout.NORTH);
        painelPrincipal.add(rolagem, BorderLayout.CENTER);

        botaoBuscar.addActionListener(e -> buscarMusicas());
        campoBusca.addActionListener(e -> buscarMusicas());

        botaoVoltar.addActionListener(e -> voltarLogin());

        return painelPrincipal;
    }

    private void buscarMusicas() {

        String termo = campoBusca.getText().trim();

        if (termo.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Digite o nome de uma música ou artista."
            );

            return;
        }

        painelResultados.removeAll();

        painelResultados.add(new JLabel("Buscando músicas..."));

        painelResultados.revalidate();
        painelResultados.repaint();

        try {

            ArrayList<Musica> musicas =
                    itunesService.buscarMusicas(termo);

            painelResultados.removeAll();

            if (musicas.isEmpty()) {

                painelResultados.add(
                        new JLabel("Nenhuma música encontrada.")
                );
            }

            for (Musica musica : musicas) {

                JPanel painelMusica = new JPanel(
                        new BorderLayout(15, 5)
                );

                painelMusica.setBorder(
                        BorderFactory.createEmptyBorder(10, 5, 10, 5)
                );

                JLabel capa = new JLabel();

                try {

                    if (musica.getImagemUrl() != null) {

                        ImageIcon imagem = new ImageIcon(
                                new URL(musica.getImagemUrl())
                        );

                        Image imagemRedimensionada = imagem.getImage()
                                .getScaledInstance(
                                        100, 100, Image.SCALE_SMOOTH
                                );

                        capa.setIcon(new ImageIcon(imagemRedimensionada));
                    }

                } catch (Exception erro) {

                    capa.setText("Sem capa");
                }

                JLabel nome = new JLabel(musica.getTitulo());

                nome.setFont(new Font("Arial", Font.BOLD, 16));

                JLabel artista = new JLabel(musica.getArtista());

                JButton botaoDetalhes = new JButton("Ver detalhes");

                botaoDetalhes.addActionListener(e -> abrirDetalhes(musica));

                JPanel informacoes = new JPanel(
                        new GridLayout(0, 1, 5, 5)
                );

                informacoes.add(nome);
                informacoes.add(artista);
                informacoes.add(botaoDetalhes);

                painelMusica.add(capa, BorderLayout.WEST);
                painelMusica.add(informacoes, BorderLayout.CENTER);

                painelResultados.add(painelMusica);

                painelResultados.add(
                        new JSeparator(SwingConstants.HORIZONTAL)
                );
            }

        } catch (IOException | InterruptedException erro) {

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível buscar as músicas."
            );

            if (erro instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        painelResultados.revalidate();
        painelResultados.repaint();
    }

    private void abrirDetalhes(Musica musica) {

        TelaMusicaDetalhes telaDetalhes = new TelaMusicaDetalhes(
                musica,
                usuario,
                avaliacaoService,
                () -> cardLayout.show(painelCards, "pesquisa")
        );

        painelCards.add(telaDetalhes, "detalhes");

        cardLayout.show(painelCards, "detalhes");
    }

    private void voltarLogin() {

        usuarioService.logout();

        TelaLogin telaLogin = new TelaLogin(usuarioService, avaliacaoService);

        telaLogin.setVisible(true);

        dispose();
    }
}
