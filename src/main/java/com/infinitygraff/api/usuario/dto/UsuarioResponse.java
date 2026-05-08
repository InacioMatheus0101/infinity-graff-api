package com.infinitygraff.api.usuario.dto;

import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO de resposta completa e segura de usuário interno da plataforma.
 *
 * <p>Usado em:
 * <ul>
 *   <li>perfil autenticado;</li>
 *   <li>detalhe de usuário;</li>
 *   <li>respostas administrativas;</li>
 *   <li>alteração de status.</li>
 * </ul>
 *
 * <p>Este DTO nunca expõe:
 * <ul>
 *   <li>senha;</li>
 *   <li>tokens;</li>
 *   <li>dados internos do Supabase Auth;</li>
 *   <li>{@code deletadoEm};</li>
 *   <li>qualquer dado sensível.</li>
 * </ul>
 *
 * <p>O campo {@code id} corresponde ao mesmo UUID do usuário no Supabase Auth.
 *
 * <p>A foto de perfil é retornada apenas como URL/path.
 * A imagem real deve ficar armazenada em serviço externo, como Supabase Storage,
 * S3, Cloudflare R2 ou equivalente.
 */
public record UsuarioResponse(

        UUID id,

        String nome,

        String email,

        String fotoPerfilUrl,

        Role role,

        boolean ativo,

        OffsetDateTime aceitoTermosEm,

        OffsetDateTime criadoEm,

        OffsetDateTime atualizadoEm
) {

    /**
     * Construtor compacto do record.
     *
     * <p>Garante que a resposta não seja criada sem campos obrigatórios.
     * O único campo opcional é {@code fotoPerfilUrl}.
     */
    public UsuarioResponse {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(email, "email não pode ser nulo");
        Objects.requireNonNull(role, "role não pode ser nula");
        Objects.requireNonNull(aceitoTermosEm, "aceitoTermosEm não pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm não pode ser nulo");
        Objects.requireNonNull(atualizadoEm, "atualizadoEm não pode ser nulo");
    }

    public static UsuarioResponse de(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario não pode ser nulo");

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getAceitoTermosEm(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }
}