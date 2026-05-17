package com.infinitygraff.api.test.integration;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Classe base para testes de integração que precisam de PostgreSQL real.
 *
 * <p>Usa Testcontainers para subir um banco PostgreSQL descartável durante
 * a execução dos testes.
 *
 * <p>O container é iniciado uma única vez por JVM de testes e reutilizado
 * por todas as subclasses. Esse padrão evita conflitos entre:
 * <ul>
 *   <li>cache de ApplicationContext do Spring;</li>
 *   <li>datasource reutilizado entre classes;</li>
 *   <li>encerramento prematuro do container em suítes completas.</li>
 * </ul>
 *
 * <p>As propriedades do datasource são injetadas dinamicamente no contexto Spring,
 * sobrescrevendo os valores de fallback definidos no {@code application-test.yml}.
 *
 * <p>As migrations Flyway continuam sendo aplicadas normalmente no banco do container.
 *
 * <p><b>Isolamento de dados:</b> o container é compartilhado entre testes que usam
 * esta base. Subclasses que inserem ou alteram dados devem usar {@code @Transactional}
 * para rollback automático quando aplicável, ou limpar/resetar o estado explicitamente
 * entre os testes.
 */
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("infinity_graff_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    /**
     * Registra dinamicamente as propriedades do datasource usadas pelos testes.
     *
     * <p>O Spring passa a usar o PostgreSQL do Testcontainers em vez do datasource
     * de fallback configurado no {@code application-test.yml}.
     */
    @DynamicPropertySource
    static void registrarPropriedadesDoContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }
}