package br.com.escutai.shared.exception;

/**
 * Exceção customizada lançada quando ocorre uma falha técnica
 * durante a comunicação com a iTunes Search API (ex: falta de internet,
 * tempo limite excedido ou código de status HTTP diferente de 200).
 */
public class ErroComunicacaoApiException extends Exception {

    public ErroComunicacaoApiException(String mensagem) {
        super(mensagem);
    }

    public ErroComunicacaoApiException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
