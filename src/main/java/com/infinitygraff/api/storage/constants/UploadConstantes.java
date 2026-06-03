package com.infinitygraff.api.storage.constants;

import java.util.Set;

/**
 * Constantes centralizadas do módulo de upload/storage.
 * <p>
 * Define limites de tamanho, MIME types permitidos e extensões aceitas.
 * Os valores devem estar alinhados com as definições do banco de dados
 * (migrations) e com as políticas de segurança da aplicação.
 * <p>
 * <b>Importante:</b> o nome do bucket NÃO é definido aqui — ele é
 * configurado via {@code application.yml} na propriedade
 * {@code supabase.storage.bucket-solicitacoes}.
 */
public final class UploadConstantes {

    /**
     * Tamanho máximo por arquivo: 20 MB (20 * 1024 * 1024 bytes).
     */
    public static final long TAMANHO_MAXIMO_ARQUIVO = 20L * 1024 * 1024;

    /**
     * Quantidade máxima de arquivos ativos por solicitação.
     */
    public static final int LIMITE_ARQUIVOS_POR_SOLICITACAO = 50;

    /**
     * Tamanho máximo do nome original do arquivo enviado pelo cliente.
     */
    public static final int TAMANHO_MAXIMO_NOME_ARQUIVO = 255;

    /**
     * MIME types aceitos no upload público de solicitações.
     * <p>
     * SVG não está incluído — é permitido apenas em contextos internos.
     * ZIP/RAR são proibidos por segurança.
     */
    public static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
             "image/svg+xml"

    );

    /**
     * MIME types que passam pelo processador de imagem (conversão + 3 versões).
     * <p>
     * SVG e PDF são tratados de forma separada.
     */
    public static final Set<String> CONTENT_TYPES_IMAGEM_RASTER = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /** MIME type para arquivos SVG. */
    public static final String MIME_SVG = "image/svg+xml";

    /** MIME type para arquivos PDF. */
    public static final String MIME_PDF = "application/pdf";

    /**
     * Extensões de arquivo permitidas no upload, alinhadas com
     * {@link #CONTENT_TYPES_PERMITIDOS}.
     */
    public static final Set<String> EXTENSOES_PERMITIDAS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf", ".svg"
    );

    private UploadConstantes() {
        throw new UnsupportedOperationException(
                "Classe utilitária não deve ser instanciada"
        );
    }
}