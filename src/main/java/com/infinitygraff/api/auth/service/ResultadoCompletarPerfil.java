package com.infinitygraff.api.auth.service;

import com.infinitygraff.api.usuario.dto.UsuarioResponse;

import java.util.Objects;

/**
 * Resultado interno do fluxo de completar perfil.
 *
 * <p>Usado pelo controller para decidir o status HTTP correto:
 * <ul>
 *   <li>{@code criado = true} → 201 Created;</li>
 *   <li>{@code criado = false} → 200 OK.</li>
 * </ul>
 *
 * <p>Este record não é o DTO de resposta da API.
 * O payload público continua sendo {@link UsuarioResponse}.
 */
public record ResultadoCompletarPerfil(

        UsuarioResponse usuario,

        boolean criado
) {

    public ResultadoCompletarPerfil {
        Objects.requireNonNull(usuario, "usuario não pode ser nulo");
    }

    public static ResultadoCompletarPerfil criado(UsuarioResponse usuario) {
        return new ResultadoCompletarPerfil(usuario, true);
    }

    public static ResultadoCompletarPerfil existente(UsuarioResponse usuario) {
        return new ResultadoCompletarPerfil(usuario, false);
    }
}