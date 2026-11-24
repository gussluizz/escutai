
package br.com.escutai.view;

import br.com.escutai.usuario.model.Usuario;
import br.com.escutai.usuario.service.UsuarioService;
import br.com.escutai.avaliacao.repository.AvaliacaoRepository;
import br.com.escutai.avaliacao.service.AvaliacaoService;

import javax.swing.*;
import java.awt.*;

public class TelaLogin extends JFrame {

    private UsuarioService usuarioService;
    private AvaliacaoService avaliacaoService;
    private JPanel painelPrincipal;
    private CardLayout cardLayout;

    private JTextField campoLogin;
    private JPasswordField campoSenha;

    private JTextField campoNome;
    private JTextField campoUsername;
    private JTextField campoEmail;
    private JPasswordField campoSenhaCadastro;
    private JPasswordField campoConfirmarSenha;

    public TelaLogin(UsuarioService usuarioService) {
        this(usuarioService, new AvaliacaoService(new AvaliacaoRepository()));
    }

    public TelaLogin(UsuarioService usuarioService, AvaliacaoService avaliacaoService) {

        this.usuarioService = usuarioService;
        this.avaliacaoService = avaliacaoService;

        setTitle("ESCUTAÍ");
        setSize(400, 450);
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

        JPanel painel = new JPanel(new GridLayout(0, 1, 10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel titulo = new JLabel("ESCUTAÍ", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));

        JLabel subtitulo = new JLabel(
                "Entre na sua conta",
                SwingConstants.CENTER
        );

        campoLogin = new JTextField();
        campoSenha = new JPasswordField();

        JButton botaoEntrar = new JButton("Entrar");
        JButton botaoCadastro = new JButton("Criar conta");

        painel.add(titulo);
        painel.add(subtitulo);
        painel.add(new JLabel("E-mail ou username:"));
        painel.add(campoLogin);
        painel.add(new JLabel("Senha:"));
        painel.add(campoSenha);
        painel.add(botaoEntrar);
        painel.add(botaoCadastro);

        botaoEntrar.addActionListener(e -> fazerLogin());

        botaoCadastro.addActionListener(e ->
                cardLayout.show(painelPrincipal, "cadastro")
        );

        return painel;
    }

    private JPanel criarPainelCadastro() {

        JPanel painel = new JPanel(new GridLayout(0, 1, 7, 7));
        painel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JLabel titulo = new JLabel(
                "Criar conta",
                SwingConstants.CENTER
        );

        titulo.setFont(new Font("Arial", Font.BOLD, 24));

        campoNome = new JTextField();
        campoUsername = new JTextField();
        campoEmail = new JTextField();
        campoSenhaCadastro = new JPasswordField();
        campoConfirmarSenha = new JPasswordField();

        JButton botaoCadastrar = new JButton("Cadastrar");
        JButton botaoVoltar = new JButton("Voltar");

        painel.add(titulo);
        painel.add(new JLabel("Nome:"));
        painel.add(campoNome);
        painel.add(new JLabel("Username:"));
        painel.add(campoUsername);
        painel.add(new JLabel("E-mail:"));
        painel.add(campoEmail);
        painel.add(new JLabel("Senha:"));
        painel.add(campoSenhaCadastro);
        painel.add(new JLabel("Confirmar senha:"));
        painel.add(campoConfirmarSenha);
        painel.add(botaoCadastrar);
        painel.add(botaoVoltar);

        botaoCadastrar.addActionListener(e -> cadastrarUsuario());

        botaoVoltar.addActionListener(e ->
                cardLayout.show(painelPrincipal, "login")
        );

        return painel;
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
        String confirmarSenha = new String(
                campoConfirmarSenha.getPassword()
        );

        if (!senha.equals(confirmarSenha)) {

            JOptionPane.showMessageDialog(
                    this,
                    "As senhas não são iguais."
            );

            return;
        }

        try {

            usuarioService.cadastrar(nome, username, email, senha);

            JOptionPane.showMessageDialog(
                    this,
                    "Cadastro realizado com sucesso!"
            );

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
                avaliacaoService
        );

        telaPrincipal.setVisible(true);

        dispose();
    }
}
