package com.infinitygraff.api.chat.controller;

import com.infinitygraff.api.auditoria.service.AuditoriaHelper;
import com.infinitygraff.api.chat.dto.*;
import com.infinitygraff.api.chat.service.ChatService;
import com.infinitygraff.api.common.response.ApiResponse;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.usuario.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Controller responsável pelo chat entre cliente e prestador de uma solicitação.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/solicitacoes/{solicitacaoId}/chat")
public class ChatController {

    private static final String PAGINA_PADRAO = "0";
    private static final String TAMANHO_PADRAO = "50";
    private static final int TAMANHO_MAXIMO = 100;

    private final ChatService chatService;
    private final SecurityContextHelper securityContextHelper;
    private final AuditoriaHelper auditoriaHelper;

    /**
     * Lista mensagens do chat de uma solicitação.
     */
    @GetMapping("/mensagens")
    public ResponseEntity<ApiResponse<PageResponse<MensagemChatResponse>>> listar(
            @PathVariable UUID solicitacaoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            @RequestParam(defaultValue = PAGINA_PADRAO) @PositiveOrZero int page,
            @RequestParam(defaultValue = TAMANHO_PADRAO) @Min(1) @Max(TAMANHO_MAXIMO) int size,
            HttpServletRequest httpRequest) {

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        Pageable pageable = PageRequest.of(page, size, Sort.by("criadoEm").ascending());

        Page<MensagemChatResponse> pagina = chatService.listar(
                solicitacaoId, inicio, fim, pageable, autenticado);

        PageResponse<MensagemChatResponse> response = PageResponse.de(pagina);
        ApiResponse<PageResponse<MensagemChatResponse>> body = ApiResponse.sucesso(
                "Mensagens carregadas com sucesso", response, httpRequest);

        return ResponseEntity.ok(body);
    }

    /**
     * Envia uma nova mensagem.
     */
    @PostMapping("/mensagens")
    public ResponseEntity<ApiResponse<MensagemChatResponse>> enviar(
            @PathVariable UUID solicitacaoId,
            @Valid @RequestBody EnviarMensagemRequest request,
            HttpServletRequest httpRequest) {

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        MensagemChatResponse resposta = chatService.enviar(
                solicitacaoId, request, autenticado,
                auditoriaHelper.criarContexto(httpRequest));

        ApiResponse<MensagemChatResponse> body = ApiResponse.sucesso(
                "Mensagem enviada com sucesso", resposta, httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Edita uma mensagem existente.
     */
    @PatchMapping("/mensagens/{mensagemId}")
    public ResponseEntity<ApiResponse<MensagemChatResponse>> editar(
            @PathVariable UUID solicitacaoId,
            @PathVariable UUID mensagemId,
            @Valid @RequestBody EditarMensagemRequest request,
            HttpServletRequest httpRequest) {

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        MensagemChatResponse resposta = chatService.editar(
                solicitacaoId, mensagemId, request, autenticado,
                auditoriaHelper.criarContexto(httpRequest));

        ApiResponse<MensagemChatResponse> body = ApiResponse.sucesso(
                "Mensagem editada com sucesso", resposta, httpRequest);

        return ResponseEntity.ok(body);
    }

    /**
     * Exclui logicamente uma mensagem.
     */
    @DeleteMapping("/mensagens/{mensagemId}")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID solicitacaoId,
            @PathVariable UUID mensagemId,
            HttpServletRequest httpRequest) {

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        chatService.deletar(solicitacaoId, mensagemId, autenticado,
                auditoriaHelper.criarContexto(httpRequest));

        return ResponseEntity.noContent().build();
    }

    /**
     * Marca um conjunto de mensagens como lidas.
     */
    @PatchMapping("/mensagens/lidas")
    public ResponseEntity<ApiResponse<Void>> marcarComoLidas(
            @PathVariable UUID solicitacaoId,
            @Valid @RequestBody MarcarLidasRequest request,
            HttpServletRequest httpRequest) {

        Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();
        chatService.marcarComoLidas(solicitacaoId, request, autenticado);

        ApiResponse<Void> body = ApiResponse.sucesso(
                "Mensagens marcadas como lidas", null, httpRequest);

        return ResponseEntity.ok(body);
    }
}