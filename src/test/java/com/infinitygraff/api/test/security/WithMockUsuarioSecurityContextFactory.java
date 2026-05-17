package com.infinitygraff.api.test.security;

import com.infinitygraff.api.usuario.model.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Factory responsável por criar o SecurityContext usado por {@link WithMockUsuario}.
 *
 * <p>O contexto gerado espelha o fluxo real do backend:
 * <ul>
 *   <li>principal do tipo {@link Usuario};</li>
 *   <li>authority no formato {@code ROLE_ADMIN}, {@code ROLE_GERENTE},
 *       {@code ROLE_CLIENTE} ou {@code ROLE_PRESTADOR}.</li>
 * </ul>
 */
public class WithMockUsuarioSecurityContextFactory
        implements WithSecurityContextFactory<WithMockUsuario> {

    private static final OffsetDateTime ACEITE_TERMOS_PADRAO =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Override
    public SecurityContext createSecurityContext(WithMockUsuario annotation) {
        Usuario usuario = Usuario.builder()
                .id(UUID.fromString(annotation.id()))
                .nome(annotation.nome())
                .email(annotation.email())
                .role(annotation.role())
                .ativo(annotation.ativo())
                .aceitoTermosEm(ACEITE_TERMOS_PADRAO)
                .build();

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + annotation.role().name())
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        authorities
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        return context;
    }
}