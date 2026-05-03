package com.infinitygraff.api.usuario.repository;

import com.infinitygraff.api.usuario.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de acesso à tabela {@code refresh_tokens}.
 *
 * <p>Queries críticas de segurança:
 * <ul>
 *   <li>Busca sempre por {@code tokenHash} — nunca pelo token puro</li>
 *   <li>Revogação individual no logout</li>
 *   <li>Revogação em massa para bloqueio, desativação ou exclusão de usuário</li>
 *   <li>Limpeza futura de tokens expirados/revogados</li>
 * </ul>
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Busca um refresh token pelo hash SHA-256.
     * Usado nos endpoints {@code /auth/refresh} e {@code /auth/logout}.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Busca um refresh token ativo pelo hash SHA-256.
     * Útil quando o fluxo quiser ignorar tokens já revogados diretamente na query.
     */
    Optional<RefreshToken> findByTokenHashAndRevogadoFalse(String tokenHash);

    /**
     * Revoga todos os tokens ativos de um usuário.
     *
     * <p>Uso previsto:
     * <ul>
     *   <li>desativação de usuário</li>
     *   <li>soft delete de usuário</li>
     *   <li>bloqueio administrativo</li>
     *   <li>suspeita de conta comprometida</li>
     * </ul>
     *
     * <p>Não deve ser usado no logout comum, pois o logout comum revoga
     * apenas o refresh token informado no request.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revogado = true,
                rt.revogadoEm = :agora
            WHERE rt.usuario.id = :usuarioId
              AND rt.revogado = false
            """)
    int revogarTodosPorUsuarioId(
            @Param("usuarioId") UUID usuarioId,
            @Param("agora") OffsetDateTime agora
    );

    /**
     * Remove fisicamente tokens expirados ou revogados do banco.
     *
     * <p>Uso previsto para rotina futura de limpeza. Não faz parte do fluxo
     * principal de autenticação da Fase 1.
     *
     * @param referencia data de corte — tokens expirados antes desta data são removidos
     * @return quantidade de registros removidos
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM RefreshToken rt
            WHERE rt.revogado = true
               OR rt.expiraEm < :referencia
            """)
    int limparTokensInvalidos(@Param("referencia") OffsetDateTime referencia);
}