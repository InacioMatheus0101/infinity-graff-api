package com.infinitygraff.api.solicitacao.repository;

import com.infinitygraff.api.solicitacao.enums.TipoArquivoSolicitacao;
import com.infinitygraff.api.solicitacao.model.ArquivoSolicitacaoArte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso aos arquivos anexados às solicitações de arte.
 * <p>
 * Todas as consultas consideram apenas arquivos não removidos
 * ({@code removido_em IS NULL}). O filtro {@code deletado_em IS NULL}
 * é aplicado automaticamente via {@code @SQLRestriction} na entidade.
 */
public interface ArquivoSolicitacaoArteRepository
        extends JpaRepository<ArquivoSolicitacaoArte, UUID> {

    /**
     * Conta quantos arquivos ativos (não removidos) uma solicitação possui.
     * Usado para validar o limite de 50 arquivos.
     *
     * @param solicitacaoId ID da solicitação
     * @return quantidade de arquivos ativos
     */
    long countBySolicitacao_IdAndRemovidoEmIsNull(UUID solicitacaoId);

    /**
     * Lista todos os arquivos ativos de uma solicitação, em ordem cronológica.
     *
     * @param solicitacaoId ID da solicitação
     * @return lista de arquivos ativos (mais antigos primeiro)
     */
    List<ArquivoSolicitacaoArte>
    findBySolicitacao_IdAndRemovidoEmIsNullOrderByCriadoEmAsc(UUID solicitacaoId);

    /**
     * Lista os arquivos ativos de uma solicitação filtrados por tipo.
     *
     * @param solicitacaoId ID da solicitação
     * @param tipo          tipo de arquivo
     * @return lista de arquivos do tipo especificado
     */
    List<ArquivoSolicitacaoArte>
    findBySolicitacao_IdAndTipoAndRemovidoEmIsNullOrderByCriadoEmAsc(
            UUID solicitacaoId, TipoArquivoSolicitacao tipo);

    /**
     * Busca um arquivo específico com escopo de solicitação.
     * <p>
     * Garante que o arquivo pertence à solicitação informada e não foi
     * removido funcionalmente. Usado para operações de detalhe e remoção.
     *
     * @param id            ID do arquivo
     * @param solicitacaoId ID da solicitação
     * @return arquivo se encontrado e ativo
     */
    Optional<ArquivoSolicitacaoArte>
    findByIdAndSolicitacao_IdAndRemovidoEmIsNull(UUID id, UUID solicitacaoId);
}