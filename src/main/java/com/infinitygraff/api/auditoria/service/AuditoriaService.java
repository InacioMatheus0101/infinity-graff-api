package com.infinitygraff.api.auditoria.service;

import com.infinitygraff.api.auditoria.dto.LogAuditoriaResponse;
import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.repository.LogAuditoriaRepository;
import com.infinitygraff.api.common.exception.AcessoNegadoException;
import com.infinitygraff.api.common.exception.NegocioException;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.security.SecurityContextHelper;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Serviço responsável por registrar e consultar logs de auditoria.
 *
 * <p><b>Registro:</b> o método {@link #registrar(LogAuditoria)} usa
 * {@code REQUIRES_NEW} para salvar o log em transação independente.
 *
 * <p>Deve ser chamado após a operação principal persistir com sucesso.
 *
 * <p><b>Consulta:</b> apenas ADMIN deve consultar logs de auditoria.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Registra um log de auditoria em transação independente.
     *
     * <p>Se o registro falhar, o erro é capturado e registrado via SLF4J.
     * A operação principal não deve ser desfeita por falha de log.
     *
     * @param entrada entidade já construída via {@link LogAuditoria#builder()}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(LogAuditoria entrada) {
        if (entrada == null) {
            log.warn("Tentativa de registrar log de auditoria nulo ignorada.");
            return;
        }

        try {
            logAuditoriaRepository.saveAndFlush(entrada);
        } catch (Exception e) {
            log.error(
                    "Falha ao registrar log de auditoria. acao={}, entidadeId={}: {}",
                    entrada.getAcao(),
                    entrada.getEntidadeId(),
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * Lista logs de auditoria paginados com filtros opcionais.
     *
     * <p>Apenas ADMIN pode consultar logs.
     *
     * <p>Ordenação fixa: mais recentes primeiro ({@code criado_em DESC}).
     * A ordenação do {@code Pageable} recebido é ignorada para manter
     * consistência com a query do repository.
     */
    @Transactional(readOnly = true)
    public PageResponse<LogAuditoriaResponse> listar(
            UUID usuarioId,
            String acao,
            String entidade,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            Pageable pageable
    ) {
        validarPermissaoConsultaAuditoria();
        validarIntervaloDatas(inicio, fim);

        Pageable semOrdenacao = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return PageResponse.de(
                logAuditoriaRepository.listarComFiltros(
                        usuarioId,
                        normalizarAcao(acao),
                        normalizarEntidade(entidade),
                        inicio,
                        fim,
                        semOrdenacao
                ),
                LogAuditoriaResponse::de
        );
    }

   private void validarPermissaoConsultaAuditoria() {
    Usuario autenticado = securityContextHelper.obterUsuarioAutenticado();

    if (autenticado.getRole() != Role.ADMIN
            && autenticado.getRole() != Role.GERENTE) {
        throw new AcessoNegadoException(
                "Apenas ADMIN e GERENTE podem consultar logs de auditoria"
        );
    }
}

    private void validarIntervaloDatas(
            OffsetDateTime inicio,
            OffsetDateTime fim
    ) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new NegocioException(
                    "Data inicial não pode ser posterior à data final",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String normalizarAcao(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarEntidade(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim().toLowerCase(Locale.ROOT);
    }
}