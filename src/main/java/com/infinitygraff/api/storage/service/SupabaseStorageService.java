        package com.infinitygraff.api.storage.service;

        import com.fasterxml.jackson.databind.JsonNode;
        import com.fasterxml.jackson.databind.ObjectMapper;
        import com.infinitygraff.api.config.SupabaseStorageProperties;
        import com.infinitygraff.api.storage.dto.UrlAssinadaResponse;
        import com.infinitygraff.api.storage.exception.StorageIntegrationException;
        import lombok.extern.slf4j.Slf4j;
        import okhttp3.MediaType;
        import okhttp3.OkHttpClient;
        import okhttp3.Request;
        import okhttp3.RequestBody;
        import okhttp3.Response;
        import okhttp3.ResponseBody;
        import org.springframework.beans.factory.annotation.Qualifier;
        import org.springframework.stereotype.Service;

        import java.io.IOException;

        /**
         * Serviço de integração com o Supabase Storage.
         * <p>
         * Responsável por upload, remoção e geração de URLs assinadas temporárias.
         * Comunica-se diretamente com a API REST do Supabase utilizando OkHttp3.
         * <p>
         * <b>Credenciais:</b> a {@code service_role key} é fornecida via
         * {@link SupabaseStorageProperties} e enviada nos headers
         * {@code Authorization: Bearer ...} e {@code apikey}.
         * Ela nunca é logada ou exposta em respostas de erro.
         * <p>
         * <b>URL base:</b> a propriedade {@code url} já deve conter o prefixo
         * {@code /storage/v1} (ex: {@code https://xxxx.supabase.co/storage/v1}).
         * <p>
         * <b>Qualidade de serviço:</b> utiliza o {@link OkHttpClient} configurado
         * com timeouts adequados para upload de até 20 MB.
         */
        @Slf4j
        @Service
        public class SupabaseStorageService {

        private static final String HEADER_AUTHORIZATION = "Authorization";
        private static final String HEADER_APIKEY        = "apikey";
        private static final String PREFIXO_BEARER       = "Bearer ";

        /** Campo JSON retornado pelo Supabase na criação de URL assinada. */
        private static final String CAMPO_SIGNED_URL     = "signedURL";

        private final OkHttpClient httpClient;
        private final SupabaseStorageProperties properties;
        private final ObjectMapper objectMapper;

        /**
         * Construtor com injeção de dependências.
         *
         * @param httpClient cliente HTTP configurado para storage
         * @param properties configurações do Supabase
         * @param objectMapper serializador JSON para parsing da resposta de URL assinada
         */
        public SupabaseStorageService(
                @Qualifier("storageHttpClient") OkHttpClient httpClient,
                SupabaseStorageProperties properties,
                ObjectMapper objectMapper
        ) {
                this.httpClient   = httpClient;
                this.properties   = properties;
                this.objectMapper = objectMapper;
        }

        /**
         * Faz upload de um arquivo para o bucket configurado.
         * <p>
         * A URL é montada como {@code {url}/object/{bucket}/{storagePath}}.
         *
         * @param storagePath caminho completo no bucket
         *                    (ex: {@code solicitacoes/{id}/REFERENCIA/original/abc.webp})
         * @param conteudo    bytes do arquivo
         * @param contentType MIME type do arquivo (ex: {@code image/webp})
         * @throws StorageIntegrationException se a requisição falhar
         */
        public void upload(String storagePath, byte[] conteudo, String contentType) {
                String url = "%s/object/%s/%s"
                        .formatted(properties.url(), properties.bucketSolicitacoes(), storagePath);

                RequestBody body = RequestBody.create(conteudo, MediaType.parse(contentType));

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header(HEADER_AUTHORIZATION, PREFIXO_BEARER + properties.serviceRoleKey())
                        .header(HEADER_APIKEY, properties.serviceRoleKey())
                        .header("Content-Type", contentType)
                        .build();

                executarRequest(request, "upload", storagePath);
        }

        /**
         * Remove um arquivo do storage.
         * <p>
         * Será utilizado futuramente por um job de limpeza após a remoção
         * lógica (soft delete) no banco de dados.
         *
         * @param storagePath caminho do arquivo no bucket
         * @throws StorageIntegrationException se a remoção falhar
         */
        public void remover(String storagePath) {
                String url = "%s/object/%s/%s"
                        .formatted(properties.url(), properties.bucketSolicitacoes(), storagePath);

                Request request = new Request.Builder()
                        .url(url)
                        .delete()
                        .header(HEADER_AUTHORIZATION, PREFIXO_BEARER + properties.serviceRoleKey())
                        .header(HEADER_APIKEY, properties.serviceRoleKey())
                        .build();

                executarRequest(request, "remoção", storagePath);
        }

        /**
         * Gera uma URL assinada temporária para acesso a um arquivo privado.
         * <p>
         * A URL expira automaticamente após o tempo configurado. Não deve ser
         * armazenada no banco — deve ser gerada sob demanda quando o front‑end
         * precisar acessar o arquivo.
         *
         * @param storagePath       caminho do arquivo no bucket
         * @param expiracaoSegundos tempo de validade em segundos (ex: 300 = 5 min)
         * @return DTO com a URL assinada e o tempo de expiração
         * @throws StorageIntegrationException se a geração falhar
         */
       public UrlAssinadaResponse gerarUrlAssinada(String storagePath, int expiracaoSegundos) {
    String url = "%s/object/sign/%s/%s"
            .formatted(properties.url(), properties.bucketSolicitacoes(), storagePath);

    String bodyJson = "{\"expiresIn\":%d}".formatted(expiracaoSegundos);

    RequestBody body = RequestBody.create(
            bodyJson.getBytes(),
            MediaType.parse("application/json")
    );

    Request request = new Request.Builder()
            .url(url)
            .post(body)
            .header(HEADER_AUTHORIZATION, PREFIXO_BEARER + properties.serviceRoleKey())
            .header(HEADER_APIKEY, properties.serviceRoleKey())
            .header("Content-Type", "application/json")
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
            throw new StorageIntegrationException(
                    "Falha ao gerar URL assinada para: %s — HTTP %d"
                            .formatted(storagePath, response.code())
            );
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new StorageIntegrationException(
                    "Resposta vazia ao gerar URL assinada para: " + storagePath
            );
        }

        String jsonResposta = responseBody.string();
        JsonNode node = objectMapper.readTree(jsonResposta);

        String signedUrl = node.path(CAMPO_SIGNED_URL).asText(null);
        if (signedUrl == null || signedUrl.isBlank()) {
            throw new StorageIntegrationException(
                    "Campo signedURL ausente na resposta do Supabase para: " + storagePath
            );
        }

        // Constrói a URL absoluta (a API retorna caminho relativo, ex: /object/sign/...)
        String urlAbsoluta = properties.url() + signedUrl;

        log.debug("URL assinada gerada para {} — expira em {}s", storagePath, expiracaoSegundos);
        return new UrlAssinadaResponse(urlAbsoluta, expiracaoSegundos);

    } catch (IOException e) {
        log.error("Erro ao gerar URL assinada para {}: {}", storagePath, e.getMessage());
        throw new StorageIntegrationException(
                "Erro de comunicação ao gerar URL assinada: " + e.getMessage(), e
        );
    }
}

        /**
         * Executa uma requisição HTTP e trata a resposta.
         * <p>
         * Para requisições bem‑sucedidas, apenas loga em DEBUG.
         * Para falhas, lança {@link StorageIntegrationException}.
         *
         * @param request     requisição HTTP configurada
         * @param operacao    nome da operação (upload, remoção) para logs
         * @param storagePath caminho do arquivo (para logs)
         * @throws StorageIntegrationException se a resposta não for bem‑sucedida
         */
        private void executarRequest(Request request, String operacao, String storagePath) {
                try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                        log.error("Falha no {} do arquivo {}: HTTP {}",
                                operacao, storagePath, response.code());
                        throw new StorageIntegrationException(
                                "Falha no %s do arquivo no storage: HTTP %d"
                                        .formatted(operacao, response.code())
                        );
                }
                log.debug("{} concluído com sucesso: {}", operacao, storagePath);
                } catch (IOException e) {
                log.error("Erro de comunicação no {} de {}: {}", operacao, storagePath, e.getMessage());
                throw new StorageIntegrationException(
                        "Erro de comunicação com o storage: " + e.getMessage(), e
                );
                }
        }
        }