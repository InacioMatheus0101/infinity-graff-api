package com.infinitygraff.api.common.dto;

import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * DTO resumido de usuário reutilizável em respostas da aplicação.
 *
 * <p>Evita expor a entidade {@link Usuario} diretamente e fornece
 * apenas os dados necessários para identificar um usuário em respostas
 * de outros módulos, como solicitações, histórico, arquivos e chat.
 */
@Getter
@AllArgsConstructor
public class UsuarioResumoResponse {

    private UUID id;
    private String nome;
    private String email;
    private Role role;

    /**
     * Converte um usuário em sua representação resumida.
     *
     * @param usuario usuário a ser resumido
     * @return DTO resumido ou {@code null} quando o usuário informado for nulo
     */
    public static UsuarioResumoResponse from(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole()
        );
    }
}