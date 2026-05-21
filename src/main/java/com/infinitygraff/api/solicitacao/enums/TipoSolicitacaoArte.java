package com.infinitygraff.api.solicitacao.enums;

/**
 * Tipos iniciais de solicitação de arte suportados na Fase 2.
 *
 * <p>Esses valores representam a natureza principal do pedido realizado
 * pelo cliente ou pela equipe interna.
 *
 * <p>Ações operacionais como vetorizar, remover fundo ou fazer mockup
 * não pertencem a este enum. Elas serão tratadas em etapa própria
 * como ações operacionais da solicitação.
 */
public enum TipoSolicitacaoArte {

    /**
     * Solicitação para recriar uma arte existente com base em materiais fornecidos.
     */
    RECRIACAO,

    /**
     * Solicitação para criar uma nova arte.
     */
    CRIACAO,

    /**
     * Solicitação para ajustar ou alterar uma arte já existente.
     */
    AJUSTE
}