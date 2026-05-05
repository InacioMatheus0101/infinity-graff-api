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
 * 2. Valida o token emitido pelo Supabase Auth.
 * 3. Extrai o UUID do usuário a partir do subject do token.
 * 4. Busca o perfil interno na tabela usuarios.
 * 5. Verifica se o usuário está ativo e não deletado.
 * 6. Registra a autenticação no SecurityContextHolder com as roles internas do sistema.
 *
 * O Supabase Auth autentica a identidade.
 * O backend valida se esse usuário pode acessar o marketplace.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";

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

        if (!supabaseJwtService.isTokenValido(token)) {
            log.debug(
                    "Token Supabase inválido ou expirado para requisição {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        autenticarUsuarioInterno(token, request);

        filterChain.doFilter(request, response);
    }

    private void autenticarUsuarioInterno(String token, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            UUID usuarioId = supabaseJwtService.extrairUsuarioId(token);

            usuarioRepository.findByIdAndAtivoTrue(usuarioId)
                    .filter(Usuario::podeAcessarSistema)
                    .ifPresentOrElse(
                            usuario -> registrarAutenticacao(usuario, request),
                            () -> log.debug(
                                    "Usuário {} autenticado no Supabase, mas sem perfil interno ativo no backend.",
                                    usuarioId
                            )
                    );

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.debug("Falha ao autenticar token Supabase no backend: {}", e.getMessage());
        }
    }

    private void registrarAutenticacao(Usuario usuario, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())
        );

        UsernamePasswordAuthenticationToken autenticacao =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        authorities
                );

        autenticacao.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(autenticacao);

        log.debug("Usuário {} autenticado no backend com role {}.", usuario.getId(), usuario.getRole());
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