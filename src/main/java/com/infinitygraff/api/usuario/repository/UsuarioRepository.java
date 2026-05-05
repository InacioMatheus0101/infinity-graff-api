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
 * Repositório de acesso à tabela usuarios.
 *
 * Todas as queries respeitam automaticamente o filtro de soft delete
 * configurado via @SQLRestriction na entidade Usuario.
 *
 * No modelo atual, usuarios.id é o mesmo UUID do usuário no Supabase Auth.
 * Esta tabela representa o perfil interno do usuário no marketplace,
 * não a autenticação por senha.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca o perfil interno pelo e-mail.
     *
     * Útil para validações administrativas, sincronização com Supabase Auth
     * e prevenção de duplicidade de perfil interno.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um perfil interno com o e-mail informado.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Verifica se existe ao menos um usuário interno com o role informado.
     *
     * Usado pelo AdminSeeder em ambiente de desenvolvimento.
     */
    boolean existsByRole(Role role);

    /**
     * Busca um usuário interno ativo pelo ID.
     *
     * Como usuarios.id é o mesmo UUID do Supabase Auth, este método é usado
     * após validar o JWT do Supabase e extrair o subject do token.
     */
    Optional<Usuario> findByIdAndAtivoTrue(UUID id);

    /**
     * Lista usuários com paginação, ordenação e filtros opcionais.
     *
     * @param role     filtra por perfil quando informado; retorna todos se null
     * @param ativo    filtra por status ativo quando informado; retorna todos se null
     * @param nome     busca parcial por nome quando informado; retorna todos se null ou vazio
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