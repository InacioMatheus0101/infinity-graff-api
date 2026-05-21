package com.infinitygraff.api.solicitacao.dto;

import com.infinitygraff.api.solicitacao.enums.TipoSolicitacaoArte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de entrada para criação de uma solicitação de arte.
 *
 * <p>Regras de negócio associadas:
 * <ul>
 *   <li>{@code clienteId} é obrigatório apenas quando a criação for feita por ADMIN ou GERENTE;</li>
 *   <li>CLIENTE cria sempre para si e não pode informar {@code clienteId};</li>
 *   <li>{@code observacoesInternas} pode ser informado apenas por ADMIN ou GERENTE;</li>
 *   <li>CLIENTE que enviar {@code observacoesInternas} preenchido deve receber erro 400.</li>
 * </ul>
 *
 * <p>As validações dependentes de role serão aplicadas no service.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CriarSolicitacaoArteRequest {

    /**
     * Cliente destinatário da solicitação.
     *
     * <p>Obrigatório apenas quando ADMIN ou GERENTE criarem a solicitação.
     * CLIENTE não pode preencher este campo.
     */
    private UUID clienteId;

    /**
     * Tipo principal da solicitação.
     */
    @NotNull(message = "O tipo da solicitação é obrigatório.")
    private TipoSolicitacaoArte tipo;

    /**
     * Título curto para identificação da solicitação.
     */
    @NotBlank(message = "O título da solicitação é obrigatório.")
    @Size(max = 150, message = "O título da solicitação deve ter no máximo 150 caracteres.")
    private String titulo;

    /**
     * Instruções informadas pelo cliente sobre como deseja a arte.
     */
    @NotBlank(message = "As instruções da solicitação são obrigatórias.")
    @Size(max = 5000, message = "As instruções devem ter no máximo 5000 caracteres.")
    private String instrucoes;

    /**
     * Medidas da arte em formato textual livre.
     *
     * <p>Exemplos: "30x40cm", "A4", "1080x1920px".
     */
    @Size(max = 100, message = "As medidas devem ter no máximo 100 caracteres.")
    private String medidas;

    /**
     * Observações internas da equipe.
     *
     * <p>Permitido apenas para ADMIN e GERENTE.
     * A restrição por role será validada no service.
     */
    private String observacoesInternas;
}