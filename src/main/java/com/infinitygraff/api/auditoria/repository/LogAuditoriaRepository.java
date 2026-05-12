package com.infinitygraff.api.auditoria.repository;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Repositório de acesso à tabela {@code logs_auditoria}.
 *
 * <p>A aplicação deve usar este repository apenas para inserção e leitura.
 * Logs são imutáveis por design: não criar métodos customizados de update ou delete.
 *
 * <p>Observação: {@link JpaRepository} ainda expõe métodos como {@code delete}.
 * Esses métodos não devem ser usados para auditoria. A imutabilidade é regra
 * da aplicação nesta fase.
 */
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    /**
     * Lista logs com filtros opcionais combinados.
     *
     * <p>Usado pelo endpoint {@code GET /api/v1/auditoria/logs}, acessível apenas por ADMIN E gerentes. 
     *
     * <p>Todos os parâmetros são opcionais. Quando {@code null} ou vazio,
     * o filtro é ignorado.
     *
     * <p>Ordenação padrão e única da Fase 1: logs mais recentes primeiro.
     *
     * <p>Este método remove qualquer {@link Sort} recebido no {@link Pageable}
     * antes de chamar a query, porque a ordenação oficial já está definida no JPQL
     * com {@code ORDER BY l.criadoEm DESC}. Isso evita conflito ou duplicidade
     * de ordenação entre JPQL e Pageable.
     *
     * @param usuarioId filtra por usuário específico
     * @param acao      filtra por tipo de ação, exemplo: {@code USUARIO_DELETADO}
     * @param entidade  filtra por entidade afetada, exemplo: {@code usuarios}
     * @param inicio    filtra registros criados a partir desta data, inclusive
     * @param fim       filtra registros criados até esta data, inclusive
     * @param pageable  configuração de paginação; qualquer Sort será ignorado
     */
    default Page<LogAuditoria> listarComFiltros(
            UUID usuarioId,
            String acao,
            String entidade,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            Pageable pageable
    ) {
        Pageable pageableSemSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.unsorted()
        );

        return listarComFiltrosQuery(
                usuarioId,
                acao,
                entidade,
                inicio,
                fim,
                pageableSemSort
        );
    }

    /**
     * Query interna de listagem de logs.
     *
     * <p>Não chamar diretamente no service. Use
     * {@link #listarComFiltros(UUID, String, String, OffsetDateTime, OffsetDateTime, Pageable)}
     * para garantir que o {@link Pageable} chegue sem {@link Sort}.
     *
     * <p>Os filtros de data usam {@code COALESCE} para evitar erro de inferência
     * de tipo no PostgreSQL quando {@code inicio} ou {@code fim} forem {@code null}.
     */
    @Query("""
            SELECT l FROM LogAuditoria l
            WHERE (:usuarioId IS NULL OR l.usuarioId = :usuarioId)
              AND (:acao IS NULL OR :acao = '' OR l.acao = UPPER(:acao))
              AND (:entidade IS NULL OR :entidade = '' OR l.entidade = LOWER(:entidade))
              AND l.criadoEm >= COALESCE(:inicio, l.criadoEm)
              AND l.criadoEm <= COALESCE(:fim, l.criadoEm)
            ORDER BY l.criadoEm DESC
            """)
    Page<LogAuditoria> listarComFiltrosQuery(
            @Param("usuarioId") UUID usuarioId,
            @Param("acao") String acao,
            @Param("entidade") String entidade,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            Pageable pageable
    );
}