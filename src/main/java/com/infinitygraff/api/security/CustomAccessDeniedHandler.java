package com.infinitygraff.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler responsável por respostas 403 Forbidden.
 *
 * Usado quando o usuário está autenticado, mas não possui permissão
 * para acessar determinado recurso.
 *
 * Nesta etapa, o JSON é montado manualmente porque ErrorResponse
 * será padronizado na etapa de tratamento global de erros.
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

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        body.put("status", status.value());
        body.put("erro", "Acesso negado");
        body.put("mensagem", "Você não tem permissão para acessar este recurso");
        body.put("path", request.getRequestURI());
        body.put("detalhes", null);

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}