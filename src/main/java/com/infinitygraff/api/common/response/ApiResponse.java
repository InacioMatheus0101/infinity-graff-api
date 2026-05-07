package com.infinitygraff.api.common.response;

import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Wrapper padrão para respostas de sucesso com body.
 *
 * <p>Regras:
 * <ul>
 *   <li>{@code sucesso} sempre será {@code true};</li>
 *   <li>{@code timestamp} sempre será gerado em UTC;</li>
 *   <li>{@code dados} contém o payload específico do endpoint;</li>
 *   <li>respostas {@code 204 No Content} não devem usar este wrapper;</li>
 *   <li>erros não usam este wrapper — erros usam {@code ErrorResponse}.</li>
 * </ul>
 *
 * @param timestamp momento da resposta em UTC
 * @param sucesso indica sucesso da operação, sempre {@code true}
 * @param mensagem mensagem curta e amigável
 * @param dados payload da resposta
 * @param path rota chamada
 * @param <T> tipo do payload
 */
public record ApiResponse<T>(

        OffsetDateTime timestamp,

        boolean sucesso,

        String mensagem,

        T dados,

        String path
) {

    /**
     * Construtor compacto do record.
     *
     * <p>Garante que {@code ApiResponse} seja usado apenas para respostas
     * de sucesso. Erros devem usar {@code ErrorResponse}.
     */
    public ApiResponse {
        Objects.requireNonNull(timestamp, "timestamp não pode ser nulo");
        Objects.requireNonNull(mensagem, "mensagem não pode ser nula");
        Objects.requireNonNull(path, "path não pode ser nulo");

        if (!sucesso) {
            throw new IllegalArgumentException("ApiResponse só pode ser usado com sucesso = true");
        }
    }

    /**
     * Factory preferencial para controllers HTTP.
     *
     * <p>O path é extraído diretamente da requisição para evitar inconsistência
     * entre a rota real chamada e o valor retornado no response.
     */
    public static <T> ApiResponse<T> sucesso(
            String mensagem,
            T dados,
            HttpServletRequest request
    ) {
        return new ApiResponse<>(
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                mensagem,
                dados,
                request.getRequestURI()
        );
    }

    /**
     * Factory preferencial para controllers HTTP quando não há payload de resposta.
     *
     * <p>Não usar para respostas {@code 204 No Content}.
     * Respostas {@code 204} devem retornar body vazio.
     */
    public static ApiResponse<Void> sucesso(
            String mensagem,
            HttpServletRequest request
    ) {
        return new ApiResponse<>(
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                mensagem,
                null,
                request.getRequestURI()
        );
    }

    /**
     * Factory auxiliar para testes ou usos fora do contexto HTTP.
     *
     * <p>Em controllers, preferir o overload que recebe {@link HttpServletRequest},
     * para garantir que o path retornado seja sempre a rota real chamada.
     */
    public static <T> ApiResponse<T> sucesso(
            String mensagem,
            T dados,
            String path
    ) {
        return new ApiResponse<>(
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                mensagem,
                dados,
                path
        );
    }

    /**
     * Factory auxiliar para testes ou usos fora do contexto HTTP quando não há payload.
     *
     * <p>Em controllers, preferir o overload que recebe {@link HttpServletRequest}.
     * Não usar para respostas {@code 204 No Content}.
     */
    public static ApiResponse<Void> sucesso(
            String mensagem,
            String path
    ) {
        return new ApiResponse<>(
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                mensagem,
                null,
                path
        );
    }
}