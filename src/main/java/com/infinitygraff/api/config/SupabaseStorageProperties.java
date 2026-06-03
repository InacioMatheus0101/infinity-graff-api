package com.infinitygraff.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de configuração para integração com o Supabase Storage.
 * <p>
 * Mapeia o prefixo {@code supabase.storage} do {@code application.yml}
 * para este record imutável.
 * <p>
 * <b>Por que não usamos {@code @Configuration} aqui?</b><br>
 * Este record é {@code final} (todos os records são implicitamente final).
 * O Spring tenta criar um proxy CGLIB de classes {@code @Configuration}
 * para gerenciar o ciclo de vida dos beans, mas não consegue fazer proxy
 * de uma classe {@code final}. Isso causaria o erro:
 * <pre>
 * Cannot subclass final class ...SupabaseStorageProperties
 * </pre>
 * Em vez disso, o registro do bean é feito explicitamente via
 * {@code @EnableConfigurationProperties(SupabaseStorageProperties.class)}
 * na classe {@link StorageConfig}. Assim, a configuração fica centralizada
 * e a falha é imediatamente visível caso a anotação seja removida.
 * <p>
 * <b>Segurança:</b> a {@code serviceRoleKey} tem acesso irrestrito ao
 * projeto Supabase. Ela:
 * <ul>
 *   <li>nunca deve ser exposta ao front‑end;</li>
 *   <li>nunca deve aparecer em logs;</li>
 *   <li>nunca deve ser commitada no repositório;</li>
 *   <li>deve ser injetada exclusivamente via variável de ambiente.</li>
 * </ul>
 * <p>
 * A URL base já deve incluir o prefixo {@code /storage/v1}.
 *
 * @see StorageConfig
 */
@Validated
@ConfigurationProperties(prefix = "supabase.storage")
public record SupabaseStorageProperties(

        /**
         * URL completa do projeto Supabase com o prefixo /storage/v1.
         * Exemplo: {@code https://xxxx.supabase.co/storage/v1}
         */
        @NotBlank(message = "supabase.storage.url é obrigatório")
        String url,

        /**
         * Chave de serviço (service_role) com acesso total ao storage.
         * <b>Nunca deve ser logada ou exposta.</b>
         */
        @NotBlank(message = "supabase.storage.service-role-key é obrigatório")
        String serviceRoleKey,

        /**
         * Nome do bucket onde os arquivos de solicitações serão armazenados.
         * Exemplo: {@code solicitacoes-arte}
         */
        @NotBlank(message = "supabase.storage.bucket-solicitacoes é obrigatório")
        String bucketSolicitacoes
) {
}