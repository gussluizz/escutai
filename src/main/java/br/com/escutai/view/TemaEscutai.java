package br.com.escutai.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class TemaEscutai {

    // Paleta oficial do projeto ESCUTAÍ
    public static final Color FUNDO_BASE = new Color(13, 5, 18);           // #0D0512
    public static final Color SUPERFICIE = new Color(30, 12, 38);          // #1E0C26
    public static final Color SUPERFICIE_HOVER = new Color(48, 18, 62);    // #30123E
    public static final Color BORDA_SUTIL = new Color(75, 30, 100);        // #4B1E64
    public static final Color ROXO_PRIMARIO = new Color(130, 30, 190);     // #821EBE
    public static final Color ROXO_CLARO = new Color(165, 65, 225);        // #A541E1
    public static final Color MAGENTA = new Color(220, 0, 130);            // #DC0082
    public static final Color MAGENTA_HOVER = new Color(240, 30, 150);     // #F01E96
    public static final Color BRANCO = Color.WHITE;                        // #FFFFFF
    public static final Color TEXTO_SECUNDARIO = new Color(200, 184, 216); // #C8B8D8
    public static final Color DOURADO_ESTRELA = new Color(255, 190, 40);   // #FFBE28

    public static final int RAIO_CAPA = 18;
    public static final int RAIO_CARD = 18;
    public static final int RAIO_BOTAO = 24;

    public static final Font FONTE_TITULO = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONTE_SECAO = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONTE_SUBTITULO = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONTE_CORPO = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONTE_PEQUENA = new Font("SansSerif", Font.PLAIN, 11);

    private static BufferedImage logoOriginalCache;
    private static BufferedImage wordmarkOriginalCache;
    private static BufferedImage fundoOriginalCache;
    private static final Map<String, ImageIcon> CACHE_CAPAS = new ConcurrentHashMap<>();

    private TemaEscutai() {
    }

    public static ImageIcon obterLogo(int largura, int altura) {
        BufferedImage base = carregarLogoBase();
        if (base != null) {
            Image escalada = base.getScaledInstance(largura, altura, Image.SCALE_SMOOTH);
            return new ImageIcon(escalada);
        }
        return gerarLogoContingencia(largura, altura);
    }

    public static ImageIcon obterWordmark(int alturaAlvo) {
        BufferedImage base = carregarWordmarkBase();
        if (base != null && base.getHeight() > 0) {
            int larguraProporcional = Math.max(1, (int) Math.round(((double) base.getWidth() / base.getHeight()) * alturaAlvo));
            Image escalada = base.getScaledInstance(larguraProporcional, alturaAlvo, Image.SCALE_SMOOTH);
            return new ImageIcon(escalada);
        }
        return gerarWordmarkContingencia(alturaAlvo);
    }

    private static BufferedImage carregarLogoBase() {
        if (logoOriginalCache != null) {
            return logoOriginalCache;
        }
        BufferedImage img = carregarImagemRecurso(new String[]{
                "/elementos/logo.png",
                "elementos/logo.png"
        });
        if (img != null) {
            logoOriginalCache = prepararLogoTransparenteBranca(img);
        }
        return logoOriginalCache;
    }

    private static BufferedImage carregarWordmarkBase() {
        if (wordmarkOriginalCache != null) {
            return wordmarkOriginalCache;
        }
        BufferedImage img = carregarImagemRecurso(new String[]{
                "/elementos/wordmark.png",
                "elementos/wordmark.png",
                "/elementos/escutaí (3).png",
                "elementos/escutaí (3).png"
        });
        if (img != null) {
            wordmarkOriginalCache = prepararLogoTransparenteBranca(img);
        }
        return wordmarkOriginalCache;
    }

    private static BufferedImage carregarImagemRecurso(String[] caminhos) {
        for (String caminho : caminhos) {
            try {
                if (caminho.startsWith("/")) {
                    URL recurso = TemaEscutai.class.getResource(caminho);
                    if (recurso != null) {
                        BufferedImage img = ImageIO.read(recurso);
                        if (img != null) return img;
                    }
                } else {
                    File arquivo = new File(caminho);
                    if (arquivo.exists()) {
                        BufferedImage img = ImageIO.read(arquivo);
                        if (img != null) return img;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static BufferedImage prepararLogoTransparenteBranca(BufferedImage origem) {
        int w = origem.getWidth();
        int h = origem.getHeight();
        BufferedImage resultado = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int cantoArgb = origem.getRGB(0, 0);
        int cantoAlpha = (cantoArgb >> 24) & 0xFF;
        boolean fundoJaTransparente = origem.getColorModel().hasAlpha() && cantoAlpha < 25;

        int minX = w, minY = h, maxX = 0, maxY = 0;
        boolean encontrouPixel = false;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = origem.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                if (fundoJaTransparente) {
                    if (a < 20) {
                        resultado.setRGB(x, y, 0x00000000);
                    } else {
                        resultado.setRGB(x, y, (a << 24) | 0x00FFFFFF);
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                        encontrouPixel = true;
                    }
                } else {
                    int luminancia = Math.max(r, Math.max(g, b));
                    if (luminancia < 25) {
                        resultado.setRGB(x, y, 0x00000000);
                    } else {
                        int alpha = Math.min(255, luminancia);
                        resultado.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                        encontrouPixel = true;
                    }
                }
            }
        }

        if (encontrouPixel && maxX > minX && maxY > minY) {
            return resultado.getSubimage(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
        }
        return resultado;
    }

    private static ImageIcon gerarLogoContingencia(int largura, int altura) {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(BRANCO);
        int margem = Math.max(4, largura / 10);
        g2.setStroke(new BasicStroke(Math.max(3f, largura / 18f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(margem, margem, largura - 2 * margem, altura - margem, 0, 180);
        g2.fillRoundRect(margem - 2, altura / 2, largura / 6, altura / 3, 8, 8);
        g2.fillRoundRect(largura - margem - largura / 6 + 2, altura / 2, largura / 6, altura / 3, 8, 8);
        g2.dispose();
        return new ImageIcon(img);
    }

    private static ImageIcon gerarWordmarkContingencia(int altura) {
        int largura = altura * 4;
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(BRANCO);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, (int) (altura * 0.78))));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("escutaí", 2, (altura + fm.getAscent()) / 2 - 2);
        g2.dispose();
        return new ImageIcon(img);
    }

    public static BufferedImage carregarImagemFundo() {
        if (fundoOriginalCache != null) {
            return fundoOriginalCache;
        }
        fundoOriginalCache = carregarImagemRecurso(new String[]{
                "/elementos/fundo.jpg",
                "elementos/fundo.jpg"
        });
        return fundoOriginalCache;
    }

    public static BufferedImage arredondarImagem(BufferedImage origem, int largura, int altura, int raio) {
        BufferedImage saida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = saida.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2.setClip(new RoundRectangle2D.Float(0, 0, largura, altura, raio, raio));
        g2.drawImage(origem, 0, 0, largura, altura, null);
        g2.dispose();
        return saida;
    }

    public static void carregarCapaAssincrona(String urlImagem, int largura, int altura,
                                              JLabel destino, Consumer<ImageIcon> aoConcluir) {
        carregarCapaAssincrona(urlImagem, largura, altura, RAIO_CAPA, destino, aoConcluir);
    }

    public static void carregarCapaAssincrona(String urlImagem, int largura, int altura, int raio,
                                              JLabel destino, Consumer<ImageIcon> aoConcluir) {
        if (urlImagem == null || urlImagem.isBlank()) {
            destino.setText("Sem capa");
            destino.setForeground(TEXTO_SECUNDARIO);
            return;
        }

        String chave = urlImagem + "_" + largura + "x" + altura + "_r" + raio;
        ImageIcon emCache = CACHE_CAPAS.get(chave);
        if (emCache != null) {
            destino.setText("");
            destino.setIcon(emCache);
            if (aoConcluir != null) {
                aoConcluir.accept(emCache);
            }
            return;
        }

        destino.setIcon(null);
        destino.setText("...");
        destino.setForeground(TEXTO_SECUNDARIO);

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage original = ImageIO.read(new URL(urlImagem));
                if (original == null) {
                    return null;
                }
                BufferedImage arredondada = arredondarImagem(original, largura, altura, raio);
                return new ImageIcon(arredondada);
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        CACHE_CAPAS.put(chave, icon);
                        destino.setText("");
                        destino.setIcon(icon);
                        if (aoConcluir != null) {
                            aoConcluir.accept(icon);
                        }
                    } else {
                        destino.setText("Sem capa");
                    }
                } catch (Exception e) {
                    destino.setText("Sem capa");
                }
            }
        }.execute();
    }

    /**
     * Oculta visualmente a barra de rolagem vertical e configura rolagem rápida e imediata pela rodinha do mouse.
     */
    public static void configurarScrollInvisivel(JScrollPane scrollPane) {
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar barraVertical = scrollPane.getVerticalScrollBar();
        barraVertical.setPreferredSize(new Dimension(0, 0));
        barraVertical.setOpaque(false);
        barraVertical.setUnitIncrement(36);
        barraVertical.setBlockIncrement(140);
    }

    /**
     * Impede que JScrollPanes internos (como carrosséis horizontais) travem a rolagem vertical da página,
     * repassando o evento da rodinha diretamente para o JScrollPane ancestral.
     */
    public static void repassarScrollParaPai(JScrollPane scrollInterno) {
        scrollInterno.setWheelScrollingEnabled(false);
        scrollInterno.addMouseWheelListener(e -> {
            Container pai = SwingUtilities.getAncestorOfClass(JScrollPane.class, scrollInterno);
            if (pai != null) {
                MouseEvent eventoRepassado = SwingUtilities.convertMouseEvent(scrollInterno, e, pai);
                pai.dispatchEvent(eventoRepassado);
            }
        });
    }

    public static BotaoArredondado criarBotaoPrimario(String texto) {
        BotaoArredondado botao = new BotaoArredondado(texto, MAGENTA, MAGENTA_HOVER, null, RAIO_BOTAO);
        botao.setFont(FONTE_SUBTITULO);
        botao.setPadding(9, 20, 9, 20);
        return botao;
    }

    public static BotaoArredondado criarBotaoSecundario(String texto) {
        BotaoArredondado botao = new BotaoArredondado(texto, ROXO_PRIMARIO, ROXO_CLARO, null, RAIO_BOTAO);
        botao.setFont(FONTE_CORPO);
        botao.setPadding(8, 18, 8, 18);
        return botao;
    }

    public static BotaoArredondado criarBotaoContorno(String texto) {
        BotaoArredondado botao = new BotaoArredondado(texto, SUPERFICIE, SUPERFICIE_HOVER, BORDA_SUTIL, RAIO_BOTAO);
        botao.setFont(FONTE_CORPO);
        botao.setPadding(8, 18, 8, 18);
        return botao;
    }

    public static void estilizarCampoTexto(JTextField campo) {
        campo.setBackground(SUPERFICIE);
        campo.setForeground(BRANCO);
        campo.setCaretColor(BRANCO);
        campo.setFont(FONTE_CORPO);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA_SUTIL, 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
    }

    /**
     * Botão customizado 100% arredondado seguindo a paleta oficial do ESCUTAÍ.
     */
    public static class BotaoArredondado extends JButton {
        private Color corNormal;
        private Color corHover;
        private Color corBorda;
        private int raio;
        private boolean hover = false;

        public BotaoArredondado(String texto, Color corNormal, Color corHover, Color corBorda, int raio) {
            super(texto);
            this.corNormal = corNormal;
            this.corHover = corHover;
            this.corBorda = corBorda;
            this.raio = raio;

            setForeground(BRANCO);
            setFont(FONTE_SUBTITULO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        public void setPadding(int top, int left, int bottom, int right) {
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        }

        public void setCores(Color corNormal, Color corHover, Color corBorda) {
            this.corNormal = corNormal;
            this.corHover = corHover;
            this.corBorda = corBorda;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arco = Math.min(raio, h);

            Color fundoAtual = hover ? corHover : corNormal;
            if (fundoAtual != null) {
                g2.setColor(fundoAtual);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arco, arco);
            }

            if (corBorda != null) {
                g2.setColor(hover ? MAGENTA : corBorda);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arco, arco);
            }
            g2.dispose();

            super.paintComponent(g);
        }
    }

    /**
     * Botão arredondado com ícone vetorial nítido de lupa centralizado via Graphics2D,
     * utilizando o mesmo fundo rosa (MAGENTA) do botão ativo FAVORITOS.
     */
    public static class BotaoLupa extends BotaoArredondado {
        public BotaoLupa() {
            this(46, 38);
        }

        public BotaoLupa(int largura, int altura) {
            super("", MAGENTA, MAGENTA_HOVER, MAGENTA, RAIO_BOTAO);
            Dimension dim = new Dimension(largura, altura);
            setPreferredSize(dim);
            setMinimumSize(dim);
            setMaximumSize(dim);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(BRANCO);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // Lente circular da lupa
            g2.drawOval(cx - 8, cy - 8, 12, 12);
            // Cabo diagonal da lupa
            g2.drawLine(cx + 2, cy + 2, cx + 7, cy + 7);

            g2.dispose();
        }
    }

    /**
     * Painel com fundo e borda arredondados via Graphics2D.
     */
    public static class PainelArredondado extends JPanel {
        private final int raio;
        private Color corFundo;
        private Color corBorda;

        public PainelArredondado(int raio, Color corFundo, Color corBorda) {
            this.raio = raio;
            this.corFundo = corFundo;
            this.corBorda = corBorda;
            setOpaque(false);
        }

        public PainelArredondado(LayoutManager layout, int raio, Color corFundo, Color corBorda) {
            super(layout);
            this.raio = raio;
            this.corFundo = corFundo;
            this.corBorda = corBorda;
            setOpaque(false);
        }

        public void setEstilo(Color corFundo, Color corBorda) {
            this.corFundo = corFundo;
            this.corBorda = corBorda;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (corFundo != null) {
                g2.setColor(corFundo);
                g2.fillRoundRect(0, 0, w - 1, h - 1, raio, raio);
            }
            if (corBorda != null) {
                g2.setColor(corBorda);
                g2.drawRoundRect(0, 0, w - 1, h - 1, raio, raio);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Botão arredondado com seta vetorial nítida (esquerda ou direita).
     */
    public static class BotaoSetaCarrossel extends BotaoArredondado {
        private final boolean apontarDireita;

        public BotaoSetaCarrossel(boolean apontarDireita) {
            this(apontarDireita, 40, 84);
        }

        public BotaoSetaCarrossel(boolean apontarDireita, int largura, int altura) {
            super("", SUPERFICIE_HOVER, MAGENTA, BORDA_SUTIL, 22);
            this.apontarDireita = apontarDireita;
            Dimension dim = new Dimension(largura, altura);
            setPreferredSize(dim);
            setMinimumSize(dim);
            setMaximumSize(dim);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(BRANCO);
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int dx = 5;
            int dy = 8;

            Path2D seta = new Path2D.Double();
            if (apontarDireita) {
                seta.moveTo(cx - dx, cy - dy);
                seta.lineTo(cx + dx, cy);
                seta.lineTo(cx - dx, cy + dy);
            } else {
                seta.moveTo(cx + dx, cy - dy);
                seta.lineTo(cx - dx, cy);
                seta.lineTo(cx + dx, cy + dy);
            }
            g2.draw(seta);
            g2.dispose();
        }
    }

    public static class PainelFundoGradiente extends JPanel {
        public PainelFundoGradiente() {
            setOpaque(true);
            setBackground(FUNDO_BASE);
        }

        public PainelFundoGradiente(LayoutManager layout) {
            super(layout);
            setOpaque(true);
            setBackground(FUNDO_BASE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();

            BufferedImage imgFundo = carregarImagemFundo();
            if (imgFundo != null) {
                g2.drawImage(imgFundo, 0, 0, w, h, null);
                g2.setColor(new Color(13, 5, 18, 95));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
                return;
            }

            g2.setColor(FUNDO_BASE);
            g2.fillRect(0, 0, w, h);

            if (w > 0 && h > 0) {
                float raio = Math.max(w, h) * 0.65f;

                RadialGradientPaint glowEsquerda = new RadialGradientPaint(
                        new Point2D.Float(0f, h * 0.42f),
                        raio,
                        new float[]{0f, 1f},
                        new Color[]{new Color(220, 0, 130, 145), new Color(13, 5, 18, 0)}
                );
                g2.setPaint(glowEsquerda);
                g2.fillRect(0, 0, w, h);

                RadialGradientPaint glowDireita = new RadialGradientPaint(
                        new Point2D.Float(w, h * 0.12f),
                        raio,
                        new float[]{0f, 1f},
                        new Color[]{new Color(130, 30, 190, 155), new Color(13, 5, 18, 0)}
                );
                g2.setPaint(glowDireita);
                g2.fillRect(0, 0, w, h);

                RadialGradientPaint glowInferior = new RadialGradientPaint(
                        new Point2D.Float(w * 0.88f, h),
                        raio * 0.7f,
                        new float[]{0f, 1f},
                        new Color[]{new Color(195, 10, 135, 110), new Color(13, 5, 18, 0)}
                );
                g2.setPaint(glowInferior);
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }
}