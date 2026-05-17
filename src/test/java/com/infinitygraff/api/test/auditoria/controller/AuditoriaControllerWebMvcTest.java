package com.infinitygraff.api.test.auditoria.controller;

import com.infinitygraff.api.auditoria.controller.AuditoriaController;
import com.infinitygraff.api.auditoria.dto.LogAuditoriaResponse;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada web do {@link AuditoriaController}.
 *
 * <p>Valida o contrato HTTP da rota {@code GET /api/v1/auditoria/logs}:
 * <ul>
 *   <li>status de sucesso;</li>
 *   <li>formato básico da resposta;</li>
 *   <li>encaminhamento correto dos filtros ao service;</li>
 *   <li>paginação padrão;</li>
 *   <li>validações de query params inválidos.</li>
 * </ul>
 *
 * <p>Os filtros de segurança são desativados nesta classe para manter
 * o teste focado exclusivamente no controller. As regras de autenticação
 * e autorização serão testadas separadamente em {@code AuditoriaSecurityWebMvcTest}.
 */
@WebMvcTest(
        controllers = AuditoriaController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuditoriaControllerWebMvcTest {

    private static final String URL_LOGS = "/api/v1/auditoria/logs";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @Test
    void deveRetornar200AoListarLogs() throws Exception {
        when(auditoriaService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(paginaVazia());

        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Logs de auditoria carregados com sucesso"))
                .andExpect(jsonPath("$.dados.conteudo").isArray())
                .andExpect(jsonPath("$.dados.pagina").value(0))
                .andExpect(jsonPath("$.dados.tamanhoPagina").value(50))
                .andExpect(jsonPath("$.dados.totalElementos").value(0))
                .andExpect(jsonPath("$.dados.totalPaginas").value(0))
                .andExpect(jsonPath("$.dados.ultima").value(true))
                .andExpect(jsonPath("$.path").value(URL_LOGS));
    }

    @Test
    void deveEncaminharFiltrosCorretamenteAoService() throws Exception {
        UUID usuarioId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String acao = "USUARIO_ATIVADO";
        String entidade = "usuarios";

        when(auditoriaService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(paginaVazia());

        mockMvc.perform(get(URL_LOGS)
                        .param("usuarioId", usuarioId.toString())
                        .param("acao", acao)
                        .param("entidade", entidade)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(auditoriaService).listar(
                eq(usuarioId),
                eq(acao),
                eq(entidade),
                isNull(),
                isNull(),
                eq(PageRequest.of(0, 10))
        );
    }

    @Test
    void deveEncaminharFiltrosDeDatasAoService() throws Exception {
        String de = "2026-05-01T00:00:00Z";
        String ate = "2026-05-12T23:59:59Z";

        OffsetDateTime deEsperado = OffsetDateTime.parse(de);
        OffsetDateTime ateEsperado = OffsetDateTime.parse(ate);

        when(auditoriaService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(paginaVazia());

        mockMvc.perform(get(URL_LOGS)
                        .param("de", de)
                        .param("ate", ate))
                .andExpect(status().isOk());

        verify(auditoriaService).listar(
                isNull(),
                isNull(),
                isNull(),
                eq(deEsperado),
                eq(ateEsperado),
                eq(PageRequest.of(0, 50))
        );
    }

    @Test
    void deveUsarPaginacaoPadrao() throws Exception {
        when(auditoriaService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(paginaVazia());

        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.pagina").value(0))
                .andExpect(jsonPath("$.dados.tamanhoPagina").value(50));

        verify(auditoriaService).listar(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(PageRequest.of(0, 50))
        );
    }

    @Test
    void deveRetornar400QuandoSizeForMaiorQue100() throws Exception {
        mockMvc.perform(get(URL_LOGS)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveRetornar400QuandoSizeForZero() throws Exception {
        mockMvc.perform(get(URL_LOGS)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveRetornar400QuandoPageForNegativa() throws Exception {
        mockMvc.perform(get(URL_LOGS)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveRetornar400QuandoDataDeForInvalida() throws Exception {
        mockMvc.perform(get(URL_LOGS)
                        .param("de", "data-invalida"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveRetornar400QuandoDataAteForInvalida() throws Exception {
        mockMvc.perform(get(URL_LOGS)
                        .param("ate", "nao-e-uma-data"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditoriaService);
    }

    private PageResponse<LogAuditoriaResponse> paginaVazia() {
        return new PageResponse<>(
                List.of(),
                0,
                50,
                0,
                0,
                true
        );
    }
  }  