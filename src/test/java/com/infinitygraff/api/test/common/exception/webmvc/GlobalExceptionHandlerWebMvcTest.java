package com.infinitygraff.api.test.common.exception.webmvc;

import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.GlobalExceptionHandler;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.security.JwtAuthenticationFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada web do {@link GlobalExceptionHandler}.
 *
 * <p>Valida o contrato HTTP das respostas de erro padronizadas da API:
 * <ul>
 *   <li>403 para acesso negado;</li>
 *   <li>404 para recurso não encontrado;</li>
 *   <li>409 para conflito de negócio;</li>
 *   <li>400 para validação de body;</li>
 *   <li>400 para validação de query params;</li>
 *   <li>400 para parâmetro com tipo inválido;</li>
 *   <li>400 para JSON malformado;</li>
 *   <li>401 para falhas de autenticação;</li>
 *   <li>500 para erro interno inesperado.</li>
 * </ul>
 *
 * <p>Os filtros de segurança são desativados porque esta classe testa
 * apenas a conversão de exceções em {@code ErrorResponse}.
 */
@WebMvcTest(
        controllers = GlobalExceptionHandlerWebMvcTest.ControllerDeExcecoes.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerWebMvcTest.ControllerDeExcecoes.class
})
class GlobalExceptionHandlerWebMvcTest {

    private static final String BASE_URL = "/teste/excecoes";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornar403QuandoOcorrerAcessoNegado() throws Exception {
        mockMvc.perform(get(BASE_URL + "/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Acesso negado"))
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para acessar este recurso"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/acesso-negado"));
    }

    @Test
    void deveRetornar404QuandoRecursoNaoForEncontrado() throws Exception {
        mockMvc.perform(get(BASE_URL + "/recurso-nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.mensagem").value("Usuário não encontrado"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/recurso-nao-encontrado"));
    }

    @Test
    void deveRetornar409QuandoOcorrerNegocioExceptionComStatusConflict() throws Exception {
        mockMvc.perform(get(BASE_URL + "/conflito-negocio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Conflito"))
                .andExpect(jsonPath("$.mensagem").value("E-mail já vinculado a outro perfil"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/conflito-negocio"));
    }

    @Test
    void deveRetornar400QuandoBodyFalharNaValidacao() throws Exception {
        mockMvc.perform(post(BASE_URL + "/validacao-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                .andExpect(jsonPath("$.mensagem").value("Erro de validação nos campos da requisição"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/validacao-body"))
                .andExpect(jsonPath("$.detalhes.nome").value("nome é obrigatório"));
    }

    @Test
    void deveRetornar400QuandoParametroFalharNaValidacao() throws Exception {
        mockMvc.perform(get(BASE_URL + "/validacao-parametro")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                .andExpect(jsonPath("$.mensagem").value("Erro de validação nos parâmetros da requisição"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/validacao-parametro"))
                .andExpect(jsonPath("$.detalhes.page").value("página deve ser maior que zero"));
    }

    @Test
    void deveRetornar400QuandoParametroPossuirTipoInvalido() throws Exception {
        mockMvc.perform(get(BASE_URL + "/tipo-parametro-invalido")
                        .param("usuarioId", "nao-e-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Parâmetros inválidos"))
                .andExpect(jsonPath("$.mensagem").value("Erro de validação nos parâmetros da requisição"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/tipo-parametro-invalido"))
                .andExpect(jsonPath("$.detalhes.usuarioId").value("Valor informado possui tipo inválido"));
    }

    @Test
    void deveRetornar400QuandoJsonForInvalidoOuMalformado() throws Exception {
        mockMvc.perform(post(BASE_URL + "/json-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido ou malformado"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/json-invalido"));
    }

    @Test
    void deveRetornar401QuandoOcorrerAuthenticationException() throws Exception {
        mockMvc.perform(get(BASE_URL + "/falha-autenticacao"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Não autorizado"))
                .andExpect(jsonPath("$.mensagem").value("Token ausente ou inválido"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/falha-autenticacao"));
    }

    @Test
    void deveRetornar401QuandoOcorrerJwtException() throws Exception {
        mockMvc.perform(get(BASE_URL + "/falha-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Não autorizado"))
                .andExpect(jsonPath("$.mensagem").value("Token ausente ou inválido"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/falha-jwt"));
    }

    @Test
    void deveRetornar500QuandoOcorrerErroInternoInesperado() throws Exception {
        mockMvc.perform(get(BASE_URL + "/erro-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.erro").value("Erro interno"))
                .andExpect(jsonPath("$.mensagem").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/erro-interno"));
    }

    /**
     * Controller sintético usado apenas para disparar exceções específicas
     * e validar o contrato do {@link GlobalExceptionHandler}.
     *
     * <p>O {@link Validated} é intencional no endpoint de validação de parâmetro,
     * pois o handler atual trata {@code ConstraintViolationException}.
     */
    @Validated
    @RestController
    @RequestMapping(BASE_URL)
    static class ControllerDeExcecoes {

        @GetMapping("/acesso-negado")
        void acessoNegado() {
            throw new AcessoNegadoException(
                    "Você não tem permissão para acessar este recurso"
            );
        }

        @GetMapping("/recurso-nao-encontrado")
        void recursoNaoEncontrado() {
            throw new RecursoNaoEncontradoException(
                    "Usuário não encontrado"
            );
        }

        @GetMapping("/conflito-negocio")
        void conflitoNegocio() {
            throw new NegocioException(
                    "E-mail já vinculado a outro perfil",
                    HttpStatus.CONFLICT
            );
        }

        @PostMapping("/validacao-body")
        void validacaoBody(
                @Valid @RequestBody CorpoValidacaoRequest request
        ) {
        }

        @GetMapping("/validacao-parametro")
        void validacaoParametro(
                @RequestParam
                @Min(value = 1, message = "página deve ser maior que zero")
                int page
        ) {
        }

        @GetMapping("/tipo-parametro-invalido")
        void tipoParametroInvalido(
                @RequestParam UUID usuarioId
        ) {
        }

        @PostMapping("/json-invalido")
        void jsonInvalido(
                @RequestBody CorpoValidacaoRequest request
        ) {
        }

        @GetMapping("/falha-autenticacao")
        void falhaAutenticacao() {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        @GetMapping("/falha-jwt")
        void falhaJwt() {
            throw new JwtException("Token inválido");
        }

        @GetMapping("/erro-interno")
        void erroInterno() {
            throw new IllegalStateException("Falha inesperada");
        }
    }

    record CorpoValidacaoRequest(

            @NotBlank(message = "nome é obrigatório")
            String nome
    ) {
    }
}