package com.infinitygraff.api.solicitacao.enums;

/**
 * Status persistidos do fluxo operacional de uma solicitação de arte.
 *
 * <p>Este enum representa apenas estados reais e duráveis da solicitação.
 *
 * <p>Eventos como aprovação administrativa e solicitação de refação
 * não são status persistidos por si só. Eles provocam transições entre
 * estes estados e serão registrados no histórico de status e na auditoria.
 */
public enum StatusSolicitacaoArte {

    /**
     * Solicitação criada e ainda sem prestador atribuído.
     */
    CRIADA,

    /**
     * Solicitação atribuída a um prestador e em execução.
     */
    EM_PRODUCAO,

    /**
     * Prestador concluiu sua etapa e aguarda validação administrativa.
     */
    AGUARDANDO_VALIDACAO_ADMIN,

    /**
     * Administração aprovou a arte e ela aguarda aceite do cliente.
     */
    AGUARDANDO_ACEITE_CLIENTE,

    /**
     * Cliente aceitou a arte e o fluxo foi encerrado com sucesso.
     */
    FINALIZADA,

    /**
     * Solicitação cancelada por regra de negócio.
     */
    CANCELADA
}