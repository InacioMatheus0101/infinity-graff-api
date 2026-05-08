package com.infinitygraff.api.usuario.model;

import com.infinitygraff.api.common.entity.BaseEntity;
import com.infinitygraff.api.usuario.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que representa o perfil interno de um usuário da plataforma INFINITY GRAFF.
 *
 * <p><b>Importante:</b> autenticação, cadastro, senha, sessão e refresh token
 * são responsabilidade do Supabase Auth.
 *
 * <p>Esta entidade não armazena senha e não implementa {@code UserDetails}.
 * Ela representa apenas os dados internos do marketplace:
 * nome, email, role, status, foto de perfil, aceite de termos e controle de soft delete.
 *
 * <p><b>ID:</b> o campo {@code id} deve ser o mesmo UUID do usuário no Supabase Auth.
 *
 * <p><b>Foto de perfil:</b> a imagem real não é armazenada no banco.
 * O campo {@code fotoPerfilUrl} guarda apenas a URL ou path da imagem em storage externo.
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "usuarios")
@SQLRestriction("deletado_em IS NULL")
@SQLDelete(sql = "UPDATE usuarios SET deletado_em = NOW(), atualizado_em = NOW(), ativo = false WHERE id = ?")
public class Usuario extends BaseEntity {

    /**
     * Mesmo UUID do usuário no Supabase Auth.
     * O backend não gera nem altera esse ID.
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

    /**
     * URL ou path da foto de perfil do usuário.
     *
     * <p>Campo opcional. A imagem real deve ficar em storage externo,
     * como Supabase Storage, S3, Cloudflare R2 ou serviço equivalente.
     *
     * <p>Não salvar imagem em Base64, {@code byte[]} ou arquivo binário nesta entidade.
     */
    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "aceito_termos_em", nullable = false)
    private OffsetDateTime aceitoTermosEm;

    /**
     * Construtor controlado usado pelo Builder.
     *
     * <p>Não expõe campos de auditoria herdados da {@code BaseEntity}.
     * Esses campos são controlados automaticamente pelo ciclo de vida JPA.
     */
    @Builder
    private Usuario(
            UUID id,
            String nome,
            String email,
            String fotoPerfilUrl,
            Role role,
            Boolean ativo,
            OffsetDateTime aceitoTermosEm
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.nome = normalizarTextoObrigatorio(nome, "nome");
        this.email = normalizarEmailObrigatorio(email);
        this.fotoPerfilUrl = normalizarTextoOpcional(fotoPerfilUrl);
        this.role = Objects.requireNonNull(role, "role não pode ser nula");
        this.ativo = ativo == null || ativo;
        this.aceitoTermosEm = Objects.requireNonNull(
                aceitoTermosEm,
                "aceitoTermosEm não pode ser nulo"
        );
    }

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
        if (this.isDeletado()) {
            return;
        }

        this.marcarComoDeletado();
        this.ativo = false;
    }

    /**
     * Ativa o usuário dentro do sistema interno.
     *
     * <p>Usuários com soft delete não podem ser ativados por este método.
     * Uma eventual restauração deve ser uma regra própria e explícita.
     */
    public void ativar() {
        validarNaoDeletado("Usuário deletado não pode ser ativado");

        this.ativo = true;
        this.marcarComoAtualizado();
    }

    /**
     * Desativa o usuário dentro do sistema interno sem aplicar soft delete.
     */
    public void desativar() {
        validarNaoDeletado("Usuário deletado não pode ser desativado");

        this.ativo = false;
        this.marcarComoAtualizado();
    }

    /**
     * Atualiza a URL/path da foto de perfil.
     *
     * <p>Não realiza upload da imagem. Apenas atualiza a referência da imagem
     * que deve estar armazenada em serviço externo.
     */
    public void atualizarFotoPerfilUrl(String fotoPerfilUrl) {
        validarNaoDeletado("Usuário deletado não pode ter foto de perfil alterada");

        this.fotoPerfilUrl = normalizarTextoOpcional(fotoPerfilUrl);
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

    @PrePersist
    @PreUpdate
    private void normalizarCamposAntesDeSalvar() {
        this.nome = normalizarTextoObrigatorio(this.nome, "nome");
        this.email = normalizarEmailObrigatorio(this.email);
        this.fotoPerfilUrl = normalizarTextoOpcional(this.fotoPerfilUrl);
    }

    private void validarNaoDeletado(String mensagem) {
        if (this.isDeletado()) {
            throw new IllegalStateException(mensagem);
        }
    }

    private static String normalizarEmailObrigatorio(String email) {
    return normalizarTextoObrigatorio(email, "email").toLowerCase(Locale.ROOT);
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

    @Override
    public final boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }

        if (outro == null) {
            return false;
        }

        if (Hibernate.getClass(this) != Hibernate.getClass(outro)) {
            return false;
        }

        Usuario usuario = (Usuario) outro;

        return this.id != null && Objects.equals(this.id, usuario.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}