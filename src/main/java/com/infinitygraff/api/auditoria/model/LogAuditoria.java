package com.infinitygraff.api.auditoria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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
 *
 * <p><b>Transação:</b> a entidade não controla transações diretamente.
 * A gravação dos logs será feita pelo {@code AuditoriaService}, com estratégia
 * transacional definida na camada de serviço.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "logs_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** UUID do usuário que realizou a ação. Nullable para ações anônimas (ex: LOGIN_FALHA). */
    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    /** Tipo de evento auditado (ex: LOGIN_SUCESSO, USUARIO_DELETADO). */
    @Column(name = "acao", nullable = false, length = 100, updatable = false)
    private String acao;

    /** Nome da entidade afetada (ex: "usuarios", "refresh_tokens"). */
    @Column(name = "entidade", length = 50, updatable = false)
    private String entidade;

    /** ID da entidade afetada quando aplicável. */
    @Column(name = "entidade_id", updatable = false)
    private UUID entidadeId;

    /** Estado anterior da entidade em JSON serializado, quando aplicável. */
    @Column(name = "dados_antes", columnDefinition = "TEXT", updatable = false)
    private String dadosAntes;

    /** Estado posterior da entidade em JSON serializado, quando aplicável. */
    @Column(name = "dados_depois", columnDefinition = "TEXT", updatable = false)
    private String dadosDepois;

    /** IP do cliente ou origem identificada pelo backend. */
    @Column(name = "ip", length = 100, updatable = false)
    private String ip;

    /** User-Agent do cliente HTTP. */
    @Column(name = "user_agent", columnDefinition = "TEXT", updatable = false)
    private String userAgent;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // =========================================================
    // Ciclo de vida JPA
    // =========================================================

    /**
     * Preenche {@code criadoEm} apenas se ainda não definido.
     * O guard evita sobrescrita quando o Builder já definiu o valor (ex: testes).
     */
    @PrePersist
    protected void aoInserir() {
        if (this.criadoEm == null) {
            this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}