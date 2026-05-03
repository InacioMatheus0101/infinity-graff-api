package com.infinitygraff.api.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Entidade base com campos compartilhados pelas entidades principais.
 *
 * <p>Gerencia automaticamente:
 * <ul>
 *   <li>{@code id} — UUID gerado pela aplicação via Hibernate</li>
 *   <li>{@code criadoEm} — preenchido uma única vez no {@code @PrePersist}</li>
 *   <li>{@code atualizadoEm} — atualizado no {@code @PrePersist} e no {@code @PreUpdate}</li>
 *   <li>{@code deletadoEm} — preenchido quando ocorrer soft delete</li>
 * </ul>
 *
 * <p>Todos os timestamps são armazenados em UTC usando {@link OffsetDateTime}.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "deletado_em")
    private OffsetDateTime deletadoEm;

    @PrePersist
    protected void aoInserir() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    protected void marcarComoDeletado() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        this.deletadoEm = agora;
        this.atualizadoEm = agora;
    }

    protected void marcarComoAtualizado() {
        this.atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public boolean isDeletado() {
        return this.deletadoEm != null;
    }
}