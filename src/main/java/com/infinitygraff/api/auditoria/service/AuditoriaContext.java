package com.infinitygraff.api.auditoria.service;

/**
 * Contexto técnico da requisição usado nos logs de auditoria.
 *
 * <p>Este record é interno da camada de auditoria.
 * Não é DTO público da API.
 *
 * @param ip IP identificado na requisição
 * @param userAgent User-Agent enviado pelo cliente HTTP
 */
public record AuditoriaContext(

        String ip,

        String userAgent
) {

    public static AuditoriaContext vazio() {
        return new AuditoriaContext(null, null);
    }
}