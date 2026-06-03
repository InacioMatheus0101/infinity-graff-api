package com.infinitygraff.api.storage.exception;

/**
 * Exceção lançada quando ocorre uma falha de comunicação com o
 * Supabase Storage (erros de rede, timeouts, respostas 5xx).
 * <p>
 * Representa um problema de infraestrutura e <b>não</b> é culpa do
 * cliente — portanto não herda de {@code UploadArquivoException}
 * nem de {@code NegocioException}.
 * <p>
 * O {@code GlobalExceptionHandler} deve tratá‑la como erro HTTP 502
 * (Bad Gateway) ou 503 (Service Unavailable).
 */
public class StorageIntegrationException extends RuntimeException {

    /**
     * Cria a exceção com uma mensagem descritiva.
     *
     * @param mensagem descrição do erro
     */
    public StorageIntegrationException(String mensagem) {
        super(mensagem);
    }

    /**
     * Cria a exceção com mensagem e causa.
     *
     * @param mensagem descrição do erro
     * @param causa    exceção original
     */
    public StorageIntegrationException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}