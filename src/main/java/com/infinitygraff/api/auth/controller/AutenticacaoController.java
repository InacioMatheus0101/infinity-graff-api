package com.infinitygraff.api.auth.controller;

import com.infinitygraff.api.auth.dto.CompletarPerfilRequest;
import com.infinitygraff.api.auth.dto.MeuPerfilResponse;
import com.infinitygraff.api.auth.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável pela ponte entre Supabase Auth e o backend interno.
 *
 * O Supabase Auth cuida de cadastro, login, sessão e refresh token.
 * Este controller expõe apenas rotas relacionadas ao perfil interno
 * do usuário dentro do marketplace.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/autenticacao")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    /**
     * Completa o perfil interno do usuário após autenticação pelo Supabase Auth.
     *
     * Requer Authorization: Bearer <access_token_supabase>.
     *
     * O token é validado pelo JwtAuthenticationFilter.
     * O service usa o SecurityContext, não valida token manualmente.
     */
    @PostMapping("/completar-perfil")
    public ResponseEntity<MeuPerfilResponse> completarPerfil(
            @Valid @RequestBody CompletarPerfilRequest request
    ) {
        MeuPerfilResponse response = autenticacaoService.completarPerfil(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna o perfil interno do usuário autenticado.
     *
     * Requer que o usuário já possua perfil interno criado na tabela usuarios.
     */
    @GetMapping("/meu-perfil")
    public ResponseEntity<MeuPerfilResponse> meuPerfil() {
        MeuPerfilResponse response = autenticacaoService.meuPerfil();
        return ResponseEntity.ok(response);
    }
}