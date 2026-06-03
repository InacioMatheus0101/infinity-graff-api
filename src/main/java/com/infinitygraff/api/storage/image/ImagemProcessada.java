package com.infinitygraff.api.storage.image;

import java.util.Map;

/**
 * Resultado imutável do processamento de uma imagem.
 * <p>
 * Armazena os bytes de cada versão gerada, indexados pelo enum
 * {@link VersaoImagem}, e a extensão final do arquivo (.webp, .pdf, .svg).
 * <p>
 * Para <b>SVG</b>: as três versões contêm os mesmos bytes (arquivo vetorial).
 * Para <b>PDF</b>: apenas {@link VersaoImagem#ORIGINAL} é preenchido;
 * as demais permanecem nulas.
 * Para <b>imagens raster</b>: todas as versões são geradas e convertidas
 * para WebP.
 *
 * @param versoes      mapa que associa cada {@link VersaoImagem} aos seus bytes
 * @param extensaoFinal extensão do arquivo processado (com ponto, ex: ".webp")
 */
public record ImagemProcessada(
        Map<VersaoImagem, byte[]> versoes,
        String extensaoFinal
) {

    /**
     * Retorna os bytes correspondentes à versão informada.
     *
     * @param versao versão desejada (ORIGINAL, MEDIO ou BAIXO)
     * @return bytes da versão ou {@code null} se não houver (ex: MEDIO de PDF)
     */
    public byte[] bytesParaVersao(VersaoImagem versao) {
        return versoes.get(versao);
    }

    /**
     * Indica se este arquivo possui versões média e baixo.
     * <p>
     * Retorna {@code true} para imagens raster e SVG; {@code false} para PDF.
     *
     * @return {@code true} se MEDIO e BAIXO estão presentes
     */
    public boolean possuiMultiplasVersoes() {
        return versoes.containsKey(VersaoImagem.MEDIO)
                && versoes.get(VersaoImagem.MEDIO) != null;
    }
}