package com.infinitygraff.api.chat.repository;

import com.infinitygraff.api.chat.model.MensagemChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para acesso às mensagens do chat.
 */
public interface MensagemChatRepository extends JpaRepository<MensagemChat, UUID> {

    /**
     * Lista mensagens não deletadas de uma solicitação, em ordem cronológica.
     */
    Page<MensagemChat> findBySolicitacaoIdAndDeletadaFalseOrderByCriadoEmAsc(
            UUID solicitacaoId, Pageable pageable);

    /**
     * Lista mensagens não deletadas com filtro de datas (opcional).
     */
    @Query("""
    SELECT m FROM MensagemChat m
    WHERE m.solicitacao.id = :solicitacaoId
      AND m.deletada = false
      AND (COALESCE(:inicio, m.criadoEm) = m.criadoEm OR m.criadoEm >= :inicio)
      AND (COALESCE(:fim, m.criadoEm) = m.criadoEm OR m.criadoEm <= :fim)
    ORDER BY m.criadoEm ASC
""")
Page<MensagemChat> listarComFiltroDeDatas(
        @Param("solicitacaoId") UUID solicitacaoId,
        @Param("inicio") OffsetDateTime inicio,
        @Param("fim") OffsetDateTime fim,
        Pageable pageable);
    /**
     * Busca uma mensagem não deletada específica, garantindo que pertence à solicitação informada.
     */
    Optional<MensagemChat> findByIdAndSolicitacaoIdAndDeletadaFalse(UUID id, UUID solicitacaoId);

    /**
     * Marca como lidas várias mensagens de uma só vez.
     * Apenas mensagens cujo destinatário é o usuário informado e que ainda não foram lidas são afetadas.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE MensagemChat m
        SET m.lida = true, m.lidaEm = :instanteLeitura
        WHERE m.id IN :ids
          AND m.solicitacao.id = :solicitacaoId
          AND m.destinatario.id = :destinatarioId
          AND m.lida = false
    """)
    int marcarComoLidas(
            @Param("ids") List<UUID> ids,
            @Param("solicitacaoId") UUID solicitacaoId,
            @Param("destinatarioId") UUID destinatarioId,
            @Param("instanteLeitura") OffsetDateTime instanteLeitura
    );
}