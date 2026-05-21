package com.infinitygraff.api.solicitacao.model;

import com.infinitygraff.api.common.entity.BaseEntity;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidade principal do fluxo operacional de solicitações de arte.
 *
 * <p>Representa um pedido de arte criado por um cliente ou pela equipe interna.
 * A solicitação nasce com status {@link StatusSolicitacaoArte#CRIADA}
 * e sem prestador atribuído.
 *
 * <p>O prestador será vinculado posteriormente por ADMIN ou GERENTE.
 *
 * <p>Cancelamento de negócio é diferente de soft delete:
 * <ul>
 *   <li>{@code canceladoEm} e {@code motivoCancelamento} representam cancelamento operacional;</li>
 *   <li>{@code deletadoEm}, herdado de {@link BaseEntity}, representa remoção lógica técnica.</li>
 * </ul>
 */
@Entity
@Table(name = "solicitacoes_arte")
@SQLRestriction("deletado_em IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SolicitacaoArte extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Cliente dono da solicitação.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    /**
     * Prestador responsável pela produção.
     *
     * <p>É nulo na criação e atribuído posteriormente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id")
    private Usuario prestador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoSolicitacaoArte tipo;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "instrucoes", nullable = false, columnDefinition = "TEXT")
    private String instrucoes;

    /**
     * Medidas informadas em formato livre.
     *
     * <p>Exemplos:
     * <ul>
     *   <li>30x40cm</li>
     *   <li>1080x1920px</li>
     *   <li>A4</li>
     * </ul>
     */
    @Column(name = "medidas", length = 100)
    private String medidas;

    /**
     * Campo interno de acompanhamento operacional.
     *
     * <p>Não deve ser exposto ao CLIENTE.
     */
    @Column(name = "observacoes_internas", columnDefinition = "TEXT")
    private String observacoesInternas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusSolicitacaoArte status;

    /**
     * Momento do cancelamento de negócio da solicitação.
     */
    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    /**
     * Motivo do cancelamento de negócio.
     */
    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    /**
     * Campos reservados para a etapa de aprovação administrativa.
     */
    @Column(name = "aprovada_admin_em")
    private OffsetDateTime aprovadaAdminEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovada_admin_por_id")
    private Usuario aprovadaAdminPor;

    /**
     * Campos reservados para a etapa de aceite final do cliente.
     */
    @Column(name = "aceita_cliente_em")
    private OffsetDateTime aceitaClienteEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aceita_cliente_por_id")
    private Usuario aceitaClientePor;

    /**
     * Cria uma nova solicitação de arte no estado inicial correto.
     */
    public static SolicitacaoArte criar(
            Usuario cliente,
            TipoSolicitacaoArte tipo,
            String titulo,
            String instrucoes,
            String medidas,
            String observacoesInternas
    ) {
        return SolicitacaoArte.builder()
                .cliente(cliente)
                .tipo(tipo)
                .titulo(titulo)
                .instrucoes(instrucoes)
                .medidas(medidas)
                .observacoesInternas(observacoesInternas)
                .status(StatusSolicitacaoArte.CRIADA)
                .build();
    }

    /**
     * Atribui o prestador responsável à solicitação.
     *
     * <p>A validação de autorização e status permitido ficará no service.
     * A entidade apenas aplica a mudança de estado já autorizada.
     */
    public void atribuirPrestador(Usuario prestador) {
        this.prestador = prestador;
        this.status = StatusSolicitacaoArte.EM_PRODUCAO;
        marcarComoAtualizado();
    }

    /**
     * Cancela a solicitação por regra de negócio.
     *
     * <p>A validação de autorização e do status permitido ficará no service.
     *
     * @param motivo motivo obrigatório do cancelamento
     * @param canceladoEm instante do cancelamento em UTC;
     *                    o service deve informar {@code OffsetDateTime.now(ZoneOffset.UTC)}
     */
    public void cancelar(String motivo, OffsetDateTime canceladoEm) {
        this.status = StatusSolicitacaoArte.CANCELADA;
        this.motivoCancelamento = motivo;
        this.canceladoEm = canceladoEm;
        marcarComoAtualizado();
    }

    /**
     * Indica se a solicitação já possui prestador atribuído.
     */
    public boolean possuiPrestador() {
        return this.prestador != null;
    }

    /**
     * Indica se a solicitação está cancelada.
     */
    public boolean isCancelada() {
        return this.status == StatusSolicitacaoArte.CANCELADA;
    }
}