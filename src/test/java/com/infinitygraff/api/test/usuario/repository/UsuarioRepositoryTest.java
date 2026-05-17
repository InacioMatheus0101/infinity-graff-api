package com.infinitygraff.api.test.usuario.repository;

import com.infinitygraff.api.test.integration.BaseIntegrationTest;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração do {@link UsuarioRepository}.
 *
 * <p>Executados contra PostgreSQL real via Testcontainers para validar:
 * <ul>
 *   <li>buscas derivadas do Spring Data;</li>
 *   <li>filtros opcionais das listagens;</li>
 *   <li>restrições de visualização por roles;</li>
 *   <li>comportamento do soft delete com {@code @SQLRestriction}.</li>
 * </ul>
 *
 * <p>Esta classe é explicitamente transacional para garantir rollback
 * automático ao final de cada teste.
 */
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest extends BaseIntegrationTest {

    private static final OffsetDateTime ACEITE_TERMOS_PADRAO =
            OffsetDateTime.parse("2026-01-01T00:00:00Z");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void deveBuscarUsuarioPorEmailIgnorandoMaiusculasEMinusculas() {
        Usuario usuario = salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000201"),
                "Cliente Teste",
                "cliente.teste@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        Optional<Usuario> resultado = usuarioRepository.findByEmailIgnoreCase(
                "CLIENTE.TESTE@INFINITYGRAFF.COM"
        );

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(usuario.getId());
    }

    @Test
    void deveVerificarExistenciaPorEmailIgnorandoMaiusculasEMinusculas() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000202"),
                "Prestador Teste",
                "prestador.teste@infinitygraff.com",
                Role.PRESTADOR,
                true
        );

        boolean existe = usuarioRepository.existsByEmailIgnoreCase(
                "PRESTADOR.TESTE@INFINITYGRAFF.COM"
        );

        assertThat(existe).isTrue();
    }

    @Test
    void deveVerificarExistenciaPorRole() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000203"),
                "Administrador Teste",
                "admin.teste@infinitygraff.com",
                Role.ADMIN,
                true
        );

        assertThat(usuarioRepository.existsByRole(Role.ADMIN)).isTrue();
        assertThat(usuarioRepository.existsByRole(Role.GERENTE)).isFalse();
    }

    @Test
    void deveListarTodosOsUsuariosQuandoFiltrosForemNulos() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000204"),
                "Cliente Lista",
                "cliente.lista@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000205"),
                "Gerente Lista",
                "gerente.lista@infinitygraff.com",
                Role.GERENTE,
                true
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltro(
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
    }

    @Test
    void deveFiltrarUsuariosPorRole() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000206"),
                "Cliente Role",
                "cliente.role@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000207"),
                "Prestador Role",
                "prestador.role@infinitygraff.com",
                Role.PRESTADOR,
                true
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltro(
                Role.CLIENTE,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getRole()).isEqualTo(Role.CLIENTE);
    }

    @Test
    void deveFiltrarUsuariosPorStatusAtivo() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000208"),
                "Cliente Ativo Filtro",
                "cliente.ativo.filtro@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000209"),
                "Cliente Inativo Filtro",
                "cliente.inativo.filtro@infinitygraff.com",
                Role.CLIENTE,
                false
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltro(
                null,
                true,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isAtivo()).isTrue();
    }

    @Test
    void deveFiltrarUsuariosPorNomeDeFormaParcialEIgnorandoMaiusculasEMinusculas() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000210"),
                "Cliente Criativo",
                "cliente.criativo@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000211"),
                "Prestador Técnico",
                "prestador.tecnico@infinitygraff.com",
                Role.PRESTADOR,
                true
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltro(
                null,
                null,
                "CLI",
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNome()).isEqualTo("Cliente Criativo");
    }

    @Test
    void deveListarApenasRolesPermitidasParaGerente() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000212"),
                "Cliente Visível",
                "cliente.visivel@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000213"),
                "Prestador Visível",
                "prestador.visivel@infinitygraff.com",
                Role.PRESTADOR,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000214"),
                "Gerente Oculto",
                "gerente.oculto@infinitygraff.com",
                Role.GERENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000215"),
                "Admin Oculto",
                "admin.oculto@infinitygraff.com",
                Role.ADMIN,
                true
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltroPorRoles(
                List.of(Role.CLIENTE, Role.PRESTADOR),
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent())
                .extracting(Usuario::getRole)
                .containsExactlyInAnyOrder(Role.CLIENTE, Role.PRESTADOR);
    }

    @Test
    void deveAplicarFiltroRoleDentroDasRolesPermitidasParaGerente() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000216"),
                "Cliente Gerenciável",
                "cliente.gerenciavel@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000217"),
                "Prestador Gerenciável",
                "prestador.gerenciavel@infinitygraff.com",
                Role.PRESTADOR,
                true
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltroPorRoles(
                List.of(Role.CLIENTE, Role.PRESTADOR),
                Role.PRESTADOR,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getRole()).isEqualTo(Role.PRESTADOR);
    }

    @Test
    void deveRetornarPaginaVaziaQuandoRolesPermitidasForemVazias() {
        salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000218"),
                "Cliente Existente",
                "cliente.existente@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        Page<Usuario> resultado = usuarioRepository.listarComFiltroPorRoles(
                List.of(),
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(resultado.getTotalElements()).isZero();
        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    void naoDeveRetornarUsuarioComSoftDeleteNasConsultasNormais() {
        Usuario usuarioDeletado = salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000219"),
                "Cliente Deletado",
                "cliente.deletado@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        Usuario usuarioVisivel = salvarUsuario(
                UUID.fromString("00000000-0000-0000-0000-000000000220"),
                "Cliente Visível",
                "cliente.visivel.softdelete@infinitygraff.com",
                Role.CLIENTE,
                true
        );

        usuarioDeletado.deletar();
        usuarioRepository.saveAndFlush(usuarioDeletado);
        entityManager.clear();

        Optional<Usuario> porId = usuarioRepository.findById(usuarioDeletado.getId());

        Page<Usuario> listagem = usuarioRepository.listarComFiltro(
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(porId).isEmpty();
        assertThat(listagem.getTotalElements()).isEqualTo(1);
        assertThat(listagem.getContent()).hasSize(1);
        assertThat(listagem.getContent().get(0).getId()).isEqualTo(usuarioVisivel.getId());
    }

    private Usuario salvarUsuario(
            UUID id,
            String nome,
            String email,
            Role role,
            boolean ativo
    ) {
        Usuario usuario = Usuario.builder()
                .id(id)
                .nome(nome)
                .email(email)
                .role(role)
                .ativo(ativo)
                .aceitoTermosEm(ACEITE_TERMOS_PADRAO)
                .build();

        return usuarioRepository.saveAndFlush(usuario);
    }
}