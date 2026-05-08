package com.infinitygraff.api.usuario.dto;

import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;

import java.util.Objects;
import java.util.UUID;

/**
 * DTO de resposta resumida de usuário.
 *
 * <p>Usado em listagens paginadas, como {@code GET /api/v1/usuarios}.
 *
 * <p>Este DTO expõe apenas os dados necessários para visualização em lista.
 *
 * <p>Não expõe:
 * <ul>
 *   <li>senha;</li>
 *   <li>tokens;</li>
 *   <li>dados internos do Supabase Auth;</li>
 *   <li>{@code aceitoTermosEm};</li>
 *   <li>{@code criadoEm};</li>
 *   <li>{@code atualizadoEm};</li>
 *   <li>{@code deletadoEm};</li>
 *   <li>qualquer dado sensível.</li>
 * </ul>
 *
 * <p>A foto de perfil é retornada apenas como URL/path.
 * A imagem real deve ficar armazenada em serviço externo.
 */
public record UsuarioResumoResponse(

        UUID id,

        String nome,

        String email,

        String fotoPerfilUrl,

        Role role,

        boolean ativo
) {

    /**
     * Construtor compacto do record.
     *
     * <p>Garante que a resposta resumida não seja criada sem campos obrigatórios.
     * O único campo opcional é {@code fotoPerfilUrl}.
     */
    public UsuarioResumoResponse {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(email, "email não pode ser nulo");
        Objects.requireNonNull(role, "role não pode ser nula");
    }

    public static UsuarioResumoResponse de(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario não pode ser nulo");

        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                usuario.getRole(),
                usuario.isAtivo()
        );
    }
}