package com.lg.platform.loyalty.container.boot;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Base class for IT classes.
 *
 * <p>Runs with {@code WebEnvironment.NONE} + profiles {@code {test, local}}
 * (RULE-012) and imports {@link TestContainersLoader} so the relevant
 * testcontainers (Postgres, Kafka, Schema Registry) start when their gate
 * properties are flipped to {@code true} (RULE-013).
 *
 * <p>Also registers a {@link LoyaltyLedgerInputPort} fallback Mockito mock
 * via {@link DefaultMocks} (gated by
 * {@code @ConditionalOnMissingBean}) so that adding the
 * {@code OrderPaidKafkaListener} {@code @Component} (TASK-009) does not
 * break unrelated data-access ITs whose context now eagerly wires that
 * listener. Once TASK-011 ships the real
 * {@code LoyaltyLedgerHandler @Service}, the {@code @ConditionalOnMissingBean}
 * gate falls through and the mock is no longer registered (production
 * behaviour preserved).
 */
@Import({TestContainersLoader.class, Bootstrap.DefaultMocks.class})
public abstract class Bootstrap extends Lg5TestBootPortNone {

    @TestConfiguration
    public static class DefaultMocks {
        @Bean
        @ConditionalOnMissingBean
        public LoyaltyLedgerInputPort loyaltyLedgerInputPortFallbackMock() {
            return Mockito.mock(LoyaltyLedgerInputPort.class);
        }
    }
}
