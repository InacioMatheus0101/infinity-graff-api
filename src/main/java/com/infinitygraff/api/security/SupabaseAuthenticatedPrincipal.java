package com.infinitygraff.api.security;

import java.util.UUID;

/**
 * Principal autenticado a partir de um access token válido do Supabase Auth.
 *
 * Usado quando o token Supabase é válido, mas o usuário ainda não possui
 * perfil interno criado na tabela usuarios.
 *
 * Exemplo:
 * - POST /api/v1/autenticacao/completar-perfil
 *
 * Nesse cenário, o backend já sabe que o usuário existe no Supabase Auth,
 * mas ainda precisa criar o perfil interno do marketplace.
 */
public record SupabaseAuthenticatedPrincipal(

        UUID id,

        String email
) {
}