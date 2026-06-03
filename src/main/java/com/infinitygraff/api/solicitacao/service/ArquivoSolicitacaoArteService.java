package com.infinitygraff.api.solicitacao.service;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.solicitacao.dto.ArquivoSolicitacaoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoArquivoSolicitacao;
import com.infinitygraff.api.solicitacao.model.ArquivoSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import com.infinitygraff.api.solicitacao.repository.ArquivoSolicitacaoArteRepository;
import com.infinitygraff.api.solicitacao.repository.SolicitacaoArteRepository;
import com.infinitygraff.api.storage.constants.UploadConstantes;
import com.infinitygraff.api.storage.dto.UrlAssinadaResponse;
import com.infinitygraff.api.storage.exception.StorageIntegrationException;
import com.infinitygraff.api.storage.image.ConfiguracaoProcessamentoImagem;
import com.infinitygraff.api.storage.image.ImagemProcessada;
import com.infinitygraff.api.storage.image.ImagemProcessadorService;
import com.infinitygraff.api.storage.image.VersaoImagem;
import com.infinitygraff.api.storage.service.SupabaseStorageService;
import com.infinitygraff.api.storage.validator.UploadArquivoValidator;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Serviço que orquestra o upload e gerenciamento de arquivos das solicitações.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArquivoSolicitacaoArteService {

    private static final String ENTIDADE_ARQUIVOS = "arquivos_solicitacao_arte";
    private static final String ACAO_ARQUIVO_ENVIADO = "ARQUIVO_SOLICITACAO_ENVIADO";
    private static final String ACAO_ARQUIVO_REMOVIDO = "ARQUIVO_SOLICITACAO_REMOVIDO";

    // Status que permitem upload de arquivos
    private static final Set<StatusSolicitacaoArte> STATUS_PERMITIDOS_UPLOAD = Set.of(
            StatusSolicitacaoArte.CRIADA,
            StatusSolicitacaoArte.EM_PRODUCAO,
            StatusSolicitacaoArte.AGUARDANDO_VALIDACAO_ADMIN,
            StatusSolicitacaoArte.AGUARDANDO_ACEITE_CLIENTE
    );

    private static final int TEMPO_EXPIRACAO_URL_SEGUNDOS = 300; // 5 minutos

    private final SolicitacaoArteRepository solicitacaoRepository;
    private final ArquivoSolicitacaoArteRepository arquivoRepository;
    private final SupabaseStorageService storageService;
    private final ImagemProcessadorService processadorService;
    private final UploadArquivoValidator uploadValidator;
    private final AuditoriaService auditoriaService;

    /**
     * Realiza o upload de um arquivo, processando e armazenando no Supabase.
     */
    @Transactional
    public ArquivoSolicitacaoResponse enviar(
            UUID solicitacaoId,
            MultipartFile file,
            TipoArquivoSolicitacao tipo,
            String descricao,
            Usuario usuarioAutenticado,
            AuditoriaContext auditoriaContext
    ) {
        // 1. Busca a solicitação respeitando o escopo do usuário
        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);
        // 2. Verifica se o status permite upload
        validarStatusParaUpload(solicitacao);
        // 3. Verifica permissão por tipo de arquivo
        validarPermissaoPorTipo(tipo, usuarioAutenticado);
        // 4. Verifica limite de 50 arquivos ativos
        validarLimiteDeArquivos(solicitacaoId);
        // 5. Validações técnicas do arquivo (tamanho, formato, etc.)
        uploadValidator.validar(file);

        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();
            String nomeOriginal = file.getOriginalFilename();

            // 6. Processa a imagem (converte para WebP e gera 3 versões)
            ConfiguracaoProcessamentoImagem config = ConfiguracaoProcessamentoImagem.padrao();
            ImagemProcessada processada = processadorService.processar(bytes, contentType, config);

            // 7. Monta os caminhos dentro do bucket
            String arquivoId = UUID.randomUUID().toString().substring(0, 8);
            String modulo = "solicitacoes";
            String bucket = "solicitacoes-arte";
            String extensao = processada.extensaoFinal();
            String nomeArmazenado = arquivoId + extensao;

            String pathOriginal = buildPath(modulo, solicitacaoId.toString(), tipo.getSubpasta(),
                    VersaoImagem.ORIGINAL.getSubpasta(), arquivoId, extensao);
            String pathMedio = null;
            String pathBaixo = null;

            if (processada.possuiMultiplasVersoes()) {
                pathMedio = buildPath(modulo, solicitacaoId.toString(), tipo.getSubpasta(),
                        VersaoImagem.MEDIO.getSubpasta(), arquivoId, extensao);
                pathBaixo = buildPath(modulo, solicitacaoId.toString(), tipo.getSubpasta(),
                        VersaoImagem.BAIXO.getSubpasta(), arquivoId, extensao);
            }

            // 8. Faz upload de cada versão para o Supabase Storage
            storageService.upload(pathOriginal, processada.bytesParaVersao(VersaoImagem.ORIGINAL),
                    contentType.startsWith("image/") ? "image/" + extensao.substring(1) : contentType);

            if (processada.possuiMultiplasVersoes()) {
                String mimeWebp = "image/" + extensao.substring(1);
                storageService.upload(pathMedio, processada.bytesParaVersao(VersaoImagem.MEDIO), mimeWebp);
                storageService.upload(pathBaixo, processada.bytesParaVersao(VersaoImagem.BAIXO), mimeWebp);
            }

            // 9. Cria a entidade e salva no banco
            ArquivoSolicitacaoArte arquivo = ArquivoSolicitacaoArte.criar(
                    solicitacao,
                    usuarioAutenticado,
                    tipo,
                    nomeOriginal,
                    nomeArmazenado,
                    contentType,
                    file.getSize(),
                    bucket,
                    pathOriginal,
                    pathMedio,
                    pathBaixo,
                    descricao
            );

            ArquivoSolicitacaoArte salvo = arquivoRepository.save(arquivo);

            // 10. Registra auditoria após o commit da transação
            UUID usuarioId = usuarioAutenticado.getId();
            UUID arquivoIdSalvo = salvo.getId();

            registrarAuditoriaAfterCommit(() -> LogAuditoria.builder()
                    .usuarioId(usuarioId)
                    .acao(ACAO_ARQUIVO_ENVIADO)
                    .entidade(ENTIDADE_ARQUIVOS)
                    .entidadeId(arquivoIdSalvo)
                    .ip(auditoriaContext.ip())
                    .userAgent(auditoriaContext.userAgent())
                    .build());

            return ArquivoSolicitacaoResponse.from(salvo);

        } catch (NegocioException e) {
            // Exceções de negócio (4xx) são propagadas sem alteração
            throw e;
        } catch (Exception e) {
            // Erros inesperados (5xx) viram StorageIntegrationException (502)
            log.error("Erro ao processar upload: {}", e.getMessage(), e);
            throw new StorageIntegrationException(
                    "Falha ao processar o arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * Lista os arquivos ativos de uma solicitação, opcionalmente filtrados por tipo.
     */
    public List<ArquivoSolicitacaoResponse> listar(
            UUID solicitacaoId,
            TipoArquivoSolicitacao tipo,
            Usuario usuarioAutenticado
    ) {
        // Valida o escopo (lança exceção se não encontrado)
        buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);

        List<ArquivoSolicitacaoArte> arquivos;
        if (tipo != null) {
            arquivos = arquivoRepository
                    .findBySolicitacao_IdAndTipoAndRemovidoEmIsNullOrderByCriadoEmAsc(
                            solicitacaoId, tipo);
        } else {
            arquivos = arquivoRepository
                    .findBySolicitacao_IdAndRemovidoEmIsNullOrderByCriadoEmAsc(
                            solicitacaoId);
        }

        return arquivos.stream()
                .map(ArquivoSolicitacaoResponse::from)
                .toList();
    }

    /**
     * Busca um arquivo específico, garantindo que pertence à solicitação informada.
     */
    public ArquivoSolicitacaoResponse buscarPorId(
            UUID solicitacaoId,
            UUID arquivoId,
            Usuario usuarioAutenticado
    ) {
        buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);

        ArquivoSolicitacaoArte arquivo = arquivoRepository
                .findByIdAndSolicitacao_IdAndRemovidoEmIsNull(arquivoId, solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado"));

        return ArquivoSolicitacaoResponse.from(arquivo);
    }

    /**
     * Gera uma URL assinada temporária para acessar o arquivo original.
     */
    public UrlAssinadaResponse gerarUrl(
            UUID solicitacaoId,
            UUID arquivoId,
            Usuario usuarioAutenticado
    ) {
        buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);

        ArquivoSolicitacaoArte arquivo = arquivoRepository
                .findByIdAndSolicitacao_IdAndRemovidoEmIsNull(arquivoId, solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado"));

        return storageService.gerarUrlAssinada(
                arquivo.getStoragePathOriginal(),
                TEMPO_EXPIRACAO_URL_SEGUNDOS
        );
    }

    /**
     * Remove logicamente um arquivo (soft delete de negócio).
     */
    @Transactional
    public void remover(
            UUID solicitacaoId,
            UUID arquivoId,
            Usuario usuarioAutenticado,
            AuditoriaContext auditoriaContext
    ) {
        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);

        ArquivoSolicitacaoArte arquivo = arquivoRepository
                .findByIdAndSolicitacao_IdAndRemovidoEmIsNull(arquivoId, solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado"));

        // Apenas ADMIN, GERENTE ou o próprio uploader podem remover
        if (usuarioAutenticado.getRole() != Role.ADMIN
                && usuarioAutenticado.getRole() != Role.GERENTE
                && !arquivo.getEnviadoPor().getId().equals(usuarioAutenticado.getId())) {
            throw new AcessoNegadoException("Você não tem permissão para remover este arquivo");
        }

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        arquivo.remover(usuarioAutenticado, agora);
        arquivoRepository.save(arquivo);

        UUID usuarioId = usuarioAutenticado.getId();

        registrarAuditoriaAfterCommit(() -> LogAuditoria.builder()
                .usuarioId(usuarioId)
                .acao(ACAO_ARQUIVO_REMOVIDO)
                .entidade(ENTIDADE_ARQUIVOS)
                .entidadeId(arquivoId)
                .ip(auditoriaContext.ip())
                .userAgent(auditoriaContext.userAgent())
                .build());
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Busca a solicitação respeitando a role do usuário.
     */
    private SolicitacaoArte buscarSolicitacaoComEscopo(UUID solicitacaoId, Usuario usuario) {
        return switch (usuario.getRole()) {
            case ADMIN, GERENTE -> solicitacaoRepository.findById(solicitacaoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
            case CLIENTE -> solicitacaoRepository.findByIdAndCliente_Id(solicitacaoId, usuario.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
            case PRESTADOR -> solicitacaoRepository.findByIdAndPrestador_Id(solicitacaoId, usuario.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
        };
    }

    /**
     * Verifica se o status da solicitação permite upload.
     */
    private void validarStatusParaUpload(SolicitacaoArte solicitacao) {
        if (!STATUS_PERMITIDOS_UPLOAD.contains(solicitacao.getStatus())) {
            throw new NegocioException(
                    "Não é permitido enviar arquivos para uma solicitação " + solicitacao.getStatus(),
                    HttpStatus.CONFLICT
            );
        }
    }

    /**
     * Verifica se o usuário tem permissão para enviar o tipo de arquivo informado.
     */
    private void validarPermissaoPorTipo(TipoArquivoSolicitacao tipo, Usuario usuario) {
        switch (usuario.getRole()) {
            case CLIENTE -> {
                if (tipo == TipoArquivoSolicitacao.ARQUIVO_FINAL) {
                    throw new AcessoNegadoException("Cliente não pode enviar arquivo final");
                }
            }
            case PRESTADOR -> {
                if (tipo != TipoArquivoSolicitacao.RASCUNHO
                        && tipo != TipoArquivoSolicitacao.ARQUIVO_FINAL) {
                    throw new AcessoNegadoException(
                            "Prestador só pode enviar rascunhos e arquivos finais");
                }
            }
            // ADMIN e GERENTE podem enviar qualquer tipo
            default -> { /* sem restrição */ }
        }
    }

    /**
     * Verifica se a solicitação já atingiu o limite de 50 arquivos ativos.
     */
    private void validarLimiteDeArquivos(UUID solicitacaoId) {
        long quantidade = arquivoRepository.countBySolicitacao_IdAndRemovidoEmIsNull(solicitacaoId);
        if (quantidade >= UploadConstantes.LIMITE_ARQUIVOS_POR_SOLICITACAO) {
            throw new NegocioException(
                    "Limite máximo de " + UploadConstantes.LIMITE_ARQUIVOS_POR_SOLICITACAO
                            + " arquivos por solicitação atingido",
                    HttpStatus.CONFLICT
            );
        }
    }

    /**
     * Monta o caminho completo do arquivo no bucket.
     * Ex: solicitacoes/{id}/{tipo}/{versao}/{arquivoId}.webp
     */
    private String buildPath(String modulo, String id, String tipo, String versao,
                             String arquivoId, String extensao) {
        return String.format("%s/%s/%s/%s/%s%s",
                modulo, id, tipo, versao, arquivoId, extensao);
    }

    /**
     * Registra a auditoria somente após o commit da transação principal.
     */
    private void registrarAuditoriaAfterCommit(Supplier<LogAuditoria> fornecedorLog) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            registrarAuditoriaSegura(fornecedorLog);
                        }
                    }
            );
            return;
        }
        registrarAuditoriaSegura(fornecedorLog);
    }

    /**
     * Executa o registro de auditoria de forma segura, capturando exceções.
     */
    private void registrarAuditoriaSegura(Supplier<LogAuditoria> fornecedorLog) {
        try {
            auditoriaService.registrar(fornecedorLog.get());
        } catch (Exception e) {
            log.error("Falha ao registrar auditoria: {}", e.getMessage(), e);
        }
    }
}