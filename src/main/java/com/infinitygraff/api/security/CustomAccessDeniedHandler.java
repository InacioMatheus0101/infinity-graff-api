package com.infinitygraff.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitygraff.api.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handler responsável por respostas 403 Forbidden.
 *
 * Usado quando o usuário possui token válido e autenticação reconhecida,
 * mas não possui role/permissão suficiente para acessar determinado recurso.
 *
 * Este handler roda dentro da cadeia do Spring Security, antes do fluxo normal
 * dos controllers. Por isso, ele não é tratado pelo GlobalExceptionHandler
 * e precisa montar explicitamente o ErrorResponse.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse body = ErrorResponse.erro(
                status,
                "Acesso negado",
                "Você não tem permissão para acessar este recurso",
                request
        );

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}