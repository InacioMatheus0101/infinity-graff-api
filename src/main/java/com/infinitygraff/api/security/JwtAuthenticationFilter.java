package com.infinitygraff.api.security;

import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de autenticação executado uma vez por requisição.
 *
 * Fluxo:
 * 1. Extrai o Bearer token do header Authorization.
 * 2. Valida e decodifica o access token emitido pelo Supabase Auth.
 * 3. Extrai o UUID do usuário a partir do subject do token.
 * 4. Busca o perfil interno na tabela usuarios.
 *
 * Cenários:
 * - Se o perfil interno existe e está ativo:
 *   principal = Usuario
 *   authorities = ROLE_CLIENTE, ROLE_PRESTADOR, ROLE_GERENTE ou ROLE_ADMIN
 *
 * - Se o token Supabase é válido, mas o perfil interno ainda não existe:
 *   principal = SupabaseAuthenticatedPrincipal
 *   authorities = ROLE_SUPABASE_AUTHENTICATED
 *
 * - Se o perfil interno existe, mas está inativo/deletado:
 *   não autentica no backend.
 *
 * O Supabase Auth autentica a identidade.
 * O backend valida se essa identidade já possui perfil interno e permissões no marketplace.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String ROLE_SUPABASE_AUTHENTICATED = "ROLE_SUPABASE_AUTHENTICATED";

    private final SupabaseJwtService supabaseJwtService;
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

        try {
            Jwt jwt = supabaseJwtService.validarEDecodificar(token);
            autenticarTokenSupabase(jwt, request);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            log.debug(
                    "Token Supabase inválido para requisição {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    e.getMessage()
            );
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarTokenSupabase(Jwt jwt, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UUID usuarioId = supabaseJwtService.extrairUsuarioId(jwt);
        String email = supabaseJwtService.extrairEmail(jwt);

        usuarioRepository.findById(usuarioId)
                .ifPresentOrElse(
                        usuario -> autenticarUsuarioInterno(usuario, request),
                        () -> autenticarUsuarioSupabaseSemPerfil(usuarioId, email, request)
                );
    }

    private void autenticarUsuarioInterno(Usuario usuario, HttpServletRequest request) {
        if (!usuario.podeAcessarSistema()) {
            SecurityContextHolder.clearContext();
            log.debug(
                    "Usuário {} possui perfil interno, mas está inativo ou deletado. Autenticação negada.",
                    usuario.getId()
            );
            return;
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())
        );

        registrarAutenticacao(usuario, authorities, request);

        log.debug("Usuário {} autenticado no backend com role {}.", usuario.getId(), usuario.getRole());
    }

    private void autenticarUsuarioSupabaseSemPerfil(
            UUID usuarioId,
            String email,
            HttpServletRequest request
    ) {
        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(usuarioId, email);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(ROLE_SUPABASE_AUTHENTICATED)
        );

        registrarAutenticacao(principal, authorities, request);

        log.debug(
                "Usuário {} autenticado no Supabase, mas ainda sem perfil interno no backend.",
                usuarioId
        );
    }

    private void registrarAutenticacao(
            Object principal,
            List<SimpleGrantedAuthority> authorities,
            HttpServletRequest request
    ) {
        UsernamePasswordAuthenticationToken autenticacao =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

        autenticacao.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(autenticacao);
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