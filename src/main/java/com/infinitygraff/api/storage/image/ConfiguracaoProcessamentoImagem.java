package com.infinitygraff.api.storage.image;

/**
 * Parâmetros configuráveis para o processamento de imagens.
 * <p>
 * Permite alterar os limites de redimensionamento e a qualidade da
 * compressão WebP sem necessidade de recompilar.
 * Os valores padrão seguem a especificação da Etapa 2 da Infinity Graff.
 * <p>
 * Uso com valores padrão:
 * <pre>
 * ConfiguracaoProcessamentoImagem config = ConfiguracaoProcessamentoImagem.padrao();
 * </pre>
 * Uso com valores customizados:
 * <pre>
 * ConfiguracaoProcessamentoImagem config =
 *     new ConfiguracaoProcessamentoImagem(3000, 600, 30, 80);
 * </pre>
 *
 * @param larguraMaximaOriginal limite do maior lado para versão original (px)
 * @param larguraMaximaMedio    limite do maior lado para versão média (px)
 * @param larguraMaximaBaixo    limite do maior lado para versão baixo (px)
 * @param qualidade             qualidade WebP de 0 a 100 (ex: 70 = 70%)
 */
public record ConfiguracaoProcessamentoImagem(
        int larguraMaximaOriginal,
        int larguraMaximaMedio,
        int larguraMaximaBaixo,
        int qualidade
) {

    /**
     * Configuração padrão conforme spec:
     * original 2000px, médio 300px, baixo 15px, qualidade 70%.
     *
     * @return instância com os valores recomendados
     */
    public static ConfiguracaoProcessamentoImagem padrao() {
        return new ConfiguracaoProcessamentoImagem(2000, 300, 15, 70);
    }

    /**
     * Retorna o limite de pixels do maior lado para a versão informada.
     *
     * @param versao versão desejada
     * @return limite em pixels
     * @throws IllegalArgumentException se a versão não for mapeada
     */
    public int limiteParaVersao(VersaoImagem versao) {
        return switch (versao) {
            case ORIGINAL -> larguraMaximaOriginal;
            case MEDIO    -> larguraMaximaMedio;
            case BAIXO    -> larguraMaximaBaixo;
        };
    }
}