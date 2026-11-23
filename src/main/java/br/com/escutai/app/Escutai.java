package br.com.escutai.app;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.JTextComponent;

import br.com.escutai.catalogo.service.ChartsService;
import br.com.escutai.catalogo.service.ItunesService;
import br.com.escutai.catalogo.service.LancamentosService;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.catalogo.model.Album;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.shared.exception.BuscaSemResultadosException;
import br.com.escutai.shared.exception.ErroComunicacaoApiException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

/**
 * ESCUTAÍ - app de avaliação de músicas estilo Letterboxd.
 * Visual "liquid glass": fundo animado + painéis de vidro translúcido.
 * Usuário de teste: demo / 123456
 */
public class Escutai extends JFrame {

    // ===================== TEMA =====================
    static final Color BASE = new Color(0x171126); // roxo profundo
    static final Color TEXT = new Color(0xF4F8FF);
    static final Color MUTED = new Color(0xC2B6D8);
    static final Color ACCENT = new Color(0xD2B4FF); // lilás claro
    static final Color ACCENT2 = new Color(0x8155C7); // roxo
    /** Intensidade do brilho azul animado no fundo (0 = fundo totalmente liso). */
    static final float GLOW = 0.45f;
    static final Color STAR = new Color(0xFFC83D);
    static final Color ERR = new Color(0xFF7A8A);
    static final Color HEART = new Color(0xFF4D7D);
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final int MAX = Integer.MAX_VALUE;
    static final int CARD_W = 172, CARD_H = 238;
    static final String FAMILY = pickFamily();

    static String pickFamily() {
        Set<String> av = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[] { "Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto", "Arial" })
            if (av.contains(f))
                return f;
        return Font.SANS_SERIF;
    }

    static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    // ===================== LOGOS =====================
    // As logos (brancas, fundo transparente) estão embutidas no fim deste arquivo
    // (classe EmbeddedLogos),
    // então não é preciso copiar nenhum PNG para o projeto.
    static final BufferedImage LOGO_WORD = decode(EmbeddedLogos.WORD); // palavra "escutaí"
    static final BufferedImage LOGO_PHONES = decode(EmbeddedLogos.PHONES); // fones + "es cu taí" (usada só no ícone da
                                                                           // janela)

    static BufferedImage decode(String base64) {
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static BufferedImage scaleStep(BufferedImage s, int w, int h) {
        BufferedImage o = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(s, 0, 0, Math.max(1, w), Math.max(1, h), null);
        g.dispose();
        return o;
    }

    /**
     * Reduz em etapas (metade por vez) para a logo ficar nítida, sem serrilhado.
     */
    static BufferedImage scaleSmooth(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = scaleStep(cur, cw, ch);
        }
        return scaleStep(cur, w, h);
    }

    /** Ícone do app: quadrado azul com os fones brancos. */
    static Image appIcon(int s) {
        BufferedImage ic = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ic.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x9A70D6), s, s, new Color(0x211532)));
        g.fillRoundRect(0, 0, s, s, s / 4, s / 4);
        if (LOGO_PHONES != null) {
            int w = (int) (s * 0.66);
            int h = (int) Math.round(w * (double) LOGO_PHONES.getHeight() / LOGO_PHONES.getWidth());
            g.drawImage(scaleSmooth(LOGO_PHONES, w, h), (s - w) / 2, (s - h) / 2, null);
        }
        g.dispose();
        return ic;
    }

    // ===================== MODELO =====================
    static class User {
        String username, email, password, bio = "";
        Set<Integer> favorites = new LinkedHashSet<>();

        User(String u, String e, String p) {
            username = u;
            email = e;
            password = p;
        }
    }

    static class Track {
        int id;
        String genre, title, artist, album;
        int year;
        boolean isAlbum;
        long duracaoMs;
        String linkAppleMusic;
        String linkSpotify;
        /** URL da capa (iTunes). Se for null, usa a capa gerada (gradiente + inicial). */
        volatile String imagemUrl;

        Track(int id, String genre, String t, String ar, String al, int y, boolean isAlbum) {
            this.id = id;
            this.genre = genre;
            title = t;
            artist = ar;
            album = al;
            year = y;
            this.isAlbum = isAlbum;
        }
    }

    static class Review {
        String username;
        int trackId, stars;
        String text;
        LocalDate date;

        Review(String u, int t, int s, String tx, LocalDate d) {
            username = u;
            trackId = t;
            stars = s;
            text = tx;
            date = d;
        }
    }

    // ===================== "BANCO" EM MEMÓRIA =====================
    // Troque esta classe por DAOs / JDBC quando for ligar ao banco de dados.
    static class Store {
        static final String[] GENRES = { "MPB e Tropicália", "Rock e Pop Brasil", "Hip-hop", "Internacional" };
        static final List<User> users = new ArrayList<>();
        static final List<Track> tracks = new ArrayList<>();
        static final List<Review> reviews = new ArrayList<>();
        /** Seção da tela inicial. Só mexer nela na thread da interface. */
        static class Secao {
            final String titulo, subtitulo;
            final List<Track> itens = new ArrayList<>();
            /** "carregando", "ok", "vazio" ou "erro". */
            String status = "carregando";
            /** Motivo da falha, mostrado na tela para facilitar o diagnóstico. */
            String erro = "";

            Secao(String titulo, String subtitulo) {
                this.titulo = titulo;
                this.subtitulo = subtitulo;
            }
        }

        static final Secao topMundo = new Secao("Top Mundo", "Músicas mais tocadas em vários países");
        static final Secao topBrasil = new Secao("Top Brasil", "Músicas mais tocadas no Brasil");
        static final Secao lancamentos = new Secao("Lançamentos", "Álbuns recentes entre os mais tocados no Brasil");
        /** Evita criar a mesma música/álbum da Apple duas vezes (chave: tipo + id da Apple). */
        static final Map<String, Track> porApple = new HashMap<>();
        static User current;

        static void t(String genre, String title, String artist, String album, int year, boolean isAlbum) {
            tracks.add(new Track(tracks.size() + 1, genre, title, artist, album, year, isAlbum));
        }

        static int id(String title) {
            for (Track t : tracks)
                if (t.title.equals(title))
                    return t.id;
            throw new IllegalArgumentException(title);
        }

        static void rev(String user, String title, int stars, String text, int daysAgo) {
            reviews.add(new Review(user, id(title), stars, text, LocalDate.now().minusDays(daysAgo)));
        }

        static {
            String mpb = GENRES[0], rock = GENRES[1], hip = GENRES[2], intl = GENRES[3];
            t(mpb, "Águas de Março", "Elis Regina & Tom Jobim", "Elis & Tom", 1974, false);
            t(mpb, "Construção", "Chico Buarque", "Construção", 1971, true);
            t(mpb, "Refazenda", "Gilberto Gil", "Refazenda", 1975, true);
            t(mpb, "Clube da Esquina", "Milton Nascimento & Lô Borges", "Clube da Esquina", 1972, true);
            t(mpb, "Sampa", "Caetano Veloso", "Muito", 1978, false);
            t(mpb, "Luz", "Djavan", "Luz", 1982, true);
            t(mpb, "Panis et Circenses", "Os Mutantes", "Os Mutantes", 1968, false);
            t(mpb, "Mais", "Marisa Monte", "Mais", 1991, true);
            t(mpb, "Samba Esquema Novo", "Jorge Ben Jor", "Samba Esquema Novo", 1963, true);
            t(mpb, "Azul da Cor do Mar", "Tim Maia", "Tim Maia", 1970, false);
            t(mpb, "Domingo", "Caetano Veloso & Gal Costa", "Domingo", 1967, true);

            t(rock, "Tempo Perdido", "Legião Urbana", "Dois", 1986, false);
            t(rock, "Ovelha Negra", "Rita Lee", "Fruto Proibido", 1975, false);
            t(rock, "Acústico MTV", "Cássia Eller", "Acústico MTV", 2001, true);
            t(rock, "Exagerado", "Cazuza", "Exagerado", 1985, true);
            t(rock, "Cabeça Dinossauro", "Titãs", "Cabeça Dinossauro", 1986, true);
            t(rock, "Selvagem?", "Os Paralamas do Sucesso", "Selvagem?", 1986, true);
            t(rock, "Krig-ha, Bandolo!", "Raul Seixas", "Krig-ha, Bandolo!", 1973, true);
            t(rock, "Samba Poconé", "Skank", "Samba Poconé", 1996, true);

            t(hip, "Fim de Semana no Parque", "Racionais MC's", "Raio X do Brasil", 1993, false);
            t(hip, "Sobrevivendo no Inferno", "Racionais MC's", "Sobrevivendo no Inferno", 1997, true);
            t(hip, "AmarElo", "Emicida", "AmarElo", 2019, true);
            t(hip, "Rap é Compromisso!", "Sabotage", "Rap é Compromisso!", 2000, true);
            t(hip, "Nó na Orelha", "Criolo", "Nó na Orelha", 2011, true);
            t(hip, "To Pimp a Butterfly", "Kendrick Lamar", "To Pimp a Butterfly", 2015, true);
            t(hip, "good kid, m.A.A.d city", "Kendrick Lamar", "good kid, m.A.A.d city", 2012, true);

            t(intl, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 1975, false);
            t(intl, "OK Computer", "Radiohead", "OK Computer", 1997, true);
            t(intl, "Blonde", "Frank Ocean", "Blonde", 2016, true);
            t(intl, "Rumours", "Fleetwood Mac", "Rumours", 1977, true);
            t(intl, "Kind of Blue", "Miles Davis", "Kind of Blue", 1959, true);
            t(intl, "Thriller", "Michael Jackson", "Thriller", 1982, true);
            t(intl, "Abbey Road", "The Beatles", "Abbey Road", 1969, true);
            t(intl, "Random Access Memories", "Daft Punk", "Random Access Memories", 2013, true);
            t(intl, "Currents", "Tame Impala", "Currents", 2015, true);

            User demo = new User("demo", "demo@escutai.com", "123456");
            demo.bio = "Ouvindo de tudo um pouco.";
            demo.favorites.addAll(Arrays.asList(id("Construção"), id("Clube da Esquina"), id("Kind of Blue")));
            users.add(demo);
            users.add(new User("ana", "ana@escutai.com", "123456"));
            users.add(new User("joao", "joao@escutai.com", "123456"));

            rev("demo", "Construção", 5, "Obra-prima. A letra e o arranjo se constroem juntos até o final.", 9);
            rev("demo", "Clube da Esquina", 5, "Disco para ouvir de cabo a rabo, de preferência com calma.", 4);
            rev("demo", "Blonde", 4, "Íntimo e estranho do melhor jeito.", 3);
            rev("ana", "Construção", 4, "Impecável, mas prefiro outros do Chico.", 7);
            rev("ana", "AmarElo", 5, "Emicida em sua melhor fase. Disco necessário.", 2);
            rev("ana", "Thriller", 5, "Não tem faixa fraca. Pop em estado de arte.", 6);
            rev("ana", "Kind of Blue", 5, "O disco de jazz que todo mundo deveria ouvir uma vez.", 5);
            rev("ana", "Rumours", 4, "Brigas viraram canções perfeitas.", 8);
            rev("joao", "OK Computer", 5, "Um marco dos anos 90. Cada faixa tem personalidade.", 12);
            rev("joao", "Tempo Perdido", 4, "Clássico do rock nacional.", 1);
            rev("joao", "Sobrevivendo no Inferno", 5, "Pesado, necessário e atemporal.", 10);
            rev("joao", "Abbey Road", 5, "O lado B inteiro é uma viagem.", 11);
            rev("joao", "Águas de Março", 5, "Simplesmente perfeita.", 13);
            rev("joao", "Exagerado", 4, "Cazuza em carne viva.", 14);
        }

        static User login(String u, String p) {
            return users.stream()
                    .filter(x -> x.username.equalsIgnoreCase(u) && x.password.equals(p))
                    .findFirst().orElse(null);
        }

        static boolean usernameTaken(String u) {
            return users.stream().anyMatch(x -> x.username.equalsIgnoreCase(u));
        }

        static boolean emailTaken(String e) {
            return users.stream().anyMatch(x -> x.email.equalsIgnoreCase(e));
        }

        static List<Review> of(int trackId) {
            return reviews.stream().filter(r -> r.trackId == trackId)
                    .sorted((a, b) -> b.date.compareTo(a.date)).collect(Collectors.toList());
        }

        static List<Review> by(String username) {
            return reviews.stream().filter(r -> r.username.equals(username))
                    .sorted((a, b) -> b.date.compareTo(a.date)).collect(Collectors.toList());
        }

        static Review find(String username, int trackId) {
            return reviews.stream().filter(r -> r.username.equals(username) && r.trackId == trackId)
                    .findFirst().orElse(null);
        }

        static void save(Review r) {
            Review old = find(r.username, r.trackId);
            if (old != null)
                reviews.remove(old);
            reviews.add(r);
        }

        static double avg(int trackId) {
            return of(trackId).stream().mapToInt(r -> r.stars).average().orElse(0);
        }

        static Track track(int id) {
            return tracks.get(id - 1);
        }

        /** Ordem de destaque: melhor média, depois mais avaliações. */
        static List<Track> featured() {
            return tracks.stream().sorted((a, b) -> {
                int c = Double.compare(avg(b.id), avg(a.id));
                if (c != 0)
                    return c;
                c = Integer.compare(of(b.id).size(), of(a.id).size());
                return c != 0 ? c : Integer.compare(a.id, b.id);
            }).collect(Collectors.toList());
        }

        static List<Track> byGenre(String g) {
            return tracks.stream().filter(t -> t.genre.equals(g)).collect(Collectors.toList());
        }
    }

    // ===================== DESENHO "LIQUID GLASS" =====================
    static Color white(double alpha) {
        return new Color(255, 255, 255, (int) Math.max(0, Math.min(255, alpha)));
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * Vidro: preenchimento translúcido + brilho especular no topo + borda de luz.
     */
    static void glass(Graphics2D g, int x, int y, int w, int h, int arc, float s) {
        if (w < 2 || h < 2)
            return;
        RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f, arc, arc);
        g.setPaint(new GradientPaint(0, y, white(34 * s), 0, y + h, white(12 * s)));
        g.fill(r);
        Area hi = new Area(r);
        hi.intersect(new Area(new Rectangle2D.Float(x, y, w, h * 0.5f)));
        g.setPaint(new GradientPaint(0, y, white(46 * s), 0, y + h * 0.5f, white(0)));
        g.fill(hi);
        g.setPaint(new GradientPaint(x, y, white(130 * Math.min(s, 1.6f)), x + w, y + h, white(28)));
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
    }

    static void fillIn(Graphics2D g, Shape clip, Shape s) {
        Area a = new Area(clip);
        a.intersect(new Area(s));
        g.fill(a);
    }

    static String ellipsize(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max)
            return s;
        int n = s.length();
        while (n > 0 && fm.stringWidth(s.substring(0, n) + "…") > max)
            n--;
        return s.substring(0, n) + "…";
    }

    /** Capa gerada (gradiente + inicial), no lugar de uma imagem de álbum. */
    static void paintCover(Graphics2D g, Track t, int x, int y, int size, int arc) {
        BufferedImage capa = CapaCache.get(t.imagemUrl);
        if (capa != null) {
            Shape clipAntigo = g.getClip();
            Object interpAntiga = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.clip(new RoundRectangle2D.Float(x, y, size, size, arc, arc));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(capa, x, y, size, size, null);
            g.setClip(clipAntigo);
            if (interpAntiga != null)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpAntiga);
            return;
        }
        float h = (Math.abs((t.title + t.artist).hashCode()) % 360) / 360f;
        Shape box = new RoundRectangle2D.Float(x, y, size, size, arc, arc);
        g.setPaint(new GradientPaint(x, y, Color.getHSBColor(h, 0.55f, 1f),
                x + size, y + size, Color.getHSBColor((h + 0.14f) % 1f, 0.80f, 0.42f)));
        g.fill(box);
        g.setColor(new Color(255, 255, 255, 38));
        fillIn(g, box, new Ellipse2D.Float(x + size * 0.30f, y + size * 0.28f, size * 0.95f, size * 0.95f));
        g.setColor(new Color(0, 0, 0, 40));
        fillIn(g, box, new Ellipse2D.Float(x - size * 0.35f, y - size * 0.55f, size * 0.9f, size * 0.9f));
        g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 70), x, y + size * 0.55f,
                new Color(255, 255, 255, 0)));
        fillIn(g, box, new Rectangle2D.Float(x, y, size, size * 0.55f));
        g.setColor(new Color(255, 255, 255, 225));
        g.setFont(font(Font.BOLD, (int) (size * 0.42)));
        FontMetrics fm = g.getFontMetrics();
        String s = t.title.substring(0, 1).toUpperCase();
        g.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
    }

    static Shape heart(double x, double y, double s) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + s * 0.5, y + s * 0.9);
        p.curveTo(x - s * 0.15, y + s * 0.4, x + s * 0.15, y - s * 0.1, x + s * 0.5, y + s * 0.25);
        p.curveTo(x + s * 0.85, y - s * 0.1, x + s * 1.15, y + s * 0.4, x + s * 0.5, y + s * 0.9);
        p.closePath();
        return p;
    }

    // ===================== HELPERS DE UI =====================
    interface Refreshable {
        void refresh();
    }

    enum Kind {
        PRIMARY, GLASS, GHOST
    }

    static JLabel label(String t, int size, boolean bold, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(c);
        return l;
    }

    /**
     * Exibe uma logo PNG com a largura pedida (mantém a proporção). Sem imagem,
     * mostra o nome em texto.
     */
    static class LogoImage extends JComponent {
        final BufferedImage img;
        final int w, h;
        BufferedImage cache;

        LogoImage(BufferedImage img, int width) {
            this.img = img;
            this.w = width;
            this.h = img == null ? Math.max(20, width / 4)
                    : Math.max(1, (int) Math.round(width * (double) img.getHeight() / img.getWidth()));
            Dimension d = new Dimension(w, h);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            if (img == null) {
                g.setColor(Color.WHITE);
                g.setFont(font(Font.BOLD, (int) (h * 0.7)));
                g.drawString("escutaí", 0, (int) (h * 0.75));
            } else {
                // renderiza já na resolução real da tela (nítido em telas com escala 125%/150%)
                double sx = Math.max(1.0, g.getTransform().getScaleX());
                int tw = (int) Math.round(w * sx), th = (int) Math.round(h * sx);
                if (cache == null || cache.getWidth() != tw)
                    cache = scaleSmooth(img, tw, th);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(cache, 0, 0, w, h, null);
            }
            g.dispose();
        }
    }

    /**
     * Centraliza um componente numa linha (dentro de containers com BoxLayout
     * vertical).
     */
    static JPanel centerRow(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.add(c);
        left(p);
        p.setMaximumSize(new Dimension(MAX, p.getPreferredSize().height));
        return p;
    }

    static JPanel vbox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    static JPanel flow(int gap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        return p;
    }

    static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    static Component gap(int h) {
        return Box.createVerticalStrut(h);
    }

    static GlassPanel glassBox(int arc, int pad) {
        GlassPanel p = new GlassPanel(arc);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(pad, pad, pad, pad));
        return p;
    }

    static JComponent labeled(String t, JComponent f) {
        JPanel p = vbox();
        p.add(left(label(t, 12, false, MUTED)));
        p.add(gap(5));
        left(f);
        f.setMaximumSize(new Dimension(MAX, 42));
        p.add(f);
        left(p);
        return p;
    }

    static void fixHeight(JComponent c) {
        left(c);
        c.setMaximumSize(new Dimension(MAX, c.getPreferredSize().height));
    }

    static void addRow(JPanel list, JComponent row) {
        fixHeight(row);
        list.add(row);
        list.add(gap(10));
    }

    static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }

    static void styleBar(JScrollBar sb) {
        sb.setUI(new GlassScrollBarUI());
        sb.setOpaque(false);
        sb.setPreferredSize(new Dimension(12, 0));
    }

    static JScrollPane scroll(JComponent content) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // necessário p/ transparência
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleBar(sp.getVerticalScrollBar());
        return sp;
    }

    // ----- campos de texto de vidro -----
    static void initField(JTextComponent f) {
        f.setOpaque(false);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setSelectionColor(new Color(59, 130, 246, 160));
        f.setSelectedTextColor(Color.WHITE);
        f.setFont(font(Font.PLAIN, 14));
        f.setBorder(new EmptyBorder(10, 16, 10, 16));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.repaint();
            }

            public void focusLost(FocusEvent e) {
                f.repaint();
            }
        });
    }

    static void paintFieldBg(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        aa(g);
        int w = c.getWidth(), h = c.getHeight();
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 20, 20);
        g.setColor(new Color(255, 255, 255, 20));
        g.fill(r);
        boolean f = c.hasFocus();
        g.setColor(f ? new Color(108, 180, 255, 210) : new Color(255, 255, 255, 64));
        g.setStroke(new BasicStroke(f ? 1.6f : 1f));
        g.draw(r);
        g.dispose();
    }

    static void paintPlaceholder(Graphics g0, JTextComponent c, String ph) {
        if (ph == null || ph.isEmpty() || !c.getText().isEmpty())
            return;
        Graphics2D g = (Graphics2D) g0.create();
        aa(g);
        g.setColor(new Color(255, 255, 255, 105));
        g.setFont(c.getFont());
        Insets in = c.getInsets();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(ph, in.left, (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
    }

    static class GlassField extends JTextField {
        final String placeholder;

        GlassField(String placeholder) {
            this.placeholder = placeholder;
            initField(this);
        }

        GlassField() {
            this("");
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintFieldBg(g, this);
            super.paintComponent(g);
            paintPlaceholder(g, this, placeholder);
        }
    }

    static class GlassPassword extends JPasswordField {
        GlassPassword() {
            initField(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintFieldBg(g, this);
            super.paintComponent(g);
        }
    }

    static class GlassArea extends JTextArea {
        GlassArea(String text, int rows, int cols) {
            super(text, rows, cols);
            initField(this);
            setBorder(new EmptyBorder(12, 16, 12, 16));
            setLineWrap(true);
            setWrapStyleWord(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintFieldBg(g, this);
            super.paintComponent(g);
        }
    }

    // ===================== COMPONENTES CUSTOM =====================

    /** Painel de vidro translúcido (os filhos devem ser não-opacos). */
    static class GlassPanel extends JPanel {
        int arc;
        float strength = 1f;

        GlassPanel(int arc) {
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            glass(g, 0, 0, getWidth(), getHeight(), arc, strength);
            g.dispose();
        }
    }

    static class GlassButton extends JButton {
        Kind kind;

        GlassButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(font(Font.BOLD, 13));
            setForeground(TEXT);
            setBorder(new EmptyBorder(10, 20, 10, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setKind(Kind k) {
            kind = k;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            boolean over = getModel().isRollover(), down = getModel().isPressed();
            switch (kind) {
                case PRIMARY: {
                    RoundRectangle2D r = new RoundRectangle2D.Float(0, 0, w, h, h, h);
                    int d = down ? -25 : (over ? 25 : 0);
                    g.setPaint(new GradientPaint(0, 0, shift(new Color(0xA77BE8), d), w, h,
                            shift(new Color(0x7044B5), d)));
                    g.fill(r);
                    g.setPaint(new GradientPaint(0, 0, white(90), 0, h * 0.55f, white(0)));
                    fillIn(g, r, new Rectangle2D.Float(0, 0, w, h * 0.55f));
                    break;
                }
                case GLASS:
                    glass(g, 0, 0, w, h, h, down ? 0.8f : (over ? 1.9f : 1.15f));
                    break;
                default:
                    if (over)
                        glass(g, 0, 0, w, h, h, down ? 0.6f : 1.0f);
            }
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(kind == Kind.PRIMARY ? Color.WHITE : TEXT);
            String s = getText();
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }

        static Color shift(Color c, int d) {
            return new Color(Math.max(0, Math.min(255, c.getRed() + d)),
                    Math.max(0, Math.min(255, c.getGreen() + d)),
                    Math.max(0, Math.min(255, c.getBlue() + d)));
        }
    }

    static class GlassScrollBarUI extends BasicScrollBarUI {
        JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d);
            b.setMinimumSize(d);
            b.setMaximumSize(d);
            return b;
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return zero();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty())
                return;
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(white(isDragging ? 120 : 70));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    /**
     * Estrelas de 1 a 5: clicável (editable) ou somente exibição (aceita média
     * fracionada).
     */
    static class StarRating extends JComponent {
        double value;
        int hover = 0;
        final int size, gap;
        final boolean editable;
        Runnable onChange;

        StarRating(double value, int size, boolean editable) {
            this.value = value;
            this.size = size;
            this.editable = editable;
            this.gap = Math.max(2, size / 5);
            Dimension d = new Dimension(5 * size + 4 * gap, size + 1);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            if (editable) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        hover = at(e.getX());
                        repaint();
                    }
                });
                addMouseListener(new MouseAdapter() {
                    public void mouseExited(MouseEvent e) {
                        hover = 0;
                        repaint();
                    }

                    public void mouseClicked(MouseEvent e) {
                        StarRating.this.value = at(e.getX());
                        repaint();
                        if (onChange != null)
                            onChange.run();
                    }
                });
            }
        }

        int at(int x) {
            return Math.max(1, Math.min(5, x / (size + gap) + 1));
        }

        int getStars() {
            return (int) value;
        }

        static Path2D star(double cx, double cy, double r) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double rad = (i % 2 == 0) ? r : r * 0.45;
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double x = cx + rad * Math.cos(a), y = cy + rad * Math.sin(a);
                if (i == 0)
                    p.moveTo(x, y);
                else
                    p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }

        static void drawStars(Graphics2D g, double v, int x0, int y0, int size, int gap) {
            for (int i = 0; i < 5; i++) {
                int x = x0 + i * (size + gap);
                Shape s = star(x + size / 2.0, y0 + size / 2.0 + 0.5, size / 2.0);
                g.setColor(new Color(255, 255, 255, 48));
                g.fill(s);
                double f = Math.max(0, Math.min(1, v - i));
                if (f > 0) {
                    Shape old = g.getClip();
                    g.clipRect(x, y0, (int) Math.ceil(size * f), size + 1);
                    g.setColor(STAR);
                    g.fill(s);
                    g.setClip(old);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            drawStars(g, hover > 0 ? hover : value, 0, 0, size, gap);
            g.dispose();
        }
    }

    /**
     * Estrelas grandes para avaliar: a estrela sob o mouse cresce, e ao clicar uma
     * "onda" dourada com brilho percorre as estrelas escolhidas.
     */
    static class BigStars extends JComponent {
        static final int PAD = 14, POP_MS = 480;
        final int size, gap;
        int value, hover;
        Runnable onChange;
        final double[] scale = { 1, 1, 1, 1, 1 };
        final long[] popStart = { -1, -1, -1, -1, -1 };
        final javax.swing.Timer anim = new javax.swing.Timer(15, e -> step());

        BigStars(int value, int size) {
            this.value = value;
            this.size = size;
            this.gap = size / 4;
            Dimension d = new Dimension(5 * size + 4 * gap + 2 * PAD, size + 2 * PAD);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    hover = at(e.getX());
                    anim.start();
                }
            });
            addMouseListener(new MouseAdapter() {
                public void mouseExited(MouseEvent e) {
                    hover = 0;
                    anim.start();
                }

                public void mouseClicked(MouseEvent e) {
                    BigStars.this.value = at(e.getX());
                    long now = System.currentTimeMillis();
                    for (int i = 0; i < 5; i++)
                        popStart[i] = i < BigStars.this.value ? now + i * 70L : -1;
                    anim.start();
                    if (onChange != null)
                        onChange.run();
                }
            });
        }

        int at(int x) {
            return Math.max(1, Math.min(5, (x - PAD) / (size + gap) + 1));
        }

        void step() {
            long now = System.currentTimeMillis();
            boolean busy = false;
            for (int i = 0; i < 5; i++) {
                double target = hover > 0 ? (i == hover - 1 ? 1.28 : (i < hover ? 1.1 : 1.0)) : 1.0;
                scale[i] += (target - scale[i]) * 0.3;
                if (Math.abs(target - scale[i]) > 0.004)
                    busy = true;
                else
                    scale[i] = target;
                if (popStart[i] >= 0) {
                    if (now - popStart[i] < POP_MS)
                        busy = true;
                    else
                        popStart[i] = -1;
                }
            }
            repaint();
            if (!busy)
                anim.stop();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int shown = hover > 0 ? hover : value;
            long now = System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                double cx = PAD + i * (size + gap) + size / 2.0, cy = PAD + size / 2.0;
                double t = popStart[i] >= 0 ? (now - popStart[i]) / (double) POP_MS : -1;
                double pulse = (t > 0 && t < 1) ? Math.sin(Math.PI * t) : 0;
                double s = scale[i] + 0.4 * pulse;
                Shape star = StarRating.star(cx, cy, size / 2.0 * s);
                if (i < shown) {
                    for (int k = 3; k >= 1; k--) {
                        g.setColor(new Color(255, 200, 61, 26 + (int) (50 * pulse)));
                        g.setStroke(new BasicStroke(k * 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.draw(star);
                    }
                    g.setPaint(new GradientPaint(0, (float) (cy - size / 2.0), new Color(0xFFE493),
                            0, (float) (cy + size / 2.0), STAR));
                    g.fill(star);
                } else {
                    g.setColor(white(38));
                    g.fill(star);
                    g.setColor(white(80));
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(star);
                }
            }
            g.dispose();
        }
    }

    /** Botão de coração: contorno = não favoritado, preenchido = favoritado. Anima ao clicar. */
    static class HeartButton extends JComponent {
        static final int BOX = 48, PAD = 10, ANIM_MS = 560;
        boolean on, over;
        double scale = 1;
        long popStart = -1;
        Runnable onToggle;
        final javax.swing.Timer anim = new javax.swing.Timer(15, e -> step());

        HeartButton(boolean on) {
            this.on = on;
            Dimension d = new Dimension(BOX + 2 * PAD, BOX + 2 * PAD);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(on ? "Remover dos favoritos" : "Favoritar");
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    anim.start();
                }

                public void mouseExited(MouseEvent e) {
                    over = false;
                    anim.start();
                }

                public void mouseClicked(MouseEvent e) {
                    HeartButton.this.on = !HeartButton.this.on;
                    setToolTipText(HeartButton.this.on ? "Remover dos favoritos" : "Favoritar");
                    popStart = System.currentTimeMillis();
                    anim.start();
                    if (onToggle != null)
                        onToggle.run();
                }
            });
        }

        void step() {
            double target = over ? 1.1 : 1.0;
            scale += (target - scale) * 0.3;
            boolean busy = Math.abs(target - scale) > 0.004;
            if (!busy)
                scale = target;
            if (popStart >= 0) {
                if (System.currentTimeMillis() - popStart < ANIM_MS)
                    busy = true;
                else
                    popStart = -1;
            }
            repaint();
            if (!busy)
                anim.stop();
        }

        /** Coração desenhado com um quadrado girado + dois círculos. */
        static Shape heart(double cx, double cy, double a) {
            double d = a / Math.sqrt(2);
            Path2D sq = new Path2D.Double();
            sq.moveTo(cx, cy - d);
            sq.lineTo(cx + d, cy);
            sq.lineTo(cx, cy + d);
            sq.lineTo(cx - d, cy);
            sq.closePath();
            Area area = new Area(sq);
            double r = a / 2;
            area.add(new Area(new Ellipse2D.Double(cx - d / 2 - r, cy - d / 2 - r, 2 * r, 2 * r)));
            area.add(new Area(new Ellipse2D.Double(cx + d / 2 - r, cy - d / 2 - r, 2 * r, 2 * r)));
            return area;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
            double t = popStart >= 0 ? (System.currentTimeMillis() - popStart) / (double) ANIM_MS : -1;
            boolean animating = t >= 0 && t < 1;
            double pulse = animating ? Math.sin(Math.PI * t) : 0;

            glass(g, PAD, PAD, BOX, BOX, BOX, over ? 1.9f : 1.15f);
            if (on) {
                g.setColor(new Color(255, 77, 121, 38));
                g.fill(new Ellipse2D.Double(PAD, PAD, BOX, BOX));
            }

            double s = scale * (on ? 1 + 0.45 * pulse : 1 - 0.15 * pulse);
            Shape h = heart(cx, cy + 2, BOX * 0.34 * s);
            if (on) {
                for (int k = 3; k >= 1; k--) {
                    g.setColor(new Color(255, 77, 121, 22 + (int) (40 * pulse)));
                    g.setStroke(new BasicStroke(k * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(h);
                }
                g.setPaint(new GradientPaint(0, (float) (cy - BOX * 0.3), new Color(0xFF8FB0),
                        0, (float) (cy + BOX * 0.3), new Color(0xE63468)));
                g.fill(h);
            } else {
                g.setColor(white(225));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(h);
            }

            if (animating && on) {
                int alpha = (int) (255 * (1 - t));
                double ring = BOX * 0.38 + t * BOX * 0.42;
                g.setColor(new Color(255, 77, 121, Math.max(0, alpha / 2)));
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(cx - ring, cy - ring, 2 * ring, 2 * ring));
                for (int k = 0; k < 8; k++) {
                    double ang = k * Math.PI / 4 + 0.4;
                    double rr = BOX * 0.5 + t * BOX * 0.4, dot = 1 + 3 * (1 - t);
                    g.setColor(new Color(255, 120, 160, Math.max(0, alpha)));
                    g.fill(new Ellipse2D.Double(cx + rr * Math.cos(ang) - dot, cy + rr * Math.sin(ang) - dot,
                            2 * dot, 2 * dot));
                }
            }
            g.dispose();
        }
    }

    static class Cover extends JComponent {
        final Track t;
        final int size;

        Cover(Track t, int size) {
            this.t = t;
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            paintCover(g, t, 0, 0, size, size / 7);
            g.dispose();
        }
    }

    /**
     * Baixa capas em segundo plano (nunca na thread da interface) e guarda em cache.
     * get() devolve a imagem se já estiver pronta; senão devolve null, inicia o
     * download e, quando termina, manda a janela se redesenhar.
     */
    static class CapaCache {
        private static final int LADO = 300;
        private static final Map<String, BufferedImage> PRONTAS = new ConcurrentHashMap<>();
        private static final Set<String> EM_ANDAMENTO = ConcurrentHashMap.newKeySet();
        private static final Set<String> FALHAS = ConcurrentHashMap.newKeySet();
        private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
            Thread th = new Thread(r, "capas-itunes");
            th.setDaemon(true);
            return th;
        });

        static BufferedImage get(String url) {
            if (url == null || url.isEmpty())
                return null;
            BufferedImage pronta = PRONTAS.get(url);
            if (pronta != null)
                return pronta;
            if (FALHAS.contains(url) || !EM_ANDAMENTO.add(url))
                return null;
            POOL.execute(() -> {
                try {
                    BufferedImage original = ImageIO.read(URI.create(url).toURL());
                    if (original != null)
                        PRONTAS.put(url, reduzir(original));
                    else
                        FALHAS.add(url);
                } catch (Exception e) {
                    FALHAS.add(url);
                    System.out.println("Não foi possível carregar a capa: " + url);
                } finally {
                    EM_ANDAMENTO.remove(url);
                    SwingUtilities.invokeLater(() -> {
                        for (Window w : Window.getWindows())
                            w.repaint();
                    });
                }
            });
            return null;
        }

        /**
         * Busca no iTunes a capa de cada música/álbum do catálogo de exemplo (Store),
         * em segundo plano e uma por vez (o iTunes limita ~20 consultas por minuto).
         * As capas vão aparecendo na tela conforme chegam. Não mexe na busca da tela.
         */
        static void carregarCapasDoCatalogo() {
            // copia da lista feita aqui (thread da interface) e não dentro da thread
            Set<Track> ordem = new LinkedHashSet<>(Store.featured());
            ordem.addAll(Store.tracks);
            Thread th = new Thread(() -> {
                ItunesService servico = new ItunesService();
                for (Track t : ordem) {
                    if (t.imagemUrl != null)
                        continue;
                    for (int tentativa = 0; tentativa < 2 && t.imagemUrl == null; tentativa++) {
                        try {
                            for (Musica m : servico.buscarMusicas(t.title + " " + t.artist)) {
                                if (m.getImagemUrl() != null) {
                                    t.imagemUrl = m.getImagemUrl();
                                    break;
                                }
                            }
                            break; // consulta funcionou (com ou sem resultado)
                        } catch (Exception e) {
                            System.out.println("Falha ao buscar capa de \"" + t.title + "\": " + e.getMessage());
                            sleep(10000); // provável limite de consultas: espera e tenta de novo
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        for (Window w : Window.getWindows())
                            w.repaint();
                    });
                    sleep(3200);
                }
            }, "capas-catalogo");
            th.setDaemon(true);
            th.start();
        }

        private static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
        }

        /** Reduz para 300x300 uma vez só, para ficar nítida ao ser desenhada pequena. */
        private static BufferedImage reduzir(BufferedImage img) {
            Image suave = img.getScaledInstance(LADO, LADO, Image.SCALE_SMOOTH);
            BufferedImage out = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(suave, 0, 0, null);
            g.dispose();
            return out;
        }
    }

    /** Capa avulsa vinda de uma URL. Agora assíncrona: não trava mais a interface. */
    static class CapaItunes extends JComponent {

        private final String imagemUrl;
        private final int tamanho;

        CapaItunes(String imagemUrl, int tamanho) {
            this.imagemUrl = imagemUrl;
            this.tamanho = tamanho;
            Dimension d = new Dimension(tamanho, tamanho);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            BufferedImage imagem = CapaCache.get(imagemUrl);
            if (imagem != null) {
                g.clip(new RoundRectangle2D.Float(0, 0, tamanho, tamanho, 14, 14));
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(imagem, 0, 0, tamanho, tamanho, this);
            }
            g.dispose();
        }
    }

    static class Avatar extends JComponent {
        final String name;
        final int size;

        Avatar(String name, int size) {
            this.name = name;
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setPaint(new GradientPaint(0, 0, ACCENT, size, size, ACCENT2));
            g.fillOval(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.setFont(font(Font.BOLD, size / 2));
            String s = name.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, (size - fm.stringWidth(s)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /**
     * Fundo animado: "blobs" de cor desfocados movendo-se devagar (desenhado em
     * baixa resolução e ampliado).
     */
    static class Backdrop extends JPanel {
        BufferedImage buf;
        final long start = System.nanoTime();

        Backdrop() {
            super(new BorderLayout());
            setOpaque(true);
            new javax.swing.Timer(60, e -> {
                if (isShowing())
                    repaint();
            }).start();
        }

        static void blob(Graphics2D g, int w, int h, double cx, double cy, double rad, Color c, float alpha) {
            float r = (float) (rad * Math.max(w, h));
            Color in = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255));
            Color out = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
            g.setPaint(new RadialGradientPaint(new Point2D.Float((float) (cx * w), (float) (cy * h)), r,
                    new float[] { 0f, 1f }, new Color[] { in, out }));
            g.fillRect(0, 0, w, h);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            int w = getWidth(), h = getHeight();
            if (w < 8 || h < 8)
                return;
            int sw = Math.max(1, w / 4), sh = Math.max(1, h / 4);
            if (buf == null || buf.getWidth() != sw || buf.getHeight() != sh)
                buf = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            Graphics2D b = buf.createGraphics();
            b.setColor(BASE);
            b.fillRect(0, 0, sw, sh);
            double t = (System.nanoTime() - start) / 1e9;
            blob(b, sw, sh, 0.15 + 0.12 * Math.sin(t * 0.21), 0.20 + 0.15 * Math.cos(t * 0.17), 0.60,
                    new Color(0x6846A0), 0.55f * GLOW);
            blob(b, sw, sh, 0.85 + 0.10 * Math.cos(t * 0.19), 0.30 + 0.15 * Math.sin(t * 0.23), 0.55,
                    new Color(0x9A70D6), 0.45f * GLOW);
            blob(b, sw, sh, 0.55 + 0.15 * Math.sin(t * 0.15), 0.95 + 0.06 * Math.cos(t * 0.27), 0.60,
                    new Color(0x49336F), 0.40f * GLOW);
            b.dispose();
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(buf, 0, 0, w, h, null);
            g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 70)));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    // ----- Carrossel: fileira horizontal com setas -----
    static class RoundArrow extends JComponent {
        final int dir;
        boolean hover;
        Runnable action;

        RoundArrow(int dir) {
            this.dir = dir;
            Dimension d = new Dimension(38, 38);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                public void mouseClicked(MouseEvent e) {
                    if (isEnabled() && action != null)
                        action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            boolean en = isEnabled();
            glass(g, 0, 0, w, h, w, en ? (hover ? 2.1f : 1.2f) : 0.5f);
            g.setColor(white(en ? 235 : 70));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = w / 2, cy = h / 2;
            g.drawLine(cx - dir * 3, cy - 6, cx + dir * 3, cy);
            g.drawLine(cx + dir * 3, cy, cx - dir * 3, cy + 6);
            g.dispose();
        }
    }

    static class Strip extends JPanel {
        static final int GAP = 16;
        double offset = 0, target = 0;
        Runnable onMove;
        final javax.swing.Timer anim = new javax.swing.Timer(14, e -> step());

        Strip() {
            super(null);
            setOpaque(false);
        }

        int contentW() {
            int w = 0;
            for (Component c : getComponents())
                w += c.getPreferredSize().width + GAP;
            return Math.max(0, w - GAP);
        }

        int maxOffset() {
            return Math.max(0, contentW() - getWidth());
        }

        @Override
        public Dimension getPreferredSize() {
            int h = 0;
            for (Component c : getComponents())
                h = Math.max(h, c.getPreferredSize().height);
            return new Dimension(200, h);
        }

        @Override
        public void doLayout() {
            offset = Math.min(offset, maxOffset());
            target = Math.min(target, maxOffset());
            int x = -(int) Math.round(offset);
            for (Component c : getComponents()) {
                Dimension d = c.getPreferredSize();
                c.setBounds(x, 0, d.width, d.height);
                x += d.width + GAP;
            }
            if (onMove != null)
                onMove.run();
        }

        void scrollBy(int dir) {
            target = Math.max(0, Math.min(maxOffset(), target + dir * Math.max(220, getWidth() * 0.8)));
            if (onMove != null)
                onMove.run();
            anim.start();
        }

        void step() {
            double d = target - offset;
            if (Math.abs(d) < 0.6) {
                offset = target;
                anim.stop();
            } else
                offset += d * 0.18;
            doLayout();
            repaint();
        }
    }

    static class Carousel extends JPanel {
        final Strip strip = new Strip();
        final RoundArrow prev = new RoundArrow(-1), next = new RoundArrow(1);

        Carousel(String title, String sub, List<? extends JComponent> cards) {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            JPanel t = vbox();
            t.add(left(label(title, 20, true, TEXT)));
            if (sub != null)
                t.add(left(label(sub, 12, false, MUTED)));
            head.add(t, BorderLayout.WEST);
            JPanel arrows = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            arrows.setOpaque(false);
            arrows.add(prev);
            arrows.add(next);
            head.add(arrows, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            add(strip, BorderLayout.CENTER);
            for (JComponent c : cards)
                strip.add(c);
            prev.action = () -> strip.scrollBy(-1);
            next.action = () -> strip.scrollBy(1);
            strip.onMove = this::updateArrows;
        }

        void updateArrows() {
            prev.setEnabled(strip.target > 1);
            next.setEnabled(strip.target < strip.maxOffset() - 1);
            prev.repaint();
            next.repaint();
        }
    }

    // ===================== JANELA PRINCIPAL =====================
    final CardLayout cards = new CardLayout();
    final JPanel body = new JPanel(cards);
    final Map<String, Refreshable> screens = new HashMap<>();
    final Map<String, GlassButton> tabs = new LinkedHashMap<>();
    JPanel nav;
    JPanel navTabs;
    GlassButton navOut;
    Backdrop root;
    TrackPanel trackPanel;

    Escutai() {
        super("ESCUTAÍ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 800);
        setMinimumSize(new Dimension(1020, 660));
        setLocationRelativeTo(null);
        setIconImages(Arrays.asList(appIcon(256), appIcon(64), appIcon(32)));

        root = new Backdrop();
        setContentPane(root);
        body.setOpaque(false);
        nav = buildNav();
        root.add(nav, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        trackPanel = new TrackPanel();
        register("login", new LoginPanel());
        register("register", new RegisterPanel());
        register("home", new HomePanel());
        register("track", trackPanel);
        register("profile", new ProfilePanel());
        register("favorites", new FavoritesPanel());

        go("login");
    }

    <T extends JPanel & Refreshable> void register(String name, T p) {
        p.setOpaque(false);
        body.add(p, name);
        screens.put(name, p);
    }

    void go(String name) {
        boolean logged = Store.current != null;
        nav.setVisible(logged); // barra discreta aparece somente após o login
        navTabs.setVisible(logged);
        navOut.setVisible(logged);
        String tab = name.equals("track") ? "home" : name;
        tabs.forEach((k, b) -> b.setKind(k.equals(tab) ? Kind.GLASS : Kind.GHOST));
        screens.get(name).refresh();
        cards.show(body, name);
        root.revalidate();
        root.repaint();
    }

    interface FonteSecao {
        List<LancamentosService.Lancamento> buscar() throws Exception;
    }

    /** Busca as três seções da tela inicial em segundo plano. */
    void carregarHome() {
        carregarSecao(Store.topMundo, "Top Mundo", false, () -> new ChartsService().topMundo(12));
        carregarSecao(Store.topBrasil, "Top Brasil", false, () -> new ChartsService().topBrasil(12));
        carregarSecao(Store.lancamentos, "Lançamentos", true, () -> new LancamentosService().buscarLancamentos(12));
    }

    void carregarSecao(Store.Secao secao, String genero, boolean album, FonteSecao fonte) {
        new SwingWorker<List<LancamentosService.Lancamento>, Void>() {
            @Override
            protected List<LancamentosService.Lancamento> doInBackground() throws Exception {
                return fonte.buscar();
            }

            @Override
            protected void done() {
                try {
                    for (LancamentosService.Lancamento l : get()) {
                        String chave = (album ? "album:" : "musica:") + l.getId();
                        Track t = Store.porApple.get(chave);
                        if (t == null) {
                            t = new Track(Store.tracks.size() + 1, genero, l.getTitulo(), l.getArtista(),
                                    album ? l.getTitulo() : "Apple Music", l.getData().getYear(), album);
                            t.imagemUrl = l.getImagemUrl();
                            Store.tracks.add(t);
                            Store.porApple.put(chave, t);
                        }
                        secao.itens.add(t);
                    }
                    secao.status = secao.itens.isEmpty() ? "vazio" : "ok";
                } catch (Exception e) {
                    secao.status = "erro";
                    secao.erro = e.getClass().getSimpleName() + ": " + e.getMessage();
                    System.out.println("Falha ao carregar \"" + secao.titulo + "\": " + e.getMessage());
                }
                screens.get("home").refresh();
            }
        }.execute();
    }

    void openTrack(Track t) {
        trackPanel.setTrack(t);
        go("track");
    }

    void addTab(JPanel p, String key, String text) {
        GlassButton b = new GlassButton(text, Kind.GHOST);
        b.addActionListener(e -> go(key));
        tabs.put(key, b);
        p.add(b);
    }

    JPanel buildNav() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(8, 28, 0, 28));

        // Barra superior compacta, somente com os atalhos de navegação.
        GlassPanel bar = new GlassPanel(24);
        bar.setLayout(new BorderLayout(8, 0));
        bar.setBorder(new EmptyBorder(2, 10, 2, 8));

        navTabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        navTabs.setOpaque(false);
        addTab(navTabs, "home", "Explorar");
        addTab(navTabs, "favorites", "Favoritos");
        addTab(navTabs, "profile", "Perfil");
        bar.add(navTabs, BorderLayout.CENTER);

        navOut = new GlassButton("Sair", Kind.GHOST);
        navOut.addActionListener(e -> {
            Store.current = null;
            go("login");
        });
        bar.add(navOut, BorderLayout.EAST);

        wrap.add(bar, BorderLayout.CENTER);
        return wrap;
    }

    // ----- Card de álbum/música (usado nos carrosséis e grades) -----
    class TrackCard extends JComponent {
        final Track t;
        final Runnable onFav;
        boolean hover;

        TrackCard(Track t, Runnable onFav) {
            this.t = t;
            this.onFav = onFav;
            Dimension d = new Dimension(CARD_W, CARD_H);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                public void mouseClicked(MouseEvent e) {
                    if (heartZone(e.getPoint())) {
                        User u = Store.current;
                        if (u == null)
                            return;
                        if (!u.favorites.remove(t.id))
                            u.favorites.add(t.id);
                        repaint();
                        if (onFav != null)
                            onFav.run();
                    } else {
                        openTrack(t);
                    }
                }
            });
        }

        boolean heartZone(Point p) {
            int bx = getWidth() - 12 - 30 - 8;
            return p.x >= bx && p.x <= bx + 30 && p.y >= 20 && p.y <= 50;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            glass(g, 0, 0, w, h, 26, hover ? 1.8f : 1f);
            if (hover) {
                g.setColor(new Color(108, 180, 255, 170));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.75f, 0.75f, w - 1.5f, h - 1.5f, 26, 26));
            }
            paintCover(g, t, 12, 12, w - 24, 18);

            boolean fav = Store.current != null && Store.current.favorites.contains(t.id);
            int bx = w - 12 - 30 - 8, by = 20;
            g.setColor(new Color(0, 0, 0, (hover || fav) ? 120 : 80));
            g.fillOval(bx, by, 30, 30);
            Shape hp = heart(bx + 7, by + 7, 16);
            if (fav) {
                g.setColor(HEART);
                g.fill(hp);
            } else {
                g.setColor(white(225));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(hp);
            }

            int ty = 12 + (w - 24) + 22;
            g.setFont(font(Font.BOLD, 13));
            g.setColor(TEXT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ellipsize(t.title, fm, w - 28), 14, ty);
            g.setFont(font(Font.PLAIN, 11));
            g.setColor(MUTED);
            fm = g.getFontMetrics();
            g.drawString(ellipsize(t.artist, fm, w - 28), 14, ty + 16);

            double avg = Store.avg(t.id);
            int n = Store.of(t.id).size();
            int sy = ty + 27;
            StarRating.drawStars(g, avg, 14, sy, 12, 3);
            g.setColor(MUTED);
            g.drawString(n == 0 ? "novo" : String.format("%.1f (%d)", avg, n), 14 + 72 + 8, sy + 10);
            g.dispose();
        }
    }

    JPanel grid(List<Track> list, Runnable onFav) {
        JPanel g = new JPanel(new GridLayout(0, 5, 16, 16));
        g.setOpaque(false);
        for (Track t : list) {
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            cell.setOpaque(false);
            cell.add(new TrackCard(t, onFav));
            g.add(cell);
        }
        return g;
    }

    // Linha de resenha reutilizável (página da música / perfil)
    JPanel reviewRow(Review r, boolean showTrack) {
        GlassPanel row = new GlassPanel(22);
        row.setLayout(new BorderLayout());
        row.setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel col = vbox();
        JPanel head = flow(10);
        if (showTrack) {
            Track t = Store.track(r.trackId);
            head.add(label(t.title + " — " + t.artist, 14, true, TEXT));
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    openTrack(t);
                }

                public void mouseEntered(MouseEvent e) {
                    row.strength = 1.6f;
                    row.repaint();
                }

                public void mouseExited(MouseEvent e) {
                    row.strength = 1f;
                    row.repaint();
                }
            });
        } else {
            head.add(label("@" + r.username, 14, true, ACCENT));
        }
        head.add(new StarRating(r.stars, 14, false));
        head.add(label(r.date.format(FMT), 12, false, MUTED));
        col.add(left(head));
        if (r.text != null && !r.text.isBlank()) {
            col.add(gap(6));
            JTextArea ta = new JTextArea(r.text, 1, 50);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setEditable(false);
            ta.setOpaque(false);
            ta.setForeground(TEXT);
            ta.setBorder(null);
            ta.setFont(font(Font.PLAIN, 13));
            ta.setFocusable(false);
            col.add(left(ta));
        }
        row.add(col, BorderLayout.CENTER);
        return row;
    }

    // ===================== TELAS =====================

    static JPanel centered(GlassPanel card) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        card.setBorder(new EmptyBorder(34, 40, 34, 40));
        p.add(card);
        return p;
    }

    class LoginPanel extends JPanel implements Refreshable {
        final GlassField user = new GlassField("seu usuário");
        final GlassPassword pass = new GlassPassword();
        final JLabel msg = label(" ", 12, false, ERR);

        LoginPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);

            GlassPanel card = glassBox(36, 0);
            card.setBorder(new EmptyBorder(38, 42, 38, 42));

            // Marca completa centralizada no cartão de login.
            card.add(centerRow(new LogoImage(LOGO_PHONES, 190)));
            card.add(gap(12));
            card.add(centerRow(label("Sua música. Sua opinião.", 19, true, TEXT)));
            card.add(centerRow(label("Avalie, resenhe e guarde suas músicas favoritas.", 12, false, MUTED)));
            card.add(gap(28));

            card.add(left(label("Bem-vindo de volta", 24, true, TEXT)));
            card.add(gap(18));
            card.add(labeled("Usuário", user));
            card.add(gap(14));
            card.add(labeled("Senha", pass));
            card.add(gap(8));
            card.add(left(msg));
            card.add(gap(12));

            GlassButton in = left(new GlassButton("Entrar", Kind.PRIMARY));
            in.setMaximumSize(new Dimension(MAX, 46));
            GlassButton reg = left(new GlassButton("Criar conta", Kind.GLASS));
            reg.setMaximumSize(new Dimension(MAX, 42));

            card.add(in);
            card.add(gap(10));
            card.add(reg);

            in.addActionListener(e -> doLogin());
            pass.addActionListener(e -> doLogin());
            user.addActionListener(e -> pass.requestFocus());
            reg.addActionListener(e -> go("register"));

            add(centered(card));
        }

        void doLogin() {
            User u = Store.login(user.getText().trim(), new String(pass.getPassword()));
            if (u == null) {
                msg.setText("Usuário ou senha inválidos.");
                return;
            }
            Store.current = u;
            go("home");
        }

        public void refresh() {
            user.setText("");
            pass.setText("");
            msg.setText(" ");
        }
    }

    class RegisterPanel extends JPanel implements Refreshable {
        final GlassField user = new GlassField("ex.: maria_musica");
        final GlassField email = new GlassField("voce@email.com");
        final GlassPassword pass = new GlassPassword();
        final GlassPassword pass2 = new GlassPassword();
        final JLabel msg = label(" ", 12, false, ERR);

        RegisterPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);

            GlassPanel card = glassBox(36, 0);
            card.setBorder(new EmptyBorder(30, 42, 30, 42));

            card.add(centerRow(new LogoImage(LOGO_PHONES, 145)));
            card.add(gap(8));
            card.add(centerRow(label("Criar conta", 24, true, TEXT)));
            card.add(centerRow(label("Junte-se à comunidade e comece a avaliar", 12, false, MUTED)));
            card.add(gap(20));

            card.add(labeled("Nome de usuário", user));
            card.add(gap(10));
            card.add(labeled("E-mail", email));
            card.add(gap(10));
            card.add(labeled("Senha (mín. 6 caracteres)", pass));
            card.add(gap(10));
            card.add(labeled("Confirmar senha", pass2));
            card.add(gap(8));
            card.add(left(msg));
            card.add(gap(8));

            GlassButton ok = left(new GlassButton("Cadastrar", Kind.PRIMARY));
            ok.setMaximumSize(new Dimension(MAX, 42));
            GlassButton back = left(new GlassButton("Já tenho conta", Kind.GLASS));
            back.setMaximumSize(new Dimension(MAX, 42));

            card.add(ok);
            card.add(gap(10));
            card.add(back);

            ok.addActionListener(e -> doRegister());
            pass2.addActionListener(e -> doRegister());
            back.addActionListener(e -> go("login"));

            add(centered(card));
        }

        void doRegister() {
            String u = user.getText().trim(), em = email.getText().trim();
            String p = new String(pass.getPassword()), p2 = new String(pass2.getPassword());
            if (u.length() < 3) {
                msg.setText("O usuário precisa ter ao menos 3 caracteres.");
                return;
            }
            if (!u.matches("[A-Za-z0-9_.]+")) {
                msg.setText("Use apenas letras, números, _ e .");
                return;
            }
            if (!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                msg.setText("E-mail inválido.");
                return;
            }
            if (p.length() < 6) {
                msg.setText("A senha precisa ter ao menos 6 caracteres.");
                return;
            }
            if (!p.equals(p2)) {
                msg.setText("As senhas não conferem.");
                return;
            }
            if (Store.usernameTaken(u)) {
                msg.setText("Este usuário já existe.");
                return;
            }
            if (Store.emailTaken(em)) {
                msg.setText("Este e-mail já está cadastrado.");
                return;
            }
            User nu = new User(u, em, p); // em produção: guardar hash (BCrypt), nunca a senha pura
            Store.users.add(nu);
            Store.current = nu;
            go("home");
        }

        public void refresh() {
            user.setText("");
            email.setText("");
            pass.setText("");
            pass2.setText("");
            msg.setText(" ");
        }
    }

    class HomePanel extends JPanel implements Refreshable {
        final GlassField search = new GlassField("Buscar músicas, álbuns ou artistas");
        final JPanel content = vbox();
        final JLabel hello = label(" ", 30, true, TEXT);
        final ItunesService itunesService = new ItunesService();
        /** Resultados da última busca no iTunes (já convertidos em Track). */
        List<Track> resultadosTracks = new ArrayList<>();
        SwingWorker<List<Musica>, Void> buscaAtual;
        String termoConsultado = "";
        /** "", "buscando", "ok" ou "erro". */
        String statusBusca = "";
        String erroBusca = "";
        /** Espera o usuário parar de digitar antes de consultar o iTunes. */
        final javax.swing.Timer debounce = new javax.swing.Timer(450, e -> buscarNoItunes());

        HomePanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(22, 34, 0, 34));

            JPanel top = new JPanel(new BorderLayout(16, 0));
            top.setOpaque(false);
            top.setBorder(new EmptyBorder(0, 0, 18, 0));
            JPanel titles = vbox();
            titles.add(left(hello));
            titles.add(left(label("O que você está ouvindo hoje?", 13, false, MUTED)));
            top.add(titles, BorderLayout.WEST);
            search.setPreferredSize(new Dimension(340, 44));
            JPanel sr = new JPanel(new GridBagLayout());
            sr.setOpaque(false);
            sr.add(search);
            top.add(sr, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);
            add(scroll(content), BorderLayout.CENTER);
            debounce.setRepeats(false);
            onChange(search, () -> {
                debounce.restart();
                refresh();
            });
        }

        /** Consulta o iTunes com o texto digitado e mostra os resultados quando chegam. */
     void buscarNoItunes() {
            String termo = search.getText().trim();
            if (buscaAtual != null)
                buscaAtual.cancel(true);
            if (termo.length() < 2) {
                termoConsultado = "";
                statusBusca = "";
                erroBusca = "";
                resultadosTracks = new ArrayList<>();
                refresh();
                return;
            }
            if (termo.equalsIgnoreCase(termoConsultado) && !statusBusca.equals("erro"))
                return;
            termoConsultado = termo;
            statusBusca = "buscando";
            erroBusca = "";
            resultadosTracks = new ArrayList<>();
            refresh();

            buscaAtual = new SwingWorker<List<Musica>, Void>() {
                final List<Track> encontrados = new ArrayList<>();

                @Override
                protected List<Musica> doInBackground() throws Exception {
                    ArrayList<ConteudoMusical> conteudos = itunesService.buscarTodos(termo);
                    for (ConteudoMusical c : conteudos) {
                        encontrados.add(paraTrack(c));
                    }
                    return new ArrayList<>();
                }

                @Override
                protected void done() {
                    if (isCancelled() || !termo.equals(search.getText().trim()))
                        return;
                    try {
                        get();
                        resultadosTracks = encontrados;
                        statusBusca = "ok";
                    } catch (Exception e) {
                        Throwable causa = e.getCause() != null ? e.getCause() : e;
                        if (causa instanceof BuscaSemResultadosException) {
                            statusBusca = "vazio";
                            erroBusca = causa.getMessage();
                        } else if (causa instanceof ErroComunicacaoApiException) {
                            statusBusca = "erro";
                            erroBusca = causa.getMessage();
                        } else {
                            statusBusca = "erro";
                            erroBusca = "Erro inesperado: " + causa.getMessage();
                        }
                    }
                    refresh();
                }
            };
            buscaAtual.execute();
        }

        /** Converte um ConteudoMusical (Musica ou Album) do iTunes em Track. */
        Track paraTrack(ConteudoMusical c) {
            boolean ehAlbum = c instanceof Album;
            String chave = (ehAlbum ? "album:" : "musica:") + c.getId();
            Track t = Store.porApple.get(chave);
            if (t == null) {
                t = new Track(Store.tracks.size() + 1, "Busca iTunes", c.getTitulo(), c.getArtista(),
                        ehAlbum ? c.getTitulo() : "Single / Apple Music", 0, ehAlbum);
                t.imagemUrl = c.getImagemUrl();
                t.linkAppleMusic = c.getLinkAppleMusic();
                t.linkSpotify = c.getLinkSpotify();
                if (c instanceof Musica m) {
                    t.duracaoMs = m.getDuracao();
                }
                Store.tracks.add(t);
                Store.porApple.put(chave, t);
            }
            return t;
        }

        String greeting() {
            int h = LocalTime.now().getHour();
            return h < 12 ? "Bom dia" : (h < 18 ? "Boa tarde" : "Boa noite");
        }

        void addSecao(Store.Secao s) {
            if (!s.itens.isEmpty()) {
                addCarousel(s.titulo, s.subtitulo, new ArrayList<>(s.itens));
                return;
            }
            content.add(left(label(s.titulo, 20, true, TEXT)));
            content.add(gap(8));
            content.add(left(label(
                    s.status.equals("carregando") ? "Carregando…" : "Não foi possível carregar agora.",
                    14, false, MUTED)));
            if (!s.erro.isEmpty())
                content.add(left(label(s.erro, 11, false, MUTED)));
            content.add(gap(28));
        }

        void addCarousel(String title, String sub, List<Track> ts) {
            List<JComponent> cs = new ArrayList<>();
            for (Track t : ts)
                cs.add(new TrackCard(t, content::repaint));
            Carousel c = new Carousel(title, sub, cs);
            fixHeight(c);
            content.add(c);
            content.add(gap(28));
        }

        JComponent banner(Track t) {
            GlassPanel b = new GlassPanel(32);
            b.setLayout(new BorderLayout(24, 0));
            b.setBorder(new EmptyBorder(20, 20, 20, 26));
            b.add(new Cover(t, 130), BorderLayout.WEST);
            JPanel info = vbox();
            info.add(Box.createVerticalGlue());
            info.add(left(label("DESTAQUE DA COMUNIDADE", 11, true, ACCENT)));
            info.add(gap(4));
            info.add(left(label(t.title, 28, true, TEXT)));
            info.add(left(label(t.artist + "  ·  " + t.year, 14, false, MUTED)));
            info.add(gap(10));
            JPanel sr = flow(10);
            double avg = Store.avg(t.id);
            int n = Store.of(t.id).size();
            sr.add(new StarRating(avg, 20, false));
            sr.add(label(n == 0 ? "ainda sem avaliações" : String.format("%.1f  ·  %d avaliação(ões)", avg, n), 13,
                    false, TEXT));
            info.add(left(sr));
            info.add(Box.createVerticalGlue());
            b.add(info, BorderLayout.CENTER);
            GlassButton go = new GlassButton("Ver e avaliar", Kind.PRIMARY);
            go.addActionListener(e -> openTrack(t));
            JPanel east = new JPanel(new GridBagLayout());
            east.setOpaque(false);
            east.add(go);
            b.add(east, BorderLayout.EAST);
            return b;
        }

        public void refresh() {
            User me = Store.current;
            if (me == null)
                return;
            hello.setText(greeting() + ", " + me.username);
            content.removeAll();
            String raw = search.getText().trim();
            String q = raw.toLowerCase();
            if (!q.isEmpty()) {
                List<Track> res = new ArrayList<>(resultadosTracks);
                for (Track t : Store.tracks)
                    if (!res.contains(t)
                            && (t.title + " " + t.artist + " " + t.album + " " + t.genre).toLowerCase().contains(q))
                        res.add(t);
                content.add(left(label("Resultados para “" + raw + "”", 20, true, TEXT)));
                content.add(gap(6));
                if (statusBusca.equals("buscando"))
                    content.add(left(label("Buscando no iTunes…", 12, false, MUTED)));
                else if (statusBusca.equals("erro"))
                    content.add(left(label("Não foi possível buscar no iTunes agora (" + erroBusca + ").", 12,
                            false, MUTED)));
                content.add(gap(10));
                if (res.isEmpty()) {
                    if (!statusBusca.equals("buscando"))
                        content.add(left(label("Nada encontrado. Tente outro termo.", 14, false, MUTED)));
                } else
                    content.add(fixed(grid(res, content::repaint)));
            } else {
                addSecao(Store.topMundo);
                addSecao(Store.topBrasil);
                addSecao(Store.lancamentos);
            }
            content.add(gap(20));
            content.revalidate();
            content.repaint();
        }

        JComponent fixed(JComponent c) {
            fixHeight(c);
            return c;
        }
    }

    class TrackPanel extends JPanel implements Refreshable {
        Track track;
        final JPanel content = vbox();

        TrackPanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(18, 34, 0, 34));
            add(scroll(content), BorderLayout.CENTER);
        }

        void setTrack(Track t) {
            track = t;
        }

        public void refresh() {
            content.removeAll();
            if (track == null || Store.current == null)
                return;
            User me = Store.current;
            Review mine = Store.find(me.username, track.id);
            boolean isFav = me.favorites.contains(track.id);

            GlassButton back = left(new GlassButton("‹  Voltar", Kind.GLASS));
            back.addActionListener(e -> go("home"));
            content.add(back);
            content.add(gap(16));

            // Cabeçalho
            GlassPanel head = new GlassPanel(32);
            head.setLayout(new BorderLayout(26, 0));
            head.setBorder(new EmptyBorder(24, 24, 24, 24));
            head.add(new Cover(track, 190), BorderLayout.WEST);
            JPanel info = vbox();
            info.add(left(label(track.isAlbum ? "ÁLBUM" : "MÚSICA", 11, true, ACCENT)));
            info.add(gap(2));
            info.add(left(label(track.title, 32, true, TEXT)));
            info.add(left(label(track.artist, 17, false, MUTED)));
            String duracaoFmt = "";
            if (!track.isAlbum && track.duracaoMs > 0) {
                long totalSeg = track.duracaoMs / 1000;
                duracaoFmt = String.format("  ·  %02d:%02d", totalSeg / 60, totalSeg % 60);
            }

            String detalhes = (track.isAlbum ? "" : track.album + "  ·  ")
                    + (track.year > 0 ? track.year + "  ·  " : "")
                    + track.genre + duracaoFmt;
            info.add(left(label(detalhes, 13, false, MUTED)));
            info.add(gap(14));

            JPanel avgRow = flow(10);
            double avg = Store.avg(track.id);
            int n = Store.of(track.id).size();
            avgRow.add(new StarRating(avg, 24, false));
            avgRow.add(label(n == 0 ? "Seja o primeiro a avaliar" : String.format("%.1f  ·  %d avaliação(ões)", avg, n),
                    13, false, TEXT));
            info.add(left(avgRow));
            info.add(gap(14));

            JPanel acoesRow = flow(10);
            HeartButton fav = new HeartButton(isFav);
            fav.onToggle = () -> {
                if (!me.favorites.remove(track.id))
                    me.favorites.add(track.id);
            };
            acoesRow.add(fav);

            String queryUrl = java.net.URLEncoder.encode(track.title + " " + track.artist,
                    java.nio.charset.StandardCharsets.UTF_8);
            String urlApple = (track.linkAppleMusic != null && !track.linkAppleMusic.isBlank())
                    ? track.linkAppleMusic
                    : "https://music.apple.com/br/search?term=" + queryUrl;
            String urlSpotify = (track.linkSpotify != null && !track.linkSpotify.isBlank())
                    ? track.linkSpotify
                    : "https://open.spotify.com/search/" + queryUrl;

            GlassButton btnApple = new GlassButton("Abrir no Apple Music ›", Kind.GLASS);
            btnApple.addActionListener(e -> abrirLinkNavegador(urlApple));
            acoesRow.add(btnApple);

            GlassButton btnSpotify = new GlassButton("Buscar no Spotify ›", Kind.GLASS);
            btnSpotify.addActionListener(e -> abrirLinkNavegador(urlSpotify));
            acoesRow.add(btnSpotify);
            info.add(left(acoesRow));
            head.add(info, BorderLayout.CENTER);
            fixHeight(head);
            content.add(head);
            content.add(gap(22));

            // Formulário de avaliação (tudo centralizado)
            GlassPanel form = glassBox(28, 26);
            JLabel formTitle = label(mine == null ? "Avalie e escreva sua resenha" : "Sua avaliação", 18, true, TEXT);
            formTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            form.add(formTitle);
            form.add(gap(14));
            BigStars sr = new BigStars(mine == null ? 0 : mine.stars, 62);
            sr.setAlignmentX(Component.CENTER_ALIGNMENT);
            form.add(sr);
            JLabel hint = label(mine == null ? "Passe o mouse e clique para dar sua nota" : mine.stars + " de 5", 13,
                    false, MUTED);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            sr.onChange = () -> hint.setText(sr.value + " de 5");
            form.add(hint);
            form.add(gap(16));
            GlassArea ta = new GlassArea(mine == null ? "" : mine.text, 5, 40);
            JScrollPane tsp = new JScrollPane(ta);
            tsp.setBorder(null);
            tsp.setOpaque(false);
            tsp.getViewport().setOpaque(false);
            styleBar(tsp.getVerticalScrollBar());
            tsp.setAlignmentX(Component.CENTER_ALIGNMENT);
            tsp.setMaximumSize(new Dimension(680, 140));
            form.add(tsp);
            form.add(gap(8));
            JLabel msg = label(" ", 12, false, ERR);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            form.add(msg);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            btns.setOpaque(false);
            btns.setAlignmentX(Component.CENTER_ALIGNMENT);
            GlassButton save = new GlassButton(mine == null ? "Publicar" : "Atualizar", Kind.PRIMARY);
            save.addActionListener(e -> {
                if (sr.value < 1) {
                    msg.setText("Escolha de 1 a 5 estrelas.");
                    return;
                }
                String txt = ta.getText().trim();
                if (txt.length() > 600) {
                    msg.setText("A resenha pode ter no máximo 600 caracteres.");
                    return;
                }
                Store.save(new Review(me.username, track.id, sr.value, txt, LocalDate.now()));
                refresh();
            });
            btns.add(save);
            if (mine != null) {
                GlassButton del = new GlassButton("Remover", Kind.GLASS);
                del.addActionListener(e -> {
                    Store.reviews.remove(mine);
                    refresh();
                });
                btns.add(del);
            }
            btns.setMaximumSize(new Dimension(MAX, btns.getPreferredSize().height));
            form.add(btns);
            fixHeight(form);
            content.add(form);
            content.add(gap(28));

            // Resenhas da comunidade
            content.add(left(label("Resenhas e avaliações", 20, true, TEXT)));
            content.add(gap(12));
            List<Review> all = Store.of(track.id);
            if (all.isEmpty())
                content.add(left(label("Ainda não há avaliações para este título.", 13, false, MUTED)));
            for (Review r : all)
                addRow(content, reviewRow(r, false));
            content.add(gap(20));

            content.revalidate();
            content.repaint();
        }
        void abrirLinkNavegador(String url) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Não foi possível abrir o navegador.\nLink: " + url,
                        "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    class ProfilePanel extends JPanel implements Refreshable {
        final JPanel content = vbox();

        ProfilePanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(22, 34, 0, 34));
            add(scroll(content), BorderLayout.CENTER);
        }

        JPanel stat(String value, String name) {
            GlassPanel p = glassBox(22, 14);
            p.setBorder(new EmptyBorder(14, 26, 14, 26));
            p.add(left(label(value, 26, true, ACCENT)));
            p.add(left(label(name, 12, false, MUTED)));
            return p;
        }

        public void refresh() {
            content.removeAll();
            User me = Store.current;
            if (me == null)
                return;
            List<Review> mine = Store.by(me.username);

            GlassPanel head = new GlassPanel(32);
            head.setLayout(new BorderLayout(22, 0));
            head.setBorder(new EmptyBorder(24, 24, 24, 24));
            head.add(new Avatar(me.username, 96), BorderLayout.WEST);
            JPanel info = vbox();
            info.add(Box.createVerticalGlue());
            info.add(left(label("@" + me.username, 30, true, TEXT)));
            info.add(left(label(me.email, 13, false, MUTED)));
            info.add(gap(12));
            double media = mine.stream().mapToInt(r -> r.stars).average().orElse(0);
            JPanel stats = flow(12);
            stats.add(stat(String.valueOf(mine.size()), "avaliações"));
            stats.add(stat(String.valueOf(me.favorites.size()), "favoritos"));
            stats.add(stat(mine.isEmpty() ? "–" : String.format("%.1f", media), "nota média"));
            info.add(left(stats));
            info.add(Box.createVerticalGlue());
            head.add(info, BorderLayout.CENTER);
            fixHeight(head);
            content.add(head);
            content.add(gap(22));

            content.add(left(label("Sobre mim", 18, true, TEXT)));
            content.add(gap(8));
            GlassArea bio = new GlassArea(me.bio, 3, 40);
            JScrollPane bsp = new JScrollPane(bio);
            bsp.setBorder(null);
            bsp.setOpaque(false);
            bsp.getViewport().setOpaque(false);
            styleBar(bsp.getVerticalScrollBar());
            left(bsp);
            bsp.setMaximumSize(new Dimension(MAX, 96));
            content.add(bsp);
            content.add(gap(10));
            JLabel saved = label(" ", 12, false, ACCENT);
            GlassButton save = left(new GlassButton("Salvar bio", Kind.GLASS));
            save.addActionListener(e -> {
                me.bio = bio.getText().trim();
                saved.setText("Bio atualizada!");
            });
            content.add(save);
            content.add(gap(4));
            content.add(left(saved));
            content.add(gap(18));

            content.add(left(label("Minhas avaliações", 20, true, TEXT)));
            content.add(gap(12));
            if (mine.isEmpty())
                content.add(left(label("Você ainda não avaliou nada. Vá em Explorar!", 13, false, MUTED)));
            for (Review r : mine)
                addRow(content, reviewRow(r, true));
            content.add(gap(20));

            content.revalidate();
            content.repaint();
        }
    }

    class FavoritesPanel extends JPanel implements Refreshable {
        final JPanel content = vbox();

        FavoritesPanel() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(22, 34, 0, 34));
            add(scroll(content), BorderLayout.CENTER);
        }

        public void refresh() {
            content.removeAll();
            User me = Store.current;
            if (me == null)
                return;
            content.add(left(label("Meus favoritos", 30, true, TEXT)));
            content.add(left(
                    label("Músicas e álbuns que você mais ama. Clique no coração para remover.", 13, false, MUTED)));
            content.add(gap(20));
            if (me.favorites.isEmpty()) {
                GlassPanel empty = glassBox(28, 28);
                empty.add(left(label("Nenhum favorito ainda", 18, true, TEXT)));
                empty.add(gap(6));
                empty.add(left(label("Toque no coração de qualquer álbum para guardá-lo aqui.", 13, false, MUTED)));
                empty.add(gap(16));
                GlassButton go = left(new GlassButton("Explorar músicas", Kind.PRIMARY));
                go.addActionListener(e -> go("home"));
                empty.add(go);
                fixHeight(empty);
                content.add(empty);
            } else {
                List<Track> fl = new ArrayList<>();
                for (int id : me.favorites)
                    fl.add(Store.track(id));
                JPanel g = grid(fl, this::refresh);
                fixHeight(g);
                content.add(g);
            }
            content.add(gap(20));
            content.revalidate();
            content.repaint();
        }
    }

    // ===================== LOGOS EMBUTIDAS (PNG em Base64) =====================
    static final class EmbeddedLogos {
        static final String WORD = "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";
        static final String PHONES = "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";
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            // Look and feel padrão do Swing: respeita as cores customizadas em qualquer
            // sistema
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            Escutai app = new Escutai();
            app.setVisible(true);
            app.carregarHome();
        });
    }
}