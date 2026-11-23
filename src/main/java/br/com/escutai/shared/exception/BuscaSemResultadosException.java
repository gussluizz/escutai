package br.com.escutai.shared.exception;

/**
 * Exceção customizada lançada quando uma busca na API do iTunes
 * não retorna nenhuma música ou álbum correspondente ao termo pesquisado,
 * ou quando o usuário envia uma pesquisa em branco.
 */
public class BuscaSemResultadosException extends Exception {

    public BuscaSemResultadosException(String mensagem) {
        super(mensagem);
    }
}
