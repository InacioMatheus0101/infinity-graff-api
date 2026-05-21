package com.infinitygraff.api.solicitacao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.infinitygraff.api.common.dto.UsuarioResumoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO detalhado de solicitação de arte.
 *
 * <p>O campo {@code observacoesInternas} deve ser preenchido apenas
 * quando o usuário autenticado tiver permissão de visualização:
 * ADMIN, GERENTE ou PRESTADOR vinculado à solicitação.
 *
 * <p>Para CLIENTE, o service deve informar {@code null},
 * fazendo com que apenas este campo seja omitido da serialização.
 */
@Getter
@AllArgsConstructor
public class SolicitacaoArteDetalheResponse {

    private UUID id;
    private String titulo;
    private TipoSolicitacaoArte tipo;
    private StatusSolicitacaoArte status;
    private String instrucoes;
    private String medidas;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String observacoesInternas;

    private UsuarioResumoResponse clienteResumo;
    private UsuarioResumoResponse prestadorResumo;
    private String motivoCancelamento;
    private OffsetDateTime canceladoEm;
    private OffsetDateTime aprovadaAdminEm;
    private UsuarioResumoResponse aprovadaAdminPorResumo;
    private OffsetDateTime aceitaClienteEm;
    private UsuarioResumoResponse aceitaClientePorResumo;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;

    /**
     * Converte uma solicitação em resposta detalhada,
     * incluindo as observações internas.
     *
     * <p>Deve ser usado apenas para ADMIN, GERENTE
     * ou PRESTADOR vinculado à solicitação.
     *
     * @param solicitacao solicitação a ser convertida
     * @return DTO detalhado com observações internas
     */
    public static SolicitacaoArteDetalheResponse fromComObservacoesInternas(
            SolicitacaoArte solicitacao
    ) {
        return montar(solicitacao, solicitacao.getObservacoesInternas());
    }

    /**
     * Converte uma solicitação em resposta detalhada,
     * omitindo as observações internas.
     *
     * <p>Deve ser usado para CLIENTE.
     *
     * @param solicitacao solicitação a ser convertida
     * @return DTO detalhado sem observações internas
     */
    public static SolicitacaoArteDetalheResponse fromSemObservacoesInternas(
            SolicitacaoArte solicitacao
    ) {
        return montar(solicitacao, null);
    }

    private static SolicitacaoArteDetalheResponse montar(
            SolicitacaoArte solicitacao,
            String observacoesInternas
    ) {
        return new SolicitacaoArteDetalheResponse(
                solicitacao.getId(),
                solicitacao.getTitulo(),
                solicitacao.getTipo(),
                solicitacao.getStatus(),
                solicitacao.getInstrucoes(),
                solicitacao.getMedidas(),
                observacoesInternas,
                UsuarioResumoResponse.from(solicitacao.getCliente()),
                UsuarioResumoResponse.from(solicitacao.getPrestador()),
                solicitacao.getMotivoCancelamento(),
                solicitacao.getCanceladoEm(),
                solicitacao.getAprovadaAdminEm(),
                UsuarioResumoResponse.from(solicitacao.getAprovadaAdminPor()),
                solicitacao.getAceitaClienteEm(),
                UsuarioResumoResponse.from(solicitacao.getAceitaClientePor()),
                solicitacao.getCriadoEm(),
                solicitacao.getAtualizadoEm()
        );
    }
}