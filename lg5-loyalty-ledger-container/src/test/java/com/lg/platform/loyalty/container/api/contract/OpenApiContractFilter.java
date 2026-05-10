package com.lg.platform.loyalty.container.api.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.filter.Filter;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RestAssured {@link Filter} that contract-tests every REST IT
 * response against {@code docs/api/openapi.yaml} (TASK-005,
 * mini-feature 002-api-specifications).
 *
 * <p>Resolves the spec path by walking up from the JVM's working
 * directory until it finds {@code docs/api/openapi.yaml}. This works
 * whether {@code mvn} is invoked from the {@code container} module
 * (working dir = {@code lg5-loyalty-ledger-container/}) or from the
 * repo root (working dir = {@code lg5-loyalty-ledger/}). The walk
 * is bounded to 6 parents — the deepest legitimate IT layout puts
 * the working dir 1 level below the repo root, so 6 leaves a wide
 * safety margin without introducing a runaway loop.
 *
 * <p>The filter is constructed once on first access and cached in a
 * {@code static final} field — building the validator parses the
 * spec, which is non-trivial work to redo per request.
 *
 * <p>Wiring:
 * <pre>{@code
 *   RestAssured.given(requestSpecification)
 *       .filter(OpenApiContractFilter.openApiValidator())
 *       ...
 * }</pre>
 *
 * <p>If a response does not match the spec (wrong status, wrong
 * content type, missing/extra/typed-wrong field, missing required
 * field, …), the filter throws and the surrounding test fails with
 * a clear, location-pointing message — that is the drift signal
 * we want.
 *
 * <p>Note: a hidden 4th surface — {@code 405 METHOD_NOT_ALLOWED} —
 * is handled by the same advice and IS in the spec; current ITs do
 * not exercise it explicitly, so the filter has no opportunity to
 * validate it. That's a coverage gap on the IT side, not a spec
 * bug. Future ITs that POST/PUT a GET-only path will validate the
 * 405 path too, for free.
 */
public final class OpenApiContractFilter {

    private static final String SPEC_RELATIVE_PATH = "docs/api/openapi.yaml";
    private static final int MAX_PARENT_WALK = 6;

    private static final Filter INSTANCE = build();

    private OpenApiContractFilter() {
        // utility
    }

    public static Filter openApiValidator() {
        return INSTANCE;
    }

    private static Filter build() {
        final Path spec = locateSpec();
        try {
            // sanity: file is readable; helps surface a clearer
            // error than the validator's own (which would say
            // "could not parse spec" without telling you which path
            // it tried).
            if (!Files.isReadable(spec)) {
                throw new IllegalStateException("OpenAPI spec is not readable at " + spec.toAbsolutePath());
            }
        } catch (final SecurityException ex) {
            throw new IllegalStateException("Cannot read OpenAPI spec at " + spec.toAbsolutePath(), ex);
        }
        final OpenApiInteractionValidator validator =
                OpenApiInteractionValidator.createForSpecificationUrl(spec.toUri().toString())
                        .withLevelResolver(LevelResolver.create()
                                // Ignore EVERY request-side validation key.
                                // Rationale: the ITs deliberately send
                                // malformed inputs (e.g. ErrorAdviceIT
                                // sends a non-UUID customerId to exercise
                                // the 400 surface). The whole point of
                                // those tests is the SERVER's response
                                // shape; a request-side validator that
                                // pre-fails such requests would defeat
                                // the test. We pin only the response
                                // contract — that is the drift signal
                                // we care about.
                                .withLevel("validation.request", ValidationReport.Level.IGNORE)
                                .build())
                        .build();
        return new OpenApiValidationFilter(validator);
    }

    private static Path locateSpec() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i <= MAX_PARENT_WALK; i++) {
            final Path candidate = dir.resolve(SPEC_RELATIVE_PATH);
            if (Files.exists(candidate)) {
                return candidate;
            }
            final Path parent = dir.getParent();
            if (parent == null) {
                break;
            }
            dir = parent;
        }
        throw new IllegalStateException(
                "Could not locate " + SPEC_RELATIVE_PATH
                        + " by walking up from " + Path.of("").toAbsolutePath()
                        + " (tried " + (MAX_PARENT_WALK + 1) + " levels)."
                        + " Run mvn from the container module or the repo root.");
    }
}
