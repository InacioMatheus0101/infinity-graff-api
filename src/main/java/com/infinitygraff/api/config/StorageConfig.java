package com.infinitygraff.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.imageio.ImageIO;
import java.time.Duration;

/**
 * Configuração central do módulo de storage: cliente HTTP e registro de plugins de imagem.
 * <p>
 * <b>Responsabilidades:</b>
 * <ul>
 *   <li>Criar e expor um bean nomeado {@code storageHttpClient} do OkHttp3
 *       com timeouts adequados para upload de arquivos de até 20 MB.</li>
 *   <li>Registrar explicitamente {@link SupabaseStorageProperties} como um
 *       bean gerenciado pelo Spring, via
 *       {@code @EnableConfigurationProperties(SupabaseStorageProperties.class)}.
 *       Isso é necessário porque a classe de propriedades é um {@code record}
 *       (final) e não pode ser anotada com {@code @Configuration}.</li>
 *   <li>Forçar o registro dos plugins de leitura/escrita de imagem (ex: WebP)
 *       através de {@link ImageIO#scanForPlugins()} no momento adequado do
 *       ciclo de vida do Spring Boot, evitando problemas de carregamento tardio
 *       de service providers.</li>
 * </ul>
 * <p>
 * <b>Por que {@code @EnableConfigurationProperties} aqui e não na classe principal?</b><br>
 * Centralizar a configuração do módulo storage neste arquivo torna explícita
 * a dependência do {@code SupabaseStorageProperties}. Se alguém remover a
 * anotação, o {@code SupabaseStorageService} não conseguirá injetar as
 * propriedades e a aplicação falhará ao iniciar — um erro visível e fácil
 * de diagnosticar.
 * <p>
 * As credenciais de acesso (service_role key) não são configuradas aqui —
 * elas são injetadas diretamente no serviço via {@link SupabaseStorageProperties}.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class StorageConfig {

    /**
     * Registra os plugins de imagem (WebP, JPEG, PNG etc.) assim que o contexto
     * do Spring estiver pronto. Isso garante que o ClassLoader consiga localizar
     * os arquivos de serviço (META-INF/services) dentro dos JARs.
     */
    @PostConstruct
    public void registrarPluginsImagem() {
        ImageIO.scanForPlugins();
        log.info("Plugins ImageIO registrados com sucesso.");
    }

    /**
     * Cliente HTTP com timeouts generosos para operações de storage.
     * <ul>
     *   <li>{@code connectTimeout}: 15 segundos — tempo para estabelecer conexão.</li>
     *   <li>{@code readTimeout}: 60 segundos — tempo para receber a resposta.</li>
     *   <li>{@code writeTimeout}: 60 segundos — tempo para enviar o arquivo.</li>
     * </ul>
     *
     * @return OkHttpClient configurado para uso no {@code SupabaseStorageService}
     */
    @Bean
    public OkHttpClient storageHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }
}