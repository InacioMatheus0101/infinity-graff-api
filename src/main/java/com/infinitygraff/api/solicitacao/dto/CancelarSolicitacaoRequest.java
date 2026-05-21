package com.infinitygraff.api.solicitacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para cancelamento de uma solicitação de arte.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CancelarSolicitacaoRequest {

    /**
     * Motivo obrigatório do cancelamento.
     */
    @NotBlank(message = "O motivo do cancelamento é obrigatório.")
    @Size(
            min = 20,
            max = 500,
            message = "O motivo deve ter entre 20 e 500 caracteres."
    )
    private String motivo;
}