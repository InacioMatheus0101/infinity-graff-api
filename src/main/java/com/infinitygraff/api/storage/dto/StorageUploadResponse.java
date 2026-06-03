package com.infinitygraff.api.storage.dto;

/**
 * Resposta interna retornada após um upload bem‑sucedido.
 * <p>
 * Contém apenas o caminho do arquivo no bucket — a URL de acesso
 * deve ser obtida posteriormente via endpoint de URL assinada,
 * pois o bucket é privado.
 *
 * @param path caminho do arquivo no storage
 */
public record StorageUploadResponse(
        String path
) {
}