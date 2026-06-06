package com.infinitygraff.api.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para marcar um conjunto de mensagens como lidas.
 *
 * @param ids lista de UUIDs das mensagens a serem marcadas (máximo de 100 IDs)
 */
public record MarcarLidasRequest(

        @NotEmpty(message = "A lista de IDs de mensagens não pode ser vazia.")
        @Size(max = 100, message = "É permitido marcar no máximo 100 mensagens por vez.")
        List<UUID> ids

) {
}