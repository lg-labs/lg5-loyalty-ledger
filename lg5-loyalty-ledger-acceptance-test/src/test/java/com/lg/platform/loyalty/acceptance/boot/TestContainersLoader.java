package com.lg.platform.loyalty.acceptance.boot;

import com.lg5.spring.testcontainer.config.ConfluentKafkaContainerCustomConfig;
import com.lg5.spring.testcontainer.config.PostgresContainerCustomConfig;
import org.springframework.context.annotation.Import;

/**
 * Aggregates every Testcontainers config the ATDD suite may need
 * (TASK-018, RULE-013).
 *
 * <p>Each {@code *ContainerCustomConfig} below is gated by its
 * own {@code testcontainers.<name>.enabled} flag, so simply
 * importing this loader does NOT start any container — the
 * scenario's {@code @TestPropertySource} (or
 * {@code application-test.yaml}) flips the relevant gate to
 * {@code true} when the scenario actually needs the dependency.
 *
 * <p>Mirrors the loader in {@code lg5-loyalty-ledger-container}'s
 * test tree (the M2/M3 ITs use the same pattern). The only
 * difference here is the inclusion of
 * {@link LoyaltyLedgerWiremockContainerCustomConfig} for
 * scenarios that need a stub HTTP backend (e.g. faking the
 * schema-registry {@code /subjects/.../versions} surface).
 *
 * <p>Caveat about {@code testcontainers.schema-registry.enabled}:
 * the framework's {@link ConfluentKafkaContainerCustomConfig}
 * registers BOTH the Kafka broker AND the schema-registry
 * sidecar under one and the same gate
 * ({@code testcontainers.kafka.enabled}). There is no separate
 * {@code testcontainers.schema-registry.enabled} property in this
 * framework SHA. Acceptance criteria worded against three
 * independent gates therefore collapse Postgres + (Kafka +
 * Schema Registry) + Wiremock into three logical groups — see the
 * M5 milestone report for the full discussion.
 */
@Import({
        PostgresContainerCustomConfig.class,
        ConfluentKafkaContainerCustomConfig.class,
        LoyaltyLedgerWiremockContainerCustomConfig.class
})
public final class TestContainersLoader {
}
