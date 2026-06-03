package com.infinitygraff.api.solicitacao.dto;

import com.infinitygraff.api.common.dto.UsuarioResumoResponse;
import com.infinitygraff.api.solicitacao.enums.TipoArquivoSolicitacao;
import com.infinitygraff.api.solicitacao.model.ArquivoSolicitacaoArte;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de resposta com metadados de um arquivo anexado.
 * <p>
 * Não expõe os caminhos do storage — o acesso ao conteúdo é feito
 * exclusivamente via endpoint de URL assinada.
 *
 * @param id               ID do arquivo
 * @param solicitacaoId    ID da solicitação vinculada
 * @param tipo             tipo do arquivo
 * @param nomeOriginal     nome original enviado
 * @param contentType      MIME type
 * @param tamanhoBytes     tamanho em bytes
 * @param descricao        descrição opcional
 * @param enviadoPorResumo resumo do usuário que enviou
 * @param criadoEm         data de criação
 */
public record ArquivoSolicitacaoResponse(
        UUID id,
        UUID solicitacaoId,
        TipoArquivoSolicitacao tipo,
        String nomeOriginal,
        String contentType,
        Long tamanhoBytes,
        String descricao,
        UsuarioResumoResponse enviadoPorResumo,
        OffsetDateTime criadoEm
) {

    public static ArquivoSolicitacaoResponse from(ArquivoSolicitacaoArte arquivo) {
        return new ArquivoSolicitacaoResponse(
                arquivo.getId(),
                arquivo.getSolicitacao().getId(),
                arquivo.getTipo(),
                arquivo.getNomeOriginal(),
                arquivo.getContentType(),
                arquivo.getTamanhoBytes(),
                arquivo.getDescricao(),
                UsuarioResumoResponse.from(arquivo.getEnviadoPor()),
                arquivo.getCriadoEm()
        );
    }
}