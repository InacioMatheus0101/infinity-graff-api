package com.infinitygraff.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(

        @NotBlank(message = "app.version é obrigatório")
        String version,

        @NotNull(message = "app.env é obrigatório")
        Environment env,

        @Valid
        @NotNull(message = "app.admin é obrigatório")
        Admin admin
) {

    public boolean isDevelopment() {
        return env == Environment.DEVELOPMENT;
    }

    public boolean isStaging() {
        return env == Environment.STAGING;
    }

    public boolean isProduction() {
        return env == Environment.PRODUCTION;
    }

    public record Admin(

            @NotBlank(message = "app.admin.email é obrigatório")
            @Email(message = "app.admin.email deve ser um e-mail válido")
            String email,

            String password
    ) {
    }

    public enum Environment {
        DEVELOPMENT,
        STAGING,
        PRODUCTION
    }
}