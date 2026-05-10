package com.lg.platform.loyalty.acceptance.boot;

import com.lg5.spring.integration.test.boot.Lg5TestBoot;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Cucumber-Spring glue (TASK-018 / TASK-019, REQ-INFRA + REQ-001..REQ-015).
 *
 * <p>{@link CucumberContextConfiguration} on a single class binds
 * the entire Cucumber JVM to one Spring application context — so
 * every {@code @Given/@When/@Then} step definition class can simply
 * be {@code @Autowired} and pull beans straight from the live
 * context (NO {@code @SpringBootTest} on the steps; that would
 * spin up a second context and silently break wiring).
 *
 * <p>Extends {@link Lg5TestBoot} ({@code WebEnvironment.RANDOM_PORT}
 * + {@code @ActiveProfiles({"test","local"})} — RULE-012) rather
 * than {@code Lg5TestBootPortNone} because TASK-019 needs both:
 * <ul>
 *   <li>the message + handler write paths (autowire the
 *       {@code LoyaltyLedgerInputPort} or send Avro to Kafka), AND</li>
 *   <li>the REST read paths (REQ-009 {@code GET /balance},
 *       REQ-010 {@code GET /movements}, REQ-013 surface assertions
 *       on the absence of mutator HTTP verbs).</li>
 * </ul>
 * The {@code @LocalServerPort}-bound {@code port} field is exposed
 * by the parent and used by the {@code RestSetupHooks} Cucumber
 * {@code @Before} hook to construct the per-scenario RestAssured
 * request specification (the parent's JUnit {@code @BeforeEach}
 * does NOT fire under the Cucumber engine).
 *
 * <p>{@code @Import(TestContainersLoader.class)} brings in the
 * three gated containers (Postgres, Kafka+SR, Wiremock).
 *
 * <p><b>Why {@code @TestPropertySource} instead of relying on
 * {@code application-test.yaml}:</b> the
 * {@code lg5-loyalty-ledger-container} jar (a runtime dep of this
 * ATDD module) ships {@code config/application-test.yaml} on the
 * runtime classpath, and Spring Boot resolves
 * {@code classpath:/config/application-test.yaml} at higher
 * precedence than {@code classpath:/application-test.yaml} from
 * {@code src/test/resources/}. The container's file defaults every
 * {@code testcontainers.*.enabled} gate to {@code false}.
 * {@code @TestPropertySource} sits at the highest precedence tier
 * in Spring's {@code PropertySource} order (above all yaml files),
 * sidestepping the precedence trap. The blank
 * {@code spring.datasource.*} entries mirror the per-IT pattern in
 * the container module ITs: they nullify the prod
 * {@code application.yaml} hard-coded
 * {@code jdbc:postgresql://localhost:54322/...} so the
 * {@code @ServiceConnection} on the
 * {@code PostgresContainerCustomConfig} bean wins.
 *
 * <p><b>Scheduler is on at 200 ms.</b> TASK-019 includes scenarios
 * (REQ-011 / REQ-012 outbound traceability, REQ-015 Kafka
 * idempotency end-to-end) that drive the outbox publish path; the
 * scheduler MUST tick during scenario execution. The 200 ms
 * cadence matches the container-module Kafka publisher IT
 * ({@code CustomerBalanceUpdatedKafkaPublisherIT}) so the network /
 * cache-key shape is identical.
 */
@Import(TestContainersLoader.class)
@CucumberContextConfiguration
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "testcontainers.kafka.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password=",
        "scheduling.enabled=true",
        "loyalty-ledger-service.outbox-scheduler-fixed-rate=200",
        "loyalty-ledger-service.outbox-scheduler-initial-delay=200"
})
public class CucumberHooks extends Lg5TestBoot {
}
