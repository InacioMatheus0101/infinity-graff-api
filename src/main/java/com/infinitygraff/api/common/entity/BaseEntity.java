package com.infinitygraff.api.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Entidade base com campos de auditoria compartilhados pelas entidades principais.
 *
 * <p>Gerencia automaticamente:
 * <ul>
 *   <li>{@code criadoEm} — preenchido uma única vez no {@code @PrePersist}</li>
 *   <li>{@code atualizadoEm} — atualizado no {@code @PrePersist} e no {@code @PreUpdate}</li>
 *   <li>{@code deletadoEm} — preenchido quando ocorrer soft delete</li>
 * </ul>
 *
 * <p>Todos os timestamps são armazenados em UTC usando {@link OffsetDateTime}.
 *
 * <p>Importante: esta classe não define o campo {@code id}.
 * Cada entidade deve declarar seu próprio identificador conforme sua regra.
 * No caso de {@code Usuario}, o ID será o mesmo UUID do Supabase Auth.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

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