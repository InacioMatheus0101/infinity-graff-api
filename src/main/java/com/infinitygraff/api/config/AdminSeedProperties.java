package com.infinitygraff.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades usadas pelo AdminSeeder em ambiente de desenvolvimento.
 *
 * <p>Não contém senha, porque autenticação, login e senha são responsabilidade
 * do Supabase Auth.
 *
 * <p>Essas propriedades são validadas pelo próprio AdminSeeder apenas quando
 * o profile development estiver ativo.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(

        String userId,

        String email,

        String name
) {
}