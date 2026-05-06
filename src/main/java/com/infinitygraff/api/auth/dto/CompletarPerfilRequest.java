package com.infinitygraff.api.auth.dto;

import com.infinitygraff.api.usuario.enums.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para completar o perfil interno do usuário
 * após autenticação pelo Supabase Auth.
 *
 * O ID e o e-mail não vêm no body da requisição.
 * Eles são extraídos do access token emitido pelo Supabase.
 *
 * A senha também não vem neste DTO, pois cadastro, login,
 * senha, sessão e refresh token são responsabilidades do Supabase Auth.
 * O backend Spring cria apenas o perfil interno do marketplace.
 */
public record CompletarPerfilRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotNull(message = "Perfil é obrigatório")
        Role role,

        @NotNull(message = "Aceite dos termos é obrigatório")
        @AssertTrue(message = "É necessário aceitar os termos de uso")
        Boolean aceitouTermos
) {
}