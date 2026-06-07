package com.lg.platform.loyalty.boot;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg5.spring.integration.test.boot.Lg5TestBoot;
import org.springframework.context.annotation.Import;

/**
 * Base class for REST-side ITs (TASK-015 / TASK-016 / TASK-017).
 *
 * <p>
 * Same shape as {@link Bootstrap} but extending {@link Lg5TestBoot}
 * (RANDOM_PORT + RestAssured request spec) instead of
 * {@code Lg5TestBootPortNone} — these ITs hit real HTTP endpoints via the
 * embedded Tomcat. RULE-012 ({@code @ActiveProfiles({test, local})}) is
 * inherited from {@code Lg5TestBoot}.
 *
 * <p>
 * {@link TestContainersLoader} brings in the Postgres
 * {@code *ContainerCustomConfig}; the Kafka one is harmless when its gate
 * property is {@code false} (RULE-013), so REST ITs simply do not flip
 * {@code testcontainers.kafka.enabled}.
 *
 * <p>
 * The {@link Bootstrap.DefaultMocks} fallback for
 * {@link LoyaltyLedgerInputPort} is reused so any REST IT that does NOT need
 * the real write-side handler (e.g. the read-only TASK-015 IT) inherits a
 * Mockito mock by default, keeping the Spring TestContext cache key small and
 * stable.
 */
@Import({TestContainersLoader.class})
public abstract class Bootstrap extends Lg5TestBoot {
}
