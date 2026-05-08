package com.infinitygraff.api.security;

import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.usuario.model.Usuario;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper central para leitura segura do SecurityContext.
 *
 * <p>Evita duplicação de lógica entre services e mantém em um único ponto
 * a regra de como obter o usuário autenticado no backend.
 *
 * <p>O Supabase Auth autentica a identidade.
 * O JwtAuthenticationFilter popula o SecurityContext com:
 * <ul>
 *   <li>{@code Usuario}, quando já existe perfil interno;</li>
 *   <li>{@code SupabaseAuthenticatedPrincipal}, quando o token é válido,
 *       mas o perfil interno ainda não existe.</li>
 * </ul>
 */
@Component
public class SecurityContextHelper {

    /**
     * Obtém a autenticação atual validada pelo Spring Security.
     *
     * @return autenticação atual
     * @throws AuthenticationCredentialsNotFoundException quando não há autenticação real
     */
    public Authentication obterAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Token ausente ou inválido");
        }

        return authentication;
    }

    /**
     * Obtém o usuário interno autenticado.
     *
     * <p>Usar em services que exigem que o perfil interno já exista na tabela {@code usuarios}.
     *
     * @return usuário interno autenticado
     * @throws RecursoNaoEncontradoException quando o token é válido, mas o perfil interno ainda não existe
     * @throws AcessoNegadoException quando o usuário está inativo ou indisponível
     */
    public Usuario obterUsuarioAutenticado() {
        Authentication authentication = obterAuthentication();

        if (!(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new RecursoNaoEncontradoException("Perfil interno ainda não foi criado");
        }

        if (!usuario.podeAcessarSistema()) {
            throw new AcessoNegadoException("Usuário inativo ou indisponível");
        }

        return usuario;
    }
}