package com.infinitygraff.api.solicitacao.controller;

import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.common.response.ApiResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.solicitacao.dto.ArquivoSolicitacaoResponse;
import com.infinitygraff.api.solicitacao.enums.TipoArquivoSolicitacao;
import com.infinitygraff.api.solicitacao.service.ArquivoSolicitacaoArteService;
import com.infinitygraff.api.storage.dto.UrlAssinadaResponse;
import com.infinitygraff.api.usuario.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller responsável pelo gerenciamento de arquivos anexados
 * às solicitações de arte.
 * <p>
 * Fornece endpoints para upload, listagem, detalhe, geração de URL
 * assinada e remoção lógica de arquivos.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/solicitacoes/{solicitacaoId}/arquivos")
public class ArquivoSolicitacaoArteController {

    private final ArquivoSolicitacaoArteService arquivoService;
    private final SecurityContextHelper securityContextHelper;
    private final AuditoriaHelper auditoriaHelper;

    /**
     * Upload de arquivo para uma solicitação.
     * <p>
     * Aceita multipart/form-data com o arquivo e parâmetros de tipo e descrição.
     * Retorna HTTP 201 com os metadados do arquivo criado.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ArquivoSolicitacaoResponse>> enviar(
            @PathVariable UUID solicitacaoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") TipoArquivoSolicitacao tipo,
            @RequestParam(value = "descricao", required = false) String descricao,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        ArquivoSolicitacaoResponse response = arquivoService.enviar(
                solicitacaoId,
                file,
                tipo,
                descricao,
                autenticado,
                auditoriaHelper.criarContexto(httpRequest)
        );

        ApiResponse<ArquivoSolicitacaoResponse> body = ApiResponse.sucesso(
                "Arquivo enviado com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Lista os arquivos ativos de uma solicitação.
     * <p>
     * Opcionalmente filtra por tipo de arquivo.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArquivoSolicitacaoResponse>>> listar(
            @PathVariable UUID solicitacaoId,
            @RequestParam(value = "tipo", required = false) TipoArquivoSolicitacao tipo,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        List<ArquivoSolicitacaoResponse> arquivos = arquivoService.listar(
                solicitacaoId, tipo, autenticado);

        ApiResponse<List<ArquivoSolicitacaoResponse>> body = ApiResponse.sucesso(
                "Arquivos carregados com sucesso",
                arquivos,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Busca metadados de um arquivo específico.
     */
    @GetMapping("/{arquivoId}")
    public ResponseEntity<ApiResponse<ArquivoSolicitacaoResponse>> buscarPorId(
            @PathVariable UUID solicitacaoId,
            @PathVariable UUID arquivoId,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        ArquivoSolicitacaoResponse response = arquivoService.buscarPorId(
                solicitacaoId, arquivoId, autenticado);

        ApiResponse<ArquivoSolicitacaoResponse> body = ApiResponse.sucesso(
                "Arquivo carregado com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Gera uma URL assinada temporária para acesso ao arquivo original.
     * <p>
     * A URL expira automaticamente após 5 minutos.
     */
    @GetMapping("/{arquivoId}/url")
    public ResponseEntity<ApiResponse<UrlAssinadaResponse>> gerarUrl(
            @PathVariable UUID solicitacaoId,
            @PathVariable UUID arquivoId,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        UrlAssinadaResponse response = arquivoService.gerarUrl(
                solicitacaoId, arquivoId, autenticado);

        ApiResponse<UrlAssinadaResponse> body = ApiResponse.sucesso(
                "URL assinada gerada com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Remove funcionalmente um arquivo (soft delete de negócio).
     * <p>
     * O registro permanece no banco para auditoria, mas o arquivo
     * deixa de contar no limite de 50 e não aparece nas listagens.
     * Retorna HTTP 204 sem corpo.
     */
    @DeleteMapping("/{arquivoId}")
    public ResponseEntity<Void> remover(
            @PathVariable UUID solicitacaoId,
            @PathVariable UUID arquivoId,
            HttpServletRequest httpRequest
    ) {
        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

        arquivoService.remover(
                solicitacaoId,
                arquivoId,
                autenticado,
                auditoriaHelper.criarContexto(httpRequest)
        );

        return ResponseEntity.noContent().build();
    }
}