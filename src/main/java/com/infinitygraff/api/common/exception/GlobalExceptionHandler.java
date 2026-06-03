package com.infinitygraff.api.common.exception;

import com.infinitygraff.api.common.response.ErrorResponse;
import com.infinitygraff.api.storage.exception.StorageIntegrationException;
import com.infinitygraff.api.storage.exception.UploadArquivoException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler global que converte exceções em respostas padronizadas {@link ErrorResponse}.
 * <p>
 * Trata desde erros de validação e negócio até falhas de autenticação
 * e integração com serviços externos (ex: Supabase Storage).
 * <p>
 * <b>Regras de segurança:</b>
 * <ul>
 *   <li>Nunca expõe stack trace, tokens, senhas ou valores rejeitados.</li>
 *   <li>Mensagens para erros inesperados (500) são genéricas.</li>
 *   <li>Loga o erro internamente apenas para diagnóstico.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MENSAGEM_TOKEN_INVALIDO = "Token ausente ou inválido";
    private static final String MENSAGEM_ERRO_INTERNO = "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErrorResponse> tratarAcessoNegado(
            AcessoNegadoException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.erro(
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.erro(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Trata exceções de negócio, incluindo {@link UploadArquivoException}
     * e suas subclasses (como {@code ArquivoInvalidoException}).
     * O status HTTP é definido pela própria exceção.
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> tratarNegocio(
            NegocioException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getStatus() != null
                ? ex.getStatus()
                : HttpStatus.BAD_REQUEST;

        ErrorResponse response = ErrorResponse.erro(
                status,
                tituloParaStatus(status),
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Trata falhas de integração com o Supabase Storage.
     * Retorna HTTP 502 Bad Gateway, indicando que um serviço externo falhou.
     */
    @ExceptionHandler(StorageIntegrationException.class)
    public ResponseEntity<ErrorResponse> tratarStorageIntegration(
            StorageIntegrationException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.erro(
                HttpStatus.BAD_GATEWAY,
                "Erro de integração com storage",
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> tratarValidacaoBody(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String campo = fieldError.getField();
            String mensagem = fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : "Valor inválido";
            detalhes.putIfAbsent(campo, mensagem);
        }
        ex.getBindingResult().getGlobalErrors().forEach(objectError -> {
            String mensagem = objectError.getDefaultMessage() != null
                    ? objectError.getDefaultMessage()
                    : "Requisição inválida";
            detalhes.putIfAbsent("requisicao", mensagem);
        });
        ErrorResponse response = ErrorResponse.validacao(
                "Erro de validação nos campos da requisição",
                detalhes,
                request
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> tratarValidacaoParametros(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String campo = extrairNomeCampo(violation);
            String mensagem = violation.getMessage() != null
                    ? violation.getMessage()
                    : "Valor inválido";
            detalhes.putIfAbsent(campo, mensagem);
        }
        ErrorResponse response = ErrorResponse.validacao(
                "Erro de validação nos parâmetros da requisição",
                detalhes,
                request
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tratarTipoParametroInvalido(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        Map<String, String> detalhes = new LinkedHashMap<>();
        detalhes.put(ex.getName(), "Valor informado possui tipo inválido");
        ErrorResponse response = ErrorResponse.validacao(
                "Parâmetros inválidos",
                "Erro de validação nos parâmetros da requisição",
                detalhes,
                request
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> tratarJsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.erro(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Corpo da requisição inválido ou malformado",
                request
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({JwtException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> tratarAutenticacao(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.erro(
                HttpStatus.UNAUTHORIZED,
                "Não autorizado",
                MENSAGEM_TOKEN_INVALIDO,
                request
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Fallback para qualquer exceção não mapeada.
     * Retorna HTTP 500 com mensagem genérica e loga o stack trace internamente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> tratarErroInterno(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Erro interno inesperado na rota {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        ErrorResponse response = ErrorResponse.erro(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                MENSAGEM_ERRO_INTERNO,
                request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Associa um status HTTP a um título legível para a resposta de erro.
     *
     * @param status status HTTP
     * @return título padronizado
     */
    private String tituloParaStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Erro de negócio";
            case UNAUTHORIZED -> "Não autorizado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Recurso não encontrado";
            case CONFLICT -> "Conflito";
            case UNPROCESSABLE_ENTITY -> "Arquivo inválido";
            default -> "Erro";
        };
    }

    /**
     * Extrai o nome do campo a partir do caminho da violação de constraint.
     *
     * @param violation violação de validação
     * @return nome do campo
     */
    private String extrairNomeCampo(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() != null
                ? violation.getPropertyPath().toString()
                : "parametro";
        int ultimoPonto = path.lastIndexOf('.');
        if (ultimoPonto >= 0 && ultimoPonto < path.length() - 1) {
            return path.substring(ultimoPonto + 1);
        }
        return path.isBlank() ? "parametro" : path;
    }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<ErrorResponse> tratarArquivoGrande(
        MaxUploadSizeExceededException ex,
        HttpServletRequest request
) {
    log.warn("Upload de arquivo muito grande rejeitado: {}", ex.getMessage());
    ErrorResponse response = ErrorResponse.erro(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Arquivo inválido",
            "O arquivo enviado excede o tamanho máximo permitido.",
            request
    );
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
}


}