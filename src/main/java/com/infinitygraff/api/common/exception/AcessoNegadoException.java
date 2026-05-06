package com.infinitygraff.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção para ações em que o usuário está autenticado,
 * mas não possui permissão para executar a operação.
 *
 * Deve ser convertida pelo GlobalExceptionHandler em resposta 403 Forbidden.
 *
 * Exemplos:
 * - CLIENTE tentando acessar recurso administrativo;
 * - PRESTADOR tentando acessar dados de outro usuário;
 * - tentativa de criar perfil ADMIN por fluxo público;
 * - GERENTE tentando executar ação exclusiva de ADMIN.
 */

public class AcessoNegadoException extends NegocioException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem, HttpStatus.FORBIDDEN);
    }
}