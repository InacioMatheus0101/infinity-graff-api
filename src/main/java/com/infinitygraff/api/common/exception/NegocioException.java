package com.infinitygraff.api.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * Exceção base para violações de regra de negócio da aplicação.
 *
 * <p>Deve ser usada quando a requisição é tecnicamente válida,
 * mas não pode ser concluída por uma regra do sistema.
 *
 * <p>Exemplos:
 * <ul>
 *   <li>e-mail já vinculado a outro perfil;</li>
 *   <li>tentativa de criar perfil ADMIN pela rota pública;</li>
 *   <li>tentativa de alterar um recurso em estado inválido;</li>
 *   <li>conflito de dados de negócio.</li>
 * </ul>
 *
 * <p>O {@code GlobalExceptionHandler} converte esta exceção
 * no {@code ErrorResponse} padronizado da API.
 */
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public NegocioException(String mensagem) {
        this(mensagem, HttpStatus.BAD_REQUEST);
    }

    public NegocioException(String mensagem, HttpStatus status) {
        super(mensagemValidaOuPadrao(mensagem));
        this.status = validarStatus(status);
    }

    public HttpStatus getStatus() {
        return status;
    }

    private static String mensagemValidaOuPadrao(String mensagem) {
        return mensagem == null || mensagem.isBlank()
                ? "A requisição não pôde ser processada"
                : mensagem;
    }

    private static HttpStatus validarStatus(HttpStatus status) {
        Objects.requireNonNull(status, "status não pode ser nulo");

        if (!status.is4xxClientError()) {
            throw new IllegalArgumentException("status da exceção de negócio deve ser 4xx");
        }

        return status;
    }
}