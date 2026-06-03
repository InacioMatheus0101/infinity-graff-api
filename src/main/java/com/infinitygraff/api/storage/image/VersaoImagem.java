package com.infinitygraff.api.storage.image;

/**
 * Representa as três versões geradas no processamento de imagem.
 * <p>
 * Cada constante mapeia para a subpasta correspondente dentro do
 * storage (ex: "original", "medio", "baixo"), seguindo a estrutura
 * de paths definida na spec.
 *
 * <p>Ordem de declaração: ORIGINAL, MEDIO, BAIXO — utilizada pelo
 * {@code EnumMap} e loops de processamento.
 */
public enum VersaoImagem {

    /** Versão com maior resolução (até 2000px por padrão). */
    ORIGINAL("original"),

    /** Versão intermediária para exibição em painéis (até 300px). */
    MEDIO("medio"),

    /** Thumbnail de baixa resolução para listagens (até 15px). */
    BAIXO("baixo");

    private final String subpasta;

    VersaoImagem(String subpasta) {
        this.subpasta = subpasta;
    }

    /**
     * Retorna o nome da subpasta no storage.
     * Exemplo: "original", "medio", "baixo".
     *
     * @return nome da subpasta
     */
    public String getSubpasta() {
        return subpasta;
    }
}