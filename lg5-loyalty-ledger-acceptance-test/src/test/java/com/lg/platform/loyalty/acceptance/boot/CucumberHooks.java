package com.lg.platform.loyalty.acceptance.boot;

import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Cucumber-Spring glue (TASK-018, REQ-INFRA).
 *
 * <p>{@link CucumberContextConfiguration} on a single class binds
 * the entire Cucumber JVM to one Spring application context — so
 * EVERY {@code @Given/@When/@Then} step definition class can simply
 * be {@code @Autowired} and pull beans straight from the live
 * context (NO {@code @SpringBootTest} on the steps; that would
 * spin up a second context and silently break wiring).
 *
 * <p>Extends {@link Lg5TestBootPortNone} (web env NONE,
 * {@code @ActiveProfiles({"test","local"})} — RULE-012) instead of
 * {@code Lg5TestBoot} because the ATDD suite drives the system
 * primarily via Kafka + JPA + the application-service input port.
 * REST-side scenarios that need HTTP can switch this to
 * {@code Lg5TestBoot} in TASK-019 if the equivalence-class
 * coverage demands it.
 *
 * <p>{@code @Import(TestContainersLoader.class)} brings in the
 * three gated containers (Postgres, Kafka+SR, Wiremock).
 *
 * <p><b>Why {@code @TestPropertySource} instead of relying on
 * {@code application-test.yaml}:</b> the
 * {@code lg5-loyalty-ledger-container} jar (a runtime dep of this
 * ATDD module) ships {@code config/application-test.yaml} on the
 * runtime classpath, and Spring Boot's profile-aware loader
 * resolves {@code classpath:/config/application-test.yaml} with
 * higher precedence than the test-classpath
 * {@code classpath:/application-test.yaml}. The container's file
 * defaults every {@code testcontainers.*.enabled} gate to
 * {@code false} (because each container IT flips them on
 * per-class via its own {@code @TestPropertySource}). Without an
 * equivalent override here the ATDD context would skip the
 * Postgres + Kafka container beans, fall through to the
 * production datasource at {@code localhost:54322}, and crash
 * Liquibase at startup with {@code Connection refused}.
 *
 * <p>{@code @TestPropertySource} sits at the highest precedence
 * tier in Spring's {@code PropertySource} order (above all yaml
 * files), so it sidesteps the precedence trap entirely. The
 * blank {@code spring.datasource.*} entries mirror the per-IT
 * pattern in {@code CustomerMovementsControllerIT} and
 * {@code OrderPaidKafkaListenerIT}: they nullify the prod
 * {@code application.yaml} hard-coded
 * {@code jdbc:postgresql://localhost:54322/...} so the
 * {@code @ServiceConnection} on the
 * {@code PostgresContainerCustomConfig} bean wins.
 */
@Import(TestContainersLoader.class)
@CucumberContextConfiguration
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "testcontainers.kafka.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password=",
        // RULE-012: scheduler off in test profile (ATDD scenarios
        // drive the outbox flush manually).
        "scheduling.enabled=false"
})
public class CucumberHooks extends Lg5TestBootPortNone {
}
