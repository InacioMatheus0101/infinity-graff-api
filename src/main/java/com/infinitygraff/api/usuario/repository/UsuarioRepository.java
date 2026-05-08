package com.infinitygraff.api.usuario.repository;

import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso à tabela {@code usuarios}.
 *
 * <p>Todas as queries respeitam automaticamente o filtro de soft delete
 * configurado via {@code @SQLRestriction} na entidade {@link Usuario}.
 *
 * <p>No modelo atual, {@code usuarios.id} é o mesmo UUID do usuário no Supabase Auth.
 * Esta tabela representa o perfil interno do usuário no marketplace,
 * não a autenticação por senha.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca o perfil interno pelo e-mail.
     *
     * <p>Útil para validações administrativas, sincronização com Supabase Auth
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
     * <p>Usado pelo AdminSeeder em ambiente de desenvolvimento.
     */
    boolean existsByRole(Role role);

    /**
     * Lista usuários com paginação, ordenação e filtros opcionais.
     *
     * <p>Uso principal: ADMIN, que pode visualizar todos os usuários não deletados.
     *
     * <p>Observação técnica: o filtro opcional com {@code :role IS NULL OR u.role = :role}
     * é suportado no Hibernate 6 usado pelo Spring Boot 3.x. Caso futuramente o projeto
     * migre de provider JPA ou adote queries nativas, este ponto deve ser reavaliado.
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
              AND (:nome IS NULL OR :nome = '' OR LOWER(u.nome) LIKE CONCAT('%', LOWER(:nome), '%'))
            """)
    Page<Usuario> listarComFiltro(
            @Param("role") Role role,
            @Param("ativo") Boolean ativo,
            @Param("nome") String nome,
            Pageable pageable
    );

    /**
     * Lista usuários restringindo os resultados a um conjunto de roles permitidas.
     *
     * <p>Uso principal: GERENTE, que pode visualizar apenas CLIENTE e PRESTADOR.
     *
     * <p>Este método protege contra coleção nula ou vazia antes de executar a query,
     * evitando SQL inválido como {@code IN ()}.
     *
     * @param rolesPermitidas conjunto de roles visíveis para a operação
     * @param role            filtro opcional por role específica
     * @param ativo           filtro opcional por status
     * @param nome            filtro opcional por nome
     * @param pageable        configuração de paginação e ordenação
     */
    default Page<Usuario> listarComFiltroPorRoles(
            Collection<Role> rolesPermitidas,
            Role role,
            Boolean ativo,
            String nome,
            Pageable pageable
    ) {
        if (rolesPermitidas == null || rolesPermitidas.isEmpty()) {
            return Page.empty(pageable);
        }

        return listarComFiltroPorRolesQuery(
                rolesPermitidas,
                role,
                ativo,
                nome,
                pageable
        );
    }

    /**
     * Query interna usada por {@link #listarComFiltroPorRoles(Collection, Role, Boolean, String, Pageable)}.
     *
     * <p>Não chamar diretamente nos services. Use o método default seguro para evitar
     * execução com {@code rolesPermitidas} vazia.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.role IN :rolesPermitidas
              AND (:role IS NULL OR u.role = :role)
              AND (:ativo IS NULL OR u.ativo = :ativo)
              AND (:nome IS NULL OR :nome = '' OR LOWER(u.nome) LIKE CONCAT('%', LOWER(:nome), '%'))
            """)
    Page<Usuario> listarComFiltroPorRolesQuery(
            @Param("rolesPermitidas") Collection<Role> rolesPermitidas,
            @Param("role") Role role,
            @Param("ativo") Boolean ativo,
            @Param("nome") String nome,
            Pageable pageable
    );
}