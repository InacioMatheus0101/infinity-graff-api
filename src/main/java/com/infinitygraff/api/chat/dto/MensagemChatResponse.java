package com.infinitygraff.api.chat.dto;

import com.infinitygraff.api.chat.model.MensagemChat;
import com.infinitygraff.api.common.dto.UsuarioResumoResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de resposta representando uma mensagem do chat.
 *
 * @param id                 identificador da mensagem
 * @param solicitacaoId      solicitação vinculada ao chat
 * @param conteudo           conteúdo textual da mensagem
 * @param editada            se a mensagem foi editada pelo menos uma vez
 * @param lida               se a mensagem foi lida pelo destinatário
 * @param expirada           se a mensagem já passou do prazo de expiração
 * @param remetenteResumo    dados resumidos do remetente
 * @param destinatarioResumo dados resumidos do destinatário
 * @param criadoEm           data de envio da mensagem
 * @param lidaEm             data em que a mensagem foi lida (nulo se não lida)
 * @param atualizadoEm       data da última edição (nulo se nunca editada)
 */
public record MensagemChatResponse(
        UUID id,
        UUID solicitacaoId,
        String conteudo,
        boolean editada,
        boolean lida,
        boolean expirada,
        UsuarioResumoResponse remetenteResumo,
        UsuarioResumoResponse destinatarioResumo,
        OffsetDateTime criadoEm,
        OffsetDateTime lidaEm,
        OffsetDateTime atualizadoEm
) {

    /**
     * Converte uma entidade {@link MensagemChat} para o DTO de resposta.
     *
     * @param mensagem entidade persistida
     * @return DTO com os dados da mensagem
     */
    public static MensagemChatResponse from(MensagemChat mensagem) {
        return new MensagemChatResponse(
                mensagem.getId(),
                mensagem.getSolicitacao().getId(),
                mensagem.getConteudo(),
                mensagem.isEditada(),
                mensagem.isLida(),
                mensagem.isExpirada(),
                UsuarioResumoResponse.from(mensagem.getRemetente()),
                UsuarioResumoResponse.from(mensagem.getDestinatario()),
                mensagem.getCriadoEm(),
                mensagem.getLidaEm(),
                mensagem.getAtualizadoEm()
        );
    }
}