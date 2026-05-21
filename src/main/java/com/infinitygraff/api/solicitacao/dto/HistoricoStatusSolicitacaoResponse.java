package com.infinitygraff.api.solicitacao.dto;

import com.infinitygraff.api.common.dto.UsuarioResumoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.HistoricoStatusSolicitacao;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de saída para um registro de histórico de status da solicitação.
 */
@Getter
@AllArgsConstructor
public class HistoricoStatusSolicitacaoResponse {

    private UUID id;
    private StatusSolicitacaoArte statusAnterior;
    private StatusSolicitacaoArte statusNovo;
    private UsuarioResumoResponse alteradoPorResumo;
    private String motivo;
    private OffsetDateTime criadoEm;

    /**
     * Converte um registro de histórico em resposta.
     *
     * @param historico histórico a ser convertido
     * @return DTO de histórico de status
     */
    public static HistoricoStatusSolicitacaoResponse from(
            HistoricoStatusSolicitacao historico
    ) {
        return new HistoricoStatusSolicitacaoResponse(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                UsuarioResumoResponse.from(historico.getAlteradoPor()),
                historico.getMotivo(),
                historico.getCriadoEm()
        );
    }
}