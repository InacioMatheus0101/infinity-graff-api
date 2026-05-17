package com.infinitygraff.api.test.usuario.service.unit;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.usuario.dto.AtualizarStatusRequest;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.dto.UsuarioResumoResponse;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import com.infinitygraff.api.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link UsuarioService}.
 *
 * <p>Valida regras de negócio sem subir contexto Spring:
 * <ul>
 *   <li>permissões de ADMIN, GERENTE, CLIENTE e PRESTADOR;</li>
 *   <li>listagem e visualização de usuários;</li>
 *   <li>ativação/desativação;</li>
 *   <li>soft delete;</li>
 *   <li>disparo de auditoria após operações relevantes.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceUnitTest {

    private static final OffsetDateTime ACEITE_TERMOS =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    private static final AuditoriaContext CONTEXTO_AUDITORIA =
            new AuditoriaContext("127.0.0.1", "JUnit/Test");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private AuditoriaHelper auditoriaHelper;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveListarTodosOsUsuariosQuandoAutenticadoForAdmin() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.listarComFiltro(
                eq(null),
                eq(null),
                eq(null),
                any()
        )).thenReturn(new PageImpl<>(
                List.of(cliente),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<UsuarioResumoResponse> resultado = usuarioService.listar(
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.conteudo()).hasSize(1);
        assertThat(resultado.totalElementos()).isEqualTo(1);

        verify(usuarioRepository).listarComFiltro(
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        verify(usuarioRepository, never()).listarComFiltroPorRoles(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void deveListarUsuariosPermitidosQuandoAutenticadoForGerente() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(usuarioRepository.listarComFiltroPorRoles(
                any(),
                eq(Role.CLIENTE),
                eq(true),
                eq("Cliente"),
                any()
        )).thenReturn(new PageImpl<>(
                List.of(cliente),
                PageRequest.of(0, 20),
                1
        ));

        PageResponse<UsuarioResumoResponse> resultado = usuarioService.listar(
                Role.CLIENTE,
                true,
                " Cliente ",
                PageRequest.of(0, 20)
        );

        assertThat(resultado.conteudo()).hasSize(1);
        assertThat(resultado.totalElementos()).isEqualTo(1);

        verify(usuarioRepository).listarComFiltroPorRoles(
                any(),
                eq(Role.CLIENTE),
                eq(true),
                eq("Cliente"),
                eq(PageRequest.of(0, 20))
        );
    }

    @Test
    void naoDevePermitirGerenteListarAdmin() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);

        assertThatThrownBy(() -> usuarioService.listar(
                Role.ADMIN, null, null, PageRequest.of(0, 20)
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Gerente não pode visualizar usuários com este perfil");

        verify(usuarioRepository, never()).listarComFiltroPorRoles(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void naoDevePermitirClienteListarUsuarios() {
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(cliente);

        assertThatThrownBy(() -> usuarioService.listar(
                null, null, null, PageRequest.of(0, 20)
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Você não tem permissão para listar usuários");

        verify(usuarioRepository, never()).listarComFiltro(any(), any(), any(), any());
        verify(usuarioRepository, never()).listarComFiltroPorRoles(any(), any(), any(), any(), any());
    }

    @Test
    void naoDevePermitirPrestadorListarUsuarios() {
        Usuario prestador = usuario(Role.PRESTADOR, "prestador@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(prestador);

        assertThatThrownBy(() -> usuarioService.listar(
                null, null, null, PageRequest.of(0, 20)
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Você não tem permissão para listar usuários");

        verify(usuarioRepository, never()).listarComFiltro(any(), any(), any(), any());
        verify(usuarioRepository, never()).listarComFiltroPorRoles(any(), any(), any(), any(), any());
    }

    @Test
    void devePermitirAdminBuscarQualquerUsuarioPorId() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        UsuarioResponse resultado = usuarioService.buscarPorId(cliente.getId());

        assertThat(resultado.id()).isEqualTo(cliente.getId());
        assertThat(resultado.role()).isEqualTo(Role.CLIENTE);
    }

    @Test
    void devePermitirGerenteBuscarClientePorId() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        UsuarioResponse resultado = usuarioService.buscarPorId(cliente.getId());

        assertThat(resultado.id()).isEqualTo(cliente.getId());
        assertThat(resultado.role()).isEqualTo(Role.CLIENTE);
    }

    @Test
    void naoDevePermitirGerenteBuscarAdminPorId() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.buscarPorId(admin.getId()))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Você não tem permissão para visualizar este usuário");
    }

    @Test
    void devePermitirClienteBuscarOProprioPerfil() {
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(cliente);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        UsuarioResponse resultado = usuarioService.buscarPorId(cliente.getId());

        assertThat(resultado.id()).isEqualTo(cliente.getId());
    }

    @Test
    void naoDevePermitirClienteBuscarOutroUsuario() {
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");
        Usuario outroCliente = usuario(Role.CLIENTE, "outro@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(cliente);
        when(usuarioRepository.findById(outroCliente.getId())).thenReturn(Optional.of(outroCliente));

        assertThatThrownBy(() -> usuarioService.buscarPorId(outroCliente.getId()))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Você não tem permissão para visualizar este usuário");
    }

    @Test
    void deveLancar404AoBuscarUsuarioInexistente() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        UUID idInexistente = UUID.randomUUID();

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(idInexistente))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void deveDesativarUsuarioQuandoAdminAlterarStatusParaFalse() {
        configurarAuditoriaExecutandoImediatamente();

        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse resultado = usuarioService.atualizarStatus(
                cliente.getId(),
                new AtualizarStatusRequest(false),
                CONTEXTO_AUDITORIA
        );

        assertThat(resultado.ativo()).isFalse();

        ArgumentCaptor<LogAuditoria> logCaptor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(logCaptor.capture());

        LogAuditoria log = logCaptor.getValue();
        assertThat(log.getUsuarioId()).isEqualTo(admin.getId());
        assertThat(log.getEntidadeId()).isEqualTo(cliente.getId());
        assertThat(log.getAcao()).isEqualTo("USUARIO_DESATIVADO");
        assertThat(log.getIp()).isEqualTo("127.0.0.1");
        assertThat(log.getUserAgent()).isEqualTo("JUnit/Test");
    }

    @Test
    void deveAtivarUsuarioQuandoGerenteAlterarStatusParaTrue() {
        configurarAuditoriaExecutandoImediatamente();

        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        Usuario prestador = usuarioInativo(Role.PRESTADOR, "prestador@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(usuarioRepository.findById(prestador.getId())).thenReturn(Optional.of(prestador));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse resultado = usuarioService.atualizarStatus(
                prestador.getId(),
                new AtualizarStatusRequest(true),
                CONTEXTO_AUDITORIA
        );

        assertThat(resultado.ativo()).isTrue();

        ArgumentCaptor<LogAuditoria> logCaptor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(logCaptor.capture());
        assertThat(logCaptor.getValue().getAcao()).isEqualTo("USUARIO_ATIVADO");
    }

    @Test
    void naoDeveGerarAuditoriaQuandoStatusSolicitadoJaForOAtual() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        UsuarioResponse resultado = usuarioService.atualizarStatus(
                cliente.getId(),
                new AtualizarStatusRequest(true),
                CONTEXTO_AUDITORIA
        );

        assertThat(resultado.ativo()).isTrue();
        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any());
        verify(auditoriaHelper, never()).executarAposCommit(any());
    }

    @Test
    void naoDevePermitirAdminDesativarASiMesmo() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.atualizarStatus(
                admin.getId(),
                new AtualizarStatusRequest(false),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Administrador não pode desativar a si mesmo");
    }

    @Test
    void naoDevePermitirGerenteAlterarStatusDeAdmin() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.atualizarStatus(
                admin.getId(),
                new AtualizarStatusRequest(false),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Você não tem permissão para alterar o status deste usuário");
    }

    @Test
    void deveRejeitarStatusNuloDefensivamenteNoService() {
        assertThatThrownBy(() -> usuarioService.atualizarStatus(
                UUID.randomUUID(),
                new AtualizarStatusRequest(null),
                CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ativo não pode ser nulo");

        verifyNoInteractions(securityContextHelper);
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void deveAplicarSoftDeleteQuandoAutenticadoForAdmin() {
        configurarAuditoriaExecutandoImediatamente();

        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        usuarioService.aplicarSoftDelete(cliente.getId(), CONTEXTO_AUDITORIA);

        assertThat(cliente.isAtivo()).isFalse();
        assertThat(cliente.isDeletado()).isTrue();

        ArgumentCaptor<LogAuditoria> logCaptor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(auditoriaService).registrar(logCaptor.capture());

        LogAuditoria log = logCaptor.getValue();
        assertThat(log.getAcao()).isEqualTo("USUARIO_DELETADO");
        assertThat(log.getUsuarioId()).isEqualTo(admin.getId());
        assertThat(log.getEntidadeId()).isEqualTo(cliente.getId());
    }

    @Test
    void naoDevePermitirGerenteAplicarSoftDelete() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> usuarioService.aplicarSoftDelete(
                cliente.getId(), CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Apenas ADMIN pode aplicar soft delete em usuários");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoDevePermitirAdminAplicarSoftDeleteEmSiMesmo() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.aplicarSoftDelete(
                admin.getId(), CONTEXTO_AUDITORIA
        ))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Administrador não pode aplicar soft delete em si mesmo");

        verify(usuarioRepository, never()).save(any());
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

    private Usuario usuario(Role role, String email) {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Teste")
                .email(email)
                .role(role)
                .ativo(true)
                .aceitoTermosEm(ACEITE_TERMOS)
                .build();

        preencherDatasAuditoria(usuario);

        return usuario;
    }

    private Usuario usuarioInativo(Role role, String email) {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Inativo")
                .email(email)
                .role(role)
                .ativo(false)
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
