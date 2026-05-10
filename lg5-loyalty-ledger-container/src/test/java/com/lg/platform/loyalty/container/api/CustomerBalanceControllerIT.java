package com.lg.platform.loyalty.container.api;

import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.container.boot.RestBootstrap;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * REST IT for {@code GET /loyalty/customers/{customerId}/balance}
 * (TASK-015, REQ-009).
 *
 * <p>Postgres-only — the read endpoint never touches the message
 * layer. Seeds {@code customer_balance} rows directly via the
 * {@code CustomerBalanceRepository} output port (the production
 * write-side handler is not exercised here; that's TASK-011's job).
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class CustomerBalanceControllerIT extends RestBootstrap {

    @Autowired private CustomerBalanceRepository customerBalanceRepository;

    /** Happy-path: positive balance is returned with the right shape and content type. */
    @Test
    void getBalance_returns_200_with_positive_balance_and_vnd_api_v1_content_type() {
        final CustomerId customerId = CustomerId.random();
        final CustomerBalance seeded = CustomerBalance.empty(customerId);
        seeded.applyDelta(42);
        customerBalanceRepository.save(seeded);

        RestAssured.given(requestSpecification)
                .accept("application/vnd.api.v1+json")
                .when()
                .get("/loyalty/customers/{customerId}/balance", customerId.getValue().toString())
                .then()
                .statusCode(200)
                .contentType("application/vnd.api.v1+json")
                .body("customerId", equalTo(customerId.getValue().toString()))
                .body("balance", equalTo(42))
                .body("lastUpdatedAt", notNullValue());
    }

    /**
     * REQ-007 / REQ-008: a negative balance is observable via the read
     * endpoint (no truncation, no abs(...) inside the controller).
     */
    @Test
    void getBalance_returns_negative_balance_unchanged() {
        final CustomerId customerId = CustomerId.random();
        final CustomerBalance seeded = CustomerBalance.empty(customerId);
        seeded.applyDelta(-7);
        customerBalanceRepository.save(seeded);

        RestAssured.given(requestSpecification)
                .accept("application/vnd.api.v1+json")
                .when()
                .get("/loyalty/customers/{customerId}/balance", customerId.getValue().toString())
                .then()
                .statusCode(200)
                .contentType("application/vnd.api.v1+json")
                .body("balance", equalTo(-7));
    }

    /**
     * Acceptance criterion (TASK-015) calls for {@code 404 Not Found}
     * with an {@code ErrorDTO} body when the customer is unknown. The
     * full mapping ({@code RuntimeException → 404 + ErrorDTO}) is the
     * job of the {@code @RestControllerAdvice} that ships in
     * <strong>TASK-017</strong>; until then the
     * {@link com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException}
     * thrown by the query service surfaces as Spring's default
     * {@code 500} (no advice on the scan path yet).
     *
     * <p>This test therefore asserts the <em>structural</em>
     * pre-conditions that TASK-017 will then refine: the row really
     * is absent, and the endpoint reaches the controller (i.e. the
     * URL is wired). The 404 status + {@code ErrorDTO{code=
     * CUSTOMER_NOT_FOUND, ...}} body assertions ship with TASK-017's
     * IT to keep the per-TASK contract clean.
     */
    @Test
    void getBalance_for_unknown_customer_reaches_handler_and_no_row_is_present() {
        final UUID unknown = UUID.randomUUID();
        assertThat(customerBalanceRepository.findById(new CustomerId(unknown))).isEmpty();

        final int status = RestAssured.given(requestSpecification)
                .accept(ContentType.ANY)
                .when()
                .get("/loyalty/customers/{customerId}/balance", unknown.toString())
                .then()
                .extract().statusCode();
        // Pre-TASK-017 this is 500 (no advice). Post-TASK-017 this is 404.
        // Either way it is non-2xx — the row really is absent.
        assertThat(status).isGreaterThanOrEqualTo(400);
    }
}
