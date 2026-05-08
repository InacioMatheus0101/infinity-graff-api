package com.infinitygraff.api.auth.controller;

import com.infinitygraff.api.auth.dto.CompletarPerfilRequest;
import com.infinitygraff.api.auth.service.AutenticacaoService;
import com.infinitygraff.api.auth.service.ResultadoCompletarPerfil;
import com.infinitygraff.api.common.response.ApiResponse;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável pela ponte entre Supabase Auth e o backend interno.
 *
 * <p>O Supabase Auth cuida de cadastro, login, senha, sessão e refresh token.
 *
 * <p>Este controller expõe apenas rotas relacionadas ao perfil interno
 * do usuário dentro do marketplace.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/autenticacao")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    /**
     * Completa ou sincroniza o perfil interno do usuário após autenticação pelo Supabase Auth.
     *
     * <p>Requer {@code Authorization: Bearer <access_token_supabase>}.
     *
     * <p>O token é validado pelo {@code JwtAuthenticationFilter}.
     * O service usa o {@code SecurityContext}, não valida token manualmente.
     *
     * <p>Esta rota é idempotente quando o perfil já existe com a mesma role:
     * nesse caso, retorna {@code 200 OK} com o perfil existente.
     * Quando o perfil é criado pela primeira vez, retorna {@code 201 Created}.
     */
    @PostMapping("/completar-perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> completarPerfil(
            @Valid @RequestBody CompletarPerfilRequest request,
            HttpServletRequest httpRequest
    ) {
        ResultadoCompletarPerfil resultado = autenticacaoService.completarPerfil(request);

        String mensagem = resultado.criado()
                ? "Perfil interno criado com sucesso"
                : "Perfil interno sincronizado com sucesso";

        ApiResponse<UsuarioResponse> body = ApiResponse.sucesso(
                mensagem,
                resultado.usuario(),
                httpRequest
        );

        HttpStatus status = resultado.criado()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Retorna o perfil interno do usuário autenticado.
     *
     * <p>Requer que o usuário já possua perfil interno criado na tabela {@code usuarios}.
     */
    @GetMapping("/meu-perfil")
    public ResponseEntity<ApiResponse<UsuarioResponse>> meuPerfil(
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = autenticacaoService.meuPerfil();

        ApiResponse<UsuarioResponse> body = ApiResponse.sucesso(
                "Perfil carregado com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }
}