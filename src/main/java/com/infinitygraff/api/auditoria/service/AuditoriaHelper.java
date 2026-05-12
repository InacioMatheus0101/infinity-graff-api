package com.infinitygraff.api.auditoria.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.infinitygraff.api.auditoria.AuditoriaConstantes.TAMANHO_MAXIMO_IP;

import java.util.Objects;

/**
 * Helper compartilhado para operações auxiliares de auditoria.
 *
 * <p>Centraliza:
 * <ul>
 *   <li>execução de ações após commit da transação principal;</li>
 *   <li>serialização segura de payloads auditáveis;</li>
 *   <li>extração de IP e User-Agent da requisição HTTP.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaHelper {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";

    private final ObjectMapper objectMapper;

    /**
     * Executa uma ação somente após o commit da transação atual.
     *
     * <p>Motivo:
     * <ul>
     *   <li>{@code afterCommit} garante que o log só será registrado após sucesso da transação principal;</li>
     *   <li>{@code REQUIRES_NEW} no {@code AuditoriaService} garante que o log use transação própria.</li>
     * </ul>
     *
     * <p>Se não houver transação ativa, executa imediatamente.
     */
    public void executarAposCommit(Runnable acao) {
        Objects.requireNonNull(acao, "acao não pode ser nula");

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            acao.run();
                        }
                    }
            );
            return;
        }

        acao.run();
    }

    /**
     * Serializa um objeto seguro para JSON.
     *
     * <p>Não deve receber entidade JPA completa, senha, token ou dados sensíveis.
     * Preferir DTOs de response já sanitizados, como {@code UsuarioResponse}.
     */
    public String serializarSeguro(Object valor) {
        if (valor == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            log.warn(
                    "Não foi possível serializar dados de auditoria. tipo={}",
                    valor.getClass().getSimpleName(),
                    e
            );
            return null;
        }
    }

    /**
     * Cria o contexto de auditoria a partir da requisição HTTP.
     */
    public AuditoriaContext criarContexto(HttpServletRequest request) {
        if (request == null) {
            return AuditoriaContext.vazio();
        }

        return new AuditoriaContext(
                extrairIp(request),
                extrairUserAgent(request)
        );
    }

    private String extrairIp(HttpServletRequest request) {
        String forwardedFor = normalizarTextoOpcional(request.getHeader(HEADER_X_FORWARDED_FOR));

        if (forwardedFor != null) {
            String primeiroIp = forwardedFor.split(",")[0].trim();
            return limitarIp(primeiroIp);
        }

        String realIp = normalizarTextoOpcional(request.getHeader(HEADER_X_REAL_IP));

        if (realIp != null) {
            return limitarIp(realIp);
        }

        return limitarIp(normalizarTextoOpcional(request.getRemoteAddr()));
    }
    
    /**
 * Extrai o User-Agent da requisição.
 *
 * <p>O campo {@code user_agent} é armazenado como {@code TEXT}
 * na migration {@code V2__create_audit_logs_table.sql}, portanto
 * não aplicamos limite de tamanho aqui.
 */

private String extrairUserAgent(HttpServletRequest request) {
    return normalizarTextoOpcional(request.getHeader(HEADER_USER_AGENT));
}
    private String limitarIp(String valor) {
        if (valor == null || valor.length() <= TAMANHO_MAXIMO_IP) {
            return valor;
        }

        log.warn(
                "IP de auditoria truncado para {} caracteres. Valor iniciado por: {}",
                TAMANHO_MAXIMO_IP,
                valor.substring(0, Math.min(valor.length(), 20))
        );

        return valor.substring(0, TAMANHO_MAXIMO_IP);
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}