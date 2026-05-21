package com.infinitygraff.api.solicitacao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de entrada para atribuição de prestador a uma solicitação de arte.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AtribuirPrestadorRequest {

    /**
     * Identificador do usuário PRESTADOR que será vinculado à solicitação.
     */
    @NotNull(message = "O prestador é obrigatório.")
    private UUID prestadorId;
}