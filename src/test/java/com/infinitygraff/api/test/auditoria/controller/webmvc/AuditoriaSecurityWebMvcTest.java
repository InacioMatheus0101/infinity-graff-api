package com.infinitygraff.api.test.auditoria.controller.webmvc;

import com.infinitygraff.api.auditoria.controller.AuditoriaController;
import com.infinitygraff.api.auditoria.dto.LogAuditoriaResponse;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.CustomAccessDeniedHandler;
import com.infinitygraff.api.security.CustomAuthenticationEntryPoint;
import com.infinitygraff.api.security.JwtAuthenticationFilter;
import com.infinitygraff.api.security.SecurityConfig;
import com.infinitygraff.api.security.SupabaseJwtService;
import com.infinitygraff.api.test.security.WithMockUsuario;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de segurança da rota de auditoria.
 *
 * <p>Valida as regras HTTP configuradas no {@link SecurityConfig}
 * para {@code GET /api/v1/auditoria/logs}.
 *
 * <p>Regras esperadas:
 * <ul>
 *   <li>ADMIN pode acessar;</li>
 *   <li>GERENTE pode acessar;</li>
 *   <li>CLIENTE recebe 403;</li>
 *   <li>PRESTADOR recebe 403;</li>
 *   <li>requisição sem autenticação recebe 401.</li>
 * </ul>
 */
@WebMvcTest(AuditoriaController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        AuditoriaSecurityWebMvcTest.CorsTesteConfig.class
})
class AuditoriaSecurityWebMvcTest {

    private static final String URL_LOGS = "/api/v1/auditoria/logs";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @MockitoBean
    private SupabaseJwtService supabaseJwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUsuario(role = Role.ADMIN)
    void devePermitirAdminConsultarLogsDeAuditoria() throws Exception {
        when(auditoriaService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(paginaVazia());

        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUsuario(role = Role.GERENTE)
    void devePermitirGerenteConsultarLogsDeAuditoria() throws Exception {
        when(auditoriaService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(paginaVazia());

        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUsuario(role = Role.CLIENTE)
    void deveRetornar403QuandoClienteTentarConsultarLogsDeAuditoria() throws Exception {
        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isForbidden());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    @WithMockUsuario(role = Role.PRESTADOR)
    void deveRetornar403QuandoPrestadorTentarConsultarLogsDeAuditoria() throws Exception {
        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isForbidden());

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get(URL_LOGS))
                .andExpect(status().isUnauthorized());

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

    /**
     * Configuração real e mínima de CORS para o contexto de teste.
     *
     * <p>Não usamos {@code @MockitoBean CorsConfigurationSource}, pois isso
     * interfere na infraestrutura MVC/Security durante a montagem do contexto.
     */
    @TestConfiguration
    static class CorsTesteConfig {

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of("http://localhost:3000"));
            configuration.setAllowedMethods(List.of(
                    "GET",
                    "POST",
                    "PATCH",
                    "DELETE",
                    "OPTIONS"
            ));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setAllowCredentials(true);

            UrlBasedCorsConfigurationSource source =
                    new UrlBasedCorsConfigurationSource();

            source.registerCorsConfiguration("/**", configuration);

            return source;
        }
    }
}