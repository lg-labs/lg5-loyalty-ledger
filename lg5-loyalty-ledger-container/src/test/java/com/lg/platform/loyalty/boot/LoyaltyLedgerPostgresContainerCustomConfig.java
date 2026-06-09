package com.lg.platform.loyalty.boot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.time.Duration;

import static com.lg5.spring.testcontainer.util.Constant.POSTGRES_17_0;
import static com.lg5.spring.testcontainer.util.Constant.POSTGRES_NETWORK_ALIAS;
import static com.lg5.spring.testcontainer.util.Constant.network;

@TestConfiguration
@ConditionalOnProperty(name = "testcontainers.postgres.enabled", havingValue = "true", matchIfMissing = true)
public class LoyaltyLedgerPostgresContainerCustomConfig {

    @Value("${docker.container.reuse:false}")
    private boolean dockerContainerReuse;

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_17_0))
                .withNetwork(network)
                .withNetworkAliases(POSTGRES_NETWORK_ALIAS)
                .withReuse(dockerContainerReuse)
                .withUrlParam("binaryTransfer", "true")
                .withUrlParam("reWriteBatchedInserts", "true")
                .withUrlParam("stringtype", "unspecified");

        postgres.start();
        waitForJdbc(postgres);
        return postgres;
    }

    private void waitForJdbc(final PostgreSQLContainer<?> postgres) {
        final long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        RuntimeException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                if (connection.isValid(1)) {
                    return;
                }
            } catch (final RuntimeException ex) {
                lastFailure = ex;
            } catch (final Exception ex) {
                lastFailure = new IllegalStateException(ex);
            }

            try {
                Thread.sleep(250);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for Postgres testcontainer", ex);
            }
        }
        throw new IllegalStateException("Postgres testcontainer did not accept JDBC connections in time", lastFailure);
    }
}
