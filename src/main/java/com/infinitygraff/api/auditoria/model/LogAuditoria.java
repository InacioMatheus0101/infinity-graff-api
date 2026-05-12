package com.infinitygraff.api.auditoria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.infinitygraff.api.auditoria.AuditoriaConstantes.TAMANHO_MAXIMO_ACAO;
import static com.infinitygraff.api.auditoria.AuditoriaConstantes.TAMANHO_MAXIMO_ENTIDADE;
import static com.infinitygraff.api.auditoria.AuditoriaConstantes.TAMANHO_MAXIMO_IP;

/**
 * Registro imutável de uma ação sensível na plataforma.
 *
 * <p><b>Imutabilidade:</b> logs de auditoria nunca são alterados ou deletados.
 * Todos os campos possuem {@code updatable = false}. Não há setters expostos.
 *
 * <p><b>Desacoplamento:</b> {@code usuarioId} é um UUID simples, não um
 * {@code @ManyToOne}, preservando o histórico mesmo após soft delete do usuário.
 *
 * <p><b>Rastreabilidade:</b> {@code dadosAntes} e {@code dadosDepois} armazenam
 * o estado da entidade em JSON serializado, permitindo reconstruir o histórico
 * de alterações quando aplicável.
 */
@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "logs_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * UUID do usuário que realizou a ação.
     *
     * <p>Nullable para ações sem usuário autenticado.
     */
    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    /**
     * Tipo de evento auditado.
     *
     * <p>Exemplos: {@code USUARIO_CADASTRADO}, {@code USUARIO_ATIVADO},
     * {@code USUARIO_DESATIVADO}, {@code USUARIO_DELETADO}.
     */
    @Column(name = "acao", nullable = false, length = TAMANHO_MAXIMO_ACAO, updatable = false)
    private String acao;

    /**
     * Nome da entidade afetada.
     *
     * <p>Exemplo: {@code usuarios}.
     */
    @Column(name = "entidade", length = TAMANHO_MAXIMO_ENTIDADE, updatable = false)
    private String entidade;

    /**
     * ID da entidade afetada quando aplicável.
     */
    @Column(name = "entidade_id", updatable = false)
    private UUID entidadeId;

    /**
     * Estado anterior da entidade em JSON serializado, quando aplicável.
     */
    @Column(name = "dados_antes", columnDefinition = "TEXT", updatable = false)
    private String dadosAntes;

    /**
     * Estado posterior da entidade em JSON serializado, quando aplicável.
     */
    @Column(name = "dados_depois", columnDefinition = "TEXT", updatable = false)
    private String dadosDepois;

    /**
     * IP do cliente ou origem identificada pelo backend.
     */
    @Column(name = "ip", length = TAMANHO_MAXIMO_IP, updatable = false)
    private String ip;

    /**
     * User-Agent do cliente HTTP.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT", updatable = false)
    private String userAgent;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /**
     * Construtor controlado usado pelo Builder.
     *
     * <p>Não recebe {@code criadoEm}. O timestamp do log deve ser preenchido
     * exclusivamente no {@code @PrePersist}, evitando criação de logs com data falsa.
     */
    @Builder
    private LogAuditoria(
            UUID usuarioId,
            String acao,
            String entidade,
            UUID entidadeId,
            String dadosAntes,
            String dadosDepois,
            String ip,
            String userAgent
    ) {
        this.usuarioId = usuarioId;
        this.acao = validarTamanhoMaximo(
                normalizarTextoObrigatorio(acao, "acao"),
                TAMANHO_MAXIMO_ACAO,
                "acao"
        );
        this.entidade = validarTamanhoMaximo(
                normalizarTextoOpcional(entidade),
                TAMANHO_MAXIMO_ENTIDADE,
                "entidade"
        );
        this.entidadeId = entidadeId;
        this.dadosAntes = normalizarTextoOpcional(dadosAntes);
        this.dadosDepois = normalizarTextoOpcional(dadosDepois);
        this.ip = truncarComAviso(
                normalizarTextoOpcional(ip),
                TAMANHO_MAXIMO_IP,
                "ip"
        );
        this.userAgent = normalizarTextoOpcional(userAgent);
    }

    /**
     * Preenche {@code criadoEm} no momento da persistência.
     */
    @PrePersist
    protected void aoInserir() {
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static String normalizarTextoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }

        return valor.trim();
    }

    private static String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private static String validarTamanhoMaximo(String valor, int tamanhoMaximo, String campo) {
        if (valor == null) {
            return null;
        }

        if (valor.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                    campo + " não pode ter mais que " + tamanhoMaximo + " caracteres"
            );
        }

        return valor;
    }

    private static String truncarComAviso(String valor, int tamanhoMaximo, String campo) {
        if (valor == null || valor.length() <= tamanhoMaximo) {
            return valor;
        }

        log.warn(
                "Campo de auditoria '{}' truncado para {} caracteres. Valor iniciado por: {}",
                campo,
                tamanhoMaximo,
                valor.substring(0, Math.min(valor.length(), 20))
        );

        return valor.substring(0, tamanhoMaximo);
    }
}