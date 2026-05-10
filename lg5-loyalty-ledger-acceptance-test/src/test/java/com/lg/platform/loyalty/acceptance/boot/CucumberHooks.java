package com.lg.platform.loyalty.acceptance.boot;

import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;

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
 * coverage demands it; for the empty TASK-018 baseline the
 * lighter NONE-env context is sufficient and keeps Tomcat out of
 * the cache key.
 *
 * <p>{@code @Import(TestContainersLoader.class)} brings in the
 * three gated containers (Postgres, Kafka+SR, Wiremock).
 * {@code application-test.yaml} defaults Postgres + Kafka to
 * {@code true} (the {@code @SpringBootApplication} wires JPA
 * and Kafka beans eagerly — flipping them off would crash
 * Hibernate at context startup); Wiremock stays {@code false}
 * by default and individual scenarios opt into it via
 * {@code @TestPropertySource} on the runner when needed.
 */
@Import(TestContainersLoader.class)
@CucumberContextConfiguration
public class CucumberHooks extends Lg5TestBootPortNone {
}
