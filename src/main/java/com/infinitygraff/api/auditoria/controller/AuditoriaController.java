package com.infinitygraff.api.auditoria.controller;

import com.infinitygraff.api.auditoria.dto.LogAuditoriaResponse;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.response.ApiResponse;
import com.infinitygraff.api.common.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Controller responsável pela consulta de logs de auditoria.
 *
 * <p>Acessível apenas por ADMIN e GERENTE.
 *
 * <p>A ordenação é fixa: logs mais recentes primeiro.
 * Não é possível alterar a ordenação via query params nesta fase.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private static final String PAGINA_PADRAO = "0";
    private static final String TAMANHO_PADRAO = "50";
    private static final int TAMANHO_MAXIMO = 100;

    private final AuditoriaService auditoriaService;

    /**
     * Lista logs de auditoria paginados com filtros opcionais.
     *
     * <p>Todos os filtros são opcionais. Sem filtros, retorna todos os logs
     * paginados do mais recente para o mais antigo.
     *
     * <p>Os filtros de data devem ser enviados em formato ISO com timezone.
     * Exemplos:
     * <ul>
     *   <li>{@code 2026-05-08T00:00:00Z}</li>
     *   <li>{@code 2026-05-08T00:00:00-03:00}</li>
     * </ul>
     *
     * @param usuarioId filtra por UUID do usuário
     * @param acao      filtra por tipo de ação, tolerante a diferença de caixa
     * @param entidade  filtra por entidade afetada, tolerante a diferença de caixa
     * @param de        filtra registros criados a partir desta data, inclusive
     * @param ate       filtra registros criados até esta data, inclusive
     * @param page      número da página, base 0
     * @param size      itens por página, máximo 100
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PageResponse<LogAuditoriaResponse>>> listar(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String entidade,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime de,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime ate,

            @RequestParam(defaultValue = PAGINA_PADRAO)
            @PositiveOrZero(message = "Página não pode ser negativa")
            int page,

            @RequestParam(defaultValue = TAMANHO_PADRAO)
            @Min(value = 1, message = "Tamanho da página deve ser maior que zero")
            @Max(value = TAMANHO_MAXIMO, message = "Tamanho da página não pode ser maior que 100")
            int size,

            HttpServletRequest httpRequest
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<LogAuditoriaResponse> response = auditoriaService.listar(
                usuarioId,
                acao,
                entidade,
                de,
                ate,
                pageable
        );

        ApiResponse<PageResponse<LogAuditoriaResponse>> body = ApiResponse.sucesso(
                "Logs de auditoria carregados com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }
}