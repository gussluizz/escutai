package br.com.escutai.view;

import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.catalogo.service.ChartsService;
import br.com.escutai.catalogo.service.ItunesService;
import br.com.escutai.catalogo.service.LancamentosService;
import br.com.escutai.catalogo.service.LancamentosService.Lancamento;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.service.UsuarioService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TelaPrincipal extends JFrame {

    private final Usuario usuario;
    private final UsuarioService usuarioService;
    private final AvaliacaoService avaliacaoService;
    private final FavoritoService favoritoService;

    private final ItunesService itunesService;
    private final ChartsService chartsService;
    private final LancamentosService lancamentosService;

    private final BarraSuperior barraSuperior;
    private final CardLayout cardLayout;
    private final JPanel painelCards;

    private JPanel painelResultadosBusca;
    private JLabel labelTituloBusca;

    public TelaPrincipal(Usuario usuario, UsuarioService usuarioService,
                         AvaliacaoService avaliacaoService, FavoritoService favoritoService) {
        this.usuario = usuario;
        this.usuarioService = usuarioService;
        this.avaliacaoService = avaliacaoService;
        this.favoritoService = favoritoService;

        this.itunesService = new ItunesService();
        this.chartsService = new ChartsService();
        this.lancamentosService = new LancamentosService();

        setTitle("ESCUTAÍ");
        setSize(1120, 740);
        setMinimumSize(new Dimension(920, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(TemaEscutai.FUNDO_BASE);

        this.barraSuperior = new BarraSuperior(
                BarraSuperior.OPCAO_NOVIDADES,
                this::navegarParaTela,
                this::pesquisarMusicas,
                this::voltarLogin
        );

        this.cardLayout = new CardLayout();
        this.painelCards = new JPanel(cardLayout);
        this.painelCards.setOpaque(false);

        painelCards.add(criarPainelNovidades(), "novidades");
        painelCards.add(criarPainelPesquisa(), "pesquisa");

        raiz.add(barraSuperior, BorderLayout.NORTH);
        raiz.add(painelCards, BorderLayout.CENTER);

        setContentPane(raiz);
        abrirNovidades();
    }

    private void navegarParaTela(String opcao) {
        if (BarraSuperior.OPCAO_NOVIDADES.equals(opcao)) {
            abrirNovidades();
        } else if (BarraSuperior.OPCAO_PERFIL.equals(opcao)) {
            abrirPerfil();
        } else if (BarraSuperior.OPCAO_FAVORITOS.equals(opcao)) {
            abrirFavoritos(this::abrirNovidades);
        }
    }

    private void abrirNovidades() {
        barraSuperior.definirTelaSelecionada(BarraSuperior.OPCAO_NOVIDADES);
        cardLayout.show(painelCards, "novidades");
    }

    private void abrirPerfil() {
        barraSuperior.definirTelaSelecionada(BarraSuperior.OPCAO_PERFIL);
        TelaPerfil perfil = new TelaPerfil(
                usuario,
                avaliacaoService,
                () -> abrirFavoritos(this::abrirPerfil),
                this::abrirNovidades,
                this::voltarLogin,
                musica -> abrirDetalhes(musica, this::abrirPerfil)
        );
        painelCards.add(perfil, "perfil");
        cardLayout.show(painelCards, "perfil");
    }

    private void abrirFavoritos(Runnable destinoVoltar) {
        barraSuperior.definirTelaSelecionada(BarraSuperior.OPCAO_FAVORITOS);
        TelaFavoritos favoritos = new TelaFavoritos(
                usuario,
                favoritoService,
                avaliacaoService,
                conteudo -> abrirDetalhes(conteudo, () -> abrirFavoritos(destinoVoltar)),
                destinoVoltar
        );
        painelCards.add(favoritos, "favoritos");
        cardLayout.show(painelCards, "favoritos");
    }

    private void abrirDetalhes(Musica musica, Runnable destinoVoltar) {
        abrirDetalhes((ConteudoMusical) musica, destinoVoltar);
    }

    private void abrirDetalhes(ConteudoMusical conteudo, Runnable destinoVoltar) {
        TelaMusicaDetalhes tela = new TelaMusicaDetalhes(
                conteudo,
                usuario,
                avaliacaoService,
                favoritoService,
                destinoVoltar
        );
        painelCards.add(tela, "detalhes");
        cardLayout.show(painelCards, "detalhes");
    }

    private JPanel criarPainelNovidades() {
        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new BorderLayout());

        PainelConteudoVertical conteudo = new PainelConteudoVertical();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setOpaque(false);
        conteudo.setBorder(new EmptyBorder(20, 28, 32, 28));

        // Saudação
        JPanel linhaSaudacao = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaSaudacao.setOpaque(false);
        linhaSaudacao.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelOla = new JLabel("Olá, @" + usuario.getUsername() + "  ");
        labelOla.setFont(new Font("SansSerif", Font.BOLD, 19));
        labelOla.setForeground(TemaEscutai.BRANCO);

        JLabel labelSub = new JLabel("O que você ouviu hoje?");
        labelSub.setFont(new Font("SansSerif", Font.PLAIN, 15));
        labelSub.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        linhaSaudacao.add(labelOla);
        linhaSaudacao.add(labelSub);

        conteudo.add(linhaSaudacao);
        conteudo.add(Box.createVerticalStrut(20));

        // 3 Vitrines obrigatórias: TOP MUNDO, TOP BRASIL e NOVIDADES
        JPanel listaTopMundo = criarLinhaCarrossel();
        JPanel listaTopBrasil = criarLinhaCarrossel();
        JPanel listaNovidades = criarLinhaCarrossel();

        conteudo.add(criarSecaoCarrossel("TOP MUNDO", listaTopMundo));
        conteudo.add(Box.createVerticalStrut(22));
        conteudo.add(criarSecaoCarrossel("TOP BRASIL", listaTopBrasil));
        conteudo.add(Box.createVerticalStrut(22));
        conteudo.add(criarSecaoCarrossel("NOVIDADES", listaNovidades));

        carregarVitrineAssincrona(listaTopMundo, () -> chartsService.topMundo(15));
        carregarVitrineAssincrona(listaTopBrasil, () -> chartsService.topBrasil(15));
        carregarVitrineAssincrona(listaNovidades, () -> lancamentosService.buscarLancamentos(15));

        JScrollPane scrollPagina = new JScrollPane(conteudo);
        TemaEscutai.configurarScrollInvisivel(scrollPagina);
        fundo.add(scrollPagina, BorderLayout.CENTER);

        return fundo;
    }

    private JPanel criarLinhaCarrossel() {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        linha.setOpaque(false);
        JLabel carregando = new JLabel("Carregando músicas...");
        carregando.setFont(TemaEscutai.FONTE_CORPO);
        carregando.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        linha.add(carregando);
        return linha;
    }

    private JPanel criarSecaoCarrossel(String tituloSecao, JPanel painelCardsHorizontal) {
        JPanel secao = new JPanel(new BorderLayout(0, 10));
        secao.setOpaque(false);
        secao.setAlignmentX(Component.LEFT_ALIGNMENT);
        secao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 285));

        // Cabeçalho com barra vertical magenta
        JPanel cabecalho = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cabecalho.setOpaque(false);

        JPanel barraMagenta = new JPanel();
        barraMagenta.setBackground(TemaEscutai.MAGENTA);
        barraMagenta.setPreferredSize(new Dimension(4, 20));

        JLabel titulo = new JLabel("  " + tituloSecao);
        titulo.setFont(TemaEscutai.FONTE_SECAO);
        titulo.setForeground(TemaEscutai.BRANCO);

        cabecalho.add(barraMagenta);
        cabecalho.add(titulo);

        // Scroll horizontal sem barra visível + botões de seta
        JScrollPane scrollHorizontal = new JScrollPane(painelCardsHorizontal);
        scrollHorizontal.setBorder(null);
        scrollHorizontal.setOpaque(false);
        scrollHorizontal.getViewport().setOpaque(false);
        scrollHorizontal.setPreferredSize(new Dimension(0, 245));
        scrollHorizontal.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollHorizontal.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TemaEscutai.repassarScrollParaPai(scrollHorizontal);

        TemaEscutai.BotaoSetaCarrossel btnEsquerda = new TemaEscutai.BotaoSetaCarrossel(false);
        TemaEscutai.BotaoSetaCarrossel btnDireita = new TemaEscutai.BotaoSetaCarrossel(true);

        btnEsquerda.addActionListener(e -> moverCarrossel(scrollHorizontal, -420));
        btnDireita.addActionListener(e -> moverCarrossel(scrollHorizontal, 420));

        JPanel envoltorioEsquerda = new JPanel(new GridBagLayout());
        envoltorioEsquerda.setOpaque(false);
        envoltorioEsquerda.add(btnEsquerda);

        JPanel envoltorioDireita = new JPanel(new GridBagLayout());
        envoltorioDireita.setOpaque(false);
        envoltorioDireita.add(btnDireita);

        JPanel corpoCarrossel = new JPanel(new BorderLayout(8, 0));
        corpoCarrossel.setOpaque(false);
        corpoCarrossel.add(envoltorioEsquerda, BorderLayout.WEST);
        corpoCarrossel.add(scrollHorizontal, BorderLayout.CENTER);
        corpoCarrossel.add(envoltorioDireita, BorderLayout.EAST);

        secao.add(cabecalho, BorderLayout.NORTH);
        secao.add(corpoCarrossel, BorderLayout.CENTER);
        return secao;
    }

    private void moverCarrossel(JScrollPane scroll, int deltaX) {
        JScrollBar barra = scroll.getHorizontalScrollBar();
        int destino = Math.max(0, Math.min(barra.getMaximum(), barra.getValue() + deltaX));
        barra.setValue(destino);
    }

    @FunctionalInterface
    private interface ProvedorLancamentos {
        ArrayList<Lancamento> carregar() throws Exception;
    }

    private void carregarVitrineAssincrona(JPanel destino, ProvedorLancamentos provedor) {
        new SwingWorker<ArrayList<Lancamento>, Void>() {
            @Override
            protected ArrayList<Lancamento> doInBackground() throws Exception {
                return provedor.carregar();
            }

            @Override
            protected void done() {
                destino.removeAll();
                try {
                    ArrayList<Lancamento> itens = get();
                    if (itens == null || itens.isEmpty()) {
                        JLabel vazio = new JLabel("Nenhum conteúdo disponível no momento.");
                        vazio.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
                        destino.add(vazio);
                    } else {
                        for (Lancamento item : itens) {
                            Musica musica = new Musica(
                                    item.getId(),
                                    item.getTitulo(),
                                    item.getArtista(),
                                    item.getImagemUrl(),
                                    null,
                                    item.getLinkAppleMusic(),
                                    0L
                            );
                            destino.add(criarCardMusica(musica, () -> abrirDetalhes(musica, thisRefNovidades())));
                        }
                    }
                } catch (Exception erro) {
                    JLabel erroLabel = new JLabel("Não foi possível carregar esta seção.");
                    erroLabel.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
                    destino.add(erroLabel);
                }
                destino.revalidate();
                destino.repaint();
            }
        }.execute();
    }

    private Runnable thisRefNovidades() {
        return this::abrirNovidades;
    }

    private JPanel criarCardMusica(Musica musica, Runnable aoClicar) {
        TemaEscutai.PainelArredondado card = new TemaEscutai.PainelArredondado(
                new BorderLayout(0, 8),
                TemaEscutai.RAIO_CARD,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.BORDA_SUTIL
        );
        card.setPreferredSize(new Dimension(176, 232));
        card.setBorder(new EmptyBorder(10, 10, 12, 10));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setToolTipText(musica.getTitulo() + " — " + musica.getArtista());

        JLabel capa = new JLabel("", SwingConstants.CENTER);
        capa.setPreferredSize(new Dimension(154, 154));
        TemaEscutai.carregarCapaAssincrona(
                musica.getImagemUrl(),
                154,
                154,
                TemaEscutai.RAIO_CAPA,
                capa,
                null
        );

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);

        JLabel titulo = new JLabel(limitarTexto(musica.getTitulo(), 20), SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 13));
        titulo.setForeground(TemaEscutai.BRANCO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel artista = new JLabel(limitarTexto(musica.getArtista(), 24), SwingConstants.CENTER);
        artista.setFont(TemaEscutai.FONTE_PEQUENA);
        artista.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        artista.setAlignmentX(Component.CENTER_ALIGNMENT);

        textos.add(titulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(artista);

        card.add(capa, BorderLayout.CENTER);
        card.add(textos, BorderLayout.SOUTH);

        MouseAdapter interacao = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setEstilo(TemaEscutai.SUPERFICIE_HOVER, TemaEscutai.MAGENTA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    card.setEstilo(TemaEscutai.SUPERFICIE, TemaEscutai.BORDA_SUTIL);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                aoClicar.run();
            }
        };

        card.addMouseListener(interacao);
        capa.addMouseListener(interacao);
        textos.addMouseListener(interacao);
        titulo.addMouseListener(interacao);
        artista.addMouseListener(interacao);

        return card;
    }

    private JPanel criarPainelPesquisa() {
        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new BorderLayout(12, 12));
        fundo.setBorder(new EmptyBorder(20, 28, 24, 28));

        JPanel topo = new JPanel(new BorderLayout(12, 0));
        topo.setOpaque(false);

        labelTituloBusca = new JLabel("Resultados da pesquisa");
        labelTituloBusca.setFont(TemaEscutai.FONTE_SECAO);
        labelTituloBusca.setForeground(TemaEscutai.BRANCO);

        TemaEscutai.BotaoArredondado btnVoltar = TemaEscutai.criarBotaoContorno("Voltar para Novidades");
        btnVoltar.addActionListener(e -> abrirNovidades());

        topo.add(labelTituloBusca, BorderLayout.WEST);
        topo.add(btnVoltar, BorderLayout.EAST);

        painelResultadosBusca = new JPanel();
        painelResultadosBusca.setLayout(new BoxLayout(painelResultadosBusca, BoxLayout.Y_AXIS));
        painelResultadosBusca.setOpaque(false);

        JScrollPane scroll = new JScrollPane(painelResultadosBusca);
        TemaEscutai.configurarScrollInvisivel(scroll);

        fundo.add(topo, BorderLayout.NORTH);
        fundo.add(scroll, BorderLayout.CENTER);
        return fundo;
    }

    private void pesquisarMusicas(String termo) {
        if (termo == null || termo.isBlank()) {
            JOptionPane.showMessageDialog(this, "Digite o nome de uma música ou artista para pesquisar.");
            return;
        }

        barraSuperior.definirTelaSelecionada("");
        labelTituloBusca.setText("Resultados para \"" + termo + "\"");
        painelResultadosBusca.removeAll();

        JLabel buscando = new JLabel("Buscando músicas no iTunes...");
        buscando.setFont(TemaEscutai.FONTE_CORPO);
        buscando.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        painelResultadosBusca.add(buscando);
        painelResultadosBusca.revalidate();
        painelResultadosBusca.repaint();

        cardLayout.show(painelCards, "pesquisa");

        new SwingWorker<ArrayList<Musica>, Void>() {
            @Override
            protected ArrayList<Musica> doInBackground() throws Exception {
                return itunesService.buscarMusicas(termo);
            }

            @Override
            protected void done() {
                painelResultadosBusca.removeAll();
                try {
                    List<Musica> musicas = get();
                    if (musicas.isEmpty()) {
                        JLabel vazio = new JLabel("Nenhuma música encontrada para \"" + termo + "\".");
                        vazio.setFont(TemaEscutai.FONTE_CORPO);
                        vazio.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
                        painelResultadosBusca.add(vazio);
                    } else {
                        for (Musica musica : musicas) {
                            painelResultadosBusca.add(criarLinhaResultadoBusca(musica));
                            painelResultadosBusca.add(Box.createVerticalStrut(10));
                        }
                    }
                } catch (Exception erro) {
                    JLabel falha = new JLabel("Não foi possível consultar a API do iTunes.");
                    falha.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
                    painelResultadosBusca.add(falha);
                }
                painelResultadosBusca.revalidate();
                painelResultadosBusca.repaint();
            }
        }.execute();
    }

    private JPanel criarLinhaResultadoBusca(Musica musica) {
        TemaEscutai.PainelArredondado item = new TemaEscutai.PainelArredondado(
                new BorderLayout(16, 6),
                TemaEscutai.RAIO_CARD,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.BORDA_SUTIL
        );
        item.setBorder(new EmptyBorder(10, 14, 10, 14));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel capa = new JLabel("", SwingConstants.CENTER);
        capa.setPreferredSize(new Dimension(88, 88));
        TemaEscutai.carregarCapaAssincrona(musica.getImagemUrl(), 88, 88, TemaEscutai.RAIO_CAPA, capa, null);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nome = new JLabel(musica.getTitulo());
        nome.setFont(new Font("SansSerif", Font.BOLD, 16));
        nome.setForeground(TemaEscutai.BRANCO);

        JLabel artista = new JLabel(musica.getArtista());
        artista.setFont(TemaEscutai.FONTE_CORPO);
        artista.setForeground(TemaEscutai.TEXTO_SECUNDARIO);

        info.add(Box.createVerticalGlue());
        info.add(nome);
        info.add(Box.createVerticalStrut(5));
        info.add(artista);
        info.add(Box.createVerticalGlue());

        TemaEscutai.BotaoArredondado btnDetalhes = TemaEscutai.criarBotaoPrimario("Avaliar / Detalhes");
        btnDetalhes.addActionListener(e -> abrirDetalhes(musica, () -> cardLayout.show(painelCards, "pesquisa")));

        JPanel direita = new JPanel(new GridBagLayout());
        direita.setOpaque(false);
        direita.add(btnDetalhes);

        item.add(capa, BorderLayout.WEST);
        item.add(info, BorderLayout.CENTER);
        item.add(direita, BorderLayout.EAST);

        MouseAdapter cliqueLinha = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setEstilo(TemaEscutai.SUPERFICIE_HOVER, TemaEscutai.MAGENTA);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), item);
                if (!item.contains(p)) {
                    item.setEstilo(TemaEscutai.SUPERFICIE, TemaEscutai.BORDA_SUTIL);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                abrirDetalhes(musica, () -> cardLayout.show(painelCards, "pesquisa"));
            }
        };

        item.addMouseListener(cliqueLinha);
        capa.addMouseListener(cliqueLinha);
        info.addMouseListener(cliqueLinha);

        return item;
    }

    private String limitarTexto(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }

    private void voltarLogin() {
        usuarioService.logout();
        TelaLogin login = new TelaLogin(usuarioService, avaliacaoService, favoritoService);
        login.setVisible(true);
        dispose();
    }

    /**
     * Garante que a largura do painel interno acompanhe exatamente a largura da janela,
     * mantendo os botões de navegação lateral dos carrosséis sempre visíveis nas bordas.
     */
    private static class PainelConteudoVertical extends JPanel implements Scrollable {
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