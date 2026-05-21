package com.infinitygraff.api.solicitacao.dto;

import com.infinitygraff.api.common.dto.UsuarioResumoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO resumido de solicitação de arte usado em listagens paginadas.
 */
@Getter
@AllArgsConstructor
public class SolicitacaoArteResumoResponse {

    private UUID id;
    private String titulo;
    private TipoSolicitacaoArte tipo;
    private StatusSolicitacaoArte status;
    private String medidas;
    private UsuarioResumoResponse clienteResumo;
    private UsuarioResumoResponse prestadorResumo;
    private OffsetDateTime criadoEm;
    private OffsetDateTime canceladoEm;

    /**
     * Converte uma solicitação de arte em resposta resumida.
     *
     * @param solicitacao solicitação a ser convertida
     * @return DTO resumido da solicitação
     */
    public static SolicitacaoArteResumoResponse from(SolicitacaoArte solicitacao) {
        return new SolicitacaoArteResumoResponse(
                solicitacao.getId(),
                solicitacao.getTitulo(),
                solicitacao.getTipo(),
                solicitacao.getStatus(),
                solicitacao.getMedidas(),
                UsuarioResumoResponse.from(solicitacao.getCliente()),
                UsuarioResumoResponse.from(solicitacao.getPrestador()),
                solicitacao.getCriadoEm(),
                solicitacao.getCanceladoEm()
        );
    }
}