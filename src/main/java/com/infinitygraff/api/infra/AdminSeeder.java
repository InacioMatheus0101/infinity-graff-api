package com.infinitygraff.api.infra;

import com.infinitygraff.api.config.AdminSeedProperties;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

/**
 * Seeder de desenvolvimento responsável por criar o perfil interno ADMIN.
 *
 * <p>Executado apenas quando {@code SPRING_PROFILES_ACTIVE=development}.
 *
 * <p>Este seeder não cria usuário no Supabase Auth, não define senha
 * e não gera token. Ele apenas cria o perfil interno ADMIN na tabela
 * {@code usuarios}, usando o UUID de um usuário que já existe no Supabase Auth.
 *
 * <p>Variáveis necessárias:
 * <ul>
 *   <li>{@code ADMIN_INITIAL_USER_ID} — UUID do usuário no Supabase Auth;</li>
 *   <li>{@code ADMIN_INITIAL_EMAIL} — e-mail do usuário no Supabase Auth;</li>
 *   <li>{@code ADMIN_INITIAL_NAME} — nome do usuário.</li>
 * </ul>
 *
 * <p>O seeder é idempotente: se já existir algum ADMIN não deletado no banco,
 * nenhuma ação é executada.
 */
@Slf4j
@Component
@Profile("development")
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final AdminSeedProperties adminSeedProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRole(Role.ADMIN)) {
            log.info("AdminSeeder: perfil ADMIN já existe no banco. Nenhuma ação necessária.");
            return;
        }

        UUID adminId = validarEConverterUserId(adminSeedProperties.userId());
        String email = normalizarEmailObrigatorio(adminSeedProperties.email());
        String nome = normalizarTextoObrigatorio(adminSeedProperties.name(), "ADMIN_INITIAL_NAME");

        if (usuarioRepository.findById(adminId).isPresent()) {
            throw new IllegalStateException(
                    "AdminSeeder: já existe um perfil interno com ADMIN_INITIAL_USER_ID, " +
                            "mas ainda não existe ADMIN no banco. Para evitar promoção silenciosa " +
                            "de privilégio, promova esse usuário manualmente ou informe outro UUID."
            );
        }

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(
                    "AdminSeeder: ADMIN_INITIAL_EMAIL já está vinculado a outro perfil interno."
            );
        }

        Usuario admin = Usuario.builder()
                .id(adminId)
                .nome(nome)
                .email(email)
                .role(Role.ADMIN)
                .ativo(true)
                .aceitoTermosEm(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        usuarioRepository.save(admin);

        log.info(
                "AdminSeeder: perfil interno ADMIN criado com sucesso. id={}, email={}",
                adminId,
                email
        );
    }

    private UUID validarEConverterUserId(String valor) {
        String userId = normalizarTextoObrigatorio(valor, "ADMIN_INITIAL_USER_ID");

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "AdminSeeder: ADMIN_INITIAL_USER_ID deve ser um UUID válido do usuário existente no Supabase Auth.",
                    e
            );
        }
    }

    private String normalizarEmailObrigatorio(String valor) {
        return normalizarTextoObrigatorio(valor, "ADMIN_INITIAL_EMAIL")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizarTextoObrigatorio(String valor, String nomeVariavel) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "AdminSeeder: " + nomeVariavel + " é obrigatório em development."
            );
        }

        return valor.trim();
    }
}