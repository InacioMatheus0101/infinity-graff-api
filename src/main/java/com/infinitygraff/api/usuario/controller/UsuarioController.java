package com.infinitygraff.api.usuario.controller;

import com.infinitygraff.api.common.response.ApiResponse;
import com.infinitygraff.api.common.response.PageResponse;
import com.infinitygraff.api.usuario.dto.AtualizarStatusRequest;
import com.infinitygraff.api.usuario.dto.UsuarioResponse;
import com.infinitygraff.api.usuario.dto.UsuarioResumoResponse;
import com.infinitygraff.api.usuario.enums.Role;
import com.infinitygraff.api.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

/**
 * Controller responsável pela gestão de usuários internos da plataforma.
 *
 * <p>O Supabase Auth cuida de identidade, login, senha e sessão.
 * Este controller lida apenas com o perfil interno salvo na tabela {@code usuarios}.
 *
 * <p>Regras de permissão são aplicadas no {@code UsuarioService}.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private static final String PAGINA_PADRAO = "0";
    private static final String TAMANHO_PADRAO = "20";
    private static final int TAMANHO_MAXIMO = 100;

    private static final String ORDENACAO_PADRAO = "criadoEm,desc";

    /**
     * Campos permitidos para ordenação.
     *
     * <p>Apenas um campo de ordenação é suportado nesta fase.
     * Formato aceito: {@code campo,direcao}.
     * Exemplo: {@code nome,asc} ou {@code criadoEm,desc}.
     */
    private static final String ORDENACAO_REGEX =
            "^(nome|email|role|ativo|criadoEm|atualizadoEm)(,([aA][sS][cC]|[dD][eE][sS][cC]))?$";

    private final UsuarioService usuarioService;

    /**
     * Lista usuários com paginação, ordenação e filtros opcionais.
     *
     * <p>ADMIN visualiza todos os usuários não deletados.
     * GERENTE visualiza apenas CLIENTE e PRESTADOR.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResumoResponse>>> listar(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String nome,

            @RequestParam(defaultValue = PAGINA_PADRAO)
            @PositiveOrZero(message = "Página não pode ser negativa")
            int page,

            @RequestParam(defaultValue = TAMANHO_PADRAO)
            @Min(value = 1, message = "Tamanho da página deve ser maior que zero")
            @Max(value = TAMANHO_MAXIMO, message = "Tamanho da página não pode ser maior que 100")
            int size,

            @RequestParam(defaultValue = ORDENACAO_PADRAO)
            @Pattern(
                    regexp = ORDENACAO_REGEX,
                    message = "Ordenação inválida. Use o formato campo,direcao com um dos campos permitidos: nome, email, role, ativo, criadoEm ou atualizadoEm"
            )
            String sort,

            HttpServletRequest httpRequest
    ) {
        Pageable pageable = criarPageable(page, size, sort);

        PageResponse<UsuarioResumoResponse> response = usuarioService.listar(
                role,
                ativo,
                nome,
                pageable
        );

        ApiResponse<PageResponse<UsuarioResumoResponse>> body = ApiResponse.sucesso(
                "Usuários carregados com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Busca os detalhes de um usuário por ID.
     *
     * <p>ADMIN pode visualizar qualquer usuário.
     * GERENTE pode visualizar CLIENTE e PRESTADOR.
     * CLIENTE e PRESTADOR só podem visualizar o próprio perfil.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> buscarPorId(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = usuarioService.buscarPorId(id);

        ApiResponse<UsuarioResponse> body = ApiResponse.sucesso(
                "Usuário carregado com sucesso",
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Ativa ou desativa um usuário.
     *
     * <p>ADMIN pode alterar usuários conforme regras do service.
     * GERENTE pode alterar apenas CLIENTE e PRESTADOR.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UsuarioResponse>> atualizarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        UsuarioResponse response = usuarioService.atualizarStatus(id, request);

        String mensagem = Boolean.TRUE.equals(request.ativo())
                ? "Usuário ativado com sucesso"
                : "Usuário desativado com sucesso";

        ApiResponse<UsuarioResponse> body = ApiResponse.sucesso(
                mensagem,
                response,
                httpRequest
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Aplica soft delete em um usuário.
     *
     * <p>Não remove fisicamente o registro do banco.
     * Retorna {@code 204 No Content}, portanto não usa {@code ApiResponse}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> aplicarSoftDelete(
            @PathVariable UUID id
    ) {
        usuarioService.aplicarSoftDelete(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable criarPageable(int page, int size, String sort) {
        Ordenacao ordenacao = interpretarOrdenacao(sort);

        return PageRequest.of(
                page,
                size,
                Sort.by(ordenacao.direcao(), ordenacao.campo())
        );
    }

    /**
     * Interpreta o parâmetro de ordenação no formato {@code campo,direcao}.
     *
     * <p>Apenas um campo de ordenação é suportado nesta fase.
     * A validação do formato é feita por {@code @Pattern} no parâmetro {@code sort}.
     */
    private Ordenacao interpretarOrdenacao(String sort) {
        String valor = sort == null || sort.isBlank()
                ? ORDENACAO_PADRAO
                : sort.trim();

        String[] partes = valor.split(",");

        String campo = partes[0].trim();

        Sort.Direction direcao = Sort.Direction.DESC;

        if (partes.length == 2) {
            String direcaoInformada = partes[1].trim().toLowerCase(Locale.ROOT);

            direcao = "asc".equals(direcaoInformada)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
        }

        return new Ordenacao(campo, direcao);
    }

    private record Ordenacao(
            String campo,
            Sort.Direction direcao
    ) {
    }
}