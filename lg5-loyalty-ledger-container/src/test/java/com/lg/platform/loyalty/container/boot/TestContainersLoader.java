package com.lg.platform.loyalty.container.boot;

import com.lg5.spring.testcontainer.config.PostgresContainerCustomConfig;
import org.springframework.context.annotation.Import;

/**
 * Aggregates the Testcontainers configurations needed by data-access ITs.
 *
 * <p>Each {@code *ContainerCustomConfig} is gated by
 * {@code testcontainers.<name>.enabled} (RULE-013); the ITs flip the
 * relevant property via {@code @TestPropertySource}.
 */
@Import({
        PostgresContainerCustomConfig.class
})
public final class TestContainersLoader {
}
