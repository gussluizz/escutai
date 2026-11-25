package br.com.escutai.view;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.usuario.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TelaFavoritos extends JPanel {

    private static final int TAMANHO_CAPA_FAVORITO = 148;
    private static final int COLUNAS_POR_LINHA = 4;

    private final Usuario usuario;
    private final FavoritoService favoritoService;
    private final AvaliacaoService avaliacaoService;
    private final Consumer<ConteudoMusical> aoAbrirConteudo;
    private final JPanel gradeFavoritos;

    public TelaFavoritos(Usuario usuario, FavoritoService favoritoService,
                         AvaliacaoService avaliacaoService, Runnable voltar) {
        this(usuario, favoritoService, avaliacaoService, conteudo -> {}, voltar);
    }

    public TelaFavoritos(Usuario usuario, FavoritoService favoritoService,
                         AvaliacaoService avaliacaoService,
                         Consumer<ConteudoMusical> aoAbrirConteudo,
                         Runnable voltar) {
        this.usuario = usuario;
        this.favoritoService = favoritoService;
        this.avaliacaoService = avaliacaoService;
        this.aoAbrirConteudo = aoAbrirConteudo;

        setLayout(new BorderLayout());
        setOpaque(false);

        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new BorderLayout(12, 16));
        fundo.setBorder(BorderFactory.createEmptyBorder(20, 28, 24, 28));

        JLabel titulo = new JLabel("Meus Favoritos");
        titulo.setFont(TemaEscutai.FONTE_TITULO);
        titulo.setForeground(TemaEscutai.BRANCO);

        JLabel subtitulo = new JLabel("Clique em qualquer música ou álbum favorito para avaliar ou editar");
        subtitulo.setFont(TemaEscutai.FONTE_CORPO);
        subtitulo.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.setOpaque(false);
        topo.add(titulo);
        topo.add(Box.createVerticalStrut(4));
        topo.add(subtitulo);

        fundo.add(topo, BorderLayout.NORTH);

        gradeFavoritos = new JPanel(new GridLayout(0, COLUNAS_POR_LINHA, 24, 26));
        gradeFavoritos.setOpaque(false);

        PainelGradeAjustavel wrapperTopo = new PainelGradeAjustavel();
        wrapperTopo.setLayout(new BorderLayout());
        wrapperTopo.setOpaque(false);
        wrapperTopo.add(gradeFavoritos, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapperTopo);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TemaEscutai.configurarScrollInvisivel(scroll);

        fundo.add(scroll, BorderLayout.CENTER);
        add(fundo, BorderLayout.CENTER);

        carregarListaFavoritos();
    }

    public void carregarListaFavoritos() {
        gradeFavoritos.removeAll();
        List<ConteudoMusical> favoritos = new ArrayList<>(favoritoService.listarFavoritos(usuario));
        Collections.reverse(favoritos);

        if (favoritos.isEmpty()) {
            JLabel vazio = new JLabel("Você ainda não adicionou músicas ou álbuns aos favoritos.");
            vazio.setFont(TemaEscutai.FONTE_CORPO);
            vazio.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
            gradeFavoritos.add(vazio);
        } else {
            for (ConteudoMusical conteudo : favoritos) {
                gradeFavoritos.add(criarCardGradeFavorito(conteudo));
            }
        }

        gradeFavoritos.revalidate();
        gradeFavoritos.repaint();
    }

    private JPanel criarCardGradeFavorito(ConteudoMusical conteudo) {
        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setOpaque(false);
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel capa = new JLabel();
        capa.setPreferredSize(new Dimension(TAMANHO_CAPA_FAVORITO, TAMANHO_CAPA_FAVORITO));
        capa.setMinimumSize(new Dimension(TAMANHO_CAPA_FAVORITO, TAMANHO_CAPA_FAVORITO));
        capa.setMaximumSize(new Dimension(TAMANHO_CAPA_FAVORITO, TAMANHO_CAPA_FAVORITO));
        capa.setHorizontalAlignment(SwingConstants.CENTER);
        capa.setAlignmentX(Component.CENTER_ALIGNMENT);
        TemaEscutai.carregarCapaAssincrona(
                conteudo.getImagemUrl(),
                TAMANHO_CAPA_FAVORITO,
                TAMANHO_CAPA_FAVORITO,
                capa,
                null
        );

        JLabel nome = new JLabel(conteudo.getTitulo(), SwingConstants.CENTER);
        nome.setFont(TemaEscutai.FONTE_SUBTITULO);
        nome.setForeground(TemaEscutai.BRANCO);
        nome.setAlignmentX(Component.CENTER_ALIGNMENT);
        nome.setMaximumSize(new Dimension(TAMANHO_CAPA_FAVORITO, 18));

        JLabel artista = new JLabel(conteudo.getArtista(), SwingConstants.CENTER);
        artista.setFont(TemaEscutai.FONTE_PEQUENA);
        artista.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        artista.setAlignmentX(Component.CENTER_ALIGNMENT);
        artista.setMaximumSize(new Dimension(TAMANHO_CAPA_FAVORITO, 16));

        Avaliacao avaliacaoUsuario = null;
        for (Avaliacao a : avaliacaoService.listarAvaliacoes(conteudo)) {
            if (a.getUsuario().getUsername().equals(usuario.getUsername())) {
                avaliacaoUsuario = a;
                break;
            }
        }

        JLabel statusNota = new JLabel("", SwingConstants.CENTER);
        statusNota.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (avaliacaoUsuario != null) {
            statusNota.setText("★".repeat(Math.max(0, avaliacaoUsuario.getNota())));
            statusNota.setFont(new Font("SansSerif", Font.BOLD, 15));
            statusNota.setForeground(TemaEscutai.DOURADO_ESTRELA);
        } else {
            statusNota.setText("Avaliar");
            statusNota.setFont(TemaEscutai.FONTE_PEQUENA);
            statusNota.setForeground(TemaEscutai.MAGENTA);
        }

        TemaEscutai.BotaoArredondado remover = TemaEscutai.criarBotaoContorno("Remover");
        remover.setFont(TemaEscutai.FONTE_PEQUENA);
        remover.setPadding(5, 16, 5, 16);
        remover.setAlignmentX(Component.CENTER_ALIGNMENT);
        remover.addActionListener(e -> {
            favoritoService.removerDaLista(usuario, conteudo);
            carregarListaFavoritos();
        });

        item.add(capa);
        item.add(Box.createVerticalStrut(8));
        item.add(nome);
        item.add(Box.createVerticalStrut(2));
        item.add(artista);
        item.add(Box.createVerticalStrut(4));
        item.add(statusNota);
        item.add(Box.createVerticalStrut(8));
        item.add(remover);

        MouseAdapter abrirAoClicar = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                nome.setForeground(TemaEscutai.MAGENTA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                nome.setForeground(TemaEscutai.BRANCO);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                aoAbrirConteudo.accept(conteudo);
            }
        };

        item.addMouseListener(abrirAoClicar);
        capa.addMouseListener(abrirAoClicar);
        nome.addMouseListener(abrirAoClicar);
        artista.addMouseListener(abrirAoClicar);
        statusNota.addMouseListener(abrirAoClicar);

        return item;
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