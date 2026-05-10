package com.lg.platform.loyalty.acceptance.hooks;

import com.lg.platform.loyalty.acceptance.support.World;
import io.cucumber.java.Before;
import io.restassured.builder.RequestSpecBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cucumber {@code @Before} hook that materialises the per-scenario
 * RestAssured request specification (TASK-019).
 *
 * <p>{@code Lg5TestBoot} (the framework parent of
 * {@link com.lg.platform.loyalty.acceptance.boot.CucumberHooks})
 * builds its own {@code requestSpecification} inside a JUnit
 * {@code @BeforeEach} method. JUnit lifecycle callbacks do NOT
 * fire under the Cucumber engine — Cucumber discovers tests via
 * its own {@code TestEngine} and bypasses Jupiter's
 * {@code BeforeEachCallback} chain — so that field would remain
 * {@code null} when a step definition tries to use it.
 *
 * <p>This hook closes the gap by:
 * <ol>
 *   <li>Reading the random server port published by Spring Boot's
 *       {@code @SpringBootTest(webEnvironment=RANDOM_PORT)} into
 *       the {@code local.server.port} property; the
 *       {@code @LocalServerPort}-bound field on
 *       {@code Lg5TestBoot} is package-private and not visible
 *       across packages, hence the {@code @Value} read here.</li>
 *   <li>Building a {@link io.restassured.specification.RequestSpecification}
 *       with the port + the v1 vendor JSON content type required
 *       by RULE-006 / REQ-016. Stashed in
 *       {@link World#setRequestSpec} so REST step-defs can
 *       {@code given(world.getRequestSpec())} without rebuilding.</li>
 * </ol>
 *
 * <p>Order is left at the default ({@code 1000}) — no other hook
 * depends on the request spec being built first; the sole consumer
 * is REST step-def {@code When} blocks which run strictly after
 * all hooks.
 */
@Component
public class RestSetupHooks {

    private final World world;

    @Value("${local.server.port}")
    private int port;

    public RestSetupHooks(final World world) {
        this.world = world;
    }

    @Before
    public void buildRestAssuredSpec() {
        world.setRequestSpec(new RequestSpecBuilder()
                .setPort(port)
                .setBaseUri("http://localhost")
                // Servers under test produce application/vnd.api.v1+json
                // (RULE-006); RestAssured's default Accept header
                // (* / *) already accepts it, so we deliberately do
                // NOT set Accept here — that lets a Then step assert
                // the response Content-Type without the request side
                // having to match it.
                .build());
    }
}
