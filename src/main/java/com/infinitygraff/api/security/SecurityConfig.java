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

/**
 * Configuração central do Spring Security.
 *
 * O Supabase Auth autentica a identidade do usuário.
 * O backend valida o token Supabase e aplica permissões internas
 * com base no perfil salvo na tabela usuarios.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PREFIXO_API = "/api/v1";

    private static final String ROLE_SUPABASE_AUTHENTICATED = "SUPABASE_AUTHENTICATED";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_GERENTE = "GERENTE";
    private static final String ROLE_PRESTADOR = "PRESTADOR";
    private static final String ROLE_CLIENTE = "CLIENTE";

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

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        /*
                         * Exige token válido do Supabase.
                         *
                         * Permite:
                         * - usuário Supabase autenticado ainda sem perfil interno;
                         * - usuário que já possui perfil interno e chamou a rota novamente.
                         */
                        .requestMatchers(HttpMethod.POST, PREFIXO_API + "/autenticacao/completar-perfil")
                                .hasAnyRole(
                                        ROLE_SUPABASE_AUTHENTICATED,
                                        ROLE_CLIENTE,
                                        ROLE_PRESTADOR,
                                        ROLE_GERENTE,
                                        ROLE_ADMIN
                                )

                        /*
                         * A partir daqui, exige perfil interno real na tabela usuarios.
                         */
                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/autenticacao/meu-perfil")
                                .hasAnyRole(ROLE_CLIENTE, ROLE_PRESTADOR, ROLE_GERENTE, ROLE_ADMIN)

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/usuarios")
                                .hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/usuarios/{id}")
                                .hasAnyRole(ROLE_CLIENTE, ROLE_PRESTADOR, ROLE_GERENTE, ROLE_ADMIN)

                        .requestMatchers(HttpMethod.PATCH, PREFIXO_API + "/usuarios/{id}/status")
                                .hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)

                        .requestMatchers(HttpMethod.DELETE, PREFIXO_API + "/usuarios/{id}")
                                .hasRole(ROLE_ADMIN)

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/auditoria/logs")
                                .hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)

                        /*
                         * Segurança defensiva:
                         * qualquer nova rota esquecida aqui exige perfil interno real.
                         */
                        .anyRequest()
                                .hasAnyRole(ROLE_CLIENTE, ROLE_PRESTADOR, ROLE_GERENTE, ROLE_ADMIN)
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}