package com.infinitygraff.api.chat.service;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.service.AuditoriaContext;
import com.infinitygraff.api.auditoria.service.AuditoriaService;
import com.infinitygraff.api.chat.dto.*;
import com.infinitygraff.api.chat.model.MensagemChat;
import com.infinitygraff.api.chat.repository.MensagemChatRepository;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.exception.RecursoNaoEncontradoException;
import com.infinitygraff.api.solicitacao.enums.StatusSolicitacaoArte;
import com.infinitygraff.api.solicitacao.model.SolicitacaoArte;
import com.infinitygraff.api.solicitacao.repository.SolicitacaoArteRepository;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
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
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Serviço responsável pela lógica de negócio do chat entre cliente e prestador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final String ENTIDADE_CHAT = "mensagens_chat";
    private static final String ACAO_ENVIADA = "CHAT_MENSAGEM_ENVIADA";
    private static final String ACAO_EDITADA = "CHAT_MENSAGEM_EDITADA";
    private static final String ACAO_DELETADA = "CHAT_MENSAGEM_DELETADA";
    private static final int DIAS_EXPIRACAO = 15;

    private final MensagemChatRepository mensagemRepository;
    private final SolicitacaoArteRepository solicitacaoRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Lista mensagens de uma solicitação, com filtro de datas opcional.
     */
    public Page<MensagemChatResponse> listar(
            UUID solicitacaoId, OffsetDateTime inicio, OffsetDateTime fim,
            Pageable pageable, Usuario usuarioAutenticado) {

        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId);
        validarAcessoChat(solicitacao, usuarioAutenticado);

        return mensagemRepository
                .listarComFiltroDeDatas(solicitacao.getId(), inicio, fim, pageable)
                .map(MensagemChatResponse::from);
    }

    /**
     * Envia uma nova mensagem.
     */
    @Transactional
    public MensagemChatResponse enviar(
            UUID solicitacaoId, EnviarMensagemRequest request,
            Usuario usuarioAutenticado, AuditoriaContext auditoriaContext) {

        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId);
        validarAcessoChat(solicitacao, usuarioAutenticado);
        validarPodeEnviarMensagem(solicitacao, usuarioAutenticado);

        Usuario destinatario = obterDestinatario(solicitacao, usuarioAutenticado);
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        MensagemChat mensagem = MensagemChat.builder()
                .solicitacao(solicitacao)
                .remetente(usuarioAutenticado)
                .destinatario(destinatario)
                .conteudo(request.conteudo())
                .expiraEm(agora.plusDays(DIAS_EXPIRACAO))
                .build();

        MensagemChat salva = mensagemRepository.save(mensagem);
        registrarAuditoria(ACAO_ENVIADA, salva, auditoriaContext);

        return MensagemChatResponse.from(salva);
    }

    /**
     * Edita o conteúdo de uma mensagem existente.
     */
    @Transactional
    public MensagemChatResponse editar(
            UUID solicitacaoId, UUID mensagemId, EditarMensagemRequest request,
            Usuario usuarioAutenticado, AuditoriaContext auditoriaContext) {

        buscarSolicitacaoComEscopo(solicitacaoId);
        MensagemChat mensagem = buscarMensagemComEscopo(mensagemId, solicitacaoId);

        validarPodeEditar(mensagem, usuarioAutenticado);

        mensagem.editar(request.conteudo());
        MensagemChat salva = mensagemRepository.save(mensagem);
        registrarAuditoria(ACAO_EDITADA, salva, auditoriaContext);

        return MensagemChatResponse.from(salva);
    }

    /**
     * Remove logicamente uma mensagem.
     */
    @Transactional
    public void deletar(
            UUID solicitacaoId, UUID mensagemId,
            Usuario usuarioAutenticado, AuditoriaContext auditoriaContext) {

        buscarSolicitacaoComEscopo(solicitacaoId);
        MensagemChat mensagem = buscarMensagemComEscopo(mensagemId, solicitacaoId);

        validarPodeDeletar(mensagem, usuarioAutenticado);

        mensagem.deletar();
        mensagemRepository.save(mensagem);
        registrarAuditoria(ACAO_DELETADA, mensagem, auditoriaContext);
    }

    /**
     * Marca um conjunto de mensagens como lidas.
     */
    @Transactional
    public void marcarComoLidas(
            UUID solicitacaoId, MarcarLidasRequest request, Usuario usuarioAutenticado) {

        SolicitacaoArte solicitacao = buscarSolicitacaoComEscopo(solicitacaoId);
        validarAcessoChat(solicitacao, usuarioAutenticado);

        int atualizadas = mensagemRepository.marcarComoLidas(
                request.ids(), solicitacao.getId(), usuarioAutenticado.getId(),
                OffsetDateTime.now(ZoneOffset.UTC));

        log.info("{} mensagens marcadas como lidas na solicitação {}", atualizadas, solicitacaoId);
    }

    // ---------- Métodos privados de validação ----------

    private SolicitacaoArte buscarSolicitacaoComEscopo(UUID solicitacaoId) {
        return solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
    }

    private void validarAcessoChat(SolicitacaoArte solicitacao, Usuario usuario) {
        boolean isAdminOuGerente = usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.GERENTE;
        boolean isParticipante = usuario.getId().equals(solicitacao.getCliente().getId())
                || (solicitacao.getPrestador() != null
                    && usuario.getId().equals(solicitacao.getPrestador().getId()));

        if (!isAdminOuGerente && !isParticipante) {
            throw new AcessoNegadoException("Você não tem acesso ao chat desta solicitação.");
        }
    }

    private void validarPodeEnviarMensagem(SolicitacaoArte solicitacao, Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.GERENTE) {
            throw new AcessoNegadoException("Administradores e gerentes não podem enviar mensagens.");
        }
        if (solicitacao.getPrestador() == null) {
            throw new NegocioException("A solicitação ainda não possui prestador atribuído.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (solicitacao.getStatus() == StatusSolicitacaoArte.FINALIZADA
                || solicitacao.getStatus() == StatusSolicitacaoArte.CANCELADA) {
            throw new NegocioException("Não é possível enviar mensagens em solicitações finalizadas ou canceladas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Usuario obterDestinatario(SolicitacaoArte solicitacao, Usuario remetente) {
        if (remetente.getId().equals(solicitacao.getCliente().getId())) {
            return solicitacao.getPrestador();
        }
        return solicitacao.getCliente();
    }

    private MensagemChat buscarMensagemComEscopo(UUID mensagemId, UUID solicitacaoId) {
        return mensagemRepository.findByIdAndSolicitacaoIdAndDeletadaFalse(mensagemId, solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mensagem não encontrada"));
    }

    private void validarPodeEditar(MensagemChat mensagem, Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.GERENTE) {
            throw new AcessoNegadoException("Administradores e gerentes não podem editar mensagens.");
        }
        if (!mensagem.getRemetente().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Apenas o remetente pode editar a mensagem.");
        }
        if (mensagem.isExpirada()) {
            throw new NegocioException("Não é possível editar uma mensagem expirada.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validarPodeDeletar(MensagemChat mensagem, Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.GERENTE) {
            throw new AcessoNegadoException("Administradores e gerentes não podem remover mensagens.");
        }
        if (!mensagem.getRemetente().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Apenas o remetente pode remover a mensagem.");
        }
    }

    // ---------- Auditoria ----------

    private void registrarAuditoria(String acao, MensagemChat mensagem, AuditoriaContext contexto) {
        Supplier<LogAuditoria> logSupplier = () -> LogAuditoria.builder()
                .usuarioId(mensagem.getRemetente().getId())
                .acao(acao)
                .entidade(ENTIDADE_CHAT)
                .entidadeId(mensagem.getId())
                .ip(contexto.ip())
                .userAgent(contexto.userAgent())
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    auditoriaService.registrar(logSupplier.get());
                }
            });
        } else {
            auditoriaService.registrar(logSupplier.get());
        }
    }
}