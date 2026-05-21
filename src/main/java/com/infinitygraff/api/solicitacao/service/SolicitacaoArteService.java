package com.infinitygraff.api.solicitacao.service;

import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.solicitacao.dto.AtribuirPrestadorRequest;
import com.infinitygraff.api.solicitacao.dto.CancelarSolicitacaoRequest;
import com.infinitygraff.api.solicitacao.dto.CriarSolicitacaoArteRequest;
import com.infinitygraff.api.solicitacao.dto.HistoricoStatusSolicitacaoResponse;
import com.infinitygraff.api.solicitacao.dto.SolicitacaoArteDetalheResponse;
import com.infinitygraff.api.solicitacao.dto.SolicitacaoArteResumoResponse;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import com.infinitygraff.api.solicitacao.repository.SolicitacaoArteRepository;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Serviço principal do fluxo de solicitações de arte.
 *
 * <p>Responsável pelas regras da Etapa 1:
 * <ul>
 *   <li>criação de solicitação por CLIENTE, ADMIN ou GERENTE;</li>
 *   <li>listagem conforme perfil autenticado;</li>
 *   <li>consulta detalhada com controle de escopo;</li>
 *   <li>atribuição inicial de prestador;</li>
 *   <li>cancelamento com motivo obrigatório;</li>
 *   <li>consulta do histórico de status.</li>
 * </ul>
 *
 * <p>O histórico de status participa da mesma transação da solicitação.
 * A auditoria é registrada somente após commit da transação principal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolicitacaoArteService {

    private static final String ENTIDADE_SOLICITACOES_ARTE = "solicitacoes_arte";

    private static final String ACAO_SOLICITACAO_CRIADA = "SOLICITACAO_CRIADA";
    private static final String ACAO_PRESTADOR_ATRIBUIDO = "PRESTADOR_ATRIBUIDO";
    private static final String ACAO_SOLICITACAO_CANCELADA = "SOLICITACAO_CANCELADA";

    private final SolicitacaoArteRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoStatusSolicitacaoService historicoStatusService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public SolicitacaoArteDetalheResponse criar(
            CriarSolicitacaoArteRequest request,
            Usuario usuarioAutenticado,
            AuditoriaContext auditoriaContext
    ) {
        validarRequest(request);
        validarUsuarioAutenticado(usuarioAutenticado);

        AuditoriaContext contextoAuditoria = normalizarAuditoriaContext(auditoriaContext);

        Usuario cliente = resolverClienteDaCriacao(request, usuarioAutenticado);

        String titulo = normalizarTextoObrigatorio(request.getTitulo(), "Título");
        String instrucoes = normalizarTextoObrigatorio(request.getInstrucoes(), "Instruções");
        String medidas = normalizarTextoOpcional(request.getMedidas());
        String observacoesInternas = resolverObservacoesInternas(request, usuarioAutenticado);

        SolicitacaoArte solicitacao = SolicitacaoArte.criar(
                cliente,
                request.getTipo(),
                titulo,
                instrucoes,
                medidas,
                observacoesInternas
        );

        SolicitacaoArte solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        historicoStatusService.registrarCriacao(solicitacaoSalva, usuarioAutenticado);

        UUID usuarioId = usuarioAutenticado.getId();
        UUID solicitacaoId = solicitacaoSalva.getId();
        UUID clienteId = cliente.getId();
        StatusSolicitacaoArte status = solicitacaoSalva.getStatus();

        registrarAuditoriaAfterCommit(() -> LogAuditoria.builder()
                .usuarioId(usuarioId)
                .acao(ACAO_SOLICITACAO_CRIADA)
                .entidade(ENTIDADE_SOLICITACOES_ARTE)
                .entidadeId(solicitacaoId)
                .dadosAntes(null)
                .dadosDepois(jsonSolicitacaoCriada(status, clienteId))
                .ip(contextoAuditoria.ip())
                .userAgent(contextoAuditoria.userAgent())
                .build());

        return montarDetalheConformePerfil(solicitacaoSalva, usuarioAutenticado);
    }

    public Page<SolicitacaoArteResumoResponse> listar(
            Usuario usuarioAutenticado,
            StatusSolicitacaoArte status,
            TipoSolicitacaoArte tipo,
            UUID clienteId,
            UUID prestadorId,
            Boolean semPrestador,
            Pageable pageable
    ) {
        validarUsuarioAutenticado(usuarioAutenticado);

        Page<SolicitacaoArte> pagina = switch (usuarioAutenticado.getRole()) {
            case ADMIN, GERENTE -> solicitacaoRepository.listarComFiltroAdministrativo(
                    status,
                    tipo,
                    clienteId,
                    prestadorId,
                    semPrestador,
                    pageable
            );
            case CLIENTE -> {
                validarFiltrosAdministrativosAusentes(clienteId, prestadorId, semPrestador);
                yield solicitacaoRepository.listarPorClienteComFiltro(
                        usuarioAutenticado.getId(),
                        status,
                        tipo,
                        pageable
                );
            }
            case PRESTADOR -> {
                validarFiltrosAdministrativosAusentes(clienteId, prestadorId, semPrestador);
                yield solicitacaoRepository.listarPorPrestadorComFiltro(
                        usuarioAutenticado.getId(),
                        status,
                        tipo,
                        pageable
                );
            }
        };

        return pagina.map(SolicitacaoArteResumoResponse::from);
    }

    public SolicitacaoArteDetalheResponse buscarPorId(
            UUID solicitacaoId,
            Usuario usuarioAutenticado
    ) {
        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);
        return montarDetalheConformePerfil(solicitacao, usuarioAutenticado);
    }

    @Transactional
    public SolicitacaoArteDetalheResponse atribuirPrestador(
            UUID solicitacaoId,
            AtribuirPrestadorRequest request,
            Usuario usuarioAutenticado,
            AuditoriaContext auditoriaContext
    ) {
        validarRequest(request);
        validarUsuarioAutenticado(usuarioAutenticado);
        validarAdminOuGerente(usuarioAutenticado);

        AuditoriaContext contextoAuditoria = normalizarAuditoriaContext(auditoriaContext);

        SolicitacaoArte solicitacao = buscarSolicitacaoOuFalhar(solicitacaoId);

        if (solicitacao.isCancelada()) {
            throw conflito("Solicitação cancelada não pode receber prestador.");
        }

        if (solicitacao.getStatus() != StatusSolicitacaoArte.CRIADA) {
            throw conflito("Prestador só pode ser atribuído quando a solicitação estiver em CRIADA.");
        }

        if (solicitacao.possuiPrestador()) {
            throw conflito("Solicitação já possui prestador atribuído.");
        }

        Usuario prestador = buscarUsuarioOuFalhar(request.getPrestadorId(), "Prestador não encontrado.");

        validarUsuarioAtivo(prestador, "Prestador informado está inativo ou removido.");
        validarRole(prestador, Role.PRESTADOR, "Usuário informado não possui perfil PRESTADOR.");

        StatusSolicitacaoArte statusAnterior = solicitacao.getStatus();

        solicitacao.atribuirPrestador(prestador);

        SolicitacaoArte solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        historicoStatusService.registrarTransicao(
                solicitacaoSalva,
                statusAnterior,
                StatusSolicitacaoArte.EM_PRODUCAO,
                usuarioAutenticado,
                null
        );

        UUID usuarioId = usuarioAutenticado.getId();
        UUID solicitacaoIdSalva = solicitacaoSalva.getId();
        UUID prestadorId = prestador.getId();
        StatusSolicitacaoArte statusNovo = solicitacaoSalva.getStatus();

        registrarAuditoriaAfterCommit(() -> LogAuditoria.builder()
                .usuarioId(usuarioId)
                .acao(ACAO_PRESTADOR_ATRIBUIDO)
                .entidade(ENTIDADE_SOLICITACOES_ARTE)
                .entidadeId(solicitacaoIdSalva)
                .dadosAntes(jsonPrestadorAtribuidoAntes(statusAnterior))
                .dadosDepois(jsonPrestadorAtribuidoDepois(statusNovo, prestadorId))
                .ip(contextoAuditoria.ip())
                .userAgent(contextoAuditoria.userAgent())
                .build());

        return montarDetalheConformePerfil(solicitacaoSalva, usuarioAutenticado);
    }

    @Transactional
    public SolicitacaoArteDetalheResponse cancelar(
            UUID solicitacaoId,
            CancelarSolicitacaoRequest request,
            Usuario usuarioAutenticado,
            AuditoriaContext auditoriaContext
    ) {
        validarRequest(request);
        validarUsuarioAutenticado(usuarioAutenticado);

        AuditoriaContext contextoAuditoria = normalizarAuditoriaContext(auditoriaContext);

        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopoParaCancelamento(
                solicitacaoId,
                usuarioAutenticado
        );

        if (solicitacao.isCancelada()) {
            throw conflito("Solicitação já está cancelada.");
        }

        validarPermissaoDeCancelamento(solicitacao, usuarioAutenticado);

        String motivo = normalizarTextoObrigatorio(request.getMotivo(), "Motivo do cancelamento");

        StatusSolicitacaoArte statusAnterior = solicitacao.getStatus();
        OffsetDateTime agoraUtc = OffsetDateTime.now(ZoneOffset.UTC);

        solicitacao.cancelar(motivo, agoraUtc);

        SolicitacaoArte solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        historicoStatusService.registrarTransicao(
                solicitacaoSalva,
                statusAnterior,
                StatusSolicitacaoArte.CANCELADA,
                usuarioAutenticado,
                motivo
        );

        UUID usuarioId = usuarioAutenticado.getId();
        UUID solicitacaoIdSalva = solicitacaoSalva.getId();
        StatusSolicitacaoArte statusNovo = solicitacaoSalva.getStatus();
        OffsetDateTime canceladoEm = solicitacaoSalva.getCanceladoEm();

        registrarAuditoriaAfterCommit(() -> LogAuditoria.builder()
                .usuarioId(usuarioId)
                .acao(ACAO_SOLICITACAO_CANCELADA)
                .entidade(ENTIDADE_SOLICITACOES_ARTE)
                .entidadeId(solicitacaoIdSalva)
                .dadosAntes(jsonCancelamentoAntes(statusAnterior))
                .dadosDepois(jsonCancelamentoDepois(statusNovo, canceladoEm, motivo))
                .ip(contextoAuditoria.ip())
                .userAgent(contextoAuditoria.userAgent())
                .build());

        return montarDetalheConformePerfil(solicitacaoSalva, usuarioAutenticado);
    }

    public List<HistoricoStatusSolicitacaoResponse> listarHistoricoStatus(
            UUID solicitacaoId,
            Usuario usuarioAutenticado
    ) {
        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId, usuarioAutenticado);
        return historicoStatusService.listarPorSolicitacao(solicitacao);
    }

    private Usuario resolverClienteDaCriacao(
            CriarSolicitacaoArteRequest request,
            Usuario usuarioAutenticado
    ) {
        return switch (usuarioAutenticado.getRole()) {
            case CLIENTE -> {
                if (request.getClienteId() != null) {
                    throw badRequest("Cliente não pode informar clienteId na criação da solicitação.");
                }

                yield usuarioAutenticado;
            }
            case ADMIN, GERENTE -> {
                if (request.getClienteId() == null) {
                    throw badRequest("clienteId é obrigatório para criação por ADMIN ou GERENTE.");
                }

                Usuario cliente = buscarUsuarioOuFalhar(request.getClienteId(), "Cliente não encontrado.");

                validarUsuarioAtivo(cliente, "Cliente informado está inativo ou removido.");
                validarRole(cliente, Role.CLIENTE, "Usuário informado não possui perfil CLIENTE.");

                yield cliente;
            }
            case PRESTADOR -> throw new AcessoNegadoException(
                    "Prestador não pode criar solicitação de arte."
            );
        };
    }

    private String resolverObservacoesInternas(
            CriarSolicitacaoArteRequest request,
            Usuario usuarioAutenticado
    ) {
        String observacoesInternas = normalizarTextoOpcional(request.getObservacoesInternas());

        if (usuarioAutenticado.getRole() == Role.CLIENTE && observacoesInternas != null) {
            throw badRequest(
                    "Clientes não podem informar observações internas na criação da solicitação."
            );
        }

        if (usuarioAutenticado.getRole() == Role.PRESTADOR && observacoesInternas != null) {
            throw new AcessoNegadoException(
                    "Prestador não pode informar observações internas na criação da solicitação."
            );
        }

        return observacoesInternas;
    }

    private SolicitacaoArte buscarSolicitacaoComEscopo(
            UUID solicitacaoId,
            Usuario usuarioAutenticado
    ) {
        validarUsuarioAutenticado(usuarioAutenticado);

        return switch (usuarioAutenticado.getRole()) {
            case ADMIN, GERENTE -> buscarSolicitacaoOuFalhar(solicitacaoId);
            case CLIENTE -> solicitacaoRepository
                    .findByIdAndCliente_Id(solicitacaoId, usuarioAutenticado.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada."));
            case PRESTADOR -> solicitacaoRepository
                    .findByIdAndPrestador_Id(solicitacaoId, usuarioAutenticado.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada."));
        };
    }

    private SolicitacaoArte buscarSolicitacaoComEscopoParaCancelamento(
            UUID solicitacaoId,
            Usuario usuarioAutenticado
    ) {
        validarUsuarioAutenticado(usuarioAutenticado);

        return switch (usuarioAutenticado.getRole()) {
            case ADMIN, GERENTE -> buscarSolicitacaoOuFalhar(solicitacaoId);
            case CLIENTE -> solicitacaoRepository
                    .findByIdAndCliente_Id(solicitacaoId, usuarioAutenticado.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada."));
            case PRESTADOR -> throw new AcessoNegadoException(
                    "Prestador não pode cancelar solicitação de arte."
            );
        };
    }

    private SolicitacaoArte buscarSolicitacaoOuFalhar(UUID solicitacaoId) {
        if (solicitacaoId == null) {
            throw badRequest("ID da solicitação é obrigatório.");
        }

        return solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada."));
    }

    private Usuario buscarUsuarioOuFalhar(UUID usuarioId, String mensagem) {
        if (usuarioId == null) {
            throw badRequest("ID do usuário é obrigatório.");
        }

        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(mensagem));
    }

    private void validarPermissaoDeCancelamento(
            SolicitacaoArte solicitacao,
            Usuario usuarioAutenticado
    ) {
        switch (usuarioAutenticado.getRole()) {
            case ADMIN, GERENTE -> {
                // Podem cancelar em qualquer status não cancelado.
            }
            case CLIENTE -> {
                if (solicitacao.getStatus() != StatusSolicitacaoArte.CRIADA
                        && solicitacao.getStatus() != StatusSolicitacaoArte.EM_PRODUCAO) {
                    throw new AcessoNegadoException(
                            "Cliente só pode cancelar solicitações em CRIADA ou EM_PRODUCAO."
                    );
                }
            }
            case PRESTADOR -> throw new AcessoNegadoException(
                    "Prestador não pode cancelar solicitação de arte."
            );
        }
    }

    private SolicitacaoArteDetalheResponse montarDetalheConformePerfil(
            SolicitacaoArte solicitacao,
            Usuario usuarioAutenticado
    ) {
        return switch (usuarioAutenticado.getRole()) {
            case ADMIN, GERENTE, PRESTADOR ->
                    SolicitacaoArteDetalheResponse.fromComObservacoesInternas(solicitacao);
            case CLIENTE ->
                    SolicitacaoArteDetalheResponse.fromSemObservacoesInternas(solicitacao);
        };
    }

    private void validarFiltrosAdministrativosAusentes(
            UUID clienteId,
            UUID prestadorId,
            Boolean semPrestador
    ) {
        if (clienteId != null || prestadorId != null || semPrestador != null) {
            throw badRequest(
                    "Filtros clienteId, prestadorId e semPrestador são permitidos apenas para ADMIN ou GERENTE."
            );
        }
    }

    private void validarAdminOuGerente(Usuario usuario) {
        if (usuario.getRole() != Role.ADMIN && usuario.getRole() != Role.GERENTE) {
            throw new AcessoNegadoException("Ação permitida apenas para ADMIN ou GERENTE.");
        }
    }

    private void validarUsuarioAutenticado(Usuario usuario) {
        if (usuario == null || usuario.getId() == null || usuario.getRole() == null) {
            throw new AcessoNegadoException("Usuário autenticado inválido.");
        }

        validarUsuarioAtivo(usuario, "Usuário autenticado está inativo ou removido.");
    }

    private void validarUsuarioAtivo(Usuario usuario, String mensagem) {
        if (usuario == null || !usuario.podeAcessarSistema()) {
            throw new AcessoNegadoException(mensagem);
        }
    }

    private void validarRole(Usuario usuario, Role roleEsperada, String mensagem) {
        if (usuario.getRole() != roleEsperada) {
            throw badRequest(mensagem);
        }
    }

    private void validarRequest(Object request) {
        if (request == null) {
            throw badRequest("Dados da requisição são obrigatórios.");
        }
    }

    private String normalizarTextoObrigatorio(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            throw badRequest(nomeCampo + " é obrigatório.");
        }

        return valor.trim();
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private AuditoriaContext normalizarAuditoriaContext(AuditoriaContext contexto) {
        if (contexto == null) {
            return AuditoriaContext.vazio();
        }

        return contexto;
    }

    private NegocioException badRequest(String mensagem) {
        return new NegocioException(mensagem, HttpStatus.BAD_REQUEST);
    }

    private NegocioException conflito(String mensagem) {
        return new NegocioException(mensagem, HttpStatus.CONFLICT);
    }

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

    private void registrarAuditoriaSegura(Supplier<LogAuditoria> fornecedorLog) {
        try {
            auditoriaService.registrar(fornecedorLog.get());
        } catch (Exception e) {
            log.error(
                    "Falha ao preparar log de auditoria da solicitação: {}",
                    e.getMessage(),
                    e
            );
        }
    }

    private String jsonSolicitacaoCriada(
            StatusSolicitacaoArte status,
            UUID clienteId
    ) {
        return "{\"status\":\"%s\",\"clienteId\":\"%s\"}"
                .formatted(status, clienteId);
    }

    private String jsonPrestadorAtribuidoAntes(
            StatusSolicitacaoArte statusAnterior
    ) {
        return "{\"status\":\"%s\",\"prestadorId\":null}"
                .formatted(statusAnterior);
    }

    private String jsonPrestadorAtribuidoDepois(
            StatusSolicitacaoArte statusNovo,
            UUID prestadorId
    ) {
        return "{\"status\":\"%s\",\"prestadorId\":\"%s\"}"
                .formatted(statusNovo, prestadorId);
    }

    private String jsonCancelamentoAntes(
            StatusSolicitacaoArte statusAnterior
    ) {
        return "{\"status\":\"%s\",\"canceladoEm\":null,\"motivoCancelamento\":null}"
                .formatted(statusAnterior);
    }

    private String jsonCancelamentoDepois(
            StatusSolicitacaoArte statusNovo,
            OffsetDateTime canceladoEm,
            String motivo
    ) {
        return "{\"status\":\"%s\",\"canceladoEm\":\"%s\",\"motivoCancelamento\":\"%s\"}"
                .formatted(statusNovo, canceladoEm, escaparJsonSimples(motivo));
    }

    private String escaparJsonSimples(String valor) {
        if (valor == null) {
            return null;
        }

        return valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}