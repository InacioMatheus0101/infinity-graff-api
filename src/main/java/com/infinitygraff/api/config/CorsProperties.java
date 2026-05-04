package com.infinitygraff.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de CORS lidas do application.yml.
 *
 * Mapeada pelo prefixo "cors":
 * - cors.allowed-origins
 */
@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(

        @NotBlank(message = "cors.allowed-origins é obrigatório")
        String allowedOrigins
) {
}