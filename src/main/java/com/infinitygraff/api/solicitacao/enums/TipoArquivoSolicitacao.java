package com.infinitygraff.api.solicitacao.enums;

/**
 * Tipos de arquivo que podem ser anexados a uma solicitação de arte.
 * <p>
 * Cada constante define a subpasta correspondente no storage
 * (ex: "referencias", "elementos", "rascunhos", "prints", "finalizado").
 * Essa estrutura permite organizar os arquivos por finalidade dentro
 * do bucket.
 * <p>
 * <b>Regras de permissão:</b>
 * <ul>
 *   <li>CLIENTE pode enviar: REFERENCIA, ELEMENTO, RASCUNHO, PRINT</li>
 *   <li>PRESTADOR pode enviar: RASCUNHO, ARQUIVO_FINAL</li>
 *   <li>ADMIN/GERENTE podem enviar todos os tipos</li>
 * </ul>
 */
public enum TipoArquivoSolicitacao {

    /** Materiais de referência enviados pelo cliente (briefing, exemplos, logos). */
    REFERENCIA("referencias"),

    /** Elementos gráficos isolados (ícones, fontes, paletas). */
    ELEMENTO("elementos"),

    /** Rascunhos e versões intermediárias compartilhadas entre prestador e cliente. */
    RASCUNHO("rascunhos"),

    /** Capturas de tela para feedback ou comprovação. */
    PRINT("prints"),

    /** Arquivo finalizado entregue pelo prestador. */
    ARQUIVO_FINAL("finalizado");

    private final String subpasta;

    TipoArquivoSolicitacao(String subpasta) {
        this.subpasta = subpasta;
    }

    /**
     * Retorna o nome da subpasta no storage.
     * Exemplo: "referencias", "elementos", etc.
     *
     * @return nome da subpasta
     */
    public String getSubpasta() {
        return subpasta;
    }
}