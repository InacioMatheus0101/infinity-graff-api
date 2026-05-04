package com.infinitygraff.api.security;

import com.infinitygraff.api.config.JwtProperties;
import com.infinitygraff.api.usuario.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço responsável pela geração, validação e extração de claims
 * de access tokens JWT.
 *
 * Importante:
 * O refresh token da aplicação não é JWT.
 * Ele será um token aleatório gerado com SecureRandom e salvo no banco apenas como hash SHA-256.
 */
@Slf4j
@Service
public class JwtService {

    private static final String ISSUER = "infinity-graff-api";

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NOME = "nome";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final SecretKey chaveAssinatura;
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.chaveAssinatura = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String gerarAccessToken(Usuario usuario) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiracao = agora.plusMinutes(jwtProperties.accessExpirationMinutes());

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(usuario.getId().toString())
                .claim(CLAIM_EMAIL, usuario.getEmail())
                .claim(CLAIM_NOME, usuario.getNome())
                .claim(CLAIM_ROLE, usuario.getRole().name())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(toDate(agora))
                .expiration(toDate(expiracao))
                .signWith(chaveAssinatura)
                .compact();
    }

    public boolean isTokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token JWT expirado: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public UUID extrairUsuarioId(String token) {
        return UUID.fromString(extrairClaims(token).getSubject());
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).get(CLAIM_EMAIL, String.class);
    }

    public String extrairRole(String token) {
        return extrairClaims(token).get(CLAIM_ROLE, String.class);
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chaveAssinatura)
                .requireIssuer(ISSUER)
                .require(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date toDate(OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }
}