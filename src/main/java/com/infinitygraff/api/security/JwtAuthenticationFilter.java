package com.infinitygraff.api.security;

import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro de autenticação JWT executado uma vez por requisição.
 *
 * Fluxo:
 * 1. Extrai o Bearer token do header Authorization.
 * 2. Valida assinatura, issuer, tipo e expiração via JwtService.
 * 3. Extrai o UUID do usuário do subject do token.
 * 4. Carrega o usuário do banco.
 * 5. Verifica se o usuário está ativo e não deletado.
 * 6. Registra a autenticação no SecurityContextHolder.
 *
 * Se qualquer etapa falhar, a requisição continua sem autenticação.
 * O SecurityConfig decide se o endpoint exige autenticação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extrairToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValido(token)) {
            log.debug("Token JWT inválido ou expirado para requisição {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        autenticar(token, request);

        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            UUID usuarioId = jwtService.extrairUsuarioId(token);

            usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
                if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) {
                    log.debug("Usuário {} inativo, bloqueado ou deletado. Autenticação negada.", usuarioId);
                    return;
                }

                UsernamePasswordAuthenticationToken autenticacao =
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                usuario.getAuthorities()
                        );

                autenticacao.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(autenticacao);

                log.debug("Usuário {} autenticado via JWT.", usuarioId);
            });

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.debug("Falha ao autenticar usuário via JWT: {}", e.getMessage());
        }
    }

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);

        if (StringUtils.hasText(header) && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }

        return null;
    }
}