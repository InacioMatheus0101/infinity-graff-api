package com.infinitygraff.api.solicitacao.repository;

import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso às solicitações de arte.
 *
 * <p>As consultas respeitam automaticamente o filtro de soft delete
 * configurado via {@code @SQLRestriction} na entidade {@link SolicitacaoArte}.
 *
 * <p>Esta camada oferece queries específicas para:
 * <ul>
 *   <li>ADMIN e GERENTE — visão global das solicitações;</li>
 *   <li>CLIENTE — apenas solicitações próprias;</li>
 *   <li>PRESTADOR — apenas solicitações atribuídas a ele.</li>
 * </ul>
 */
public interface SolicitacaoArteRepository extends JpaRepository<SolicitacaoArte, UUID> {

    /**
     * Lista solicitações para ADMIN e GERENTE, com filtros opcionais.
     *
     * @param status       status da solicitação, quando informado
     * @param tipo         tipo da solicitação, quando informado
     * @param clienteId    cliente da solicitação, quando informado
     * @param prestadorId  prestador atribuído, quando informado
     * @param semPrestador quando {@code true}, retorna apenas solicitações sem prestador;
     *                     quando {@code false}, retorna apenas solicitações com prestador;
     *                     quando {@code null}, não aplica esse filtro
     * @param pageable     paginação e ordenação
     * @return página de solicitações compatíveis com os filtros
     */
    @Query("""
            SELECT s FROM SolicitacaoArte s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:tipo IS NULL OR s.tipo = :tipo)
              AND (:clienteId IS NULL OR s.cliente.id = :clienteId)
              AND (:prestadorId IS NULL OR s.prestador.id = :prestadorId)
              AND (
                    :semPrestador IS NULL
                    OR (:semPrestador = true AND s.prestador IS NULL)
                    OR (:semPrestador = false AND s.prestador IS NOT NULL)
                  )
            """)
    Page<SolicitacaoArte> listarComFiltroAdministrativo(
            @Param("status") StatusSolicitacaoArte status,
            @Param("tipo") TipoSolicitacaoArte tipo,
            @Param("clienteId") UUID clienteId,
            @Param("prestadorId") UUID prestadorId,
            @Param("semPrestador") Boolean semPrestador,
            Pageable pageable
    );

    /**
     * Lista apenas as solicitações pertencentes a um cliente específico.
     *
     * @param clienteId cliente autenticado
     * @param status    status da solicitação, quando informado
     * @param tipo      tipo da solicitação, quando informado
     * @param pageable  paginação e ordenação
     * @return página de solicitações do cliente
     */
    @Query("""
            SELECT s FROM SolicitacaoArte s
            WHERE s.cliente.id = :clienteId
              AND (:status IS NULL OR s.status = :status)
              AND (:tipo IS NULL OR s.tipo = :tipo)
            """)
    Page<SolicitacaoArte> listarPorClienteComFiltro(
            @Param("clienteId") UUID clienteId,
            @Param("status") StatusSolicitacaoArte status,
            @Param("tipo") TipoSolicitacaoArte tipo,
            Pageable pageable
    );

    /**
     * Lista apenas as solicitações atribuídas a um prestador específico.
     *
     * @param prestadorId prestador autenticado
     * @param status       status da solicitação, quando informado
     * @param tipo         tipo da solicitação, quando informado
     * @param pageable     paginação e ordenação
     * @return página de solicitações atribuídas ao prestador
     */
    @Query("""
            SELECT s FROM SolicitacaoArte s
            WHERE s.prestador.id = :prestadorId
              AND (:status IS NULL OR s.status = :status)
              AND (:tipo IS NULL OR s.tipo = :tipo)
            """)
    Page<SolicitacaoArte> listarPorPrestadorComFiltro(
            @Param("prestadorId") UUID prestadorId,
            @Param("status") StatusSolicitacaoArte status,
            @Param("tipo") TipoSolicitacaoArte tipo,
            Pageable pageable
    );

    /**
     * Busca uma solicitação específica pertencente a um cliente.
     *
     * <p>Usado para consultas de detalhe e histórico quando o usuário autenticado
     * possui role CLIENTE, evitando buscar uma solicitação fora do seu escopo.
     *
     * @param id identificador da solicitação
     * @param clienteId identificador do cliente autenticado
     * @return solicitação quando pertence ao cliente informado
     */
    Optional<SolicitacaoArte> findByIdAndCliente_Id(UUID id, UUID clienteId);

    /**
     * Busca uma solicitação específica atribuída a um prestador.
     *
     * <p>Usado para consultas de detalhe e histórico quando o usuário autenticado
     * possui role PRESTADOR, evitando buscar uma solicitação fora do seu escopo.
     *
     * @param id identificador da solicitação
     * @param prestadorId identificador do prestador autenticado
     * @return solicitação quando está atribuída ao prestador informado
     */
    Optional<SolicitacaoArte> findByIdAndPrestador_Id(UUID id, UUID prestadorId);
}