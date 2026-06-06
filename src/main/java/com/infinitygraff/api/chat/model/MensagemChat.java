package com.infinitygraff.api.chat.model;

import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import com.infinitygraff.api.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Representa uma mensagem do chat entre cliente e prestador de uma solicitação.
 * <p>
 * <b>Ciclo de vida:</b>
 * <ul>
 *   <li>Criada com {@code expira_em = criadoEm + 15 dias}.</li>
 *   <li>Pode ser editada e removida (soft delete) apenas pelo remetente.</li>
 *   <li>Mensagens expiradas permanecem visíveis (histórico), mas não podem ser editadas.</li>
 *   <li>Não herda {@code BaseEntity} — usa campos próprios para soft delete de negócio.</li>
 * </ul>
 */
@Entity
@Table(name = "mensagens_chat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MensagemChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Solicitação à qual o chat pertence. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoArte solicitacao;

    /** Usuário que enviou a mensagem. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    /** Usuário que deve receber a mensagem (o outro participante). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    /** Conteúdo textual da mensagem. */
    @Column(name = "conteudo", nullable = false)
    private String conteudo;

    /** Indica se a mensagem foi editada pelo menos uma vez. */
    @Column(name = "editada", nullable = false)
    private boolean editada;

    /** Se a mensagem foi lida pelo destinatário. */
    @Column(name = "lida", nullable = false)
    private boolean lida;

    /** Momento em que a mensagem foi lida (nulo se ainda não foi). */
    @Column(name = "lida_em")
    private OffsetDateTime lidaEm;

    /** Soft delete de negócio (removida pelo remetente). */
    @Column(name = "deletada", nullable = false)
    private boolean deletada;

    /** Momento do soft delete de negócio. */
    @Column(name = "deletada_em")
    private OffsetDateTime deletadaEm;

    /** Data de criação (imutável após persistência). */
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /** Data da última atualização. */
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Data de expiração (criadoEm + 15 dias). */
    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @PrePersist
    protected void aoInserir() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Marca a mensagem como lida pelo destinatário.
     *
     * @param instanteLeitura momento da leitura
     */
    public void marcarLida(OffsetDateTime instanteLeitura) {
        this.lida = true;
        this.lidaEm = instanteLeitura;
    }

    /**
     * Edita o conteúdo da mensagem.
     *
     * @param novoConteudo novo texto (não pode ser vazio)
     * @throws IllegalArgumentException se a mensagem estiver expirada, deletada ou conteúdo vazio
     */
    public void editar(String novoConteudo) {
        if (isExpirada()) {
            throw new IllegalArgumentException("Não é possível editar uma mensagem expirada.");
        }
        if (this.deletada) {
            throw new IllegalStateException("Não é possível editar uma mensagem removida.");
        }
        if (novoConteudo == null || novoConteudo.trim().isEmpty()) {
            throw new IllegalArgumentException("O conteúdo da mensagem não pode ser vazio.");
        }
        this.conteudo = novoConteudo;
        this.editada = true;
        this.atualizadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Remove logicamente a mensagem (soft delete de negócio).
     */
    public void deletar() {
        if (this.deletada) {
            return; // já está deletada, nada a fazer
        }
        this.deletada = true;
        this.deletadaEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Verifica se a mensagem já expirou.
     *
     * @return true se a data atual for posterior a {@code expiraEm}
     */
    public boolean isExpirada() {
        return this.expiraEm != null && OffsetDateTime.now(ZoneOffset.UTC).isAfter(this.expiraEm);
    }
}