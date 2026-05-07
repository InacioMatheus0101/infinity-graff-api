package com.infinitygraff.api.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Wrapper padrão para payloads paginados.
 *
 * <p>Este DTO deve ser usado dentro de {@code ApiResponse}, por exemplo:
 * {@code ApiResponse<PageResponse<UsuarioResumoResponse>>}.
 *
 * <p>Regras:
 * <ul>
 *   <li>{@code conteudo} nunca deve ser {@code null}; quando vazio, retorna lista vazia;</li>
 *   <li>{@code conteudo} não pode conter itens {@code null};</li>
 *   <li>{@code conteudo} é copiado para uma lista imutável;</li>
 *   <li>{@code pagina}, {@code totalElementos} e {@code totalPaginas} não podem ser negativos;</li>
 *   <li>{@code tamanhoPagina} deve ser maior que zero;</li>
 *   <li>{@code totalPaginas} pode ser zero quando não houver resultados;</li>
 *   <li>não contém timestamp, mensagem ou path — isso pertence ao {@code ApiResponse};</li>
 *   <li>controllers não devem expor entidades diretamente; preferir DTOs.</li>
 * </ul>
 *
 * @param conteudo lista de itens da página
 * @param pagina número da página atual, base 0
 * @param tamanhoPagina quantidade configurada de itens por página
 * @param totalElementos total de elementos encontrados
 * @param totalPaginas total de páginas disponíveis
 * @param ultima indica se esta é a última página
 * @param <T> tipo dos itens da lista, preferencialmente um DTO de response
 */
public record PageResponse<T>(

        List<T> conteudo,

        int pagina,

        int tamanhoPagina,

        long totalElementos,

        int totalPaginas,

        boolean ultima
) {

    /**
     * Construtor compacto do record.
     *
     * <p>Garante que a resposta paginada não seja criada com estado inválido.
     */
    public PageResponse {
        conteudo = conteudo == null
                ? List.of()
                : List.copyOf(conteudo);

        if (conteudo.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("conteudo não pode conter itens nulos");
        }

        if (pagina < 0) {
            throw new IllegalArgumentException("pagina não pode ser negativa");
        }

        if (tamanhoPagina < 1) {
            throw new IllegalArgumentException("tamanhoPagina deve ser maior que zero");
        }

        if (totalElementos < 0) {
            throw new IllegalArgumentException("totalElementos não pode ser negativo");
        }

        if (totalPaginas < 0) {
            throw new IllegalArgumentException("totalPaginas não pode ser negativo");
        }
    }

    /**
     * Cria um {@code PageResponse} a partir de um {@link Page} já contendo DTOs.
     *
     * <p>Usar quando o conteúdo da página já foi convertido para DTO antes
     * de chegar nesta factory.
     */
    public static <T> PageResponse<T> de(Page<T> page) {
        Objects.requireNonNull(page, "page não pode ser nulo");

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Cria um {@code PageResponse} a partir de uma {@link Page} de entidades
     * ou outro tipo interno, aplicando um mapper para converter cada item em DTO.
     *
     * <p>Esse é o método recomendado quando o repository retorna entidades JPA.
     * Assim evitamos expor entidades diretamente nos controllers.
     *
     * <p>O mapper não deve retornar {@code null}. Caso retorne, a criação do
     * {@code PageResponse} falhará com erro claro no construtor compacto.
     */
    public static <S, T> PageResponse<T> de(
            Page<S> page,
            Function<S, T> mapper
    ) {
        Objects.requireNonNull(page, "page não pode ser nulo");
        Objects.requireNonNull(mapper, "mapper não pode ser nulo");

        Page<T> pageMapeada = page.map(mapper);

        return de(pageMapeada);
    }
}