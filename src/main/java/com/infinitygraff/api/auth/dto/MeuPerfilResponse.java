package com.infinitygraff.api.auth.dto;

import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.model.Usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de resposta com os dados do perfil interno do usuário autenticado.
 *
 * Este DTO nunca expõe informações sensíveis.
 * Não inclui senha, tokens, dados de sessão do Supabase ou campo deletadoEm.
 *
 * O usuário já foi autenticado pelo Supabase Auth.
 * Esta resposta representa apenas o perfil interno salvo na tabela usuarios.
 */
public record MeuPerfilResponse(

        UUID id,

        String nome,

        String email,

        Role role,

        boolean ativo,

        OffsetDateTime aceitoTermosEm,

        OffsetDateTime criadoEm,

        OffsetDateTime atualizadoEm
) {

    public static MeuPerfilResponse de(Usuario usuario) {
        return new MeuPerfilResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.isAtivo(),
                usuario.getAceitoTermosEm(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }
}