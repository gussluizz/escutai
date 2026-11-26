package br.com.escutai;
import br.com.escutai.avaliacao.repository.AvaliacaoRepository;
import br.com.escutai.avaliacao.service.AvaliacaoService;
import br.com.escutai.biblioteca.repository.FavoritoRepository;
import br.com.escutai.biblioteca.service.FavoritoService;
import br.com.escutai.shared.persistence.RepositorioJson;
import br.com.escutai.usuario.repository.UsuarioRepository;
import br.com.escutai.usuario.service.UsuarioService;
import br.com.escutai.view.TelaLogin;
import javax.swing.SwingUtilities;
public class App {
 public static void main(String[] args) { SwingUtilities.invokeLater(() -> {
  RepositorioJson json = new RepositorioJson();
  UsuarioRepository usuarios = new UsuarioRepository(json);
  AvaliacaoRepository avaliacoes = new AvaliacaoRepository(json,usuarios);
  UsuarioService usuarioService = new UsuarioService(usuarios);
  AvaliacaoService avaliacaoService = new AvaliacaoService(avaliacoes);
  FavoritoService favoritoService = new FavoritoService(new FavoritoRepository());
  TelaLogin tela = new TelaLogin(usuarioService,avaliacaoService,favoritoService); tela.setVisible(true);
 }); }
}
