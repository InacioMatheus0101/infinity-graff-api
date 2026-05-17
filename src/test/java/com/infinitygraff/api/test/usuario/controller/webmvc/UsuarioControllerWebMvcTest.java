package com.infinitygraff.api.test.usuario.controller.webmvc;

import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.JwtAuthenticationFilter;
import com.infinitygraff.api.usuario.controller.UsuarioController;
import com.infinitygraff.api.usuario.dto.AtualizarStatusRequest;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.dto.UsuarioResumoResponse;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada web do {@link UsuarioController}.
 *
 * <p>Valida o contrato HTTP das rotas de usuários:
 * <ul>
 *   <li>listagem paginada;</li>
 *   <li>filtros e ordenação;</li>
 *   <li>busca por ID;</li>
 *   <li>atualização de status;</li>
 *   <li>soft delete;</li>
 *   <li>validações de parâmetros e body.</li>
 * </ul>
 *
 * <p>Os filtros de segurança são desativados nesta classe para manter
 * o teste focado exclusivamente no controller. As regras de autorização
 * já são cobertas nos testes unitários do service.
 */
@WebMvcTest(
        controllers = UsuarioController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerWebMvcTest {

    private static final String URL_USUARIOS = "/api/v1/usuarios";

    private static final UUID USUARIO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000301");

    private static final AuditoriaContext CONTEXTO_AUDITORIA =
            new AuditoriaContext("127.0.0.1", "JUnit/WebMvc");

    private static final OffsetDateTime DATA_REFERENCIA =
            OffsetDateTime.parse("2026-05-12T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AuditoriaHelper auditoriaHelper;

    @Test
    void deveRetornar200AoListarUsuarios() throws Exception {
        when(usuarioService.listar(any(), any(), any(), any()))
                .thenReturn(paginaUsuariosVazia());

        mockMvc.perform(get(URL_USUARIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Usuários carregados com sucesso"))
                .andExpect(jsonPath("$.dados.conteudo").isArray())
                .andExpect(jsonPath("$.dados.pagina").value(0))
                .andExpect(jsonPath("$.dados.tamanhoPagina").value(20))
                .andExpect(jsonPath("$.dados.totalElementos").value(0))
                .andExpect(jsonPath("$.dados.totalPaginas").value(0))
                .andExpect(jsonPath("$.dados.ultima").value(true))
                .andExpect(jsonPath("$.path").value(URL_USUARIOS));

        verify(usuarioService).listar(
                eq(null),
                eq(null),
                eq(null),
                eq(PageRequest.of(
                        0,
                        20,
                        Sort.by(Sort.Direction.DESC, "criadoEm")
                ))
        );
    }

    @Test
    void deveEncaminharFiltrosPaginacaoEOrdenacaoAoService() throws Exception {
        when(usuarioService.listar(any(), any(), any(), any()))
                .thenReturn(paginaUsuariosVazia());

        mockMvc.perform(get(URL_USUARIOS)
                        .param("role", "CLIENTE")
                        .param("ativo", "true")
                        .param("nome", "Matheus")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "nome,asc"))
                .andExpect(status().isOk());

        verify(usuarioService).listar(
                eq(Role.CLIENTE),
                eq(true),
                eq("Matheus"),
                eq(PageRequest.of(
                        1,
                        10,
                        Sort.by(Sort.Direction.ASC, "nome")
                ))
        );
    }

    @Test
    void deveRetornar400QuandoPageForNegativa() throws Exception {
        mockMvc.perform(get(URL_USUARIOS)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveRetornar400QuandoSizeForZero() throws Exception {
        mockMvc.perform(get(URL_USUARIOS)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveRetornar400QuandoSizeForMaiorQue100() throws Exception {
        mockMvc.perform(get(URL_USUARIOS)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveRetornar400QuandoSortForInvalido() throws Exception {
        mockMvc.perform(get(URL_USUARIOS)
                        .param("sort", "campoInvalido,asc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveRetornar200AoBuscarUsuarioPorId() throws Exception {
        when(usuarioService.buscarPorId(USUARIO_ID))
                .thenReturn(usuarioResponse(true));

        mockMvc.perform(get(URL_USUARIOS + "/{id}", USUARIO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Usuário carregado com sucesso"))
                .andExpect(jsonPath("$.dados.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.dados.nome").value("Usuário Teste"))
                .andExpect(jsonPath("$.dados.email").value("usuario.teste@infinitygraff.com"))
                .andExpect(jsonPath("$.dados.role").value("CLIENTE"))
                .andExpect(jsonPath("$.dados.ativo").value(true))
                .andExpect(jsonPath("$.path").value(URL_USUARIOS + "/" + USUARIO_ID));

        verify(usuarioService).buscarPorId(USUARIO_ID);
    }

    @Test
    void deveRetornar200AoAtivarUsuario() throws Exception {
        when(auditoriaHelper.criarContexto(any()))
                .thenReturn(CONTEXTO_AUDITORIA);

        when(usuarioService.atualizarStatus(
                eq(USUARIO_ID),
                eq(new AtualizarStatusRequest(true)),
                eq(CONTEXTO_AUDITORIA)
        )).thenReturn(usuarioResponse(true));

        mockMvc.perform(patch(URL_USUARIOS + "/{id}/status", USUARIO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Usuário ativado com sucesso"))
                .andExpect(jsonPath("$.dados.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.dados.ativo").value(true))
                .andExpect(jsonPath("$.path").value(URL_USUARIOS + "/" + USUARIO_ID + "/status"));

        verify(auditoriaHelper).criarContexto(any());
        verify(usuarioService).atualizarStatus(
                USUARIO_ID,
                new AtualizarStatusRequest(true),
                CONTEXTO_AUDITORIA
        );
    }

    @Test
    void deveRetornar200AoDesativarUsuario() throws Exception {
        when(auditoriaHelper.criarContexto(any()))
                .thenReturn(CONTEXTO_AUDITORIA);

        when(usuarioService.atualizarStatus(
                eq(USUARIO_ID),
                eq(new AtualizarStatusRequest(false)),
                eq(CONTEXTO_AUDITORIA)
        )).thenReturn(usuarioResponse(false));

        mockMvc.perform(patch(URL_USUARIOS + "/{id}/status", USUARIO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ativo": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Usuário desativado com sucesso"))
                .andExpect(jsonPath("$.dados.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.dados.ativo").value(false));

        verify(auditoriaHelper).criarContexto(any());
        verify(usuarioService).atualizarStatus(
                USUARIO_ID,
                new AtualizarStatusRequest(false),
                CONTEXTO_AUDITORIA
        );
    }

    @Test
    void deveRetornar400QuandoAtivoForNuloNoPatchDeStatus() throws Exception {
        mockMvc.perform(patch(URL_USUARIOS + "/{id}/status", USUARIO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ativo": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar400QuandoBodyEstiverAusenteNoPatchDeStatus() throws Exception {
        mockMvc.perform(patch(URL_USUARIOS + "/{id}/status", USUARIO_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar204AoAplicarSoftDelete() throws Exception {
        when(auditoriaHelper.criarContexto(any()))
                .thenReturn(CONTEXTO_AUDITORIA);

        mockMvc.perform(delete(URL_USUARIOS + "/{id}", USUARIO_ID))
                .andExpect(status().isNoContent());

        verify(auditoriaHelper).criarContexto(any());
        verify(usuarioService).aplicarSoftDelete(
                USUARIO_ID,
                CONTEXTO_AUDITORIA
        );
    }

    private PageResponse<UsuarioResumoResponse> paginaUsuariosVazia() {
        return new PageResponse<>(
                List.of(),
                0,
                20,
                0,
                0,
                true
        );
    }

    private UsuarioResponse usuarioResponse(boolean ativo) {
        return new UsuarioResponse(
                USUARIO_ID,
                "Usuário Teste",
                "usuario.teste@infinitygraff.com",
                null,
                Role.CLIENTE,
                ativo,
                DATA_REFERENCIA,
                DATA_REFERENCIA,
                DATA_REFERENCIA
        );
    }
}