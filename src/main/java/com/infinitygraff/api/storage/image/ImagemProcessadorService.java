package com.infinitygraff.api.storage.image;

import com.infinitygraff.api.storage.exception.ArquivoInvalidoException;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Serviço de processamento de imagens.
 * <p>
 * Converte imagens raster (JPEG, PNG, WEBP) para WebP com qualidade configurável,
 * gera três versões redimensionadas proporcionalmente (original, médio e baixo),
 * e trata arquivos SVG e PDF de forma diferenciada.
 * <p>
 * Para WebP, utiliza a biblioteca Scrimage (puro Java), que não depende de
 * service providers nativos e funciona de forma consistente em ambientes
 * como Spring Boot e contêineres de servlet.
 */
@Slf4j
@Service
public class ImagemProcessadorService {

    private static final String EXTENSAO_WEBP = ".webp";
    private static final String EXTENSAO_PDF  = ".pdf";
    private static final String EXTENSAO_SVG  = ".svg";
    private static final String MIME_SVG      = "image/svg+xml";
    private static final String MIME_PDF      = "application/pdf";

    /**
     * Processa o arquivo de acordo com o tipo MIME informado.
     *
     * @param bytes       conteúdo original
     * @param contentType MIME type do arquivo
     * @param config      parâmetros de tamanho e qualidade
     * @return resultado com os bytes de cada versão e a extensão final
     */
    public ImagemProcessada processar(
            byte[] bytes,
            String contentType,
            ConfiguracaoProcessamentoImagem config
    ) {
        if (MIME_PDF.equals(contentType)) {
            return processarPdf(bytes);
        }
        if (MIME_SVG.equals(contentType)) {
            return processarSvg(bytes);
        }
        return processarImagemRaster(bytes, contentType, config);
    }

    /**
     * PDF: apenas versão original. As demais permanecem nulas.
     */
    private ImagemProcessada processarPdf(byte[] bytes) {
        Map<VersaoImagem, byte[]> versoes = new EnumMap<>(VersaoImagem.class);
        versoes.put(VersaoImagem.ORIGINAL, bytes);
        log.debug("PDF processado — apenas versão original gerada");
        return new ImagemProcessada(versoes, EXTENSAO_PDF);
    }

    /**
     * SVG: arquivo vetorial. Os mesmos bytes são associados às três versões.
     */
    private ImagemProcessada processarSvg(byte[] bytes) {
        Map<VersaoImagem, byte[]> versoes = new EnumMap<>(VersaoImagem.class);
        versoes.put(VersaoImagem.ORIGINAL, bytes);
        versoes.put(VersaoImagem.MEDIO, bytes);
        versoes.put(VersaoImagem.BAIXO, bytes);
        log.debug("SVG processado — mesmos bytes para as 3 versões (vetorial)");
        return new ImagemProcessada(versoes, EXTENSAO_SVG);
    }

    /**
     * Imagens raster: decodifica, redimensiona cada versão e converte para WebP.
     */
    private ImagemProcessada processarImagemRaster(
            byte[] bytes,
            String contentType,
            ConfiguracaoProcessamentoImagem config
    ) {
        BufferedImage imagemOriginal = lerImagem(bytes, contentType);
        Map<VersaoImagem, byte[]> versoes = new EnumMap<>(VersaoImagem.class);

        for (VersaoImagem versao : VersaoImagem.values()) {
            int limitePixels = config.limiteParaVersao(versao);
            BufferedImage redimensionada = redimensionar(imagemOriginal, limitePixels);
            byte[] webp = converterParaWebp(redimensionada, config.qualidade());
            versoes.put(versao, webp);
            log.debug("Versão {} gerada: {}x{} → {} bytes WebP",
                    versao, redimensionada.getWidth(), redimensionada.getHeight(), webp.length);
        }

        return new ImagemProcessada(versoes, EXTENSAO_WEBP);
    }

    /**
     * Redimensiona a imagem proporcionalmente, sem ampliar.
     */
    private BufferedImage redimensionar(BufferedImage imagem, int limitePixels) {
        int larguraOriginal = imagem.getWidth();
        int alturaOriginal  = imagem.getHeight();

        if (larguraOriginal <= limitePixels && alturaOriginal <= limitePixels) {
            return imagem;
        }

        double proporcao = (double) limitePixels / Math.max(larguraOriginal, alturaOriginal);
        int novaLargura = Math.max(1, (int) Math.round(larguraOriginal * proporcao));
        int novaAltura  = Math.max(1, (int) Math.round(alturaOriginal  * proporcao));

        // Preserva o tipo original da imagem, usando ARGB como fallback
        int tipoImagem = imagem.getType() == BufferedImage.TYPE_CUSTOM
                ? BufferedImage.TYPE_INT_ARGB
                : imagem.getType();

        BufferedImage redimensionada = new BufferedImage(novaLargura, novaAltura, tipoImagem);
        Graphics2D g2d = redimensionada.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(imagem, 0, 0, novaLargura, novaAltura, null);
        } finally {
            g2d.dispose();
        }
        return redimensionada;
    }

    /**
     * Converte a imagem para WebP usando a biblioteca Scrimage.
     *
     * @param imagem    imagem a ser convertida
     * @param qualidade valor inteiro entre 0 e 100 (ex: 70 = 70%)
     * @return bytes WebP
     */
    private byte[] converterParaWebp(BufferedImage imagem, int qualidade) {
        try {
            return ImmutableImage.fromAwt(imagem)
                    .bytes(WebpWriter.DEFAULT.withQ(qualidade));
        } catch (Exception e) {
            throw new ArquivoInvalidoException(
                    "Falha ao converter imagem para WebP: " + e.getMessage());
        }
    }

    /**
     * Lê a imagem a partir dos bytes recebidos, usando o carregador do Scrimage.
     *
     * @param bytes       conteúdo do arquivo
     * @param contentType MIME type original (usado apenas para mensagem de erro)
     * @return imagem decodificada como BufferedImage
     */
    private BufferedImage lerImagem(byte[] bytes, String contentType) {
        try {
            return ImmutableImage.loader()
                    .fromBytes(bytes)
                    .awt();
        } catch (Exception e) {
            throw new ArquivoInvalidoException(
                    "Erro ao ler imagem do tipo " + contentType + ": " + e.getMessage());
        }
    }
}