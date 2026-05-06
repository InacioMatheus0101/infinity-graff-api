package com.infinitygraff.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de autenticação do Supabase Auth.
 *
 * O Supabase Auth é responsável por cadastro, login, sessão e refresh token.
 *
 * O backend Spring usa estas propriedades apenas para validar o access token
 * recebido no header Authorization.
 *
 * Como os tokens do projeto estão assinados com ES256, a validação deve ser feita
 * por JWKS/Signing Keys, não por Legacy JWT Secret.
 */

@Validated
@ConfigurationProperties(prefix = "supabase.auth")
public record SupabaseAuthProperties(

        @NotBlank(message = "supabase.auth.project-url é obrigatório")
        String projectUrl,

        @NotBlank(message = "supabase.auth.issuer é obrigatório")
        String issuer,

        @NotBlank(message = "supabase.auth.audience é obrigatório")
        String audience,

        @NotBlank(message = "supabase.auth.jwks-url é obrigatório")
        String jwksUrl
) {
}