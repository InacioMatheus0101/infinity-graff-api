package com.infinitygraff.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção para recursos não encontrados ou indisponíveis.
 *
 * Deve ser convertida pelo GlobalExceptionHandler em resposta 404 Not Found.
 *
 * Exemplos:
 * - usuário não encontrado;
 * - perfil interno ainda não criado;
 * - recurso deletado logicamente;
 * - pedido inexistente em fases futuras.
 */
public class RecursoNaoEncontradoException extends NegocioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem, HttpStatus.NOT_FOUND);
    }
}