package br.com.escutai.persistencia;

import br.com.escutai.modelo.Avaliacao;
import br.com.escutai.modelo.Usuario;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepositorioJson {
    private final Path pastaDados;
    private final Path arquivoUsuarios;
    private final Path arquivoAvaliacoes;

    public RepositorioJson() {
        this(Paths.get("dados"));
    }

    public RepositorioJson(Path pastaDados) {
        this.pastaDados = pastaDados;
        this.arquivoUsuarios = pastaDados.resolve("usuarios.json");
        this.arquivoAvaliacoes = pastaDados.resolve("avaliacoes.json");
    }

    public void salvarUsuarios(List<Usuario> usuarios) throws IOException {
        prepararPasta();
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < usuarios.size(); i++) {
            Usuario u = usuarios.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escapar(u.getId())).append("\",\n");
            sb.append("    \"nome\": \"").append(escapar(u.getNome())).append("\",\n");
            sb.append("    \"email\": \"").append(escapar(u.getEmail())).append("\"\n");
            sb.append("  }").append(i < usuarios.size() - 1 ? "," : "").append("\n");
        }
        sb.append("]\n");
        Files.writeString(arquivoUsuarios, sb.toString(), StandardCharsets.UTF_8);
    }

    public List<Usuario> carregarUsuarios() throws IOException {
        List<Usuario> lista = new ArrayList<>();
        String conteudo = lerArquivoSeExistir(arquivoUsuarios);
        if (conteudo.isBlank()) {
            return lista;
        }
        for (String objeto : extrairObjetos(conteudo)) {
            String id = extrairCampoString(objeto, "id");
            String nome = extrairCampoString(objeto, "nome");
            String email = extrairCampoString(objeto, "email");
            lista.add(new Usuario(id, nome, email));
        }
        return lista;
    }

    public void salvarAvaliacoes(List<Avaliacao> avaliacoes) throws IOException {
        prepararPasta();
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < avaliacoes.size(); i++) {
            Avaliacao a = avaliacoes.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escapar(a.getId())).append("\",\n");
            sb.append("    \"usuarioEmail\": \"").append(escapar(a.getUsuarioEmail())).append("\",\n");
            sb.append("    \"conteudoTitulo\": \"").append(escapar(a.getConteudoTitulo())).append("\",\n");
            sb.append("    \"tipoConteudo\": \"").append(escapar(a.getTipoConteudo())).append("\",\n");
            sb.append("    \"nota\": ").append(a.getNota()).append(",\n");
            sb.append("    \"comentario\": \"").append(escapar(a.getComentario())).append("\"\n");
            sb.append("  }").append(i < avaliacoes.size() - 1 ? "," : "").append("\n");
        }
        sb.append("]\n");
        Files.writeString(arquivoAvaliacoes, sb.toString(), StandardCharsets.UTF_8);
    }

    public List<Avaliacao> carregarAvaliacoes() throws IOException {
        List<Avaliacao> lista = new ArrayList<>();
        String conteudo = lerArquivoSeExistir(arquivoAvaliacoes);
        if (conteudo.isBlank()) {
            return lista;
        }
        for (String objeto : extrairObjetos(conteudo)) {
            String id = extrairCampoString(objeto, "id");
            String usuarioEmail = extrairCampoString(objeto, "usuarioEmail");
            String conteudoTitulo = extrairCampoString(objeto, "conteudoTitulo");
            String tipoConteudo = extrairCampoString(objeto, "tipoConteudo");
            int nota = extrairCampoInt(objeto, "nota");
            String comentario = extrairCampoString(objeto, "comentario");
            lista.add(new Avaliacao(id, usuarioEmail, conteudoTitulo, tipoConteudo, nota, comentario));
        }
        return lista;
    }

    private void prepararPasta() throws IOException {
        if (Files.notExists(pastaDados)) {
            Files.createDirectories(pastaDados);
        }
    }

    private String lerArquivoSeExistir(Path arquivo) throws IOException {
        if (Files.notExists(arquivo)) {
            return "";
        }
        return Files.readString(arquivo, StandardCharsets.UTF_8);
    }

    private List<String> extrairObjetos(String jsonArray) {
        List<String> objetos = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{[^{}]*\\}").matcher(jsonArray);
        while (matcher.find()) {
            objetos.add(matcher.group());
        }
        return objetos;
    }

    private String extrairCampoString(String objeto, String chave) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(objeto);
        if (!matcher.find()) {
            return "";
        }
        return desescapar(matcher.group(1));
    }

    private int extrairCampoInt(String objeto, String chave) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(chave) + "\"\\s*:\\s*(-?\\d+)").matcher(objeto);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String desescapar(String texto) {
        return texto
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}