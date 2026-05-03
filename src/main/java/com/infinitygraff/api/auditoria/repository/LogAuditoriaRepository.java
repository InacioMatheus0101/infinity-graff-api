package com.infinitygraff.api.auditoria.repository;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Repositório de acesso à tabela {@code logs_auditoria}.
 *
 * <p>Apenas operações de leitura e inserção devem ser usadas.
 * Logs são imutáveis por design: não criar métodos de update ou delete.
 */
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    /**
     * Lista logs com filtros opcionais combinados.
     * Usado pelo endpoint {@code GET /api/v1/auditoria/logs} (ADMIN).
     *
     * <p>Todos os parâmetros são opcionais — quando {@code null} ou vazio,
     * o filtro é ignorado.
     *
     * @param usuarioId filtra por usuário específico
     * @param acao      filtra por tipo de ação, exemplo: "LOGIN_FALHA"
     * @param entidade  filtra por entidade afetada, exemplo: "usuarios"
     * @param inicio    filtra registros criados a partir desta data, inclusive
     * @param fim       filtra registros criados até esta data, inclusive
     * @param pageable  configuração de paginação e ordenação
     */
    @Query("""
            SELECT l FROM LogAuditoria l
            WHERE (:usuarioId IS NULL OR l.usuarioId = :usuarioId)
              AND (:acao IS NULL OR :acao = '' OR l.acao = :acao)
              AND (:entidade IS NULL OR :entidade = '' OR l.entidade = :entidade)
              AND (:inicio IS NULL OR l.criadoEm >= :inicio)
              AND (:fim IS NULL OR l.criadoEm <= :fim)
            """)
    Page<LogAuditoria> listarComFiltros(
            @Param("usuarioId") UUID usuarioId,
            @Param("acao") String acao,
            @Param("entidade") String entidade,
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            Pageable pageable
    );
}