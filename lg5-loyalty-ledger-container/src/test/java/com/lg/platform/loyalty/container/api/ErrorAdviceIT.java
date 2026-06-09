package com.lg.platform.loyalty.container.api;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerQueryService;
import com.lg.platform.loyalty.boot.Bootstrap;
import com.lg.platform.loyalty.container.api.contract.OpenApiContractFilter;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

import static org.hamcrest.Matchers.*;

/**
 * Targeted IT for the two contract surfaces of
 * {@link com.lg.platform.loyalty.api.rest.LoyaltyLedgerExceptionAdvice} that
 * {@link CustomerBalanceControllerIT} cannot exercise end-to-end (TASK-017,
 * REQ-009 + REQ-015):
 *
 * <ul>
 * <li>{@code 400 INVALID_REQUEST} — malformed UUID in the path. The
 * router-level type coercion fails before the controller method is ever
 * invoked, so this needs no mock override.</li>
 * <li>{@code 500 INTERNAL} — unexpected {@code RuntimeException} from the read
 * port, induced via a {@code @Primary} Mockito override of
 * {@link LoyaltyLedgerQueryService}.</li>
 * </ul>
 *
 * <p>
 * The {@code 404 CUSTOMER_NOT_FOUND} surface is already pinned in
 * {@link CustomerBalanceControllerIT} via the unknown-customer test, which
 * exercises the real {@code LoyaltyLedgerQueryServiceImpl} and the real
 * {@code CustomerBalanceNotFoundException} — that's the most honest mapping
 * test we can write, so we don't duplicate it with a mock here.
 *
 * <p>
 * Each assertion pins ALL three contract dimensions — status,
 * {@code Content-Type: application/vnd.api.v1+json} (RULE-006), and
 * {@link com.lg.platform.loyalty.api.dto.ErrorDTO} body shape — so any future
 * regression in any of the three surfaces here.
 */
class ErrorAdviceIT extends Bootstrap {

	/**
	 * The {@code @Primary} override wins over the production
	 * {@code @Service}-registered {@code LoyaltyLedgerQueryServiceImpl}. Default
	 * Mockito behaviour is no-op / null returns; the 500 test arms it with
	 * {@code Mockito.doThrow(...)} explicitly.
	 *
	 * <p>
	 * The 400 test never reaches the controller method (path- variable coercion
	 * fails first) so the mock is irrelevant for it. There is no 404 test in this
	 * class — see the class Javadoc.
	 */
	@TestConfiguration
	static class MockOverrides {
		@Bean
		@Primary
		LoyaltyLedgerQueryService loyaltyLedgerQueryServiceMock() {
			return Mockito.mock(LoyaltyLedgerQueryService.class);
		}
	}

	@Autowired
	private LoyaltyLedgerQueryService queryServiceMock;

	@Test
	void malformedUuidInPath_returns400_invalidRequest() {
		RestAssured.given(this.requestSpecification).filter(OpenApiContractFilter.openApiValidator())
				.accept("application/vnd.api.v1+json").when().get("/loyalty/customers/not-a-uuid/balance").then()
				.statusCode(400).contentType("application/vnd.api.v1+json").body("code", equalTo("INVALID_REQUEST"))
				.body("message", notNullValue()).body("traceId", nullValue());
	}

	@Test
	void unexpectedRuntimeException_returns500_internalWithTraceId() {
		Mockito.doThrow(new RuntimeException("boom — induced for 500-path IT")).when(this.queryServiceMock)
				.getBalance(Mockito.any());

		RestAssured.given(this.requestSpecification).filter(OpenApiContractFilter.openApiValidator())
				.accept("application/vnd.api.v1+json").when()
				.get("/loyalty/customers/{id}/balance", UUID.randomUUID().toString()).then().statusCode(500)
				.contentType("application/vnd.api.v1+json").body("code", equalTo("INTERNAL"))
				.body("message", notNullValue())
				// traceId must be a UUID v4-ish string (we generate
				// it via UUID.randomUUID().toString()).
				.body("traceId", matchesRegex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
	}
}
