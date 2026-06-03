package com.infinitygraff.api.solicitacao.model;

import com.infinitygraff.api.common.entity.BaseEntity;
import com.infinitygraff.api.solicitacao.enums.TipoArquivoSolicitacao;
import com.infinitygraff.api.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Arquivo anexado a uma solicitação de arte.
 * <p>
 * Cada registro representa um upload concluído com sucesso. Os caminhos
 * no storage (original, médio, baixo) são armazenados para permitir a
 * geração de URLs assinadas sob demanda.
 * <p>
 * <b>Soft delete duplo:</b>
 * <ul>
 *   <li>{@code deletadoEm} — remoção lógica técnica (herdado de {@link BaseEntity});</li>
 *   <li>{@code removidoEm} / {@code removidoPor} — remoção funcional pelo usuário,
 *       que mantém o registro para auditoria mas libera o limite de 50 arquivos.</li>
 * </ul>
 * <p>
 * A entidade só é considerada "ativa" quando ambos os campos são nulos:
 * {@code deletadoEm IS NULL AND removidoEm IS NULL}.
 * <p>
 * <b>Nota sobre herança:</b> Esta classe herda {@code criadoEm}, {@code atualizadoEm}
 * e {@code deletadoEm} de {@link BaseEntity}, mas declara seu próprio {@code @Id}
 * para manter controle total sobre a estratégia de geração (UUID via Hibernate).
 */
@Entity
@Table(name = "arquivos_solicitacao_arte")
@SQLRestriction("deletado_em IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ArquivoSolicitacaoArte extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Solicitação à qual o arquivo pertence. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false, updatable = false)
    private SolicitacaoArte solicitacao;

    /** Usuário que realizou o upload. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviado_por_id", nullable = false, updatable = false)
    private Usuario enviadoPor;

    /** Tipo do arquivo (define a subpasta no storage). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30, updatable = false)
    private TipoArquivoSolicitacao tipo;

    /** Nome original do arquivo enviado pelo usuário. */
    @Column(name = "nome_original", nullable = false, length = 255, updatable = false)
    private String nomeOriginal;

    /** Nome gerado internamente para o arquivo no storage (UUID + extensão). */
    @Column(name = "nome_armazenado", nullable = false, length = 255, updatable = false)
    private String nomeArmazenado;

    /** MIME type do arquivo (ex: image/jpeg, application/pdf). */
    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    private String contentType;

    /** Tamanho do arquivo original em bytes (antes do processamento). */
    @Column(name = "tamanho_bytes", nullable = false, updatable = false)
    private Long tamanhoBytes;

    /** Nome do bucket no Supabase Storage. */
    @Column(name = "storage_bucket", nullable = false, length = 100, updatable = false)
    private String storageBucket;

    /** Caminho completo da versão original no storage. */
    @Column(name = "storage_path_original", nullable = false, unique = true, updatable = false)
    private String storagePathOriginal;

    /** Caminho completo da versão média no storage (nulo para PDF). */
    @Column(name = "storage_path_medio", updatable = false)
    private String storagePathMedio;

    /** Caminho completo da versão baixo no storage (nulo para PDF). */
    @Column(name = "storage_path_baixo", updatable = false)
    private String storagePathBaixo;

    /** Descrição opcional fornecida pelo usuário no upload. */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /** Data da remoção funcional pelo usuário. */
    @Column(name = "removido_em")
    private OffsetDateTime removidoEm;

    /** Usuário que removeu o arquivo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removido_por_id")
    private Usuario removidoPor;

    /**
     * Cria um novo registro de arquivo com os dados obrigatórios.
     *
     * @param solicitacao        solicitação vinculada
     * @param enviadoPor         usuário que fez o upload
     * @param tipo               tipo do arquivo
     * @param nomeOriginal       nome original do arquivo
     * @param nomeArmazenado     nome gerado internamente
     * @param contentType        MIME type
     * @param tamanhoBytes       tamanho em bytes
     * @param storageBucket      nome do bucket
     * @param storagePathOriginal caminho da versão original
     * @param storagePathMedio   caminho da versão média (ou null)
     * @param storagePathBaixo   caminho da versão baixo (ou null)
     * @param descricao          descrição opcional
     * @return nova instância pronta para persistência
     */
    public static ArquivoSolicitacaoArte criar(
            SolicitacaoArte solicitacao,
            Usuario enviadoPor,
            TipoArquivoSolicitacao tipo,
            String nomeOriginal,
            String nomeArmazenado,
            String contentType,
            Long tamanhoBytes,
            String storageBucket,
            String storagePathOriginal,
            String storagePathMedio,
            String storagePathBaixo,
            String descricao
    ) {
        return ArquivoSolicitacaoArte.builder()
                .solicitacao(solicitacao)
                .enviadoPor(enviadoPor)
                .tipo(tipo)
                .nomeOriginal(nomeOriginal)
                .nomeArmazenado(nomeArmazenado)
                .contentType(contentType)
                .tamanhoBytes(tamanhoBytes)
                .storageBucket(storageBucket)
                .storagePathOriginal(storagePathOriginal)
                .storagePathMedio(storagePathMedio)
                .storagePathBaixo(storagePathBaixo)
                .descricao(descricao)
                .build();
    }

    /**
     * Marca o arquivo como removido funcionalmente.
     * <p>
     * O registro permanece no banco para auditoria, mas não conta mais
     * no limite de 50 arquivos ativos e não aparece nas listagens.
     *
     * @param usuario usuário que está removendo
     * @param agora   instante da remoção (deve ser {@code OffsetDateTime.now(ZoneOffset.UTC)})
     */
    public void remover(Usuario usuario, OffsetDateTime agora) {
        this.removidoEm = agora;
        this.removidoPor = usuario;
    }

    /**
     * Indica se o arquivo está removido funcionalmente.
     *
     * @return {@code true} se {@code removidoEm} não é nulo
     */
    public boolean isRemovido() {
        return this.removidoEm != null;
    }

    /**
     * Indica se o arquivo é uma imagem (raster ou SVG).
     * PDF retorna {@code false}.
     *
     * @return {@code true} para imagens, {@code false} para PDF
     */
    public boolean isImagem() {
        return !"application/pdf".equals(this.contentType);
    }
}