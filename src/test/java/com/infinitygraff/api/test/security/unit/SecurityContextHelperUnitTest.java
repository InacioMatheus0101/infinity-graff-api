package com.infinitygraff.api.test.security.unit;

import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.security.SupabaseAuthenticatedPrincipal;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do {@link SecurityContextHelper}.
 *
 * <p>Valida a leitura segura do {@code SecurityContext}:
 * <ul>
 *   <li>autenticação válida;</li>
 *   <li>ausência de autenticação;</li>
 *   <li>autenticação anônima;</li>
 *   <li>autenticação não autenticada;</li>
 *   <li>principal interno do tipo {@link Usuario};</li>
 *   <li>token válido sem perfil interno;</li>
 *   <li>usuário inativo;</li>
 *   <li>usuário deletado.</li>
 * </ul>
 */
class SecurityContextHelperUnitTest {

    private static final OffsetDateTime ACEITE_TERMOS =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    private final SecurityContextHelper securityContextHelper =
            new SecurityContextHelper();

    @AfterEach
    void limparSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornarAuthenticationQuandoAutenticacaoForValida() {
        UsernamePasswordAuthenticationToken authentication =
                authenticationAutenticada("principal-valido");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        var resultado = securityContextHelper.obterAuthentication();

        assertThat(resultado).isSameAs(authentication);
        assertThat(resultado.isAuthenticated()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoNaoExistirAutenticacaoNoContexto() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityContextHelper.obterAuthentication())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Token ausente ou inválido");
    }

    @Test
    void deveLancarExcecaoQuandoAutenticacaoForAnonima() {
        AnonymousAuthenticationToken authentication =
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> securityContextHelper.obterAuthentication())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Token ausente ou inválido");
    }

    @Test
    void deveLancarExcecaoQuandoAuthenticationNaoEstiverAutenticada() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "principal-nao-autenticado",
                        null
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(authentication.isAuthenticated()).isFalse();

        assertThatThrownBy(() -> securityContextHelper.obterAuthentication())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Token ausente ou inválido");
    }

    @Test
    void deveRetornarUsuarioAutenticadoQuandoPrincipalForUsuarioAtivo() {
        Usuario usuario = usuarioAtivo();

        SecurityContextHolder.getContext().setAuthentication(
                authenticationAutenticada(usuario)
        );

        Usuario resultado = securityContextHelper.obterUsuarioAutenticado();

        assertThat(resultado).isSameAs(usuario);
        assertThat(resultado.getId()).isEqualTo(usuario.getId());
        assertThat(resultado.podeAcessarSistema()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoTokenForValidoMasPerfilInternoAindaNaoExistir() {
        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(
                        UUID.randomUUID(),
                        "usuario.sem.perfil@infinitygraff.com"
                );

        SecurityContextHolder.getContext().setAuthentication(
                authenticationAutenticada(principal)
        );

        assertThatThrownBy(() -> securityContextHelper.obterUsuarioAutenticado())
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Perfil interno ainda não foi criado");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioEstiverInativo() {
        Usuario usuario = usuarioInativo();

        SecurityContextHolder.getContext().setAuthentication(
                authenticationAutenticada(usuario)
        );

        assertThatThrownBy(() -> securityContextHelper.obterUsuarioAutenticado())
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Usuário inativo ou indisponível");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioEstiverDeletado() {
        Usuario usuario = usuarioAtivo();
        usuario.deletar();

        SecurityContextHolder.getContext().setAuthentication(
                authenticationAutenticada(usuario)
        );

        assertThatThrownBy(() -> securityContextHelper.obterUsuarioAutenticado())
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Usuário inativo ou indisponível");
    }

    @Test
    void devePropagarExcecaoDeCredenciaisAoObterUsuarioQuandoNaoHouverAutenticacao() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityContextHelper.obterUsuarioAutenticado())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("Token ausente ou inválido");
    }

    private UsernamePasswordAuthenticationToken authenticationAutenticada(Object principal) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
    }

    private Usuario usuarioAtivo() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Ativo")
                .email("usuario.ativo@infinitygraff.com")
                .role(Role.CLIENTE)
                .ativo(true)
                .aceitoTermosEm(ACEITE_TERMOS)
                .build();
    }

    private Usuario usuarioInativo() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Inativo")
                .email("usuario.inativo@infinitygraff.com")
                .role(Role.CLIENTE)
                .ativo(false)
                .aceitoTermosEm(ACEITE_TERMOS)
                .build();
    }
}