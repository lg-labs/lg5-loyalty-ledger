package com.lg.platform.loyalty.acceptance.boot;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ConfigurationParameters;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform Suite that boots Cucumber (TASK-018).
 *
 * <p>Named {@code AcceptanceTestRunnerIT} so Maven Failsafe
 * (configured at the parent-pom level with
 * {@code -Dit.test=**\/*IT.java}) picks it up in the existing
 * "Integration tests (Testcontainers)" CI step — no new CI job
 * needed to wire the ATDD layer in.
 *
 * <p>{@link SelectClasspathResource} points at
 * {@code src/test/resources/features/}; with zero feature files
 * the suite reports {@code 0 / 0 / 0} (pass / fail / skipped) and
 * exits green — that is precisely the TASK-018 acceptance
 * criterion ("empty scenarios run pass = 0 / fail = 0 / skipped =
 * 0"). When TASK-019 lands its scenarios under that folder, the
 * same runner picks them up automatically.
 *
 * <p>Glue scans {@code com.lg.platform.loyalty.acceptance} so any
 * step-definition class under that package — alongside the
 * {@link CucumberHooks} {@code @CucumberContextConfiguration} —
 * is discovered. Reports land under {@code target/atdd-reports/}
 * (gitignored) to mirror the food-ordering reference layout.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
        @ConfigurationParameter(
                key = Constants.PLUGIN_PROPERTY_NAME,
                value = "pretty, "
                        + "json:target/atdd-reports/cucumber.json, "
                        + "html:target/atdd-reports/cucumber-reports.html, "
                        + "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"),
        @ConfigurationParameter(
                key = Constants.GLUE_PROPERTY_NAME,
                value = "com.lg.platform.loyalty.acceptance")
})
public class AcceptanceTestRunnerIT {
}
