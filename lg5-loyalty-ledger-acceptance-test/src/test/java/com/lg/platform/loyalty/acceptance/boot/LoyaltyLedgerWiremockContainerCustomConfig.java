package com.lg.platform.loyalty.acceptance.boot;

import com.lg5.spring.testcontainer.config.WiremockContainerCustomConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Concrete Wiremock Testcontainers config for the loyalty-ledger
 * acceptance-test module (TASK-018).
 *
 * <p>The framework's {@link WiremockContainerCustomConfig} is
 * {@code abstract} on purpose — every consumer service is expected
 * to supply a thin concrete subclass so the {@code @ConditionalOnProperty}
 * gate can be re-targeted (RULE-013 — opt-in containers; default
 * {@code matchIfMissing = false} for this service so an unrelated
 * IT in another module never accidentally spins up Wiremock).
 *
 * <p>Wiremock is used here primarily as a stand-in for the
 * Confluent Schema Registry's {@code /subjects/.../versions}
 * endpoints in scenarios where a full Schema Registry container is
 * overkill. The default Wiremock bind port (7070) is shared with
 * the food-ordering reference service; we keep the same default
 * because no two services run their ATDD on the same host at the
 * same time, and switching the default would diverge from the
 * canonical pattern without benefit.
 */
@TestConfiguration
@ConditionalOnProperty(name = "testcontainers.wiremock.enabled", havingValue = "true", matchIfMissing = false)
public class LoyaltyLedgerWiremockContainerCustomConfig extends WiremockContainerCustomConfig {
}
