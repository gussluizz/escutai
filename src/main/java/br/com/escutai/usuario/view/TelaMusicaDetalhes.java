
package br.com.escutai.view;

import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.shared.exception.NotaInvalidaException;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.net.URL;
import java.util.List;

public class TelaMusicaDetalhes extends JPanel {

    private Musica musica;
    private Usuario usuario;
    private AvaliacaoService avaliacaoService;
    private Runnable acaoVoltar;

    private BotaoEstrela[] estrelas;
    private int notaSelecionada;

    private JTextArea campoComentario;
    private JLabel textoNota;
    private JLabel textoMedia;
    private JPanel painelListaAvaliacoes;

    public TelaMusicaDetalhes(
            Musica musica,
            Usuario usuario,
            AvaliacaoService avaliacaoService,
            Runnable acaoVoltar
    ) {

        this.musica = musica;
        this.usuario = usuario;
        this.avaliacaoService = avaliacaoService;
        this.acaoVoltar = acaoVoltar;

        setLayout(new BorderLayout());

        JPanel painelConteudo = new JPanel();

        painelConteudo.setLayout(
                new BoxLayout(painelConteudo, BoxLayout.Y_AXIS)
        );

        painelConteudo.setBorder(
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        );

        painelConteudo.add(criarPainelMusica());
        painelConteudo.add(Box.createVerticalStrut(15));
        painelConteudo.add(new JSeparator());
        painelConteudo.add(Box.createVerticalStrut(15));
        painelConteudo.add(criarPainelAvaliacao());
        painelConteudo.add(Box.createVerticalStrut(20));
        painelConteudo.add(new JSeparator());
        painelConteudo.add(Box.createVerticalStrut(15));
        painelConteudo.add(criarPainelAvaliacoes());

        JScrollPane rolagem = new JScrollPane(painelConteudo);

        rolagem.setBorder(null);
        rolagem.getVerticalScrollBar().setUnitIncrement(16);

        add(rolagem, BorderLayout.CENTER);

        atualizarAvaliacoes();
    }

    private JPanel criarPainelMusica() {

        JPanel painel = new JPanel(new BorderLayout(20, 5));

        painel.setPreferredSize(new Dimension(620, 175));
        painel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175));
        painel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel capa = new JLabel();
        capa.setPreferredSize(new Dimension(150, 150));
        capa.setHorizontalAlignment(SwingConstants.CENTER);

        try {

            if (musica.getImagemUrl() != null) {

                ImageIcon imagem = new ImageIcon(
                        new URL(musica.getImagemUrl())
                );

                Image imagemRedimensionada = imagem.getImage()
                        .getScaledInstance(
                                140, 140, Image.SCALE_SMOOTH
                        );

                capa.setIcon(new ImageIcon(imagemRedimensionada));
            }

        } catch (Exception erro) {

            capa.setText("Sem capa");
        }

        JPanel informacoes = new JPanel();

        informacoes.setLayout(
                new BoxLayout(informacoes, BoxLayout.Y_AXIS)
        );

        JLabel titulo = new JLabel(musica.getTitulo());
        titulo.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel artista = new JLabel(musica.getArtista());

        artista.setFont(new Font("Arial", Font.PLAIN, 16));

        long minutos = musica.getDuracao() / 60000;
        long segundos = (musica.getDuracao() % 60000) / 1000;

        JLabel duracao = new JLabel(
                "Duração: " + String.format("%d:%02d", minutos, segundos)
        );

        JButton botaoAppleMusic = new JButton("Ouvir no Apple Music");
        JButton botaoVoltar = new JButton("Voltar à pesquisa");

        botaoAppleMusic.addActionListener(e -> abrirAppleMusic());
        botaoVoltar.addActionListener(e -> acaoVoltar.run());

        JPanel botoes = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 0)
        );

        botoes.add(botaoAppleMusic);
        botoes.add(botaoVoltar);

        informacoes.add(titulo);
        informacoes.add(Box.createVerticalStrut(8));
        informacoes.add(artista);
        informacoes.add(Box.createVerticalStrut(8));
        informacoes.add(duracao);
        informacoes.add(Box.createVerticalStrut(15));
        informacoes.add(botoes);

        painel.add(capa, BorderLayout.WEST);
        painel.add(informacoes, BorderLayout.CENTER);

        return painel;
    }

    private JPanel criarPainelAvaliacao() {

        JPanel painel = new JPanel();

        painel.setLayout(
                new BoxLayout(painel, BoxLayout.Y_AXIS)
        );

        painel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titulo = new JLabel("Avalie esta música");

        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel painelEstrelas = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 3, 5)
        );

        estrelas = new BotaoEstrela[5];

        for (int i = 0; i < 5; i++) {

            final int nota = i + 1;

            estrelas[i] = new BotaoEstrela();

            estrelas[i].addActionListener(
                    e -> selecionarNota(nota)
            );

            painelEstrelas.add(estrelas[i]);
        }

        textoNota = new JLabel("Selecione uma nota de 1 a 5.");

        JLabel textoComentario = new JLabel("Sua crítica:");

        campoComentario = new JTextArea(4, 40);

        campoComentario.setLineWrap(true);
        campoComentario.setWrapStyleWord(true);
        campoComentario.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane rolagemComentario = new JScrollPane(
                campoComentario
        );

        rolagemComentario.setPreferredSize(
                new Dimension(550, 90)
        );

        rolagemComentario.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 90)
        );

        JButton botaoEnviar = new JButton("Publicar avaliação");

        botaoEnviar.addActionListener(
                e -> publicarAvaliacao()
        );

        painel.add(titulo);
        painel.add(Box.createVerticalStrut(8));
        painel.add(painelEstrelas);
        painel.add(textoNota);
        painel.add(Box.createVerticalStrut(12));
        painel.add(textoComentario);
        painel.add(Box.createVerticalStrut(5));
        painel.add(rolagemComentario);
        painel.add(Box.createVerticalStrut(10));
        painel.add(botaoEnviar);

        return painel;
    }

    private JPanel criarPainelAvaliacoes() {

        JPanel painel = new JPanel(new BorderLayout(5, 8));

        painel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titulo = new JLabel("Avaliações dos usuários");

        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        textoMedia = new JLabel("Média: sem avaliações");

        textoMedia.setFont(new Font("Arial", Font.BOLD, 15));

        JPanel painelTopo = new JPanel();

        painelTopo.setLayout(
                new BoxLayout(painelTopo, BoxLayout.Y_AXIS)
        );

        painelTopo.add(titulo);
        painelTopo.add(Box.createVerticalStrut(5));
        painelTopo.add(textoMedia);

        painelListaAvaliacoes = new JPanel();

        painelListaAvaliacoes.setLayout(
                new BoxLayout(painelListaAvaliacoes, BoxLayout.Y_AXIS)
        );

        painel.add(painelTopo, BorderLayout.NORTH);
        painel.add(painelListaAvaliacoes, BorderLayout.CENTER);

        return painel;
    }

    private void selecionarNota(int nota) {

        notaSelecionada = nota;

        for (int i = 0; i < 5; i++) {
            estrelas[i].setSelecionada(i < nota);
        }

        textoNota.setText(
                "Sua nota: " + nota + " de 5 estrelas."
        );
    }

    private void publicarAvaliacao() {

        if (notaSelecionada == 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Selecione uma nota de 1 a 5 estrelas."
            );

            return;
        }

        String comentario = campoComentario.getText().trim();

        try {

            avaliacaoService.publicarAvaliacao(
                    usuario,
                    musica,
                    notaSelecionada,
                    comentario
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Sua avaliação foi publicada!"
            );

            notaSelecionada = 0;
            campoComentario.setText("");

            for (int i = 0; i < 5; i++) {
                estrelas[i].setSelecionada(false);
            }

            textoNota.setText("Selecione uma nota de 1 a 5.");

            atualizarAvaliacoes();

        } catch (NotaInvalidaException | RuntimeException erro) {

            JOptionPane.showMessageDialog(
                    this,
                    erro.getMessage(),
                    "Erro ao avaliar",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void atualizarAvaliacoes() {

        List<Avaliacao> avaliacoes =
                avaliacaoService.listarAvaliacoes(musica);

        double media =
                avaliacaoService.calcularMediaNotas(musica);

        painelListaAvaliacoes.removeAll();

        if (avaliacoes.isEmpty()) {

            textoMedia.setText("Média: sem avaliações");

            painelListaAvaliacoes.add(
                    new JLabel("Esta música ainda não possui avaliações.")
            );

        } else {

            textoMedia.setText(
                    String.format(
                            "Média: %.1f / 5 (%d avaliações)",
                            media,
                            avaliacoes.size()
                    )
            );

            for (Avaliacao avaliacao : avaliacoes) {

                painelListaAvaliacoes.add(
                        criarPainelUmaAvaliacao(avaliacao)
                );

                painelListaAvaliacoes.add(
                        Box.createVerticalStrut(10)
                );
            }
        }

        painelListaAvaliacoes.revalidate();
        painelListaAvaliacoes.repaint();
    }

    private JPanel criarPainelUmaAvaliacao(Avaliacao avaliacao) {

        JPanel painel = new JPanel(new BorderLayout(5, 5));

        painel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                )
        );

        JLabel nomeUsuario = new JLabel(
                avaliacao.getUsuario().getUsername()
        );

        nomeUsuario.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        JLabel nota = new JLabel(
                avaliacao.getNota() + "/5 estrelas"
        );

        JLabel data = new JLabel(
                "Data: " + avaliacao.getData()
        );

        JTextArea comentario = new JTextArea(
                avaliacao.getComentario()
        );

        comentario.setEditable(false);
        comentario.setLineWrap(true);
        comentario.setWrapStyleWord(true);
        comentario.setOpaque(false);

        JPanel informacoes = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 0)
        );

        informacoes.add(nomeUsuario);
        informacoes.add(nota);
        informacoes.add(data);

        painel.add(informacoes, BorderLayout.NORTH);
        painel.add(comentario, BorderLayout.CENTER);

        boolean avaliacaoDoUsuario =
                avaliacao.getUsuario().getUsername()
                        .equals(usuario.getUsername());

        if (avaliacaoDoUsuario) {

            JButton botaoEditar = new JButton("Editar");
            JButton botaoExcluir = new JButton("Excluir");

            botaoEditar.addActionListener(
                    e -> editarAvaliacao(avaliacao)
            );

            botaoExcluir.addActionListener(
                    e -> excluirAvaliacao(avaliacao)
            );

            JPanel botoes = new JPanel(
                    new FlowLayout(FlowLayout.RIGHT)
            );

            botoes.add(botaoEditar);
            botoes.add(botaoExcluir);

            painel.add(botoes, BorderLayout.SOUTH);
        }

        painel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 150)
        );

        return painel;
    }

    private void editarAvaliacao(Avaliacao avaliacao) {

        JPanel painelEdicao = new JPanel();

        painelEdicao.setLayout(
                new BoxLayout(painelEdicao, BoxLayout.Y_AXIS)
        );

        painelEdicao.setPreferredSize(new Dimension(320, 200));

        JLabel textoNotaEdicao = new JLabel("Escolha sua nova nota:");

        JPanel painelEstrelasEdicao = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 3, 5)
        );

        BotaoEstrela[] estrelasEdicao = new BotaoEstrela[5];

        int[] novaNota = {avaliacao.getNota()};

        for (int i = 0; i < 5; i++) {

            final int nota = i + 1;

            estrelasEdicao[i] = new BotaoEstrela();

            estrelasEdicao[i].setSelecionada(
                    i < avaliacao.getNota()
            );

            estrelasEdicao[i].addActionListener(e -> {

                novaNota[0] = nota;

                for (int j = 0; j < 5; j++) {

                    estrelasEdicao[j].setSelecionada(j < nota);
                }
            });

            painelEstrelasEdicao.add(estrelasEdicao[i]);
        }

        JLabel textoComentarioEdicao = new JLabel("Novo comentário:");

        JTextArea campoNovoComentario = new JTextArea(
                avaliacao.getComentario(), 5, 25
        );

        campoNovoComentario.setLineWrap(true);
        campoNovoComentario.setWrapStyleWord(true);

        JScrollPane rolagem = new JScrollPane(campoNovoComentario);

        painelEdicao.add(textoNotaEdicao);
        painelEdicao.add(painelEstrelasEdicao);
        painelEdicao.add(Box.createVerticalStrut(10));
        painelEdicao.add(textoComentarioEdicao);
        painelEdicao.add(rolagem);

        int opcao = JOptionPane.showConfirmDialog(
                this,
                painelEdicao,
                "Editar avaliação",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (opcao != JOptionPane.OK_OPTION) {
            return;
        }

        String novoComentario =
                campoNovoComentario.getText().trim();

        try {

            avaliacaoService.editarAvaliacao(
                    usuario,
                    avaliacao,
                    novaNota[0],
                    novoComentario
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Avaliação atualizada!"
            );

            atualizarAvaliacoes();

        } catch (NotaInvalidaException | RuntimeException erro) {

            JOptionPane.showMessageDialog(
                    this,
                    erro.getMessage(),
                    "Erro ao editar",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void excluirAvaliacao(Avaliacao avaliacao) {

        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja excluir sua avaliação?",
                "Excluir avaliação",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            boolean excluida =
                    avaliacaoService.excluirAvaliacao(
                            usuario,
                            avaliacao
                    );

            if (excluida) {

                JOptionPane.showMessageDialog(
                        this,
                        "Avaliação excluída!"
                );

                atualizarAvaliacoes();

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Não foi possível excluir a avaliação."
                );
            }

        } catch (RuntimeException erro) {

            JOptionPane.showMessageDialog(
                    this,
                    erro.getMessage(),
                    "Erro ao excluir",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void abrirAppleMusic() {

        try {

            if (musica.getLinkAppleMusic() == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "Link do Apple Music indisponível."
                );

                return;
            }

            Desktop.getDesktop().browse(
                    new URL(musica.getLinkAppleMusic()).toURI()
            );

        } catch (Exception erro) {

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível abrir o Apple Music."
            );
        }
    }

    private class BotaoEstrela extends JButton {

        private boolean selecionada;

        public BotaoEstrela() {

            setPreferredSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));

            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);

            setCursor(
                    new Cursor(Cursor.HAND_CURSOR)
            );
        }

        public void setSelecionada(boolean selecionada) {

            this.selecionada = selecionada;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;

            double raioExterno = 17;
            double raioInterno = 7.5;

            Path2D estrela = new Path2D.Double();

            for (int i = 0; i < 10; i++) {

                double angulo = Math.toRadians(-90 + i * 36);

                double raio;

                if (i % 2 == 0) {
                    raio = raioExterno;
                } else {
                    raio = raioInterno;
                }

                double x = centroX + Math.cos(angulo) * raio;
                double y = centroY + Math.sin(angulo) * raio;

                if (i == 0) {
                    estrela.moveTo(x, y);
                } else {
                    estrela.lineTo(x, y);
                }
            }

            estrela.closePath();

            if (selecionada) {
                g2.setColor(new Color(245, 180, 0));
            } else {
                g2.setColor(Color.LIGHT_GRAY);
            }

            g2.fill(estrela);

            g2.setColor(new Color(150, 120, 40));
            g2.draw(estrela);

            g2.dispose();
        }
    }
}
