package com.infinitygraff.api.storage.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando um arquivo não atende aos critérios técnicos
 * de validação (tamanho, formato, MIME type, etc.).
 * <p>
 * Resulta em HTTP 422 (Unprocessable Entity) — a requisição foi recebida,
 * mas o arquivo é inválido e não pode ser processado.
 */
public class ArquivoInvalidoException extends UploadArquivoException {

    /**
     * Cria uma nova exceção de arquivo inválido.
     *
     * @param mensagem descrição do problema
     */
    public ArquivoInvalidoException(String mensagem) {
        super(mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}