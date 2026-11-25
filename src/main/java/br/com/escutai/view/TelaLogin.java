package br.com.escutai.view;

import br.com.escutai.avaliacao.repository.AvaliacaoRepository;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.biblioteca.repository.FavoritoRepository;
import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.service.UsuarioService;

import javax.swing.*;
import java.awt.*;

public class TelaLogin extends JFrame {

    private final UsuarioService usuarioService;
    private final AvaliacaoService avaliacaoService;
    private final FavoritoService favoritoService;
    private final JPanel painelPrincipal;
    private final CardLayout cardLayout;

    private JTextField campoLogin;
    private JPasswordField campoSenha;

    private JTextField campoNome;
    private JTextField campoUsername;
    private JTextField campoEmail;
    private JPasswordField campoSenhaCadastro;
    private JPasswordField campoConfirmarSenha;

    public TelaLogin(UsuarioService usuarioService) {
        this(usuarioService, new AvaliacaoService(new AvaliacaoRepository()), new FavoritoService(new FavoritoRepository()));
    }

    public TelaLogin(UsuarioService usuarioService, AvaliacaoService avaliacaoService) {
        this(usuarioService, avaliacaoService, new FavoritoService(new FavoritoRepository()));
    }

    public TelaLogin(UsuarioService usuarioService, AvaliacaoService avaliacaoService, FavoritoService favoritoService) {
        this.usuarioService = usuarioService;
        this.avaliacaoService = avaliacaoService;
        this.favoritoService = favoritoService;

        setTitle("ESCUTAÍ - Entrar");
        setSize(480, 620);
        setMinimumSize(new Dimension(440, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);

        painelPrincipal.add(criarPainelLogin(), "login");
        painelPrincipal.add(criarPainelCadastro(), "cadastro");

        add(painelPrincipal);
        cardLayout.show(painelPrincipal, "login");
    }

    private JPanel criarPainelLogin() {
        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new GridBagLayout());

        JPanel cartaoCentral = new JPanel();
        cartaoCentral.setLayout(new BoxLayout(cartaoCentral, BoxLayout.Y_AXIS));
        cartaoCentral.setOpaque(false);
        cartaoCentral.setPreferredSize(new Dimension(340, 490));

        JLabel labelLogo = new JLabel(TemaEscutai.obterLogo(165, 165));
        labelLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Entre na sua conta", SwingConstants.CENTER);
        subtitulo.setFont(TemaEscutai.FONTE_SUBTITULO);
        subtitulo.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        TemaEscutai.PainelArredondado formulario = new TemaEscutai.PainelArredondado(
                22,
                new Color(22, 9, 30, 225),
                TemaEscutai.BORDA_SUTIL
        );
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        formulario.setMaximumSize(new Dimension(340, 275));
        formulario.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblUsuario = new JLabel("Usuário ou e-mail:");
        lblUsuario.setForeground(TemaEscutai.BRANCO);
        lblUsuario.setFont(TemaEscutai.FONTE_CORPO);
        lblUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);

        campoLogin = new JTextField();
        TemaEscutai.estilizarCampoTexto(campoLogin);
        campoLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        campoLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSenha = new JLabel("Senha:");
        lblSenha.setForeground(TemaEscutai.BRANCO);
        lblSenha.setFont(TemaEscutai.FONTE_CORPO);
        lblSenha.setAlignmentX(Component.LEFT_ALIGNMENT);

        campoSenha = new JPasswordField();
        TemaEscutai.estilizarCampoTexto(campoSenha);
        campoSenha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        campoSenha.setAlignmentX(Component.LEFT_ALIGNMENT);

        TemaEscutai.BotaoArredondado botaoEntrar = TemaEscutai.criarBotaoPrimario("Entrar");
        botaoEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        botaoEntrar.setAlignmentX(Component.LEFT_ALIGNMENT);

        TemaEscutai.BotaoArredondado botaoCadastro = TemaEscutai.criarBotaoContorno("Criar conta");
        botaoCadastro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        botaoCadastro.setAlignmentX(Component.LEFT_ALIGNMENT);

        formulario.add(lblUsuario);
        formulario.add(Box.createVerticalStrut(5));
        formulario.add(campoLogin);
        formulario.add(Box.createVerticalStrut(12));
        formulario.add(lblSenha);
        formulario.add(Box.createVerticalStrut(5));
        formulario.add(campoSenha);
        formulario.add(Box.createVerticalStrut(18));
        formulario.add(botaoEntrar);
        formulario.add(Box.createVerticalStrut(8));
        formulario.add(botaoCadastro);

        cartaoCentral.add(labelLogo);
        cartaoCentral.add(Box.createVerticalStrut(10));
        cartaoCentral.add(subtitulo);
        cartaoCentral.add(Box.createVerticalStrut(18));
        cartaoCentral.add(formulario);

        fundo.add(cartaoCentral);

        botaoEntrar.addActionListener(e -> fazerLogin());
        campoSenha.addActionListener(e -> fazerLogin());
        botaoCadastro.addActionListener(e -> cardLayout.show(painelPrincipal, "cadastro"));

        return fundo;
    }

    private JPanel criarPainelCadastro() {
        TemaEscutai.PainelFundoGradiente fundo = new TemaEscutai.PainelFundoGradiente(new GridBagLayout());

        TemaEscutai.PainelArredondado caixa = new TemaEscutai.PainelArredondado(
                22,
                new Color(22, 9, 30, 230),
                TemaEscutai.BORDA_SUTIL
        );
        caixa.setLayout(new BoxLayout(caixa, BoxLayout.Y_AXIS));
        caixa.setBorder(BorderFactory.createEmptyBorder(20, 26, 20, 26));
        caixa.setPreferredSize(new Dimension(350, 505));

        JLabel titulo = new JLabel("Criar conta");
        titulo.setFont(TemaEscutai.FONTE_TITULO);
        titulo.setForeground(TemaEscutai.BRANCO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        campoNome = new JTextField();
        campoUsername = new JTextField();
        campoEmail = new JTextField();
        campoSenhaCadastro = new JPasswordField();
        campoConfirmarSenha = new JPasswordField();

        JTextField[] campos = {campoNome, campoUsername, campoEmail, campoSenhaCadastro, campoConfirmarSenha};
        for (JTextField c : campos) {
            TemaEscutai.estilizarCampoTexto(c);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        TemaEscutai.BotaoArredondado botaoCadastrar = TemaEscutai.criarBotaoPrimario("Cadastrar");
        botaoCadastrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        botaoCadastrar.setAlignmentX(Component.LEFT_ALIGNMENT);

        TemaEscutai.BotaoArredondado botaoVoltar = TemaEscutai.criarBotaoContorno("← Voltar ao login");
        botaoVoltar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        botaoVoltar.setAlignmentX(Component.LEFT_ALIGNMENT);

        caixa.add(titulo);
        caixa.add(Box.createVerticalStrut(12));
        adicionarCampoComRotulo(caixa, "Nome:", campoNome);
        adicionarCampoComRotulo(caixa, "Username:", campoUsername);
        adicionarCampoComRotulo(caixa, "E-mail:", campoEmail);
        adicionarCampoComRotulo(caixa, "Senha:", campoSenhaCadastro);
        adicionarCampoComRotulo(caixa, "Confirmar senha:", campoConfirmarSenha);
        caixa.add(Box.createVerticalStrut(10));
        caixa.add(botaoCadastrar);
        caixa.add(Box.createVerticalStrut(8));
        caixa.add(botaoVoltar);

        fundo.add(caixa);

        botaoCadastrar.addActionListener(e -> cadastrarUsuario());
        botaoVoltar.addActionListener(e -> cardLayout.show(painelPrincipal, "login"));

        return fundo;
    }

    private void adicionarCampoComRotulo(JPanel painel, String rotulo, JComponent campo) {
        JLabel label = new JLabel(rotulo);
        label.setForeground(TemaEscutai.TEXTO_SECUNDARIO);
        label.setFont(TemaEscutai.FONTE_CORPO);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(label);
        painel.add(Box.createVerticalStrut(3));
        painel.add(campo);
        painel.add(Box.createVerticalStrut(8));
    }

    private void fazerLogin() {
        String login = campoLogin.getText();
        String senha = new String(campoSenha.getPassword());

        try {
            Usuario usuario = usuarioService.login(login, senha);
            mostrarInicio(usuario);
        } catch (RuntimeException erro) {
            JOptionPane.showMessageDialog(
                    this,
                    erro.getMessage(),
                    "Erro no login",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void cadastrarUsuario() {
        String nome = campoNome.getText();
        String username = campoUsername.getText();
        String email = campoEmail.getText();
        String senha = new String(campoSenhaCadastro.getPassword());
        String confirmarSenha = new String(campoConfirmarSenha.getPassword());

        if (!senha.equals(confirmarSenha)) {
            JOptionPane.showMessageDialog(this, "As senhas não são iguais.", "Erro no cadastro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            usuarioService.cadastrar(nome, username, email, senha);
            campoLogin.setText(username);
            cardLayout.show(painelPrincipal, "login");
        } catch (RuntimeException erro) {
            JOptionPane.showMessageDialog(
                    this,
                    erro.getMessage(),
                    "Erro no cadastro",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarInicio(Usuario usuario) {
        TelaPrincipal telaPrincipal = new TelaPrincipal(
                usuario,
                usuarioService,
                avaliacaoService,
                favoritoService
        );
        telaPrincipal.setVisible(true);
        dispose();
    }
}