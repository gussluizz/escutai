package br.com.escutai.view;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.shared.exception.NotaInvalidaException;
import br.com.escutai.usuario.model.Usuario;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TelaPerfil extends JPanel {

    private static final int TAMANHO_FOTO_PERFIL = 104;
    private static final int TAMANHO_CAPA_GRADE = 148;
    private static final int COLUNAS_POR_LINHA = 4;

    private final Usuario usuario;
    private final AvaliacaoService avaliacaoService;
    private final Consumer<Musica> aoAbrirDetalhes;

    private JLabel labelFotoPerfil;
    private JLabel labelMediaGeral;
    private JLabel labelTotalAvaliacoes;
    private JPanel gradeAvaliados;

    public TelaPerfil(Usuario usuario, AvaliacaoService avaliacaoService,
                      Runnable abrirFavoritos, Runnable abrirNovidades, Runnable voltarLogin) {
        this(usuario, avaliacaoService, abrirFavoritos, abrirNovidades, voltarLogin, musica -> {});
    }

    public TelaPerfil(Usuario usuario, AvaliacaoService avaliacaoService,
                      Runnable abrirFavoritos, Runnable abrirNovidades, Runnable voltarLogin,
                      Consumer<Musica> aoAbrirDetalhes) {
        this.usuario = usuario;
        this.avaliacaoService = avaliacaoService;
        this.aoAbrirDetalhes = aoAbrirDetalhes;

        setLayout(new BorderLayout());
        setOpaque(false);

        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new BorderLayout(12, 14));
        fundo.setBorder(BorderFactory.createEmptyBorder(18, 28, 24, 28));

        // Cabeçalho compacto com foto de perfil + username + estatísticas
        TemaEscutai.PainelArredondado cabecalho = new TemaEscutai.PainelArredondado(
                new BorderLayout(18, 0),
                TemaEscutai.RAIO_CARD,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.BORDA_SUTIL
        );
        cabecalho.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Foto circular do usuário + botão alterar foto
        JPanel blocoFoto = new JPanel();
        blocoFoto.setLayout(new BoxLayout(blocoFoto, BoxLayout.Y_AXIS));
        blocoFoto.setOpaque(false);

        labelFotoPerfil = new JLabel();
        labelFotoPerfil.setPreferredSize(new Dimension(TAMANHO_FOTO_PERFIL, TAMANHO_FOTO_PERFIL));
        labelFotoPerfil.setMinimumSize(new Dimension(TAMANHO_FOTO_PERFIL, TAMANHO_FOTO_PERFIL));
        labelFotoPerfil.setMaximumSize(new Dimension(TAMANHO_FOTO_PERFIL, TAMANHO_FOTO_PERFIL));
        labelFotoPerfil.setAlignmentX(Component.CENTER_ALIGNMENT);
        atualizarFotoPerfil();

        TemaEscutai.BotaoArredondado btnAlterarFoto = TemaEscutai.criarBotaoContorno("Alterar foto");
        btnAlterarFoto.setFont(TemaEscutai.FONTE_PEQUENA);
        btnAlterarFoto.setPadding(3, 12, 3, 12);
        btnAlterarFoto.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAlterarFoto.addActionListener(e -> escolherFotoPerfil());

        blocoFoto.add(labelFotoPerfil);
        blocoFoto.add(Box.createVerticalStrut(5));
        blocoFoto.add(btnAlterarFoto);

        // Informações centrais do usuário
        JPanel infoUsuario = new JPanel();
        infoUsuario.setLayout(new BoxLayout(infoUsuario, BoxLayout.Y_AXIS));
        infoUsuario.setOpaque(false);

        JLabel nomeUsuario = new JLabel("@" + usuario.getUsername());
        nomeUsuario.setFont(TemaEscutai.FONTE_TITULO);
        nomeUsuario.setForeground(TemaEscutai.BRANCO);

        JLabel emailUsuario = new JLabel(usuario.getEmail());
        emailUsuario.setFont(TemaEscutai.FONTE_CORPO);
        emailUsuario.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        infoUsuario.add(Box.createVerticalGlue());
        infoUsuario.add(nomeUsuario);
        infoUsuario.add(Box.createVerticalStrut(2));
        infoUsuario.add(emailUsuario);
        infoUsuario.add(Box.createVerticalGlue());

        // Cards de estatísticas (Média das avaliações e Total avaliado)
        JPanel painelEstatisticas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        painelEstatisticas.setOpaque(false);

        labelMediaGeral = new JLabel("—");
        labelMediaGeral.setFont(new Font("SansSerif", Font.BOLD, 21));
        labelMediaGeral.setForeground(TemaEscutai.DOURADO_ESTRELA);

        labelTotalAvaliacoes = new JLabel("0");
        labelTotalAvaliacoes.setFont(new Font("SansSerif", Font.BOLD, 21));
        labelTotalAvaliacoes.setForeground(TemaEscutai.BRANCO);

        painelEstatisticas.add(criarBoxEstatistica("MÉDIA DE NOTAS", labelMediaGeral));
        painelEstatisticas.add(criarBoxEstatistica("AVALIADOS", labelTotalAvaliacoes));

        cabecalho.add(blocoFoto, BorderLayout.WEST);
        cabecalho.add(infoUsuario, BorderLayout.CENTER);
        cabecalho.add(painelEstatisticas, BorderLayout.EAST);

        fundo.add(cabecalho, BorderLayout.NORTH);

        // Corpo: Grade de músicas/álbuns avaliados recentemente
        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setOpaque(false);

        JPanel tituloSecao = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tituloSecao.setOpaque(false);

        JPanel barraMagenta = new JPanel();
        barraMagenta.setBackground(TemaEscutai.MAGENTA);
        barraMagenta.setPreferredSize(new Dimension(4, 20));

        JLabel lblSecao = new JLabel("  AVALIADOS RECENTEMENTE");
        lblSecao.setFont(TemaEscutai.FONTE_SECAO);
        lblSecao.setForeground(TemaEscutai.BRANCO);

        JLabel dicaClique = new JLabel("   (clique em uma capa para ver ou editar sua resenha)");
        dicaClique.setFont(TemaEscutai.FONTE_PEQUENA);
        dicaClique.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        tituloSecao.add(barraMagenta);
        tituloSecao.add(lblSecao);
        tituloSecao.add(dicaClique);

        gradeAvaliados = new JPanel(new GridLayout(0, COLUNAS_POR_LINHA, 24, 26));
        gradeAvaliados.setOpaque(false);

        PainelGradeAjustavel wrapperGrade = new PainelGradeAjustavel();
        wrapperGrade.setLayout(new BorderLayout());
        wrapperGrade.setOpaque(false);
        wrapperGrade.add(gradeAvaliados, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapperGrade);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TemaEscutai.configurarScrollInvisivel(scroll);

        corpo.add(tituloSecao, BorderLayout.NORTH);
        corpo.add(scroll, BorderLayout.CENTER);

        fundo.add(corpo, BorderLayout.CENTER);
        add(fundo, BorderLayout.CENTER);

        recarregarDadosPerfil();
    }

    private JPanel criarBoxEstatistica(String rotulo, JLabel valor) {
        TemaEscutai.PainelArredondado box = new TemaEscutai.PainelArredondado(
                new BorderLayout(0, 2),
                16,
                TemaEscutai.FUNDO_BASE,
                TemaEscutai.BORDA_SUTIL
        );
        box.setPreferredSize(new Dimension(146, 70));
        box.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        valor.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lbl = new JLabel(rotulo, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        box.add(valor, BorderLayout.CENTER);
        box.add(lbl, BorderLayout.SOUTH);
        return box;
    }

    public void recarregarDadosPerfil() {
        List<Avaliacao> avaliacoesOriginais = avaliacaoService.listarAvaliacoesDoUsuario(usuario);
        double media = avaliacoesOriginais.stream()
                .mapToInt(Avaliacao::getNota)
                .average()
                .orElse(0.0);

        labelTotalAvaliacoes.setText(String.valueOf(avaliacoesOriginais.size()));
        labelMediaGeral.setText(avaliacoesOriginais.isEmpty() ? "—" : String.format("%.1f ★", media));

        List<Avaliacao> avaliacoes = new ArrayList<>(avaliacoesOriginais);
        Collections.reverse(avaliacoes);

        gradeAvaliados.removeAll();

        if (avaliacoes.isEmpty()) {
            JLabel vazio = new JLabel("Você ainda não avaliou nenhuma música ou álbum.");
            vazio.setFont(TemaEscutai.FONTE_CORPO);
            vazio.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
            gradeAvaliados.add(vazio);
        } else {
            for (Avaliacao avaliacao : avaliacoes) {
                gradeAvaliados.add(criarCardGradeAvaliacao(avaliacao));
            }
        }

        gradeAvaliados.revalidate();
        gradeAvaliados.repaint();
    }

    private JPanel criarCardGradeAvaliacao(Avaliacao avaliacao) {
        ConteudoMusical conteudo = avaliacao.getConteudo();

        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setOpaque(false);
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel capa = new JLabel();
        capa.setPreferredSize(new Dimension(TAMANHO_CAPA_GRADE, TAMANHO_CAPA_GRADE));
        capa.setMinimumSize(new Dimension(TAMANHO_CAPA_GRADE, TAMANHO_CAPA_GRADE));
        capa.setMaximumSize(new Dimension(TAMANHO_CAPA_GRADE, TAMANHO_CAPA_GRADE));
        capa.setHorizontalAlignment(SwingConstants.CENTER);
        capa.setAlignmentX(Component.CENTER_ALIGNMENT);
        TemaEscutai.carregarCapaAssincrona(
                conteudo.getImagemUrl(),
                TAMANHO_CAPA_GRADE,
                TAMANHO_CAPA_GRADE,
                capa,
                null
        );

        JLabel titulo = new JLabel(conteudo.getTitulo(), SwingConstants.CENTER);
        titulo.setFont(TemaEscutai.FONTE_SUBTITULO);
        titulo.setForeground(TemaEscutai.BRANCO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setMaximumSize(new Dimension(TAMANHO_CAPA_GRADE, 18));

        JLabel artista = new JLabel(conteudo.getArtista(), SwingConstants.CENTER);
        artista.setFont(TemaEscutai.FONTE_PEQUENA);
        artista.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        artista.setAlignmentX(Component.CENTER_ALIGNMENT);
        artista.setMaximumSize(new Dimension(TAMANHO_CAPA_GRADE, 16));

        JLabel estrelas = new JLabel(
                "★".repeat(Math.max(0, avaliacao.getNota())),
                SwingConstants.CENTER
        );
        estrelas.setFont(new Font("SansSerif", Font.BOLD, 15));
        estrelas.setForeground(TemaEscutai.DOURADO_ESTRELA);
        estrelas.setAlignmentX(Component.CENTER_ALIGNMENT);

        item.add(capa);
        item.add(Box.createVerticalStrut(8));
        item.add(titulo);
        item.add(Box.createVerticalStrut(2));
        item.add(artista);
        item.add(Box.createVerticalStrut(4));
        item.add(estrelas);

        MouseAdapter abrirAoClicar = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                titulo.setForeground(TemaEscutai.MAGENTA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                titulo.setForeground(TemaEscutai.BRANCO);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                abrirPopoutAvaliacao(avaliacao);
            }
        };

        item.addMouseListener(abrirAoClicar);
        capa.addMouseListener(abrirAoClicar);
        titulo.addMouseListener(abrirAoClicar);
        artista.addMouseListener(abrirAoClicar);
        estrelas.addMouseListener(abrirAoClicar);

        return item;
    }

    private void abrirPopoutAvaliacao(Avaliacao avaliacao) {
        ConteudoMusical conteudo = avaliacao.getConteudo();
        Window janelaPai = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(janelaPai, "Avaliação de " + conteudo.getTitulo(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(490, 400);
        dialog.setLocationRelativeTo(this);

        JPanel raiz = new JPanel(new BorderLayout(14, 14));
        raiz.setBackground(TemaEscutai.FUNDO_BASE);
        raiz.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        // Topo com capa, título, artista e link para página da música
        JPanel topo = new JPanel(new BorderLayout(14, 0));
        topo.setOpaque(false);

        JLabel capa = new JLabel();
        capa.setPreferredSize(new Dimension(88, 88));
        TemaEscutai.carregarCapaAssincrona(conteudo.getImagemUrl(), 88, 88, capa, null);

        JPanel infoMusica = new JPanel();
        infoMusica.setLayout(new BoxLayout(infoMusica, BoxLayout.Y_AXIS));
        infoMusica.setOpaque(false);

        JLabel titulo = new JLabel(conteudo.getTitulo());
        titulo.setFont(TemaEscutai.FONTE_SECAO);
        titulo.setForeground(TemaEscutai.BRANCO);

        JLabel artista = new JLabel(conteudo.getArtista() + "  •  " + avaliacao.getData());
        artista.setFont(TemaEscutai.FONTE_PEQUENA);
        artista.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        infoMusica.add(titulo);
        infoMusica.add(Box.createVerticalStrut(4));
        infoMusica.add(artista);

        if (conteudo instanceof Musica musica) {
            TemaEscutai.BotaoArredondado btnPaginaMusica = TemaEscutai.criarBotaoContorno("Ver página da música");
            btnPaginaMusica.setFont(TemaEscutai.FONTE_PEQUENA);
            btnPaginaMusica.setPadding(4, 12, 4, 12);
            btnPaginaMusica.addActionListener(e -> {
                dialog.dispose();
                aoAbrirDetalhes.accept(musica);
            });
            infoMusica.add(Box.createVerticalStrut(8));
            infoMusica.add(btnPaginaMusica);
        }

        topo.add(capa, BorderLayout.WEST);
        topo.add(infoMusica, BorderLayout.CENTER);

        // Centro: seletor de nota + caixa de resenha
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);

        JPanel linhaNota = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        linhaNota.setOpaque(false);

        JLabel lblNota = new JLabel("Sua nota:  ");
        lblNota.setFont(TemaEscutai.FONTE_SUBTITULO);
        lblNota.setForeground(TemaEscutai.BRANCO);
        linhaNota.add(lblNota);

        int[] notaEscolhida = {avaliacao.getNota()};
        JButton[] botoesEstrela = new JButton[5];
        for (int i = 0; i < 5; i++) {
            final int valorNota = i + 1;
            JButton estrela = new JButton(i < notaEscolhida[0] ? "★" : "☆");
            estrela.setFont(new Font("SansSerif", Font.BOLD, 24));
            estrela.setForeground(i < notaEscolhida[0] ? TemaEscutai.DOURADO_ESTRELA : TemaEscutai.TEXTO_SECUNDARIO);
            estrela.setBorderPainted(false);
            estrela.setContentAreaFilled(false);
            estrela.setFocusPainted(false);
            estrela.setCursor(new Cursor(Cursor.HAND_CURSOR));
            estrela.setMargin(new Insets(0, 2, 0, 2));
            estrela.addActionListener(e -> {
                notaEscolhida[0] = valorNota;
                for (int j = 0; j < 5; j++) {
                    botoesEstrela[j].setText(j < valorNota ? "★" : "☆");
                    botoesEstrela[j].setForeground(j < valorNota ? TemaEscutai.DOURADO_ESTRELA : TemaEscutai.TEXTO_SECUNDARIO);
                }
            });
            botoesEstrela[i] = estrela;
            linhaNota.add(estrela);
        }

        JTextArea campoResenha = new JTextArea(
                avaliacao.getComentario() == null ? "" : avaliacao.getComentario()
        );
        campoResenha.setLineWrap(true);
        campoResenha.setWrapStyleWord(true);
        campoResenha.setFont(TemaEscutai.FONTE_CORPO);
        campoResenha.setOpaque(false);
        campoResenha.setForeground(TemaEscutai.BRANCO);
        campoResenha.setCaretColor(TemaEscutai.BRANCO);

        JScrollPane scrollResenha = new JScrollPane(campoResenha);
        TemaEscutai.configurarScrollInvisivel(scrollResenha);

        TemaEscutai.PainelArredondado boxResenha = new TemaEscutai.PainelArredondado(
                new BorderLayout(), 16, TemaEscutai.SUPERFICIE, TemaEscutai.BORDA_SUTIL
        );
        boxResenha.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        boxResenha.add(scrollResenha, BorderLayout.CENTER);

        centro.add(linhaNota, BorderLayout.NORTH);
        centro.add(boxResenha, BorderLayout.CENTER);

        // Rodapé: Excluir | Fechar | Salvar alterações
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);

        TemaEscutai.BotaoArredondado btnExcluir = TemaEscutai.criarBotaoContorno("Excluir avaliação");
        btnExcluir.addActionListener(e -> {
            avaliacaoService.excluirAvaliacao(usuario, avaliacao);
            dialog.dispose();
            recarregarDadosPerfil();
        });

        JPanel direitaBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        direitaBotoes.setOpaque(false);

        TemaEscutai.BotaoArredondado btnFechar = TemaEscutai.criarBotaoContorno("Fechar");
        btnFechar.addActionListener(e -> dialog.dispose());

        TemaEscutai.BotaoArredondado btnSalvar = TemaEscutai.criarBotaoPrimario("Salvar alterações");
        btnSalvar.addActionListener(e -> {
            try {
                avaliacaoService.editarAvaliacao(
                        usuario,
                        avaliacao,
                        notaEscolhida[0],
                        campoResenha.getText().trim()
                );
                dialog.dispose();
                recarregarDadosPerfil();
            } catch (NotaInvalidaException | RuntimeException erro) {
                JOptionPane.showMessageDialog(dialog, erro.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        direitaBotoes.add(btnFechar);
        direitaBotoes.add(btnSalvar);

        rodape.add(btnExcluir, BorderLayout.WEST);
        rodape.add(direitaBotoes, BorderLayout.EAST);

        raiz.add(topo, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(rodape, BorderLayout.SOUTH);

        dialog.setContentPane(raiz);
        dialog.setVisible(true);
    }

    private void escolherFotoPerfil() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Escolha sua foto de perfil");
        seletor.setFileFilter(new FileNameExtensionFilter("Imagens (PNG, JPG, JPEG)", "png", "jpg", "jpeg"));

        if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selecionado = seletor.getSelectedFile();
            try {
                Path pastaFotos = Paths.get("data", "fotos_perfil");
                Files.createDirectories(pastaFotos);
                String extensao = selecionado.getName().contains(".")
                        ? selecionado.getName().substring(selecionado.getName().lastIndexOf('.'))
                        : ".png";
                Path destino = pastaFotos.resolve(usuario.getUsername() + extensao);
                Files.copy(selecionado.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
                atualizarFotoPerfil();
            } catch (Exception erro) {
                JOptionPane.showMessageDialog(this, "Não foi possível carregar a imagem selecionada.", "Foto de perfil", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void atualizarFotoPerfil() {
        BufferedImage avatar = new BufferedImage(TAMANHO_FOTO_PERFIL, TAMANHO_FOTO_PERFIL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = avatar.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        BufferedImage fotoSalva = carregarArquivoFotoUsuario();
        Shape circulo = new Ellipse2D.Double(2, 2, TAMANHO_FOTO_PERFIL - 4, TAMANHO_FOTO_PERFIL - 4);
        g2.setClip(circulo);

        if (fotoSalva != null) {
            g2.drawImage(fotoSalva, 2, 2, TAMANHO_FOTO_PERFIL - 4, TAMANHO_FOTO_PERFIL - 4, null);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, TemaEscutai.MAGENTA, TAMANHO_FOTO_PERFIL, TAMANHO_FOTO_PERFIL, TemaEscutai.SUPERFICIE);
            g2.setPaint(gp);
            g2.fill(circulo);

            String inicial = usuario.getUsername().isEmpty() ? "U" : usuario.getUsername().substring(0, 1).toUpperCase();
            g2.setColor(TemaEscutai.BRANCO);
            g2.setFont(new Font("SansSerif", Font.BOLD, 40));
            FontMetrics fm = g2.getFontMetrics();
            int x = (TAMANHO_FOTO_PERFIL - fm.stringWidth(inicial)) / 2;
            int y = ((TAMANHO_FOTO_PERFIL - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(inicial, x, y);
        }

        g2.setClip(null);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(TemaEscutai.MAGENTA);
        g2.draw(circulo);
        g2.dispose();

        labelFotoPerfil.setIcon(new ImageIcon(avatar));
    }

    private BufferedImage carregarArquivoFotoUsuario() {
        String[] extensoes = {".png", ".jpg", ".jpeg"};
        for (String ext : extensoes) {
            Path p = Paths.get("data", "fotos_perfil", usuario.getUsername() + ext);
            if (Files.exists(p)) {
                try {
                    return ImageIO.read(p.toFile());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Garante que a grade ocupe exatamente a largura visível do JScrollPane,
     * impedindo que os cards ultrapassem a borda direita da tela.
     */
    private static class PainelGradeAjustavel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 36;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 140;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}