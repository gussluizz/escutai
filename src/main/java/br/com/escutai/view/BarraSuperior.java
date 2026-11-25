package br.com.escutai.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class BarraSuperior extends JPanel {

    public static final String OPCAO_NOVIDADES = "Novidades";
    public static final String OPCAO_PERFIL = "Perfil";
    public static final String OPCAO_FAVORITOS = "Favoritos";

    private final TemaEscutai.BotaoArredondado btnNovidades;
    private final TemaEscutai.BotaoArredondado btnPerfil;
    private final TemaEscutai.BotaoArredondado btnFavoritos;
    private final JTextField campoBusca;

    public BarraSuperior(String telaAtiva, Consumer<String> aoSelecionarTela,
                         Consumer<String> aoPesquisar, Runnable aoSair) {
        setLayout(new BorderLayout(16, 0));
        setBackground(new Color(18, 7, 25));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TemaEscutai.BORDA_SUTIL),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Esquerda: Logo + Wordmark + Botões NOVIDADES, PERFIL e FAVORITOS
        JPanel painelEsquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        painelEsquerda.setOpaque(false);

        MouseAdapter irParaInicio = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                aoSelecionarTela.accept(OPCAO_NOVIDADES);
            }
        };

        JLabel labelLogo = new JLabel(TemaEscutai.obterLogo(42, 42));
        labelLogo.setToolTipText("ESCUTAÍ");
        labelLogo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        labelLogo.addMouseListener(irParaInicio);

        JLabel labelWordmark = new JLabel(TemaEscutai.obterWordmark(24));
        labelWordmark.setToolTipText("ESCUTAÍ");
        labelWordmark.setCursor(new Cursor(Cursor.HAND_CURSOR));
        labelWordmark.addMouseListener(irParaInicio);

        btnNovidades = criarBotaoNavegacao("NOVIDADES");
        btnPerfil = criarBotaoNavegacao("PERFIL");
        btnFavoritos = criarBotaoNavegacao("FAVORITOS");

        btnNovidades.addActionListener(e -> aoSelecionarTela.accept(OPCAO_NOVIDADES));
        btnPerfil.addActionListener(e -> aoSelecionarTela.accept(OPCAO_PERFIL));
        btnFavoritos.addActionListener(e -> aoSelecionarTela.accept(OPCAO_FAVORITOS));

        painelEsquerda.add(labelLogo);
        painelEsquerda.add(labelWordmark);
        painelEsquerda.add(Box.createHorizontalStrut(8));
        painelEsquerda.add(btnNovidades);
        painelEsquerda.add(btnPerfil);
        painelEsquerda.add(btnFavoritos);

        // Centro: Barra de pesquisa expansível + Botão Lupa vetorial
        JPanel painelCentro = new JPanel(new GridBagLayout());
        painelCentro.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 6, 0, 8);

        TemaEscutai.PainelArredondado envoltorioCampoBusca = new TemaEscutai.PainelArredondado(
                new BorderLayout(),
                24,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.BORDA_SUTIL
        );
        envoltorioCampoBusca.setPreferredSize(new Dimension(220, 38));
        envoltorioCampoBusca.setMinimumSize(new Dimension(120, 38));

        campoBusca = new JTextField();
        campoBusca.setOpaque(false);
        campoBusca.setForeground(TemaEscutai.BRANCO);
        campoBusca.setCaretColor(TemaEscutai.BRANCO);
        campoBusca.setFont(TemaEscutai.FONTE_CORPO);
        campoBusca.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        campoBusca.setToolTipText("Pesquisar músicas ou artistas no iTunes");
        envoltorioCampoBusca.add(campoBusca, BorderLayout.CENTER);

        painelCentro.add(envoltorioCampoBusca, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);

        TemaEscutai.BotaoLupa botaoBuscar = new TemaEscutai.BotaoLupa();
        botaoBuscar.setToolTipText("Pesquisar");
        painelCentro.add(botaoBuscar, gbc);

        Runnable dispararBusca = () -> {
            if (aoPesquisar != null) {
                aoPesquisar.accept(campoBusca.getText().trim());
            }
        };
        botaoBuscar.addActionListener(e -> dispararBusca.run());
        campoBusca.addActionListener(e -> dispararBusca.run());

        // Direita: Botão SAIR em CAPS LOCK arredondado
        JPanel painelDireita = new JPanel(new GridBagLayout());
        painelDireita.setOpaque(false);

        TemaEscutai.BotaoArredondado botaoSair = criarBotaoNavegacao("SAIR");
        botaoSair.addActionListener(e -> aoSair.run());
        painelDireita.add(botaoSair);

        add(painelEsquerda, BorderLayout.WEST);
        add(painelCentro, BorderLayout.CENTER);
        add(painelDireita, BorderLayout.EAST);

        definirTelaSelecionada(telaAtiva);
    }

    private TemaEscutai.BotaoArredondado criarBotaoNavegacao(String rotulo) {
        TemaEscutai.BotaoArredondado botao = new TemaEscutai.BotaoArredondado(
                rotulo,
                TemaEscutai.SUPERFICIE,
                TemaEscutai.SUPERFICIE_HOVER,
                TemaEscutai.BORDA_SUTIL,
                26
        );
        botao.setFont(new Font("SansSerif", Font.BOLD, 13));
        botao.setPadding(9, 18, 9, 18);
        return botao;
    }

    public void definirTelaSelecionada(String tela) {
        aplicarEstadoAtivo(btnNovidades, OPCAO_NOVIDADES.equals(tela));
        aplicarEstadoAtivo(btnPerfil, OPCAO_PERFIL.equals(tela));
        aplicarEstadoAtivo(btnFavoritos, OPCAO_FAVORITOS.equals(tela));
    }

    private void aplicarEstadoAtivo(TemaEscutai.BotaoArredondado botao, boolean ativo) {
        if (ativo) {
            botao.setCores(TemaEscutai.MAGENTA, TemaEscutai.MAGENTA_HOVER, TemaEscutai.MAGENTA);
        } else {
            botao.setCores(TemaEscutai.SUPERFICIE, TemaEscutai.SUPERFICIE_HOVER, TemaEscutai.BORDA_SUTIL);
        }
    }
}