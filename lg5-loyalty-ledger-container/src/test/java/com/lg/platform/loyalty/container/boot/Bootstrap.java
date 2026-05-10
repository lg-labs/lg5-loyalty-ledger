package com.lg.platform.loyalty.container.boot;

import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
import org.springframework.context.annotation.Import;

/**
 * Base class for data-access integration tests.
 *
 * <p>Runs with {@code WebEnvironment.NONE} + profiles {@code {test, local}}
 * (RULE-012) and imports {@link TestContainersLoader} so a Postgres testcontainer
 * is started when {@code testcontainers.postgres.enabled=true}.
 */
@Import(TestContainersLoader.class)
public abstract class Bootstrap extends Lg5TestBootPortNone {
}
