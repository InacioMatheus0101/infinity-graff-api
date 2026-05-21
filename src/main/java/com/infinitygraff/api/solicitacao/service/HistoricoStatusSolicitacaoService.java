package com.infinitygraff.api.solicitacao.service;

import com.infinitygraff.api.solicitacao.dto.HistoricoStatusSolicitacaoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.HistoricoStatusSolicitacao;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import com.infinitygraff.api.solicitacao.repository.HistoricoStatusSolicitacaoRepository;
import com.infinitygraff.api.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço responsável por registrar e consultar o histórico imutável
 * de status das solicitações de arte.
 *
 * <p>Este serviço não decide regras de negócio da solicitação.
 * Ele apenas registra transições já validadas pelo serviço principal
 * {@link SolicitacaoArteService}.
 */
@Service
@RequiredArgsConstructor
public class HistoricoStatusSolicitacaoService {

    private final HistoricoStatusSolicitacaoRepository historicoRepository;

    /**
     * Registra o histórico inicial de criação da solicitação.
     *
     * <p>A transição registrada é:
     * {@code null -> CRIADA}.
     *
     * @param solicitacao solicitação recém-criada
     * @param criadoPor usuário responsável pela criação
     */
    @Transactional
    public void registrarCriacao(SolicitacaoArte solicitacao, Usuario criadoPor) {
        validarSolicitacaoEUsuario(solicitacao, criadoPor);

        HistoricoStatusSolicitacao historico =
                HistoricoStatusSolicitacao.registrarCriacao(solicitacao, criadoPor);

        historicoRepository.save(historico);
    }

    /**
     * Registra uma transição de status da solicitação.
     *
     * <p>Na Etapa 1, o motivo é obrigatório em cancelamentos.
     * Futuramente, também será obrigatório em refações.
     *
     * @param solicitacao solicitação alterada
     * @param statusAnterior status anterior da solicitação
     * @param statusNovo novo status da solicitação
     * @param alteradoPor usuário responsável pela alteração
     * @param motivo motivo da transição, quando exigido
     */
    @Transactional
    public void registrarTransicao(
            SolicitacaoArte solicitacao,
            StatusSolicitacaoArte statusAnterior,
            StatusSolicitacaoArte statusNovo,
            Usuario alteradoPor,
            String motivo
    ) {
        validarSolicitacaoEUsuario(solicitacao, alteradoPor);
        validarTransicao(statusAnterior, statusNovo);

        String motivoNormalizado = normalizarTextoOpcional(motivo);

        if (statusNovo == StatusSolicitacaoArte.CANCELADA && motivoNormalizado == null) {
            throw new IllegalArgumentException(
                    "Motivo é obrigatório para registrar cancelamento no histórico."
            );
        }

        HistoricoStatusSolicitacao historico =
                HistoricoStatusSolicitacao.registrarTransicao(
                        solicitacao,
                        statusAnterior,
                        statusNovo,
                        alteradoPor,
                        motivoNormalizado
                );

        historicoRepository.save(historico);
    }

    /**
     * Lista o histórico de uma solicitação em ordem cronológica crescente.
     *
     * <p>A autorização de acesso à solicitação deve ser validada antes
     * pelo {@link SolicitacaoArteService}.
     *
     * @param solicitacao solicitação cujo histórico será consultado
     * @return lista cronológica de registros de histórico
     */
    @Transactional(readOnly = true)
    public List<HistoricoStatusSolicitacaoResponse> listarPorSolicitacao(
            SolicitacaoArte solicitacao
    ) {
        if (solicitacao == null || solicitacao.getId() == null) {
            throw new IllegalArgumentException("Solicitação é obrigatória para consultar histórico.");
        }

        return historicoRepository
                .findBySolicitacaoIdOrderByCriadoEmAsc(solicitacao.getId())
                .stream()
                .map(HistoricoStatusSolicitacaoResponse::from)
                .toList();
    }

    private void validarSolicitacaoEUsuario(SolicitacaoArte solicitacao, Usuario usuario) {
        if (solicitacao == null || solicitacao.getId() == null) {
            throw new IllegalArgumentException("Solicitação é obrigatória para registrar histórico.");
        }

        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException("Usuário responsável é obrigatório para registrar histórico.");
        }
    }

    private void validarTransicao(
            StatusSolicitacaoArte statusAnterior,
            StatusSolicitacaoArte statusNovo
    ) {
        if (statusNovo == null) {
            throw new IllegalArgumentException("Status novo é obrigatório para registrar histórico.");
        }

        if (statusAnterior != null && statusAnterior == statusNovo) {
            throw new IllegalArgumentException(
                    "Status anterior e status novo não podem ser iguais no histórico."
            );
        }
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}