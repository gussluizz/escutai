package br.com.escutai;

import br.com.escutai.avaliacao.repository.AvaliacaoRepository;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.shared.persistence.RepositorioJson;
import br.com.escutai.usuario.repository.UsuarioRepository;
import br.com.escutai.usuario.service.UsuarioService;
import br.com.escutai.view.TelaLogin;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            RepositorioJson repositorioJson = new RepositorioJson();

            UsuarioRepository usuarioRepository =
                    new UsuarioRepository(repositorioJson);

            AvaliacaoRepository avaliacaoRepository =
                    new AvaliacaoRepository(repositorioJson, usuarioRepository);

            UsuarioService usuarioService =
                    new UsuarioService(usuarioRepository);

            AvaliacaoService avaliacaoService =
                    new AvaliacaoService(avaliacaoRepository);

            TelaLogin tela = new TelaLogin(usuarioService, avaliacaoService);

            tela.setVisible(true);

        });
    }
}
