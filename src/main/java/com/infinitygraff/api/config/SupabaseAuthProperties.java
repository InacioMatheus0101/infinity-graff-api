package com.infinitygraff.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de autenticação do Supabase Auth.
 *
 * O Supabase Auth é responsável por cadastro, login, sessão e refresh token.
 * O backend Spring usa estas propriedades apenas para validar o access token
 * recebido no header Authorization.
 */
@Validated
@ConfigurationProperties(prefix = "supabase.auth")
public record SupabaseAuthProperties(

        @NotBlank(message = "supabase.auth.project-url é obrigatório")
        String projectUrl,

        @NotBlank(message = "supabase.auth.jwt-secret é obrigatório")
        @Size(min = 32, message = "supabase.auth.jwt-secret deve ter no mínimo 32 caracteres")
        String jwtSecret,

        @NotBlank(message = "supabase.auth.issuer é obrigatório")
        String issuer,

        @NotBlank(message = "supabase.auth.audience é obrigatório")
        String audience
) {
}