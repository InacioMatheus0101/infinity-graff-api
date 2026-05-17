package com.infinitygraff.api.test.security;

import com.infinitygraff.api.usuario.enums.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de teste para simular um usuário interno autenticado no backend.
 *
 * <p>Cria um {@code SecurityContext} com:
 * <ul>
 *   <li>{@code principal = Usuario};</li>
 *   <li>{@code authority = ROLE_{ROLE}}.</li>
 * </ul>
 *
 * <p>Isso espelha o comportamento real do {@code JwtAuthenticationFilter}
 * quando o token Supabase pertence a um usuário que já possui perfil interno.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = WithMockUsuarioSecurityContextFactory.class)
public @interface WithMockUsuario {

    String id() default "00000000-0000-0000-0000-000000000001";

    String nome() default "Usuário Teste";

    String email() default "usuario.teste@infinitygraff.com";

    Role role() default Role.CLIENTE;

    boolean ativo() default true;
}