package com.lg.platform.loyalty.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the TASK-018 smoke feature
 * ({@code 000_smoke.feature}).
 *
 * <p>These two steps are intentionally devoid of business logic —
 * their sole purpose is to give the Cucumber engine SOMETHING to
 * discover so the JUnit Platform Suite (default
 * {@code failIfNoTests=true}) does not raise
 * {@code NoTestsDiscoveredException} on an empty suite. Both
 * steps will be deleted alongside {@code 000_smoke.feature} once
 * TASK-019 lands its first real scenario.
 *
 * <p>The class is package-private and Cucumber-only; it is NOT a
 * Spring component (no {@code @Component}, no {@code @Autowired}).
 * Cucumber's Spring integration ({@link io.cucumber.spring.CucumberContextConfiguration}
 * on {@code CucumberHooks}) will instantiate it per-scenario via
 * its glue scanner.
 */
public class SmokeSteps {

    @Given("the ATDD harness is wired")
    public void the_atdd_harness_is_wired() {
        // No-op — context startup proves the wiring; this step's
        // body just gives the @Given a binding target.
    }

    @Then("the trivial step passes")
    public void the_trivial_step_passes() {
        // Pin a non-trivial assertion so a regression that
        // accidentally short-circuits Cucumber's step execution
        // surfaces as a failure rather than a silent skip.
        assertThat(true).isTrue();
    }
}
