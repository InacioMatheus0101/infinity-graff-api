package com.infinitygraff.api.test.auth.controller.webmvc;

import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.auth.controller.AutenticacaoController;
import com.infinitygraff.api.auth.service.AutenticacaoService;
import com.infinitygraff.api.auth.service.ResultadoCompletarPerfil;
import com.infinitygraff.api.security.JwtAuthenticationFilter;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada web do {@link AutenticacaoController}.
 *
 * <p>Valida o contrato HTTP das rotas:
 * <ul>
 *   <li>{@code POST /api/v1/autenticacao/completar-perfil};</li>
 *   <li>{@code GET /api/v1/autenticacao/meu-perfil}.</li>
 * </ul>
 *
 * <p>Os filtros de segurança são desativados nesta classe para manter
 * o teste focado exclusivamente no controller. Regras de autenticação
 * e autorização são cobertas em testes específicos.
 */
@WebMvcTest(
        controllers = AutenticacaoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AutenticacaoControllerWebMvcTest {

    private static final String URL_COMPLETAR_PERFIL =
            "/api/v1/autenticacao/completar-perfil";

    private static final String URL_MEU_PERFIL =
            "/api/v1/autenticacao/meu-perfil";

    private static final UUID USUARIO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");

    private static final OffsetDateTime DATA_REFERENCIA =
            OffsetDateTime.parse("2026-05-12T18:00:00Z");

    private static final AuditoriaContext CONTEXTO_AUDITORIA =
            new AuditoriaContext("127.0.0.1", "JUnit/AuthWebMvc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private AuditoriaHelper auditoriaHelper;

    @Test
    void deveRetornar201QuandoPerfilForCriadoPelaPrimeiraVez() throws Exception {
        when(auditoriaHelper.criarContexto(any()))
                .thenReturn(CONTEXTO_AUDITORIA);

        when(autenticacaoService.completarPerfil(any(), eq(CONTEXTO_AUDITORIA)))
                .thenReturn(ResultadoCompletarPerfil.criado(usuarioResponse()));

        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Teste",
                                  "role": "CLIENTE",
                                  "aceitouTermos": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Perfil interno criado com sucesso"))
                .andExpect(jsonPath("$.dados.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.dados.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.dados.email").value("cliente.teste@infinitygraff.com"))
                .andExpect(jsonPath("$.dados.role").value("CLIENTE"))
                .andExpect(jsonPath("$.dados.ativo").value(true))
                .andExpect(jsonPath("$.path").value(URL_COMPLETAR_PERFIL));

        verify(auditoriaHelper).criarContexto(any());
        verify(autenticacaoService).completarPerfil(any(), eq(CONTEXTO_AUDITORIA));
    }

    @Test
    void deveRetornar200QuandoPerfilJaExistirESincronizacaoForIdempotente() throws Exception {
        when(auditoriaHelper.criarContexto(any()))
                .thenReturn(CONTEXTO_AUDITORIA);

        when(autenticacaoService.completarPerfil(any(), eq(CONTEXTO_AUDITORIA)))
                .thenReturn(ResultadoCompletarPerfil.existente(usuarioResponse()));

        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Teste",
                                  "role": "CLIENTE",
                                  "aceitouTermos": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Perfil interno sincronizado com sucesso"))
                .andExpect(jsonPath("$.dados.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.path").value(URL_COMPLETAR_PERFIL));

        verify(auditoriaHelper).criarContexto(any());
        verify(autenticacaoService).completarPerfil(any(), eq(CONTEXTO_AUDITORIA));
    }

    @Test
    void deveRetornar400QuandoNomeForVazioNoCompletarPerfil() throws Exception {
        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "role": "CLIENTE",
                                  "aceitouTermos": true
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autenticacaoService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar400QuandoNomeTiverMenosDeDoisCaracteresNoCompletarPerfil() throws Exception {
        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "A",
                                  "role": "CLIENTE",
                                  "aceitouTermos": true
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autenticacaoService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar400QuandoRoleForNulaNoCompletarPerfil() throws Exception {
        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Teste",
                                  "role": null,
                                  "aceitouTermos": true
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autenticacaoService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar400QuandoAceiteDeTermosForFalseNoCompletarPerfil() throws Exception {
        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Cliente Teste",
                                  "role": "CLIENTE",
                                  "aceitouTermos": false
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autenticacaoService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar400QuandoBodyEstiverAusenteNoCompletarPerfil() throws Exception {
        mockMvc.perform(post(URL_COMPLETAR_PERFIL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(autenticacaoService);
        verifyNoInteractions(auditoriaHelper);
    }

    @Test
    void deveRetornar200AoBuscarMeuPerfil() throws Exception {
        when(autenticacaoService.meuPerfil())
                .thenReturn(usuarioResponse());

        mockMvc.perform(get(URL_MEU_PERFIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.mensagem").value("Perfil carregado com sucesso"))
                .andExpect(jsonPath("$.dados.id").value(USUARIO_ID.toString()))
                .andExpect(jsonPath("$.dados.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.dados.email").value("cliente.teste@infinitygraff.com"))
                .andExpect(jsonPath("$.dados.role").value("CLIENTE"))
                .andExpect(jsonPath("$.dados.ativo").value(true))
                .andExpect(jsonPath("$.path").value(URL_MEU_PERFIL));

        verify(autenticacaoService).meuPerfil();
    }

    private UsuarioResponse usuarioResponse() {
        return new UsuarioResponse(
                USUARIO_ID,
                "Cliente Teste",
                "cliente.teste@infinitygraff.com",
                null,
                Role.CLIENTE,
                true,
                DATA_REFERENCIA,
                DATA_REFERENCIA,
                DATA_REFERENCIA
        );
    }
}