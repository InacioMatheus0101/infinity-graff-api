package com.infinitygraff.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção para ações em que o usuário está autenticado,
 * mas não possui permissão para executar a operação.
 *
 * <p>Deve ser convertida pelo {@code GlobalExceptionHandler}
 * em resposta {@code 403 Forbidden}.
 *
 * <p>Exemplos:
 * <ul>
 *   <li>CLIENTE tentando acessar recurso administrativo;</li>
 *   <li>PRESTADOR tentando acessar dados de outro usuário;</li>
 *   <li>tentativa de criar perfil ADMIN por fluxo público;</li>
 *   <li>GERENTE tentando executar ação exclusiva de ADMIN.</li>
 * </ul>
 */
public class AcessoNegadoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem, HttpStatus.FORBIDDEN);
    }
}