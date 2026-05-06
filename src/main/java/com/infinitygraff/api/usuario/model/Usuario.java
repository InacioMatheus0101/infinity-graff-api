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
 * <p><b>Soft delete — padrão oficial de uso:</b>
 * o service deve chamar {@code usuario.deletar()} seguido de {@code usuarioRepository.save(usuario)}.
 * Esse é o caminho oficial da aplicação porque mantém a entidade em memória coerente
 * com o estado que será persistido no banco.
 *
 * <p>O {@link SQLDelete} existe apenas como proteção defensiva caso algum trecho do código
 * use {@code repository.delete(usuario)} por engano. Ele evita delete físico acidental,
 * mas não deve ser o caminho principal da regra de negócio.
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

    /**
     * E-mail normalizado do usuário.
     *
     * <p>A unicidade case-insensitive é garantida pela migration através do índice:
     * {@code idx_usuarios_email_lower ON usuarios (LOWER(email))}.
     *
     * <p>Não usamos {@code unique = true} aqui porque a constraint real no banco
     * é baseada em {@code LOWER(email)}, e não em uma constraint simples case-sensitive.
     */
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
     * Realiza o soft delete do usuário.
     *
     * <p>Caminho oficial de uso no service:
     * <pre>
     * usuario.deletar();
     * usuarioRepository.save(usuario);
     * </pre>
     *
     * <p>Não usar {@code usuarioRepository.delete(usuario)} como fluxo principal,
     * pois nesse caso o {@code @SQLDelete} atualiza o banco diretamente, mas a entidade
     * em memória não reflete imediatamente o valor de {@code deletadoEm}.
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
     * <p>O Supabase Auth autentica o token, mas o backend ainda precisa validar
     * se o usuário interno está ativo e não deletado.
     */
    public boolean podeAcessarSistema() {
        return this.ativo && !this.isDeletado();
    }
}