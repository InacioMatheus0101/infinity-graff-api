package com.infinitygraff.api;

import com.infinitygraff.api.test.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de carregamento completo do contexto Spring da aplicação.
 *
 * <p>Herda {@link BaseIntegrationTest} para:
 * <ul>
 *   <li>ativar o profile {@code test};</li>
 *   <li>usar PostgreSQL real via Testcontainers;</li>
 *   <li>sobrescrever dinamicamente as propriedades de datasource.</li>
 * </ul>
 */
@SpringBootTest
class InfinityGraffApiApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }
}