package com.infinitygraff.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção para recursos não encontrados ou indisponíveis.
 *
 * <p>Deve ser convertida pelo {@code GlobalExceptionHandler}
 * em resposta {@code 404 Not Found}.
 *
 * <p>Exemplos:
 * <ul>
 *   <li>usuário não encontrado;</li>
 *   <li>perfil interno ainda não criado;</li>
 *   <li>recurso deletado logicamente;</li>
 *   <li>pedido inexistente em fases futuras.</li>
 * </ul>
 */
public class RecursoNaoEncontradoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem, HttpStatus.NOT_FOUND);
    }
}