package com.infinitygraff.api.storage.exception;

import com.infinitygraff.api.common.exception.NegocioException;
import org.springframework.http.HttpStatus;

/**
 * Exceção base para erros relacionados a upload de arquivos (4xx).
 * <p>
 * Herda de {@link NegocioException} e é automaticamente tratada pelo
 * {@code GlobalExceptionHandler} com o status HTTP definido.
 * <p>
 * Subclasses devem definir o status HTTP mais adequado ao contexto
 * (ex: {@link ArquivoInvalidoException} usa 422).
 * <p>
 * Erros de integração com o storage (5xx) devem usar
 * {@link StorageIntegrationException}, que <b>não</b> herda desta classe.
 */
public class UploadArquivoException extends NegocioException {

    /**
     * Constrói a exceção com a mensagem e o status HTTP.
     *
     * @param mensagem descrição do erro
     * @param status   status HTTP correspondente
     */
    public UploadArquivoException(String mensagem, HttpStatus status) {
        super(mensagem, status);
    }
}