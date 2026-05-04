package com.infinitygraff.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler responsável por respostas 401 Unauthorized.
 *
 * Usado quando a requisição não possui autenticação válida:
 * - token ausente;
 * - token inválido;
 * - token expirado;
 * - credenciais inválidas.
 *
 * Nesta etapa, o JSON é montado manualmente porque ErrorResponse
 * será padronizado na etapa de tratamento global de erros.
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

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        body.put("status", status.value());
        body.put("erro", "Não autorizado");
        body.put("mensagem", "Token ausente ou inválido");
        body.put("path", request.getRequestURI());
        body.put("detalhes", null);

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}