package com.infinitygraff.api.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para envio de uma nova mensagem no chat.
 *
 * @param conteudo texto da mensagem (não pode ser vazio e deve ter no máximo 5000 caracteres)
 */
public record EnviarMensagemRequest(

        @NotBlank(message = "O conteúdo da mensagem é obrigatório.")
        @Size(max = 5000, message = "A mensagem deve ter no máximo 5000 caracteres.")
        String conteudo

) {
}