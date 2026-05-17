package com.infinitygraff.api.test.auditoria.repository;

import com.infinitygraff.api.auditoria.model.LogAuditoria;
import com.infinitygraff.api.auditoria.repository.LogAuditoriaRepository;
import com.infinitygraff.api.test.integration.BaseIntegrationTest;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Testes de integração do {@link LogAuditoriaRepository}.
 *
 * <p>Executados contra PostgreSQL real via Testcontainers para validar:
 * <ul>
 *   <li>compatibilidade real das queries JPQL com PostgreSQL;</li>
 *   <li>filtros opcionais;</li>
 *   <li>ordenação fixa por {@code criadoEm DESC};</li>
 *   <li>correção dos filtros temporais com {@code COALESCE}.</li>
 * </ul>
 *
 * <p>Esta classe é explicitamente transacional para garantir rollback
 * automático ao final de cada teste, inclusive quando são executadas queries
 * nativas auxiliares.
 */
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LogAuditoriaRepositoryTest extends BaseIntegrationTest {

    private static final OffsetDateTime ACEITE_TERMOS_PADRAO =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void naoDeveQuebrarQueryQuandoInicioEFimForemNulos() {
        salvarLog("USUARIO_ATIVADO", "usuarios", null);

        assertThatNoException().isThrownBy(() ->
                logAuditoriaRepository.listarComFiltros(
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 50)
                )
        );
    }

    @Test
    void deveListarLogsSemFiltrosQuandoParametrosForemNulos() {
        salvarLog("USUARIO_ATIVADO", "usuarios", null);
        salvarLog("USUARIO_DESATIVADO", "usuarios", null);

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
    }

    @Test
    void deveOrdenarLogsDoMaisRecenteParaOMaisAntigoMesmoQuandoPageableRecebeSort() {
        LogAuditoria primeiro = salvarLog("USUARIO_DESATIVADO", "usuarios", null);
        LogAuditoria segundo = salvarLog("USUARIO_ATIVADO", "usuarios", null);

        ajustarCriadoEm(primeiro.getId(), OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        ajustarCriadoEm(segundo.getId(), OffsetDateTime.parse("2026-01-02T10:00:00Z"));

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "acao"))
        );

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(segundo.getId());
        assertThat(resultado.getContent().get(1).getId()).isEqualTo(primeiro.getId());

        assertThat(resultado.getContent())
                .extracting(LogAuditoria::getCriadoEm)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void deveFiltrarPorAcaoIgnorandoDiferencaDeCaixa() {
        salvarLog("USUARIO_ATIVADO", "usuarios", null);
        salvarLog("USUARIO_DESATIVADO", "usuarios", null);

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                "usuario_ativado",
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getAcao()).isEqualTo("USUARIO_ATIVADO");
    }

    @Test
    void deveFiltrarPorEntidadeIgnorandoDiferencaDeCaixa() {
        salvarLog("USUARIO_ATIVADO", "usuarios", null);
        salvarLog("USUARIO_DESATIVADO", "pedidos", null);

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                null,
                "USUARIOS",
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEntidade()).isEqualTo("usuarios");
    }

    @Test
    void deveFiltrarPorUsuarioId() {
        Usuario usuarioA = salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                "usuario.a@teste.com"
        );

        Usuario usuarioB = salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000102"),
                "usuario.b@teste.com"
        );

        salvarLog("USUARIO_ATIVADO", "usuarios", usuarioA.getId());
        salvarLog("USUARIO_DESATIVADO", "usuarios", usuarioB.getId());

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                usuarioA.getId(),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getUsuarioId()).isEqualTo(usuarioA.getId());
    }

    @Test
    void deveFiltrarPorDataInicial() {
        LogAuditoria antigo = salvarLog("USUARIO_DESATIVADO", "usuarios", null);
        LogAuditoria recente = salvarLog("USUARIO_ATIVADO", "usuarios", null);

        ajustarCriadoEm(antigo.getId(), OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        ajustarCriadoEm(recente.getId(), OffsetDateTime.parse("2026-01-03T10:00:00Z"));

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                null,
                null,
                OffsetDateTime.parse("2026-01-02T00:00:00Z"),
                null,
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(recente.getId());
    }

    @Test
    void deveFiltrarPorDataFinal() {
        LogAuditoria antigo = salvarLog("USUARIO_DESATIVADO", "usuarios", null);
        LogAuditoria recente = salvarLog("USUARIO_ATIVADO", "usuarios", null);

        ajustarCriadoEm(antigo.getId(), OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        ajustarCriadoEm(recente.getId(), OffsetDateTime.parse("2026-01-03T10:00:00Z"));

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                null,
                null,
                null,
                OffsetDateTime.parse("2026-01-02T23:59:59Z"),
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(antigo.getId());
    }

    @Test
    void deveFiltrarPorIntervaloDeDatas() {
        LogAuditoria foraAntes = salvarLog("USUARIO_CADASTRADO", "usuarios", null);
        LogAuditoria dentro = salvarLog("USUARIO_DESATIVADO", "usuarios", null);
        LogAuditoria foraDepois = salvarLog("USUARIO_ATIVADO", "usuarios", null);

        ajustarCriadoEm(foraAntes.getId(), OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        ajustarCriadoEm(dentro.getId(), OffsetDateTime.parse("2026-01-05T10:00:00Z"));
        ajustarCriadoEm(foraDepois.getId(), OffsetDateTime.parse("2026-01-10T10:00:00Z"));

        Page<LogAuditoria> resultado = logAuditoriaRepository.listarComFiltros(
                null,
                null,
                null,
                OffsetDateTime.parse("2026-01-03T00:00:00Z"),
                OffsetDateTime.parse("2026-01-07T23:59:59Z"),
                PageRequest.of(0, 50)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(dentro.getId());
    }

    private LogAuditoria salvarLog(
            String acao,
            String entidade,
            UUID usuarioId
    ) {
        LogAuditoria log = LogAuditoria.builder()
                .usuarioId(usuarioId)
                .acao(acao)
                .entidade(entidade)
                .entidadeId(UUID.randomUUID())
                .dadosAntes("{\"antes\":true}")
                .dadosDepois("{\"depois\":true}")
                .ip("127.0.0.1")
                .userAgent("JUnit/Testcontainers")
                .build();

        return logAuditoriaRepository.saveAndFlush(log);
    }

    private Usuario salvarUsuario(
            UUID id,
            String email
    ) {
        Usuario usuario = Usuario.builder()
                .id(id)
                .nome("Usuário de Teste")
                .email(email)
                .role(Role.CLIENTE)
                .ativo(true)
                .aceitoTermosEm(ACEITE_TERMOS_PADRAO)
                .build();

        return usuarioRepository.saveAndFlush(usuario);
    }

    private void ajustarCriadoEm(
            UUID logId,
            OffsetDateTime criadoEm
    ) {
        entityManager.flush();

        entityManager.getEntityManager()
                .createNativeQuery("""
                        UPDATE logs_auditoria
                        SET criado_em = :criadoEm
                        WHERE id = :logId
                        """)
                .setParameter("criadoEm", criadoEm)
                .setParameter("logId", logId)
                .executeUpdate();

        entityManager.clear();
    }
}