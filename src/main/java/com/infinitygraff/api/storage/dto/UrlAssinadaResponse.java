package com.infinitygraff.api.storage.dto;

/**
 * DTO que representa a resposta da API de URL assinada do Supabase Storage.
 *
 * @param signedUrl URL assinada temporária para acesso ao arquivo privado
 * @param expiresIn tempo de expiração em segundos
 */
public record UrlAssinadaResponse(
        String signedUrl,
        int expiresIn
) {
}