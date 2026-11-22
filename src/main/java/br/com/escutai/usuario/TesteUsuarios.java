package br.com.escutai.usuario;

import br.com.escutai.usuario.repository.UsuarioRepository;
import br.com.escutai.usuario.service.UsuarioService;
import br.com.escutai.usuario.view.UsuarioView;

public class TesteUsuarios {

    public static void main(String[] args) {
        UsuarioRepository repository = new UsuarioRepository();
        UsuarioService service = new UsuarioService(repository);
        UsuarioView view = new UsuarioView(service);

        System.out.println("=== 1. Testando Cadastros Válidos ===");
        view.cadastrar("Ana Silva", "anasilva", "ana@email.com", "123456");
        view.cadastrar("Carlos Souza", "carloss", "carlos@email.com", "abcdef");

        System.out.println("\n=== 2. Testando Erros de Cadastro ===");
        view.cadastrar("", "semnome", "semnome@email.com", "123"); // Nome vazio
        view.cadastrar("Outra Ana", "ana2", "ana@email.com", "999999"); // E-mail duplicado
        view.cadastrar("Outro Carlos", "carloss", "novo@email.com", "999999"); // Username duplicado

        view.exibirUsuariosCadastrados();

        System.out.println("\n=== 3. Testando Login e Logout ===");
        view.login("ana@email.com", "senhaerrada"); // Senha incorreta
        view.login("anasilva", "123456"); // Login por username
        view.logout(); // Logout
        view.logout(); // Tentando logout sem ninguém logado
    }
}