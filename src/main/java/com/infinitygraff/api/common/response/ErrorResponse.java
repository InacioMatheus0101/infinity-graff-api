package com.infinitygraff.api.common.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Formato padrão para respostas de erro da API.
 *
 * <p>Regras:
 * <ul>
 *   <li>erros nunca usam {@code ApiResponse};</li>
 *   <li>{@code timestamp} sempre é gerado em UTC;</li>
 *   <li>{@code detalhes} só deve ser preenchido em erros de validação por campo;</li>
 *   <li>nos demais erros, {@code detalhes} deve ser {@code null};</li>
 *   <li>não deve expor stack trace, tokens, senhas ou valores rejeitados sensíveis.</li>
 * </ul>
 *
 * @param timestamp momento do erro em UTC
 * @param status status HTTP numérico
 * @param erro título curto do erro
 * @param mensagem mensagem amigável para o cliente
 * @param path rota chamada
 * @param detalhes erros por campo, usado apenas em validação
 */
public record ErrorResponse(

        OffsetDateTime timestamp,

        int status,

        String erro,

        String mensagem,

        String path,

        Map<String, String> detalhes
) {

    /**
     * Construtor compacto do record.
     *
     * <p>Garante consistência mínima do payload de erro.
     */
    public ErrorResponse {
        Objects.requireNonNull(timestamp, "timestamp não pode ser nulo");

        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status HTTP inválido");
        }

        if (semTexto(erro)) {
            throw new IllegalArgumentException("erro não pode ser vazio");
        }

        if (semTexto(mensagem)) {
            throw new IllegalArgumentException("mensagem não pode ser vazia");
        }

        if (semTexto(path)) {
            throw new IllegalArgumentException("path não pode ser vazio");
        }

        detalhes = normalizarDetalhes(detalhes);
    }

    /**
     * Factory preferencial para erros comuns em contexto HTTP.
     *
     * <p>O path é extraído diretamente da requisição para evitar inconsistência
     * entre a rota real chamada e o valor retornado no response.
     */
    public static ErrorResponse erro(
            HttpStatus status,
            String erro,
            String mensagem,
            HttpServletRequest request
    ) {
        Objects.requireNonNull(status, "status não pode ser nulo");
        Objects.requireNonNull(request, "request não pode ser nulo");

        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                erro,
                mensagem,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Factory preferencial para erros de validação em contexto HTTP.
     *
     * <p>Usa o título padrão {@code Dados inválidos}.
     * O mapa de detalhes deve conter apenas nome do campo e mensagem.
     * Não incluir valores rejeitados para evitar exposição de dados sensíveis.
     */
    public static ErrorResponse validacao(
            String mensagem,
            Map<String, String> detalhes,
            HttpServletRequest request
    ) {
        return validacao("Dados inválidos", mensagem, detalhes, request);
    }

    /**
     * Factory para erros de validação em contexto HTTP com título customizado.
     *
     * <p>Usar apenas quando o título padrão {@code Dados inválidos}
     * não representar bem o tipo de validação, por exemplo validação de query params.
     */
    public static ErrorResponse validacao(
            String erro,
            String mensagem,
            Map<String, String> detalhes,
            HttpServletRequest request
    ) {
        Objects.requireNonNull(request, "request não pode ser nulo");

        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.BAD_REQUEST.value(),
                erro,
                mensagem,
                request.getRequestURI(),
                detalhes
        );
    }

    /**
     * Factory auxiliar para testes ou usos fora do contexto HTTP.
     *
     * <p>Em handlers HTTP, preferir o overload que recebe {@link HttpServletRequest},
     * para garantir que o path retornado seja sempre a rota real chamada.
     */
    public static ErrorResponse erro(
            HttpStatus status,
            String erro,
            String mensagem,
            String path
    ) {
        Objects.requireNonNull(status, "status não pode ser nulo");

        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                erro,
                mensagem,
                path,
                null
        );
    }

    /**
     * Factory auxiliar para testes ou usos fora do contexto HTTP em erros de validação.
     *
     * <p>Em handlers HTTP, preferir o overload que recebe {@link HttpServletRequest}.
     */
    public static ErrorResponse validacao(
            String mensagem,
            Map<String, String> detalhes,
            String path
    ) {
        return validacao("Dados inválidos", mensagem, detalhes, path);
    }

    /**
     * Factory auxiliar para testes ou usos fora do contexto HTTP em erros de validação
     * com título customizado.
     *
     * <p>Em handlers HTTP, preferir o overload que recebe {@link HttpServletRequest}.
     */
    public static ErrorResponse validacao(
            String erro,
            String mensagem,
            Map<String, String> detalhes,
            String path
    ) {
        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.BAD_REQUEST.value(),
                erro,
                mensagem,
                path,
                detalhes
        );
    }

    private static Map<String, String> normalizarDetalhes(Map<String, String> detalhes) {
        if (detalhes == null || detalhes.isEmpty()) {
            return null;
        }

        Map<String, String> normalizado = new LinkedHashMap<>();

        detalhes.forEach((campo, mensagem) -> {
            if (!semTexto(campo) && !semTexto(mensagem)) {
                normalizado.putIfAbsent(campo, mensagem);
            }
        });

        return normalizado.isEmpty()
                ? null
                : Collections.unmodifiableMap(normalizado);
    }

    private static boolean semTexto(String valor) {
        return valor == null || valor.isBlank();
    }
}