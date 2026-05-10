package com.lg.platform.loyalty.container.api;

import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.container.boot.RestBootstrap;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
     * REQ-009 + TASK-017: an unknown customer surfaces as
     * {@code 404 CUSTOMER_NOT_FOUND} via the
     * {@link com.lg.platform.loyalty.api.rest.LoyaltyLedgerExceptionAdvice}
     * (TASK-017 added the advice on the scan path; previously this
     * was Spring's default {@code 500}).
     *
     * <p>This used to be a structural-only check (the row really is
     * absent + the endpoint reaches the controller); now that the
     * advice is in place the full contract — status, content-type,
     * and {@code ErrorDTO} body — is pinned here, completing the
     * fix-up note from {@code 7cc71c5}.
     */
    @Test
    void getBalance_for_unknown_customer_returns_404_customerNotFound_errorDto() {
        final UUID unknown = UUID.randomUUID();
        assertThat(customerBalanceRepository.findById(new CustomerId(unknown))).isEmpty();

        RestAssured.given(requestSpecification)
                .accept("application/vnd.api.v1+json")
                .when()
                .get("/loyalty/customers/{customerId}/balance", unknown.toString())
                .then()
                .statusCode(404)
                .contentType("application/vnd.api.v1+json")
                .body("code", equalTo("CUSTOMER_NOT_FOUND"))
                .body("message", notNullValue())
                .body("traceId", nullValue());
    }
}
