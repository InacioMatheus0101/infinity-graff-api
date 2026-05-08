package com.infinitygraff.api.usuario.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para ativar ou desativar um usuário.
 *
 * <p>Usado em {@code PATCH /api/v1/usuarios/{id}/status}.
 *
 * <p>O campo usa {@link Boolean} em vez de {@code boolean} primitivo
 * para permitir validação correta quando o frontend omitir o campo
 * ou enviar {@code null}.
 *
 * <p>Regras de permissão não pertencem a este DTO.
 * Elas são aplicadas no {@code UsuarioService}.
 *
 * @param ativo novo status do usuário
 */
public record AtualizarStatusRequest(

        @NotNull(message = "Status ativo é obrigatório")
        Boolean ativo
) {
}