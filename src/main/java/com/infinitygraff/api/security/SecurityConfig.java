package com.infinitygraff.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PREFIXO_API = "/api/v1";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/health").permitAll()

                        .requestMatchers(HttpMethod.POST, PREFIXO_API + "/autenticacao/completar-perfil").permitAll()

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/autenticacao/meu-perfil")
                                .authenticated()

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/usuarios")
                                .hasAnyRole("ADMIN", "GERENTE")

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/usuarios/{id}")
                                .authenticated()

                        .requestMatchers(HttpMethod.PATCH, PREFIXO_API + "/usuarios/{id}/status")
                                .hasAnyRole("ADMIN", "GERENTE")

                        .requestMatchers(HttpMethod.DELETE, PREFIXO_API + "/usuarios/{id}")
                                .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/auditoria/logs")
                                .hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}