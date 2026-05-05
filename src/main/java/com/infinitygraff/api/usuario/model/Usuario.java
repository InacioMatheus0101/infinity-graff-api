package com.infinitygraff.api.usuario.model;

import com.infinitygraff.api.common.entity.BaseEntity;
import com.infinitygraff.api.usuario.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade que representa o perfil interno de um usuário da plataforma INFINITY GRAFF.
 *
 * <p><b>Importante:</b> autenticação, cadastro, senha, sessão e refresh token
 * são responsabilidade do Supabase Auth.
 *
 * <p>Esta entidade não armazena senha e não implementa {@code UserDetails}.
 * Ela representa apenas os dados internos do marketplace:
 * nome, email, role, status, aceite de termos e controle de soft delete.
 *
 * <p><b>ID:</b> o campo {@code id} deve ser o mesmo UUID do usuário no Supabase Auth.
 *
 * <p><b>Soft Delete:</b> registros nunca são removidos fisicamente do banco.
 * A exclusão preenche {@code deletadoEm}, atualiza {@code atualizadoEm}
 * e desativa o usuário.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
@SQLRestriction("deletado_em IS NULL")
@SQLDelete(sql = "UPDATE usuarios SET deletado_em = NOW(), atualizado_em = NOW(), ativo = false WHERE id = ?")
public class Usuario extends BaseEntity {

    /**
     * Mesmo UUID do usuário no Supabase Auth.
     * O backend não gera esse ID.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "aceito_termos_em", nullable = false)
    private OffsetDateTime aceitoTermosEm;

    /**
     * Realiza o soft delete: preenche deletadoEm, atualiza atualizadoEm
     * e desativa o acesso interno do usuário.
     */
    public void deletar() {
        this.marcarComoDeletado();
        this.ativo = false;
    }

    /**
     * Ativa o usuário dentro do sistema interno.
     */
    public void ativar() {
        this.ativo = true;
        this.marcarComoAtualizado();
    }

    /**
     * Desativa o usuário dentro do sistema interno sem aplicar soft delete.
     */
    public void desativar() {
        this.ativo = false;
        this.marcarComoAtualizado();
    }

    /**
     * Indica se o usuário pode acessar as regras internas do marketplace.
     *
     * <p>O Supabase Auth pode autenticar o token, mas o backend ainda precisa
     * validar se o usuário interno está ativo e não deletado.
     */
    public boolean podeAcessarSistema() {
        return this.ativo && !this.isDeletado();
    }
}