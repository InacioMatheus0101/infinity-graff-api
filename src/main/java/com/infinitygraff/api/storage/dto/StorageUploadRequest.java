package com.infinitygraff.api.storage.dto;

/**
 * DTO interno que encapsula os dados necessários para uma operação
 * de upload no {@code SupabaseStorageService}.
 *
 * @param path        caminho completo do arquivo no bucket
 * @param contentType MIME type do arquivo
 * @param bytes       conteúdo binário do arquivo
 */
public record StorageUploadRequest(
        String path,
        String contentType,
        byte[] bytes
) {
}