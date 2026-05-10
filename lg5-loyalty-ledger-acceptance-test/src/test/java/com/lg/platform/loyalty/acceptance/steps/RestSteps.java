package com.lg.platform.loyalty.acceptance.steps;

import com.lg.platform.loyalty.acceptance.support.World;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REST step definitions for REQ-009 (GET /balance), REQ-010
 * (GET /movements paged), REQ-013 (append-only HTTP surface)
 * and REQ-016 (vendor JSON content type) (TASK-019).
 *
 * <p>RestAssured request specification is built once per scenario
 * by {@link com.lg.platform.loyalty.acceptance.hooks.RestSetupHooks}
 * and stashed in {@link World}; this class reads it back and
 * stashes the {@code Response} so {@code Then} steps can assert
 * on status, headers, and body without re-issuing the call.
 *
 * <p><b>NOT annotated {@code @Component}</b> — Cucumber-Spring
 * 7.x rejects glue classes carrying that stereotype. Instances
 * are created by Cucumber-Spring's own factory and Spring still
 * autowires the constructor. RULE-006 is verified by the
 * {@code Then the response Content-Type starts with the v1
 * vendor JSON} step which asserts the controller's
 * {@code produces=...} propagates through both the happy path
 * and the {@code RestControllerAdvice} error path.
 */
@Slf4j
public class RestSteps {

    private static final String VENDOR_JSON = "application/vnd.api.v1+json";

    private final World world;

    public RestSteps(final World world) {
        this.world = world;
    }

    @When("I GET the customer balance")
    public void getBalance() {
        world.setLastResponse(RestAssured.given(world.getRequestSpec())
                .when()
                .get("/loyalty/customers/{id}/balance", world.customerOrNew().getValue()));
    }

    @When("I GET the balance for an unknown customer")
    public void getBalanceUnknown() {
        // Guarantee a fresh UUID — DO NOT use World.customerOrNew()
        // because the scenario explicitly wants an id with NO prior
        // movements / no CustomerBalance row.
        final UUID unknown = UUID.randomUUID();
        world.setLastResponse(RestAssured.given(world.getRequestSpec())
                .when()
                .get("/loyalty/customers/{id}/balance", unknown));
    }

    @When("I GET the movements page {int} size {int}")
    public void getMovementsPaged(final int page, final int size) {
        world.setLastResponse(RestAssured.given(world.getRequestSpec())
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get("/loyalty/customers/{id}/movements", world.customerOrNew().getValue()));
    }

    /**
     * REQ-013 surface assertion: PUT/DELETE/PATCH on the read-only
     * controller surface SHOULD respond {@code 405 Method Not
     * Allowed} (Spring's default {@code DispatcherServlet}
     * behaviour for an unmapped HTTP method on a registered URI).
     * The customer-id path is irrelevant — the rejection is
     * verb-level.
     */
    @When("I {string} the balance endpoint")
    public void mutateBalanceEndpoint(final String httpVerb) {
        final var req = RestAssured.given(world.getRequestSpec());
        final String path = "/loyalty/customers/" + world.customerOrNew().getValue() + "/balance";
        world.setLastResponse(switch (httpVerb.toUpperCase()) {
            case "PUT" -> req.when().put(path);
            case "POST" -> req.when().post(path);
            case "DELETE" -> req.when().delete(path);
            case "PATCH" -> req.when().patch(path);
            default -> throw new IllegalArgumentException("Unsupported verb: " + httpVerb);
        });
    }

    @When("I {string} the movements endpoint")
    public void mutateMovementsEndpoint(final String httpVerb) {
        final var req = RestAssured.given(world.getRequestSpec());
        final String path = "/loyalty/customers/" + world.customerOrNew().getValue() + "/movements";
        world.setLastResponse(switch (httpVerb.toUpperCase()) {
            case "PUT" -> req.when().put(path);
            case "POST" -> req.when().post(path);
            case "DELETE" -> req.when().delete(path);
            case "PATCH" -> req.when().patch(path);
            default -> throw new IllegalArgumentException("Unsupported verb: " + httpVerb);
        });
    }

    @Then("the response status is {int}")
    public void responseStatus(final int code) {
        assertThat(world.getLastResponse().getStatusCode()).isEqualTo(code);
    }

    /**
     * RULE-006 / REQ-016. RestAssured normalises the header to
     * lowercase but preserves casing inside the value; we compare
     * by {@code startsWith} so an optional {@code ;charset=UTF-8}
     * suffix added by Spring does not break the assertion.
     * Spring Boot 3.4 with stock Jackson does NOT append the
     * charset on a {@code MediaType.parseMediaType} produced
     * surface, but the framework reserves the right to do so.
     */
    @Then("the response Content-Type starts with the v1 vendor JSON")
    public void responseContentTypeIsVendorJson() {
        final String ct = world.getLastResponse().getContentType();
        assertThat(ct)
                .as("Content-Type")
                .isNotNull()
                .startsWith(VENDOR_JSON);
    }

    @Then("the response JSON balance is {long}")
    public void responseJsonBalance(final long expected) {
        assertThat(world.getLastResponse().jsonPath().getLong("balance")).isEqualTo(expected);
    }

    @Then("the response JSON has {int} movements and totalElements {long}")
    public void responseMovementsAndTotal(final int movements, final long total) {
        assertThat(world.getLastResponse().jsonPath().getList("movements")).hasSize(movements);
        assertThat(world.getLastResponse().jsonPath().getLong("totalElements")).isEqualTo(total);
    }

    @Then("the response JSON page is {int} and size is {int}")
    public void responsePageAndSize(final int page, final int size) {
        assertThat(world.getLastResponse().jsonPath().getInt("page")).isEqualTo(page);
        assertThat(world.getLastResponse().jsonPath().getInt("size")).isEqualTo(size);
    }

    @Then("the response JSON error code is {string}")
    public void responseErrorCode(final String expected) {
        assertThat(world.getLastResponse().jsonPath().getString("code")).isEqualTo(expected);
    }
}
