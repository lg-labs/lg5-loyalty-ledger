package com.lg.platform.loyalty.container.boot;

import com.lg5.spring.testcontainer.config.ConfluentKafkaContainerCustomConfig;
import com.lg5.spring.testcontainer.config.PostgresContainerCustomConfig;
import org.springframework.context.annotation.Import;

/**
 * Aggregates the Testcontainers configurations needed by ITs.
 *
 * <p>Each {@code *ContainerCustomConfig} is gated by
 * {@code testcontainers.<name>.enabled} (RULE-013); the ITs flip the
 * relevant property via {@code @TestPropertySource}. Including a config
 * here whose gate is {@code false} is a no-op (the {@code
 * @ConditionalOnProperty} prevents bean registration), so this loader
 * is safe to share across data-access ITs (Postgres only) and Kafka
 * listener / producer ITs (Postgres + Kafka + Schema Registry).
 */
@Import({
        PostgresContainerCustomConfig.class,
        ConfluentKafkaContainerCustomConfig.class
})
public final class TestContainersLoader {
}
