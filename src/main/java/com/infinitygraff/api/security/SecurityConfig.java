package com.infinitygraff.api.security;

import com.infinitygraff.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Locale;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PREFIXO_API = "/api/v1";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final UsuarioRepository usuarioRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider
    ) throws Exception {
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

                        .requestMatchers(HttpMethod.POST, PREFIXO_API + "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, PREFIXO_API + "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, PREFIXO_API + "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, PREFIXO_API + "/auth/logout").permitAll()

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, PREFIXO_API + "/auth/me").authenticated()

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

                .authenticationProvider(authenticationProvider)

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            String emailNormalizado = normalizarEmail(email);

            if (emailNormalizado.isBlank()) {
                throw new UsernameNotFoundException("Usuário não encontrado");
            }

            return usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private String normalizarEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }
}