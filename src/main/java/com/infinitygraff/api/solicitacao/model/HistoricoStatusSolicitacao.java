package com.infinitygraff.api.solicitacao.model;

import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.usuario.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Registro imutável de transição de status de uma solicitação de arte.
 *
 * <p>Esta entidade representa a trilha cronológica de mudanças de estado
 * da {@link SolicitacaoArte}.
 *
 * <p>Exemplos:
 * <ul>
 *   <li>{@code null -> CRIADA}, na criação da solicitação;</li>
 *   <li>{@code CRIADA -> EM_PRODUCAO}, na atribuição de prestador;</li>
 *   <li>{@code EM_PRODUCAO -> CANCELADA}, no cancelamento.</li>
 * </ul>
 *
 * <p>Esta tabela não possui soft delete nem atualização funcional.
 * Cada mudança de status gera um novo registro.
 */
@Entity
@Table(name = "historico_status_solicitacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HistoricoStatusSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Solicitação vinculada ao histórico.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoArte solicitacao;

    /**
     * Status anterior da solicitação.
     *
     * <p>É {@code null} apenas no registro inicial de criação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 50)
    private StatusSolicitacaoArte statusAnterior;

    /**
     * Novo status assumido pela solicitação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 50)
    private StatusSolicitacaoArte statusNovo;

    /**
     * Usuário responsável pela ação que provocou a transição.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alterado_por_id", nullable = false)
    private Usuario alteradoPor;

    /**
     * Motivo da transição quando exigido pela regra de negócio.
     *
     * <p>Na Etapa 1, será obrigatório em cancelamentos.
     * Futuramente também será utilizado em solicitações de refação.
     */
    @Column(name = "motivo", length = 500)
    private String motivo;

    /**
     * Momento imutável de criação do registro de histórico.
     */
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /**
     * Define o instante do histórico em UTC antes da persistência.
     */
    @PrePersist
    protected void aoInserir() {
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Cria o registro inicial da solicitação:
     * {@code null -> CRIADA}.
     *
     * @param solicitacao solicitação recém-criada
     * @param criadoPor usuário que criou a solicitação
     * @return histórico inicial de status
     */
    public static HistoricoStatusSolicitacao registrarCriacao(
            SolicitacaoArte solicitacao,
            Usuario criadoPor
    ) {
        return HistoricoStatusSolicitacao.builder()
                .solicitacao(solicitacao)
                .statusAnterior(null)
                .statusNovo(StatusSolicitacaoArte.CRIADA)
                .alteradoPor(criadoPor)
                .motivo(null)
                .build();
    }

    /**
     * Cria um registro de transição entre dois status.
     *
     * @param solicitacao solicitação alterada
     * @param statusAnterior status antes da mudança
     * @param statusNovo novo status assumido
     * @param alteradoPor usuário responsável pela ação
     * @param motivo motivo da transição, quando exigido pela regra de negócio
     * @return novo registro de histórico
     */
    public static HistoricoStatusSolicitacao registrarTransicao(
            SolicitacaoArte solicitacao,
            StatusSolicitacaoArte statusAnterior,
            StatusSolicitacaoArte statusNovo,
            Usuario alteradoPor,
            String motivo
    ) {
        return HistoricoStatusSolicitacao.builder()
                .solicitacao(solicitacao)
                .statusAnterior(statusAnterior)
                .statusNovo(statusNovo)
                .alteradoPor(alteradoPor)
                .motivo(motivo)
                .build();
    }
}