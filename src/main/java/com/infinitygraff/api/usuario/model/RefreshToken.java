package com.infinitygraff.api.usuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Entidade que representa um refresh token persistido no banco.
 *
 * <p><b>Segurança — hash obrigatório:</b> apenas o hash SHA-256 do token
 * é armazenado na coluna {@code token_hash}. O token puro (plain text)
 * gerado pelo {@code SecureRandom} nunca é persistido.
 *
 * <p><b>Rotação:</b> a cada uso do {@code /auth/refresh}, o token atual
 * é revogado (com {@code revogadoEm} preenchido) e um novo par
 * (access token + refresh token) é gerado.
 *
 * <p>Não estende {@link com.infinitygraff.api.common.entity.BaseEntity}
 * pois não possui {@code atualizadoEm} nem soft delete próprio.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    /**
     * Hash SHA-256 do token puro em formato hexadecimal (64 caracteres).
     * Nunca armazenar o token puro neste campo.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private OffsetDateTime expiraEm;

    @Builder.Default
    @Column(name = "revogado", nullable = false)
    private boolean revogado = false;

    /** Preenchido no momento da revogação. Null enquanto o token está ativo. */
    @Column(name = "revogado_em")
    private OffsetDateTime revogadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /** IP do request que originou o token. */
    @Column(name = "ip_criacao", length = 100)
    private String ipCriacao;

    /** User-Agent do request que originou o token. */
    @Column(name = "user_agent_criacao", columnDefinition = "TEXT")
    private String userAgentCriacao;

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

    // =========================================================
    // Métodos de negócio
    // =========================================================

    /**
     * Revoga o token preenchendo {@code revogadoEm} com o instante atual em UTC.
     * Chamado no logout e na rotação do /refresh.
     */
    public void revogar() {
        this.revogado = true;
        this.revogadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Retorna {@code true} se o token está expirado pelo relógio UTC atual.
     */
    public boolean isExpirado() {
        return OffsetDateTime.now(ZoneOffset.UTC).isAfter(this.expiraEm);
    }

    /**
     * Retorna {@code true} se o token está válido para uso:
     * não revogado e não expirado.
     */
    public boolean isValido() {
        return !this.revogado && !isExpirado();
    }
}