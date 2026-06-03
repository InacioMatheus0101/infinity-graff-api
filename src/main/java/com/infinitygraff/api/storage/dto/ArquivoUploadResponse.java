package com.infinitygraff.api.storage.dto;

/**
 * DTO de resposta para o cliente após um upload.
 * <p>
 * Retorna metadados do arquivo. A URL de acesso deve ser obtida
 * separadamente via endpoint dedicado, já que o bucket é privado
 * e o acesso se dá por URLs assinadas temporárias.
 *
 * @param nomeOriginal nome original do arquivo enviado
 * @param nomeStorage  nome gerado internamente para o storage
 * @param contentType  MIME type do arquivo
 * @param tamanho      tamanho do arquivo em bytes
 */
public record ArquivoUploadResponse(
        String nomeOriginal,
        String nomeStorage,
        String contentType,
        Long tamanho
) {
}