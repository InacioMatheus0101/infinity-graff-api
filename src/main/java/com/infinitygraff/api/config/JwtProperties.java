package com.infinitygraff.api.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de configuração JWT lidas do application.yml.
 *
 * Mapeadas pelo prefixo "jwt":
 * - jwt.secret
 * - jwt.access-expiration-minutes
 * - jwt.refresh-expiration-days
 *
 * A validação declarativa garante que a aplicação falhe na inicialização
 * caso alguma propriedade obrigatória esteja ausente ou inválida.
 *
 * A validação mais rigorosa do tamanho real em bytes e regras específicas
 * para production deve ficar no StartupValidationConfig.
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        @NotBlank(message = "JWT_SECRET é obrigatório")
        @Size(min = 32, message = "JWT_SECRET deve ter no mínimo 32 caracteres")
        String secret,

        @Min(value = 1, message = "jwt.access-expiration-minutes deve ser maior que zero")
        int accessExpirationMinutes,

        @Min(value = 1, message = "jwt.refresh-expiration-days deve ser maior que zero")
        int refreshExpirationDays
) {
}