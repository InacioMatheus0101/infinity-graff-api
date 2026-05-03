package com.infinitygraff.api.usuario.repository;

import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso à tabela {@code usuarios}.
 *
 * <p>Todas as queries respeitam automaticamente o filtro de soft delete
 * ({@code deletado_em IS NULL}) configurado via {@code @SQLRestriction}
 * na entidade {@link Usuario}. Não é necessário adicionar este filtro
 * manualmente nas queries derivadas ou JPQL.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca um usuário pelo e-mail para autenticação.
     * Retorna {@code Optional.empty()} se não encontrado ou se deletado.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um usuário com o e-mail informado.
     * Usado na validação de registro para evitar duplicatas.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Verifica se existe ao menos um usuário com o role informado.
     * Usado pelo {@code AdminSeeder} para garantir idempotência.
     */
    boolean existsByRole(Role role);

    /**
     * Lista usuários com paginação, ordenação e filtros opcionais.
     * Usado pelo endpoint {@code GET /api/v1/usuarios} (ADMIN e GERENTE).
     *
     * @param role     filtra por perfil quando informado; retorna todos se {@code null}
     * @param ativo    filtra por status ativo quando informado; retorna todos se {@code null}
     * @param nome     busca parcial por nome quando informado; retorna todos se {@code null} ou vazio
     * @param pageable configuração de paginação e ordenação
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:ativo IS NULL OR u.ativo = :ativo)
              AND (:nome IS NULL OR :nome = '' OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
            """)
    Page<Usuario> listarComFiltro(
            @Param("role") Role role,
            @Param("ativo") Boolean ativo,
            @Param("nome") String nome,
            Pageable pageable
    );
}