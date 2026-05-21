package com.infinitygraff.api.solicitacao.controller;

import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.common.response.ApiResponse;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.solicitacao.dto.AtribuirPrestadorRequest;
import com.infinitygraff.api.solicitacao.dto.CancelarSolicitacaoRequest;
import com.infinitygraff.api.solicitacao.dto.CriarSolicitacaoArteRequest;
import com.infinitygraff.api.solicitacao.dto.HistoricoStatusSolicitacaoResponse;
import com.infinitygraff.api.solicitacao.dto.SolicitacaoArteDetalheResponse;
import com.infinitygraff.api.solicitacao.dto.SolicitacaoArteResumoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
import com.infinitygraff.api.solicitacao.service.SolicitacaoArteService;
import com.infinitygraff.api.usuario.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Controller responsável pelas solicitações de arte da plataforma.
 *
 * <p>Esta etapa cobre:
 * <ul>
 *   <li>criação de solicitação;</li>
 *   <li>listagem com paginação, ordenação e filtros;</li>
 *   <li>consulta detalhada;</li>
 *   <li>atribuição inicial de prestador;</li>
 *   <li>cancelamento com motivo obrigatório;</li>
 *   <li>consulta do histórico de status.</li>
 * </ul>
 *
 * <p>As regras de permissão são aplicadas no {@code SolicitacaoArteService}.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoArteController {

    private static final String PAGINA_PADRAO = "0";
    private static final String TAMANHO_PADRAO = "20";
    private static final int TAMANHO_MAXIMO = 100;

    private static final String ORDENACAO_PADRAO = "criadoEm,desc";

    /**
     * Campos permitidos para ordenação.
     *
     * <p>Apenas um campo de ordenação é suportado nesta etapa.
     * Formato aceito: {@code campo,direcao}.
     * Exemplo: {@code titulo,asc} ou {@code criadoEm,desc}.
     */
    private static final String ORDENACAO_REGEX =
            "^(titulo|tipo|status|criadoEm|atualizadoEm|canceladoEm)(,([aA][sS][cC]|[dD][eE][sS][cC]))?$";

    private final SolicitacaoArteService solicitacaoArteService;
    private final SecurityContextHelper securityContextHelper;
    private final AuditoriaHelper auditoriaHelper;

    /**
     * Cria uma nova solicitação de arte.
     *
     * <p>CLIENTE cria para si.
     * ADMIN e GERENTE podem criar para um cliente informado.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SolicitacaoArteDetalheResponse>> criar(
            @Valid @RequestBody CriarSolicitacaoArteRequest request,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        SolicitacaoArteDetalheResponse response = solicitacaoArteService.criar(
                request,
                autenticado,
                auditoriaHelper.criarContexto(httpRequest)
        );

        ApiResponse<SolicitacaoArteDetalheResponse> body = ApiResponse.sucesso(
                "Solicitação de arte criada com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Lista solicitações de arte conforme a role do usuário autenticado.
     *
     * <p>ADMIN e GERENTE podem usar filtros administrativos.
     * CLIENTE e PRESTADOR ficam restritos ao próprio escopo.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SolicitacaoArteResumoResponse>>> listar(
            @RequestParam(required = false) StatusSolicitacaoArte status,
            @RequestParam(required = false) TipoSolicitacaoArte tipo,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID prestadorId,
            @RequestParam(required = false) Boolean semPrestador,

            @RequestParam(defaultValue = PAGINA_PADRAO)
            @PositiveOrZero(message = "Página não pode ser negativa")
            int page,

            @RequestParam(defaultValue = TAMANHO_PADRAO)
            @Min(value = 1, message = "Tamanho da página deve ser maior que zero")
            @Max(value = TAMANHO_MAXIMO, message = "Tamanho da página não pode ser maior que 100")
            int size,

            @RequestParam(defaultValue = ORDENACAO_PADRAO)
            @Pattern(
                    regexp = ORDENACAO_REGEX,
                    message = "Ordenação inválida. Use o formato campo,direcao com um dos campos permitidos: titulo, tipo, status, criadoEm, atualizadoEm ou canceladoEm"
            )
            String sort,

            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        Pageable pageable = criarPageable(page, size, sort);

        Page<SolicitacaoArteResumoResponse> pagina = solicitacaoArteService.listar(
                autenticado,
                status,
                tipo,
                clienteId,
                prestadorId,
                semPrestador,
                pageable
        );

        PageResponse<SolicitacaoArteResumoResponse> response = PageResponse.de(pagina);

        ApiResponse<PageResponse<SolicitacaoArteResumoResponse>> body = ApiResponse.sucesso(
                "Solicitações de arte carregadas com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Busca os detalhes de uma solicitação de arte por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SolicitacaoArteDetalheResponse>> buscarPorId(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        SolicitacaoArteDetalheResponse response = solicitacaoArteService.buscarPorId(
                id,
                autenticado
        );

        ApiResponse<SolicitacaoArteDetalheResponse> body = ApiResponse.sucesso(
                "Solicitação de arte carregada com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Atribui o prestador inicial à solicitação de arte.
     *
     * <p>Apenas ADMIN e GERENTE podem executar esta ação.
     */
    @PatchMapping("/{id}/atribuir-prestador")
    public ResponseEntity<ApiResponse<SolicitacaoArteDetalheResponse>> atribuirPrestador(
            @PathVariable UUID id,
            @Valid @RequestBody AtribuirPrestadorRequest request,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        SolicitacaoArteDetalheResponse response = solicitacaoArteService.atribuirPrestador(
                id,
                request,
                autenticado,
                auditoriaHelper.criarContexto(httpRequest)
        );

        ApiResponse<SolicitacaoArteDetalheResponse> body = ApiResponse.sucesso(
                "Prestador atribuído com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Cancela uma solicitação de arte com motivo obrigatório.
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<SolicitacaoArteDetalheResponse>> cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody CancelarSolicitacaoRequest request,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        SolicitacaoArteDetalheResponse response = solicitacaoArteService.cancelar(
                id,
                request,
                autenticado,
                auditoriaHelper.criarContexto(httpRequest)
        );

        ApiResponse<SolicitacaoArteDetalheResponse> body = ApiResponse.sucesso(
                "Solicitação de arte cancelada com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Lista o histórico de status de uma solicitação de arte.
     */
    @GetMapping("/{id}/historico-status")
    public ResponseEntity<ApiResponse<List<HistoricoStatusSolicitacaoResponse>>> listarHistoricoStatus(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        List<HistoricoStatusSolicitacaoResponse> response =
                solicitacaoArteService.listarHistoricoStatus(id, autenticado);

        ApiResponse<List<HistoricoStatusSolicitacaoResponse>> body = ApiResponse.sucesso(
                "Histórico de status carregado com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    private Pageable criarPageable(int page, int size, String sort) {
        Ordenacao ordenacao = interpretarOrdenacao(sort);

        return PageRequest.of(
                page,
                size,
                Sort.by(ordenacao.direcao(), ordenacao.campo())
        );
    }

    /**
     * Interpreta o parâmetro de ordenação no formato {@code campo,direcao}.
     *
     * <p>Apenas um campo de ordenação é suportado nesta etapa.
     * A validação do formato é feita por {@code @Pattern} no parâmetro {@code sort}.
     */
    private Ordenacao interpretarOrdenacao(String sort) {
        String valor = sort == null || sort.isBlank()
                ? ORDENACAO_PADRAO
                : sort.trim();

        String[] partes = valor.split(",");

        String campo = partes[0].trim();

        Sort.Direction direcao = Sort.Direction.DESC;

        if (partes.length == 2) {
            String direcaoInformada = partes[1].trim().toLowerCase(Locale.ROOT);

            direcao = "asc".equals(direcaoInformada)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
        }

        return new Ordenacao(campo, direcao);
    }

    private record Ordenacao(
            String campo,
            Sort.Direction direcao
    ) {
    }
}