package com.infinitygraff.api.test.auth.service.unit;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.auth.dto.CompletarPerfilRequest;
import com.infinitygraff.api.auth.service.AutenticacaoService;
import com.infinitygraff.api.auth.service.ResultadoCompletarPerfil;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.security.SupabaseAuthenticatedPrincipal;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link AutenticacaoService}.
 *
 * <p>Valida:
 * <ul>
 *   <li>criação de perfil interno a partir de principal Supabase;</li>
 *   <li>sincronização idempotente quando o perfil já existe;</li>
 *   <li>regras de role pública;</li>
 *   <li>conflitos por ID e e-mail;</li>
 *   <li>auditoria de cadastro de usuário;</li>
 *   <li>retorno do próprio perfil.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceUnitTest {

    private static final OffsetDateTime ACEITE_TERMOS =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    private static final AuditoriaContext CONTEXTO_AUDITORIA =
            new AuditoriaContext("127.0.0.1", "JUnit/Auth");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private AuditoriaHelper auditoriaHelper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AutenticacaoService autenticacaoService;

    @Test
    void deveCriarPerfilInternoComSucessoERegistrarAuditoria() {
        configurarAuditoriaExecutandoImediatamente();

        UUID usuarioId = UUID.randomUUID();

        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(
                        usuarioId,
                        "  Cliente.Teste@InfinityGraff.com  "
                );

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(usuarioRepository.existsById(usuarioId)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("cliente.teste@infinitygraff.com"))
                .thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario salvo = invocation.getArgument(0);
                    preencherDatasAuditoria(salvo);
                    return salvo;
                });

        OffsetDateTime antesDaCriacao = OffsetDateTime.now(ZoneOffset.UTC);

        ResultadoCompletarPerfil resultado = autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "  Cliente Teste  ",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        );

        OffsetDateTime depoisDaCriacao = OffsetDateTime.now(ZoneOffset.UTC);

        assertThat(resultado.criado()).isTrue();
        assertThat(resultado.usuario().id()).isEqualTo(usuarioId);
        assertThat(resultado.usuario().nome()).isEqualTo("Cliente Teste");
        assertThat(resultado.usuario().email()).isEqualTo("cliente.teste@infinitygraff.com");
        assertThat(resultado.usuario().role()).isEqualTo(Role.CLIENTE);
        assertThat(resultado.usuario().ativo()).isTrue();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());

        Usuario salvo = usuarioCaptor.getValue();

        assertThat(salvo.getId()).isEqualTo(usuarioId);
        assertThat(salvo.getNome()).isEqualTo("Cliente Teste");
        assertThat(salvo.getEmail()).isEqualTo("cliente.teste@infinitygraff.com");
        assertThat(salvo.getRole()).isEqualTo(Role.CLIENTE);
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getCriadoEm()).isNotNull();
        assertThat(salvo.getAtualizadoEm()).isNotNull();
        assertThat(salvo.getAceitoTermosEm()).isNotNull();
        assertThat(salvo.getAceitoTermosEm()).isBetween(antesDaCriacao, depoisDaCriacao);

        verify(usuarioRepository).existsById(usuarioId);
        verify(usuarioRepository).existsByEmailIgnoreCase("cliente.teste@infinitygraff.com");

        ArgumentCaptor<LogAuditoria> logCaptor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(logCaptor.capture());

        LogAuditoria log = logCaptor.getValue();

        assertThat(log.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(log.getAcao()).isEqualTo("USUARIO_CADASTRADO");
        assertThat(log.getEntidade()).isEqualTo("usuarios");
        assertThat(log.getEntidadeId()).isEqualTo(usuarioId);
        assertThat(log.getDadosAntes()).isNull();
        assertThat(log.getDadosDepois()).isEqualTo("{\"mock\":true}");
        assertThat(log.getIp()).isEqualTo("127.0.0.1");
        assertThat(log.getUserAgent()).isEqualTo("JUnit/Auth");
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ADMIN", "GERENTE"}
    )
    void naoDevePermitirCadastroPublicoComRoleAdministrativa(Role role) {
        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(
                        UUID.randomUUID(),
                        "usuario@teste.com"
                );

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Usuário Teste",
                        role,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Cadastro público permitido apenas para CLIENTE ou PRESTADOR");

        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService, never()).registrar(any());
        verify(auditoriaHelper, never()).executarAposCommit(any());
    }

    @Test
    void naoDeveCriarPerfilQuandoTokenSupabaseNaoPossuirEmailValido() {
        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(
                        UUID.randomUUID(),
                        "   "
                );

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Teste",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Token Supabase não possui e-mail válido");

        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void naoDeveCriarPerfilQuandoJaExistirUsuarioComMesmoId() {
        UUID usuarioId = UUID.randomUUID();

        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(
                        usuarioId,
                        "cliente@teste.com"
                );

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(usuarioRepository.existsById(usuarioId)).thenReturn(true);

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Teste",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Perfil interno já existe para este usuário");

        verify(usuarioRepository).existsById(usuarioId);
        verify(usuarioRepository, never()).existsByEmailIgnoreCase(any());
        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void naoDeveCriarPerfilQuandoEmailJaEstiverVinculadoAOutroPerfil() {
        UUID usuarioId = UUID.randomUUID();

        SupabaseAuthenticatedPrincipal principal =
                new SupabaseAuthenticatedPrincipal(
                        usuarioId,
                        "cliente@teste.com"
                );

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(usuarioRepository.existsById(usuarioId)).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("cliente@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Teste",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(NegocioException.class)
                .hasMessage("E-mail já vinculado a outro perfil");

        verify(usuarioRepository).existsById(usuarioId);
        verify(usuarioRepository).existsByEmailIgnoreCase("cliente@teste.com");
        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void deveRetornarPerfilExistenteQuandoPrincipalJaForUsuarioComMesmaRole() {
        Usuario usuario = usuario(Role.CLIENTE, "cliente.existente@teste.com", true);

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(usuario);

        ResultadoCompletarPerfil resultado = autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Existente",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        );

        assertThat(resultado.criado()).isFalse();
        assertThat(resultado.usuario().id()).isEqualTo(usuario.getId());
        assertThat(resultado.usuario().role()).isEqualTo(Role.CLIENTE);

        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService, never()).registrar(any());
        verify(auditoriaHelper, never()).executarAposCommit(any());
    }

    @Test
    void naoDeveRetornarPerfilExistenteQuandoUsuarioEstiverInativo() {
        Usuario usuario = usuario(Role.CLIENTE, "cliente.inativo@teste.com", false);

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(usuario);

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Inativo",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Usuário inativo ou indisponível");

        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void naoDeveRetornarPerfilExistenteQuandoRoleSolicitadaForDiferente() {
        Usuario usuario = usuario(Role.CLIENTE, "cliente@teste.com", true);

        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(usuario);

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Teste",
                        Role.PRESTADOR,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Perfil interno já existe com role diferente");

        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void deveLancarBadCredentialsQuandoPrincipalNaoForReconhecido() {
        when(securityContextHelper.obterAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("principal-invalido");

        assertThatThrownBy(() -> autenticacaoService.completarPerfil(
                new CompletarPerfilRequest(
                        "Cliente Teste",
                        Role.CLIENTE,
                        true
                ),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Token ausente ou inválido");

        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService, never()).registrar(any());
    }

    @Test
    void deveRetornarMeuPerfil() {
        Usuario usuario = usuario(Role.PRESTADOR, "prestador@teste.com", true);

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(usuario);

        UsuarioResponse response = autenticacaoService.meuPerfil();

        assertThat(response.id()).isEqualTo(usuario.getId());
        assertThat(response.nome()).isEqualTo(usuario.getNome());
        assertThat(response.email()).isEqualTo(usuario.getEmail());
        assertThat(response.role()).isEqualTo(Role.PRESTADOR);
        assertThat(response.ativo()).isTrue();
    }

    private void configurarAuditoriaExecutandoImediatamente() {
        when(auditoriaHelper.serializarSeguro(any()))
                .thenReturn("{\"mock\":true}");

        doAnswer(invocation -> {
            Runnable acao = invocation.getArgument(0);
            acao.run();
            return null;
        }).when(auditoriaHelper).executarAposCommit(any());
    }

    private Usuario usuario(
            Role role,
            String email,
            boolean ativo
    ) {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Teste")
                .email(email)
                .role(role)
                .ativo(ativo)
                .aceitoTermosEm(ACEITE_TERMOS)
                .build();

        preencherDatasAuditoria(usuario);

        return usuario;
    }

    private void preencherDatasAuditoria(Usuario usuario) {
        ReflectionTestUtils.setField(usuario, "criadoEm", ACEITE_TERMOS);
        ReflectionTestUtils.setField(usuario, "atualizadoEm", ACEITE_TERMOS);
    }
}