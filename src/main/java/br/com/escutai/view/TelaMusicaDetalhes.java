package br.com.escutai.view;

import br.com.escutai.avaliacao.model.Avaliacao;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.catalogo.model.ConteudoMusical;
import br.com.escutai.catalogo.model.Musica;
import br.com.escutai.shared.exception.NotaInvalidaException;
import br.com.escutai.usuario.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.net.URI;
import java.util.List;

public class TelaMusicaDetalhes extends JPanel {

    private final ConteudoMusical conteudo;
    private final Usuario usuario;
    private final AvaliacaoService avaliacaoService;
    private final FavoritoService favoritoService;
    private final Runnable acaoVoltar;

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
            FavoritoService favoritoService,
            Runnable acaoVoltar
    ) {
        this((ConteudoMusical) musica, usuario, avaliacaoService, favoritoService, acaoVoltar);
    }

    public TelaMusicaDetalhes(
            ConteudoMusical conteudo,
            Usuario usuario,
            AvaliacaoService avaliacaoService,
            FavoritoService favoritoService,
            Runnable acaoVoltar
    ) {
        this.conteudo = conteudo;
        this.usuario = usuario;
        this.avaliacaoService = avaliacaoService;
        this.favoritoService = favoritoService;
        this.acaoVoltar = acaoVoltar;

        setLayout(new BorderLayout());
        setOpaque(false);

        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new BorderLayout());

        // Topo com botão de voltar arredondado apenas com a setinha vetorial
        TemaEscutai.BotaoSetaCarrossel botaoVoltar = new TemaEscutai.BotaoSetaCarrossel(false, 44, 38);
        botaoVoltar.setToolTipText("Voltar");
        botaoVoltar.addActionListener(e -> acaoVoltar.run());

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 10));
        topo.setOpaque(false);
        topo.add(botaoVoltar);

        // Layout em 2 colunas: Esquerda (Música + Estrelas/Avaliação) | Direita (Avaliações dos usuários)
        JPanel gradeDuasColunas = new JPanel(new GridBagLayout());
        gradeDuasColunas.setOpaque(false);
        gradeDuasColunas.setBorder(BorderFactory.createEmptyBorder(2, 28, 28, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Coluna Esquerda
        gbc.gridx = 0;
        gbc.weightx = 0.48;
        gbc.insets = new Insets(0, 0, 0, 20);
        gradeDuasColunas.add(criarColunaEsquerdaMusicaEAvaliacao(), gbc);

        // Coluna Direita
        gbc.gridx = 1;
        gbc.weightx = 0.52;
        gbc.insets = new Insets(0, 20, 0, 0);
        gradeDuasColunas.add(criarColunaDireitaAvaliacoes(), gbc);

        JScrollPane rolagem = new JScrollPane(gradeDuasColunas);
        TemaEscutai.configurarScrollInvisivel(rolagem);

        fundo.add(topo, BorderLayout.NORTH);
        fundo.add(rolagem, BorderLayout.CENTER);
        add(fundo, BorderLayout.CENTER);

        atualizarAvaliacoes();
        SwingUtilities.invokeLater(() -> rolagem.getViewport().setViewPosition(new Point(0, 0)));
    }

    private String obterUrlAltaResolucao(String url) {
        if (url == null) return null;
        return url.replace("100x100bb", "600x600bb")
                  .replace("100x100", "600x600");
    }

    /**
     * Coluna da esquerda: Capa HD arredondada, título, artista, botões em negrito e bloco "Avalie este conteúdo".
     */
    private JPanel criarColunaEsquerdaMusicaEAvaliacao() {
        JPanel colunaEsquerda = new JPanel();
        colunaEsquerda.setLayout(new BoxLayout(colunaEsquerda, BoxLayout.Y_AXIS));
        colunaEsquerda.setOpaque(false);

        JLabel capa = new JLabel();
        capa.setPreferredSize(new Dimension(200, 200));
        capa.setMinimumSize(new Dimension(200, 200));
        capa.setMaximumSize(new Dimension(200, 200));
        capa.setHorizontalAlignment(SwingConstants.CENTER);
        capa.setAlignmentX(Component.CENTER_ALIGNMENT);
        TemaEscutai.carregarCapaAssincrona(
                obterUrlAltaResolucao(conteudo.getImagemUrl()),
                200,
                200,
                22,
                capa,
                null
        );

        JLabel titulo = new JLabel(conteudo.getTitulo(), SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(TemaEscutai.BRANCO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        String subtituloTexto = conteudo.getArtista();
        if (conteudo instanceof Musica musica && musica.getDuracao() > 0) {
            long minutos = musica.getDuracao() / 60000;
            long segundos = (musica.getDuracao() % 60000) / 1000;
            subtituloTexto += "  •  " + String.format("%d:%02d", minutos, segundos);
        }

        JLabel artista = new JLabel(subtituloTexto, SwingConstants.CENTER);
        artista.setFont(new Font("SansSerif", Font.PLAIN, 14));
        artista.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        artista.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botões com a mesma fonte em negrito
        TemaEscutai.BotaoArredondado botaoAppleMusic = TemaEscutai.criarBotaoSecundario("Ouvir no Apple Music");
        botaoAppleMusic.setFont(TemaEscutai.FONTE_SUBTITULO);
        botaoAppleMusic.setPadding(9, 20, 9, 20);

        boolean jaFavoritado = favoritoService.listarFavoritos(usuario).stream()
                .anyMatch(c -> c.getId() == conteudo.getId());
        TemaEscutai.BotaoArredondado botaoFavorito = TemaEscutai.criarBotaoPrimario(
                jaFavoritado ? "Remover dos favoritos" : "Adicionar aos favoritos"
        );

        botaoAppleMusic.addActionListener(e -> abrirAppleMusic());
        botaoFavorito.addActionListener(e -> alternarFavorito(botaoFavorito));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        botoes.setOpaque(false);
        botoes.setAlignmentX(Component.CENTER_ALIGNMENT);
        botoes.add(botaoAppleMusic);
        botoes.add(botaoFavorito);

        // Seção de avaliação
        JLabel tituloAvaliar = new JLabel("Avalie este conteúdo", SwingConstants.CENTER);
        tituloAvaliar.setFont(new Font("SansSerif", Font.BOLD, 19));
        tituloAvaliar.setForeground(TemaEscutai.BRANCO);
        tituloAvaliar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel painelEstrelas = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        painelEstrelas.setOpaque(false);
        painelEstrelas.setAlignmentX(Component.CENTER_ALIGNMENT);

        estrelas = new BotaoEstrela[5];
        for (int i = 0; i < 5; i++) {
            final int nota = i + 1;
            estrelas[i] = new BotaoEstrela(52);
            estrelas[i].addActionListener(e -> selecionarNota(nota));
            painelEstrelas.add(estrelas[i]);
        }

        textoNota = new JLabel("Selecione uma nota de 1 a 5 estrelas.", SwingConstants.CENTER);
        textoNota.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        textoNota.setFont(TemaEscutai.FONTE_CORPO);
        textoNota.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel textoComentario = new JLabel("Sua crítica / resenha:", SwingConstants.CENTER);
        textoComentario.setForeground(TemaEscutai.BRANCO);
        textoComentario.setFont(TemaEscutai.FONTE_CORPO);
        textoComentario.setAlignmentX(Component.CENTER_ALIGNMENT);

        campoComentario = new JTextArea(3, 32);
        campoComentario.setLineWrap(true);
        campoComentario.setWrapStyleWord(true);
        campoComentario.setFont(TemaEscutai.FONTE_CORPO);
        campoComentario.setOpaque(false);
        campoComentario.setForeground(TemaEscutai.BRANCO);
        campoComentario.setCaretColor(TemaEscutai.BRANCO);
        campoComentario.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane rolagemComentario = new JScrollPane(campoComentario);
        TemaEscutai.configurarScrollInvisivel(rolagemComentario);
        TemaEscutai.repassarScrollParaPai(rolagemComentario);

        TemaEscutai.PainelArredondado caixaArredondadaCritica = new TemaEscutai.PainelArredondado(
                new BorderLayout(),
                22,
                new Color(22, 9, 32, 210),
                TemaEscutai.BORDA_SUTIL
        );
        caixaArredondadaCritica.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        caixaArredondadaCritica.setPreferredSize(new Dimension(440, 82));
        caixaArredondadaCritica.setMaximumSize(new Dimension(480, 82));
        caixaArredondadaCritica.setAlignmentX(Component.CENTER_ALIGNMENT);
        caixaArredondadaCritica.add(rolagemComentario, BorderLayout.CENTER);

        TemaEscutai.BotaoArredondado botaoEnviar = TemaEscutai.criarBotaoPrimario("Publicar avaliação");
        botaoEnviar.setAlignmentX(Component.CENTER_ALIGNMENT);
        botaoEnviar.addActionListener(e -> publicarAvaliacao());

        colunaEsquerda.add(capa);
        colunaEsquerda.add(Box.createVerticalStrut(10));
        colunaEsquerda.add(titulo);
        colunaEsquerda.add(Box.createVerticalStrut(4));
        colunaEsquerda.add(artista);
        colunaEsquerda.add(Box.createVerticalStrut(12));
        colunaEsquerda.add(botoes);
        colunaEsquerda.add(Box.createVerticalStrut(18));
        colunaEsquerda.add(tituloAvaliar);
        colunaEsquerda.add(Box.createVerticalStrut(4));
        colunaEsquerda.add(painelEstrelas);
        colunaEsquerda.add(textoNota);
        colunaEsquerda.add(Box.createVerticalStrut(10));
        colunaEsquerda.add(textoComentario);
        colunaEsquerda.add(Box.createVerticalStrut(6));
        colunaEsquerda.add(caixaArredondadaCritica);
        colunaEsquerda.add(Box.createVerticalStrut(12));
        colunaEsquerda.add(botaoEnviar);

        return colunaEsquerda;
    }

    /**
     * Coluna da direita: Avaliações dos usuários destacadas logo ao abrir a tela.
     */
    private JPanel criarColunaDireitaAvaliacoes() {
        JPanel painel = new JPanel(new BorderLayout(8, 14));
        painel.setOpaque(false);

        JPanel cabecalhoAvaliacoes = new JPanel(new BorderLayout(10, 0));
        cabecalhoAvaliacoes.setOpaque(false);

        JLabel titulo = new JLabel("Avaliações dos usuários");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(TemaEscutai.BRANCO);
        titulo.setBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, TemaEscutai.MAGENTA));
        titulo.setIconTextGap(8);

        JPanel boxTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        boxTitulo.setOpaque(false);
        boxTitulo.add(titulo);

        TemaEscutai.PainelArredondado badgeMedia = new TemaEscutai.PainelArredondado(
                new FlowLayout(FlowLayout.CENTER, 14, 6),
                20,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.MAGENTA
        );
        textoMedia = new JLabel("Média: sem avaliações");
        textoMedia.setFont(TemaEscutai.FONTE_SUBTITULO);
        textoMedia.setForeground(TemaEscutai.DOURADO_ESTRELA);
        badgeMedia.add(textoMedia);

        cabecalhoAvaliacoes.add(boxTitulo, BorderLayout.WEST);
        cabecalhoAvaliacoes.add(badgeMedia, BorderLayout.EAST);

        painelListaAvaliacoes = new JPanel();
        painelListaAvaliacoes.setLayout(new BoxLayout(painelListaAvaliacoes, BoxLayout.Y_AXIS));
        painelListaAvaliacoes.setOpaque(false);

        painel.add(cabecalhoAvaliacoes, BorderLayout.NORTH);
        painel.add(painelListaAvaliacoes, BorderLayout.CENTER);

        return painel;
    }

    private void alternarFavorito(TemaEscutai.BotaoArredondado botao) {
        try {
            boolean jaFavoritado = favoritoService.listarFavoritos(usuario).stream()
                    .anyMatch(c -> c.getId() == conteudo.getId());
            if (jaFavoritado) {
                favoritoService.removerDaLista(usuario, conteudo);
                botao.setText("Adicionar aos favoritos");
            } else {
                favoritoService.adicionarNaLista(usuario, conteudo);
                botao.setText("Remover dos favoritos");
            }
        } catch (RuntimeException erro) {
            JOptionPane.showMessageDialog(this, erro.getMessage(), "Favoritos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selecionarNota(int nota) {
        notaSelecionada = nota;
        for (int i = 0; i < 5; i++) {
            estrelas[i].setSelecionada(i < nota);
        }
        textoNota.setText("Sua nota: " + nota + " de 5 estrelas.");
    }

    private void limparSelecaoNota() {
        notaSelecionada = 0;
        for (int i = 0; i < 5; i++) {
            estrelas[i].setSelecionada(false);
        }
        textoNota.setText("Selecione uma nota de 1 a 5 estrelas.");
    }

    private void publicarAvaliacao() {
        if (notaSelecionada == 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma nota de 1 a 5 estrelas.", "Atenção", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String comentario = campoComentario.getText().trim();

        try {
            avaliacaoService.publicarAvaliacao(
                    usuario,
                    conteudo,
                    notaSelecionada,
                    comentario
            );

            campoComentario.setText("");
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
        List<Avaliacao> avaliacoes = avaliacaoService.listarAvaliacoes(conteudo);
        double media = avaliacaoService.calcularMediaNotas(conteudo);

        Avaliacao avaliacaoDoUsuario = null;
        for (Avaliacao a : avaliacoes) {
            if (a.getUsuario() != null && usuario != null
                    && a.getUsuario().getUsername().equalsIgnoreCase(usuario.getUsername())) {
                avaliacaoDoUsuario = a;
                break;
            }
        }

        if (avaliacaoDoUsuario != null) {
            selecionarNota(avaliacaoDoUsuario.getNota());
        } else {
            limparSelecaoNota();
        }

        painelListaAvaliacoes.removeAll();

        if (avaliacoes.isEmpty()) {
            textoMedia.setText("Média: sem avaliações");
            JLabel semAv = new JLabel("Este conteúdo ainda não possui avaliações. Seja o primeiro a avaliar!");
            semAv.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
            semAv.setFont(TemaEscutai.FONTE_CORPO);
            painelListaAvaliacoes.add(semAv);
        } else {
            textoMedia.setText(String.format("Média: %.1f / 5 ★ (%d %s)",
                    media,
                    avaliacoes.size(),
                    avaliacoes.size() == 1 ? "avaliação" : "avaliações"));
            for (Avaliacao avaliacao : avaliacoes) {
                painelListaAvaliacoes.add(criarPainelUmaAvaliacao(avaliacao));
                painelListaAvaliacoes.add(Box.createVerticalStrut(10));
            }
        }

        painelListaAvaliacoes.revalidate();
        painelListaAvaliacoes.repaint();
    }

    private JPanel criarPainelUmaAvaliacao(Avaliacao avaliacao) {
        TemaEscutai.PainelArredondado painel = new TemaEscutai.PainelArredondado(
                new BorderLayout(8, 8),
                TemaEscutai.RAIO_CARD,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.BORDA_SUTIL
        );
        painel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel nomeUsuario = new JLabel("@" + avaliacao.getUsuario().getUsername());
        nomeUsuario.setFont(TemaEscutai.FONTE_SUBTITULO);
        nomeUsuario.setForeground(TemaEscutai.BRANCO);

        JLabel nota = new JLabel("★".repeat(avaliacao.getNota()) + "☆".repeat(5 - avaliacao.getNota()));
        nota.setFont(new Font("SansSerif", Font.BOLD, 15));
        nota.setForeground(TemaEscutai.DOURADO_ESTRELA);

        JLabel data = new JLabel("• " + avaliacao.getData());
        data.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        data.setFont(TemaEscutai.FONTE_PEQUENA);

        JTextArea comentario = new JTextArea(
                (avaliacao.getComentario() == null || avaliacao.getComentario().isBlank())
                        ? "(Sem comentário escrito)"
                        : avaliacao.getComentario()
        );
        comentario.setEditable(false);
        comentario.setLineWrap(true);
        comentario.setWrapStyleWord(true);
        comentario.setOpaque(false);
        comentario.setForeground(TemaEscutai.BRANCO);
        comentario.setFont(TemaEscutai.FONTE_CORPO);

        JPanel informacoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        informacoes.setOpaque(false);
        informacoes.add(nomeUsuario);
        informacoes.add(nota);
        informacoes.add(data);

        painel.add(informacoes, BorderLayout.NORTH);
        painel.add(comentario, BorderLayout.CENTER);

        boolean avaliacaoDoUsuario = avaliacao.getUsuario().getUsername().equals(usuario.getUsername());
        if (avaliacaoDoUsuario) {
            TemaEscutai.BotaoArredondado botaoEditar = TemaEscutai.criarBotaoSecundario("Editar");
            TemaEscutai.BotaoArredondado botaoExcluir = TemaEscutai.criarBotaoContorno("Excluir");

            botaoEditar.addActionListener(e -> editarAvaliacao(avaliacao));
            botaoExcluir.addActionListener(e -> excluirAvaliacao(avaliacao));

            JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            botoes.setOpaque(false);
            botoes.add(botaoEditar);
            botoes.add(botaoExcluir);

            painel.add(botoes, BorderLayout.SOUTH);
        }

        painel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
        return painel;
    }

    private void editarAvaliacao(Avaliacao avaliacao) {
        Window janelaPai = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(janelaPai, "Editar avaliação", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(430, 310);
        dialog.setLocationRelativeTo(this);

        JPanel painel = new JPanel(new BorderLayout(10, 12));
        painel.setBackground(TemaEscutai.FUNDO_BASE);
        painel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.setOpaque(false);

        JLabel textoNotaEdicao = new JLabel("Escolha sua nova nota:");
        textoNotaEdicao.setForeground(TemaEscutai.BRANCO);
        textoNotaEdicao.setFont(TemaEscutai.FONTE_SUBTITULO);
        textoNotaEdicao.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel painelEstrelasEdicao = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        painelEstrelasEdicao.setOpaque(false);
        BotaoEstrela[] estrelasEdicao = new BotaoEstrela[5];
        int[] novaNota = {avaliacao.getNota()};

        for (int i = 0; i < 5; i++) {
            final int nota = i + 1;
            estrelasEdicao[i] = new BotaoEstrela(38);
            estrelasEdicao[i].setSelecionada(i < avaliacao.getNota());
            estrelasEdicao[i].addActionListener(e -> {
                novaNota[0] = nota;
                for (int j = 0; j < 5; j++) {
                    estrelasEdicao[j].setSelecionada(j < nota);
                }
            });
            painelEstrelasEdicao.add(estrelasEdicao[i]);
        }

        topo.add(textoNotaEdicao);
        topo.add(painelEstrelasEdicao);

        JTextArea campoNovoComentario = new JTextArea(avaliacao.getComentario(), 4, 25);
        campoNovoComentario.setLineWrap(true);
        campoNovoComentario.setWrapStyleWord(true);
        campoNovoComentario.setFont(TemaEscutai.FONTE_CORPO);
        campoNovoComentario.setOpaque(false);
        campoNovoComentario.setForeground(TemaEscutai.BRANCO);
        campoNovoComentario.setCaretColor(TemaEscutai.BRANCO);

        JScrollPane rolagem = new JScrollPane(campoNovoComentario);
        TemaEscutai.configurarScrollInvisivel(rolagem);

        TemaEscutai.PainelArredondado caixaTexto = new TemaEscutai.PainelArredondado(
                new BorderLayout(), 18, TemaEscutai.SUPERFICIE, TemaEscutai.BORDA_SUTIL
        );
        caixaTexto.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        caixaTexto.add(rolagem, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodape.setOpaque(false);

        TemaEscutai.BotaoArredondado btnSalvar = TemaEscutai.criarBotaoPrimario("Salvar");
        TemaEscutai.BotaoArredondado btnCancelar = TemaEscutai.criarBotaoContorno("Cancelar");

        btnCancelar.addActionListener(e -> dialog.dispose());
        btnSalvar.addActionListener(e -> {
            try {
                avaliacaoService.editarAvaliacao(
                        usuario,
                        avaliacao,
                        novaNota[0],
                        campoNovoComentario.getText().trim()
                );
                dialog.dispose();
                atualizarAvaliacoes();
            } catch (NotaInvalidaException | RuntimeException erro) {
                JOptionPane.showMessageDialog(
                        dialog,
                        erro.getMessage(),
                        "Erro ao editar",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        rodape.add(btnSalvar);
        rodape.add(btnCancelar);

        painel.add(topo, BorderLayout.NORTH);
        painel.add(caixaTexto, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);

        dialog.setContentPane(painel);
        dialog.setVisible(true);
    }

    private void excluirAvaliacao(Avaliacao avaliacao) {
        try {
            boolean excluida = avaliacaoService.excluirAvaliacao(usuario, avaliacao);
            if (excluida) {
                atualizarAvaliacoes();
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível excluir a avaliação.", "Erro", JOptionPane.ERROR_MESSAGE);
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
            if (conteudo.getLinkAppleMusic() == null) {
                JOptionPane.showMessageDialog(this, "Link do Apple Music indisponível.", "Apple Music", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Desktop.getDesktop().browse(new URI(conteudo.getLinkAppleMusic()));
        } catch (Exception erro) {
            JOptionPane.showMessageDialog(this, "Não foi possível abrir o Apple Music.", "Apple Music", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class BotaoEstrela extends JButton {

        private boolean selecionada;
        private final int tamanho;

        public BotaoEstrela(int tamanho) {
            this.tamanho = tamanho;
            setPreferredSize(new Dimension(tamanho, tamanho));
            setMinimumSize(new Dimension(tamanho, tamanho));
            setMaximumSize(new Dimension(tamanho, tamanho));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void setSelecionada(boolean selecionada) {
            this.selecionada = selecionada;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int centroX = getWidth() / 2;
            int centroY = getHeight() / 2;
            double raioExterno = tamanho * 0.43;
            double raioInterno = raioExterno * 0.45;

            Path2D estrela = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double angulo = Math.toRadians(-90 + i * 36);
                double raio = (i % 2 == 0) ? raioExterno : raioInterno;
                double x = centroX + Math.cos(angulo) * raio;
                double y = centroY + Math.sin(angulo) * raio;
                if (i == 0) {
                    estrela.moveTo(x, y);
                } else {
                    estrela.lineTo(x, y);
                }
            }
            estrela.closePath();

            g2.setColor(selecionada ? TemaEscutai.DOURADO_ESTRELA : new Color(90, 75, 105));
            g2.fill(estrela);
            g2.setStroke(new BasicStroke(1.6f));
            g2.setColor(new Color(180, 140, 48));
            g2.draw(estrela);
            g2.dispose();
        }
    }
}