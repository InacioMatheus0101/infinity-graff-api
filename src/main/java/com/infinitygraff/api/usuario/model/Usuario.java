package com.infinitygraff.api.usuario.model;

import com.infinitygraff.api.common.entity.BaseEntity;
import com.infinitygraff.api.usuario.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidade que representa um usuário da plataforma INFINITY GRAFF.
 *
 * <p><b>Soft Delete:</b> registros nunca são removidos fisicamente do banco.
 * A exclusão preenche {@code deletadoEm} com o timestamp atual, atualiza
 * {@code atualizadoEm} e desativa o usuário.
 *
 * <p><b>Segurança:</b> o campo {@code senhaHash} armazena exclusivamente
 * o hash BCrypt da senha. A senha em texto puro nunca é persistida.
 *
 * <p><b>Spring Security:</b> implementa {@link UserDetails} para integração
 * nativa com o mecanismo de autenticação. {@code getUsername()} retorna o
 * e-mail e {@code getPassword()} retorna o hash BCrypt.
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
public class Usuario extends BaseEntity implements UserDetails {

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "aceito_termos_em", nullable = false)
    private OffsetDateTime aceitoTermosEm;

    // =========================================================
    // Métodos de negócio
    // =========================================================

    /**
     * Realiza o soft delete: preenche {@code deletadoEm}, atualiza
     * {@code atualizadoEm} e desativa o acesso do usuário.
     */
    public void deletar() {
        this.marcarComoDeletado();
        this.ativo = false;
    }

    /**
     * Ativa o usuário, permitindo que ele realize login.
     */
    public void ativar() {
        this.ativo = true;
        this.marcarComoAtualizado();
    }

    /**
     * Desativa o usuário sem deletá-lo, impedindo o login.
     */
    public void desativar() {
        this.ativo = false;
        this.marcarComoAtualizado();
    }

    // =========================================================
    // UserDetails — Spring Security
    // =========================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getPassword() {
        return this.senhaHash;
    }

    /** Retorna o e-mail como identificador principal de autenticação. */
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Conta bloqueada se o usuário estiver inativo ou soft-deletado. */
    @Override
    public boolean isAccountNonLocked() {
        return this.ativo && !this.isDeletado();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Conta habilitada somente se ativa e não deletada. */
    @Override
    public boolean isEnabled() {
        return this.ativo && !this.isDeletado();
    }
}