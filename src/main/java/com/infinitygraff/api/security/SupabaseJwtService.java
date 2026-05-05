package com.infinitygraff.api.security;

import com.infinitygraff.api.config.SupabaseAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

/**
 * Serviço responsável por validar access tokens emitidos pelo Supabase Auth.
 *
 * O backend Spring não gera access token nem refresh token.
 * Cadastro, login, sessão e refresh token são responsabilidade do Supabase Auth.
 *
 * Este serviço apenas:
 * - valida assinatura do JWT;
 * - valida issuer;
 * - valida audience;
 * - extrai o subject, que representa o UUID do usuário no Supabase Auth.
 */
@Slf4j
@Service
public class SupabaseJwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_AUDIENCE = "aud";

    private final SupabaseAuthProperties supabaseAuthProperties;
    private final SecretKey chaveAssinatura;

    public SupabaseJwtService(SupabaseAuthProperties supabaseAuthProperties) {
        this.supabaseAuthProperties = supabaseAuthProperties;
        this.chaveAssinatura = Keys.hmacShaKeyFor(
                supabaseAuthProperties.jwtSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean isTokenValido(String token) {
        try {
            Claims claims = extrairClaims(token);
            return audienceValida(claims);
        } catch (ExpiredJwtException e) {
            log.debug("Token Supabase expirado: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token Supabase inválido: {}", e.getMessage());
            return false;
        }
    }

    public UUID extrairUsuarioId(String token) {
        String subject = extrairClaims(token).getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtException("Token Supabase sem subject");
        }

        return UUID.fromString(subject);
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).get(CLAIM_EMAIL, String.class);
    }

    private Claims extrairClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chaveAssinatura)
                .requireIssuer(supabaseAuthProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!audienceValida(claims)) {
            throw new JwtException("Audience inválida no token Supabase");
        }

        return claims;
    }

    private boolean audienceValida(Claims claims) {
        Object audience = claims.get(CLAIM_AUDIENCE);

        if (audience instanceof String aud) {
            return supabaseAuthProperties.audience().equals(aud);
        }

        if (audience instanceof Collection<?> audiences) {
            return audiences.contains(supabaseAuthProperties.audience());
        }

        return false;
    }
}