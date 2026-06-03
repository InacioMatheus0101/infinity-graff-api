package com.infinitygraff.api.storage.validator;

import com.infinitygraff.api.storage.constants.UploadConstantes;
import com.infinitygraff.api.storage.exception.ArquivoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Validador técnico de arquivos recebidos via upload.
 * <p>
 * Executa as seguintes verificações em ordem:
 * <ol>
 *   <li>Se o arquivo não é nulo</li>
 *   <li>Se o nome do arquivo está presente e dentro do limite</li>
 *   <li>Se o arquivo não está vazio</li>
 *   <li>Se o tamanho não excede 20 MB</li>
 *   <li>Se o MIME type é permitido</li>
 *   <li>Se a extensão é permitida</li>
 *   <li>Se a extensão é compatível com o MIME type declarado</li>
 * </ol>
 * <p>
 * Em caso de falha, lança {@link ArquivoInvalidoException} com HTTP 422.
 */
@Component
public class UploadArquivoValidator {

    /**
     * Valida o arquivo de acordo com as regras técnicas.
     *
     * @param arquivo arquivo recebido via multipart/form-data
     * @throws ArquivoInvalidoException se qualquer validação falhar
     */
    public void validar(MultipartFile arquivo) {
        if (arquivo == null) {
            throw new ArquivoInvalidoException("Arquivo é obrigatório");
        }

        validarNome(arquivo);
        validarArquivoVazio(arquivo);
        validarTamanho(arquivo);
        validarContentType(arquivo);
        validarExtensao(arquivo);
        validarCompatibilidade(arquivo);
    }

    private void validarNome(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            throw new ArquivoInvalidoException("Nome do arquivo é obrigatório");
        }
        if (nome.length() > UploadConstantes.TAMANHO_MAXIMO_NOME_ARQUIVO) {
            throw new ArquivoInvalidoException("Nome do arquivo excede o limite permitido");
        }
    }

    private void validarArquivoVazio(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Arquivo enviado está vazio");
        }
    }

    private void validarTamanho(MultipartFile arquivo) {
        if (arquivo.getSize() > UploadConstantes.TAMANHO_MAXIMO_ARQUIVO) {
            throw new ArquivoInvalidoException("Arquivo excede o limite máximo de 20 MB");
        }
    }

    private void validarContentType(MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        if (contentType == null
                || !UploadConstantes.CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new ArquivoInvalidoException("Tipo de arquivo não permitido");
        }
    }

    private void validarExtensao(MultipartFile arquivo) {
        String extensao = obterExtensao(arquivo.getOriginalFilename());
        if (!UploadConstantes.EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new ArquivoInvalidoException("Extensão de arquivo não permitida");
        }
    }

    /**
     * Verifica se a extensão do arquivo é coerente com o MIME type informado.
     * Exemplo: um arquivo .jpg deve ter content type "image/jpeg".
     */
    private void validarCompatibilidade(MultipartFile arquivo) {
        String extensao    = obterExtensao(arquivo.getOriginalFilename());
        String contentType = arquivo.getContentType();

        boolean valido = switch (extensao) {
            case ".jpg", ".jpeg" -> "image/jpeg".equals(contentType);
            case ".png"          -> "image/png".equals(contentType);
            case ".webp"         -> "image/webp".equals(contentType);
            case ".pdf"          -> "application/pdf".equals(contentType);
            case ".svg"          -> "image/svg+xml".equals(contentType);
            default              -> false;
        };

        if (!valido) {
            throw new ArquivoInvalidoException(
                    "Extensão do arquivo incompatível com o content type informado"
            );
        }
    }

    /**
     * Extrai a extensão do nome do arquivo.
     * Exemplo: "foto.jpg" → ".jpg".
     *
     * @param nomeArquivo nome original do arquivo
     * @return extensão em minúsculo (com ponto), ou string vazia se não houver extensão
     */
    private String obterExtensao(String nomeArquivo) {
        int ultimoPonto = nomeArquivo.lastIndexOf('.');
        if (ultimoPonto < 0) {
            return "";
        }
        return nomeArquivo.substring(ultimoPonto).toLowerCase(Locale.ROOT).trim();
    }
}