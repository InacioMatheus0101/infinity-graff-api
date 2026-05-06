package com.infinitygraff.api.security;

import com.infinitygraff.api.config.SupabaseAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Serviço responsável por validar access tokens emitidos pelo Supabase Auth.
 *
 * O backend Spring não gera access token nem refresh token.
 * Cadastro, login, senha, sessão e refresh token são responsabilidade do Supabase Auth.
 *
 * Como os tokens do projeto são assinados com ES256, a validação é feita
 * usando JWKS/Signing Keys do Supabase, não Legacy JWT Secret.
 *
 * Este serviço:
 * - valida assinatura via JWKS;
 * - valida expiração;
 * - valida issuer;
 * - valida audience;
 * - extrai subject, que representa o UUID do usuário no Supabase Auth;
 * - extrai email.
 */
@Slf4j
@Service
public class SupabaseJwtService {

    private static final String CLAIM_EMAIL = "email";

    private final SupabaseAuthProperties supabaseAuthProperties;
    private final JwtDecoder jwtDecoder;

    public SupabaseJwtService(SupabaseAuthProperties supabaseAuthProperties) {
        this.supabaseAuthProperties = supabaseAuthProperties;

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(supabaseAuthProperties.jwksUrl())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();

        decoder.setJwtValidator(criarValidador());

        this.jwtDecoder = decoder;
    }

    /**
     * Valida e decodifica o token uma única vez.
     *
     * O JwtAuthenticationFilter deve usar este método e reutilizar o Jwt retornado
     * para extrair id, email e demais claims, evitando múltiplas validações na mesma requisição.
     */
    public Jwt validarEDecodificar(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * Método utilitário para validações pontuais.
     *
     * No fluxo principal de autenticação, prefira usar validarEDecodificar(token)
     * para evitar decodificações repetidas.
     */
    public boolean isTokenValido(String token) {
        try {
            validarEDecodificar(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token Supabase inválido: {}", e.getMessage());
            return false;
        }
    }

    public UUID extrairUsuarioId(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtException("Token Supabase sem subject");
        }

        return UUID.fromString(subject);
    }

    public String extrairEmail(Jwt jwt) {
        return jwt.getClaimAsString(CLAIM_EMAIL);
    }

    private OAuth2TokenValidator<Jwt> criarValidador() {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(supabaseAuthProperties.issuer()),
                validarAudience()
        );
    }

    private OAuth2TokenValidator<Jwt> validarAudience() {
        return jwt -> {
            if (jwt.getAudience().contains(supabaseAuthProperties.audience())) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error erro = new OAuth2Error(
                    "invalid_token",
                    "Audience inválida no token Supabase",
                    null
            );

            return OAuth2TokenValidatorResult.failure(erro);
        };
    }
}