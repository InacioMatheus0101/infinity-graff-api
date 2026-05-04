package com.infinitygraff.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de CORS da aplicação.
 *
 * Lê as origens permitidas de CorsProperties, que por sua vez lê
 * cors.allowed-origins do application.yml.
 *
 * Segurança:
 * - as origens devem ser explicitamente listadas;
 * - wildcard "*" será validado/bloqueado em produção pelo StartupValidationConfig;
 * - JWT será enviado via header Authorization, portanto cookies não são necessários nesta fase.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();

        List<String> origensPermitidas = Arrays.stream(corsProperties.allowedOrigins().split(","))
                .map(String::trim)
                .filter(origem -> !origem.isBlank())
                .toList();

        configuracao.setAllowedOrigins(origensPermitidas);

        configuracao.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuracao.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        configuracao.setExposedHeaders(List.of(
                "Authorization"
        ));

        configuracao.setAllowCredentials(false);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracao);

        return source;
    }
}