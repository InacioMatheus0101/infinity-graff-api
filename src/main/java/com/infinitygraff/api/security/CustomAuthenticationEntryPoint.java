package com.infinitygraff.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitygraff.api.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handler responsável por respostas 401 Unauthorized.
 *
 * Usado quando a requisição não possui autenticação válida no backend:
 * - token Supabase ausente;
 * - token Supabase inválido;
 * - token Supabase expirado;
 * - perfil interno inativo/deletado, quando o filtro não autentica o usuário.
 *
 * Este handler roda dentro da cadeia do Spring Security, antes do fluxo normal
 * dos controllers. Por isso, ele não é tratado pelo GlobalExceptionHandler
 * e precisa montar explicitamente o ErrorResponse.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErrorResponse body = ErrorResponse.erro(
                status,
                "Não autorizado",
                "Token ausente ou inválido",
                request
        );

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}