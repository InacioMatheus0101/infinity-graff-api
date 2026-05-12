package com.infinitygraff.api.auditoria.dto;

import com.infinitygraff.api.auditoria.model.LogAuditoria;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO de resposta de um registro de auditoria.
 *
 * <p>Representa um log imutável de ação sensível na plataforma.
 *
 * <p>Este DTO nunca deve expor:
 * <ul>
 *   <li>dados internos do Supabase Auth;</li>
 *   <li>senhas;</li>
 *   <li>tokens;</li>
 *   <li>qualquer dado sensível.</li>
 * </ul>
 *
 * <p>{@code dadosAntes} e {@code dadosDepois} contêm JSON serializado
 * do estado da entidade antes e depois da ação, quando aplicável.
 *
 * <p>A sanitização desses dados deve ser feita no {@code AuditoriaService}
 * antes da criação do log. Este DTO apenas representa o que foi salvo.
 *
 * <p>Este response deve ser usado apenas em rotas administrativas,
 * acessíveis por {@code ADMIN}.
 */
public record LogAuditoriaResponse(

        UUID id,

        UUID usuarioId,

        String acao,

        String entidade,

        UUID entidadeId,

        String dadosAntes,

        String dadosDepois,

        String ip,

        String userAgent,

        OffsetDateTime criadoEm
) {

    /**
     * Construtor compacto do record.
     *
     * <p>Garante que a resposta represente um log já persistido e válido.
     */
    public LogAuditoriaResponse {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm não pode ser nulo");

        if (acao == null || acao.isBlank()) {
            throw new IllegalArgumentException("acao não pode ser vazia");
        }
    }

    public static LogAuditoriaResponse de(LogAuditoria log) {
        Objects.requireNonNull(log, "log não pode ser nulo");

        return new LogAuditoriaResponse(
                log.getId(),
                log.getUsuarioId(),
                log.getAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                log.getDadosAntes(),
                log.getDadosDepois(),
                log.getIp(),
                log.getUserAgent(),
                log.getCriadoEm()
        );
    }
}