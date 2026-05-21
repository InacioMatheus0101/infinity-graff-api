package com.infinitygraff.api.solicitacao.repository;

import com.infinitygraff.api.solicitacao.model.HistoricoStatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório de acesso ao histórico imutável de status das solicitações.
 *
 * <p>Os registros são gravados a cada transição de status e consultados
 * em ordem cronológica crescente para composição da timeline da solicitação.
 */
public interface HistoricoStatusSolicitacaoRepository
        extends JpaRepository<HistoricoStatusSolicitacao, UUID> {

    /**
     * Lista o histórico de uma solicitação em ordem cronológica crescente.
     *
     * @param solicitacaoId identificador da solicitação
     * @return histórico do mais antigo para o mais recente
     */
    List<HistoricoStatusSolicitacao> findBySolicitacaoIdOrderByCriadoEmAsc(UUID solicitacaoId);
}