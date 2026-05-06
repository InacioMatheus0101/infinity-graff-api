package com.infinitygraff.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base para violações de regra de negócio da aplicação.
 *
 * Deve ser usada quando a requisição é tecnicamente válida,
 * mas não pode ser concluída por uma regra do sistema.
 *
 * Exemplos:
 * - e-mail já vinculado a outro perfil;
 * - tentativa de criar perfil ADMIN pela rota pública;
 * - tentativa de alterar um recurso em estado inválido;
 * - conflito de dados de negócio.
 *
 * O GlobalExceptionHandler será responsável por converter esta exceção
 * no ErrorResponse padronizado da API.
 */

public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(String mensagem) {
        super(mensagem);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public NegocioException(String mensagem, HttpStatus status) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}