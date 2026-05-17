package com.infinitygraff.api.test.auditoria.service.unit;

import com.infinitygraff.api.auditoria.dto.LogAuditoriaResponse;
import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.repository.LogAuditoriaRepository;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link AuditoriaService}.
 *
 * <p>Valida:
 * <ul>
 *   <li>registro de logs de auditoria;</li>
 *   <li>falha de persistência sem propagação de erro;</li>
 *   <li>consulta permitida para ADMIN e GERENTE;</li>
 *   <li>bloqueio de CLIENTE e PRESTADOR;</li>
 *   <li>normalização dos filtros de ação e entidade;</li>
 *   <li>remoção de ordenação dinâmica do Pageable;</li>
 *   <li>validação de intervalo de datas.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceUnitTest {

    private static final OffsetDateTime DATA_REFERENCIA =
            OffsetDateTime.parse("2026-05-12T18:00:00Z");

    @Mock
    private LogAuditoriaRepository logAuditoriaRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @Test
    void deveRegistrarLogComSucesso() {
        LogAuditoria entrada = logAuditoriaNovo();

        auditoriaService.registrar(entrada);

        verify(logAuditoriaRepository).saveAndFlush(entrada);
    }

    @Test
    void naoDeveLancarErroQuandoLogForNulo() {
        assertThatNoException().isThrownBy(() ->
                auditoriaService.registrar(null)
        );

        verify(logAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoDevePropagarExcecaoQuandoFalharAoRegistrarLog() {
        LogAuditoria entrada = logAuditoriaNovo();

        when(logAuditoriaRepository.saveAndFlush(entrada))
                .thenThrow(new RuntimeException("Falha simulada no banco"));

        assertThatNoException().isThrownBy(() ->
                auditoriaService.registrar(entrada)
        );

        verify(logAuditoriaRepository).saveAndFlush(entrada);
    }

    @Test
    void deveListarLogsQuandoUsuarioForAdmin() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");
        LogAuditoria log = logAuditoriaPersistido(
                "USUARIO_ATIVADO",
                "usuarios"
        );

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(logAuditoriaRepository.listarComFiltros(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(0, 50))
        )).thenReturn(new PageImpl<>(
                List.of(log),
                PageRequest.of(0, 50),
                1
        ));

        PageResponse<LogAuditoriaResponse> resultado = auditoriaService.listar(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.conteudo()).hasSize(1);
        assertThat(resultado.totalElementos()).isEqualTo(1);
        assertThat(resultado.conteudo().get(0).acao()).isEqualTo("USUARIO_ATIVADO");
    }

    @Test
    void deveListarLogsQuandoUsuarioForGerente() {
        Usuario gerente = usuario(Role.GERENTE, "gerente@teste.com");
        LogAuditoria log = logAuditoriaPersistido(
                "USUARIO_DESATIVADO",
                "usuarios"
        );

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(gerente);
        when(logAuditoriaRepository.listarComFiltros(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(0, 50))
        )).thenReturn(new PageImpl<>(
                List.of(log),
                PageRequest.of(0, 50),
                1
        ));

        PageResponse<LogAuditoriaResponse> resultado = auditoriaService.listar(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.conteudo()).hasSize(1);
        assertThat(resultado.totalElementos()).isEqualTo(1);
        assertThat(resultado.conteudo().get(0).acao()).isEqualTo("USUARIO_DESATIVADO");
    }

    @Test
    void naoDevePermitirClienteConsultarLogs() {
        Usuario cliente = usuario(Role.CLIENTE, "cliente@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(cliente);

        assertThatThrownBy(() -> auditoriaService.listar(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Apenas ADMIN e GERENTE podem consultar logs de auditoria");

        verify(logAuditoriaRepository, never()).listarComFiltros(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void naoDevePermitirPrestadorConsultarLogs() {
        Usuario prestador = usuario(Role.PRESTADOR, "prestador@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(prestador);

        assertThatThrownBy(() -> auditoriaService.listar(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        ))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Apenas ADMIN e GERENTE podem consultar logs de auditoria");

        verify(logAuditoriaRepository, never()).listarComFiltros(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void deveNormalizarAcaoEEntidadeAntesDeConsultarRepository() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(logAuditoriaRepository.listarComFiltros(
                eq(null),
                eq("USUARIO_ATIVADO"),
                eq("usuarios"),
                eq(null),
                eq(null),
                eq(PageRequest.of(0, 50))
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(0, 50),
                0
        ));

        PageResponse<LogAuditoriaResponse> resultado = auditoriaService.listar(
                null,
                "  usuario_ativado  ",
                "  USUARIOS  ",
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.conteudo()).isEmpty();

        verify(logAuditoriaRepository).listarComFiltros(
                null,
                "USUARIO_ATIVADO",
                "usuarios",
                null,
                null,
                PageRequest.of(0, 50)
        );
    }

    @Test
    void deveConverterAcaoEEntidadeEmNullQuandoVieremEmBranco() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(logAuditoriaRepository.listarComFiltros(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(0, 50))
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(0, 50),
                0
        ));

        PageResponse<LogAuditoriaResponse> resultado = auditoriaService.listar(
                null,
                "   ",
                "   ",
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.conteudo()).isEmpty();

        verify(logAuditoriaRepository).listarComFiltros(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );
    }

    @Test
    void deveRemoverOrdenacaoDoPageableAntesDeConsultarRepository() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);
        when(logAuditoriaRepository.listarComFiltros(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(2, 25))
        )).thenReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(2, 25),
                0
        ));

        PageResponse<LogAuditoriaResponse> resultado = auditoriaService.listar(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(2, 25, Sort.by(Sort.Direction.ASC, "acao"))
        );

        assertThat(resultado.conteudo()).isEmpty();

        verify(logAuditoriaRepository).listarComFiltros(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(2, 25)
        );
    }

    @Test
    void naoDevePermitirIntervaloDeDatasInvalido() {
        Usuario admin = usuario(Role.ADMIN, "admin@teste.com");

        OffsetDateTime inicio = OffsetDateTime.parse("2026-05-12T20:00:00Z");
        OffsetDateTime fim = OffsetDateTime.parse("2026-05-12T10:00:00Z");

        when(securityContextHelper.obterUsuarioAutenticado()).thenReturn(admin);

        assertThatThrownBy(() -> auditoriaService.listar(
                null,
                null,
                null,
                inicio,
                fim,
                PageRequest.of(0, 50)
        ))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Data inicial não pode ser posterior à data final");

        verify(logAuditoriaRepository, never()).listarComFiltros(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private LogAuditoria logAuditoriaNovo() {
        return LogAuditoria.builder()
                .usuarioId(UUID.randomUUID())
                .acao("USUARIO_ATIVADO")
                .entidade("usuarios")
                .entidadeId(UUID.randomUUID())
                .dadosAntes("{\"ativo\":false}")
                .dadosDepois("{\"ativo\":true}")
                .ip("127.0.0.1")
                .userAgent("JUnit/Auditoria")
                .build();
    }

    private LogAuditoria logAuditoriaPersistido(
            String acao,
            String entidade
    ) {
        LogAuditoria log = LogAuditoria.builder()
                .usuarioId(UUID.randomUUID())
                .acao(acao)
                .entidade(entidade)
                .entidadeId(UUID.randomUUID())
                .dadosAntes("{\"antes\":true}")
                .dadosDepois("{\"depois\":true}")
                .ip("127.0.0.1")
                .userAgent("JUnit/Auditoria")
                .build();

        ReflectionTestUtils.setField(log, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(log, "criadoEm", DATA_REFERENCIA);

        return log;
    }

    private Usuario usuario(
            Role role,
            String email
    ) {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Usuário Auditoria")
                .email(email)
                .role(role)
                .ativo(true)
                .aceitoTermosEm(DATA_REFERENCIA)
                .build();

        ReflectionTestUtils.setField(usuario, "criadoEm", DATA_REFERENCIA);
        ReflectionTestUtils.setField(usuario, "atualizadoEm", DATA_REFERENCIA);

        return usuario;
    }
}