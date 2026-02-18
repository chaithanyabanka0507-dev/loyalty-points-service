package com.airline.loyalty.component.component;

import com.airline.loyalty.component.BaseTest;
import com.airline.loyalty.component.models.QuoteTestDTO;
import com.airline.loyalty.component.util.QuoteTestDataLoader;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith({
        VertxExtension.class,
        BaseTest.class
})

/**
 * Component-level integration tests for the Points Quote REST API.
 *
 * These tests deploy the {@link com.airline.loyalty.points.api.PointsQuoteVerticle}
 * with mocked external services (FX and Promo) using WireMock.
 *
 * The test suite verifies:
 * - Successful points calculation scenarios
 * - Validation failures
 * - HTTP status code handling (400, 404, 405, 415, 503)
 * - Health endpoint availability
 * - Content-Type enforcement
 *
 * Test cases are data-driven using {@link QuoteTestDataLoader}
 * and executed as parameterized tests for scalability and maintainability.
 */
class PointsQuoteComponentTest {


    /**
     * Verifies that the quote API behaves correctly for
     * all scenarios defined in TestData.json.
     *
     * Scenarios include:
     * - Valid requests
     * - Invalid inputs
     * - Boundary conditions
     * - Promo and tier calculations
     *
     * @param data       test case DTO loaded from JSON
     * @param vertx      Vert.x instance injected by extension
     * @param serverPort dynamically assigned HTTP server port
     * @param ctx        Vert.x test context for async completion
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void quote_api_behaves_correctly(
            QuoteTestDTO data,
            Vertx vertx,
            Integer serverPort,
            VertxTestContext ctx) {

        WebClient client = WebClient.create(vertx);

        client.post(serverPort, "localhost", "/v1/points/quote")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(data.request(), ar -> {

                    if (ar.failed()) {
                        ctx.failNow(ar.cause());
                        return;
                    }

                    if (data.expectedStatus() == 200) {
                        JsonObject body = ar.result().bodyAsJsonObject();

                        assertThat(body.getInteger("basePoints")).isNotNull();
                        assertThat(body.getInteger("totalPoints")).isGreaterThan(0);
                        assertThat(body.getDouble("effectiveFxRate")).isGreaterThan(0);
                        if(data.name().equalsIgnoreCase("points cap applied"))
                        {
                            assertThat(body.getInteger("totalPoints")).isEqualTo(50000);
                        }
                        else
                        {
                            assertThat(body.getInteger("totalPoints")).isLessThan(50000);
                        }

                    }
                    else {
                        if (data.expectedError() != null) {
                            JsonObject body = ar.result().bodyAsJsonObject();
                            assertThat(body.getString("error"))
                                    .isEqualTo(data.expectedError());
                        }
                    }

                    System.out.println("******** TEST CASE: " + data.name() + "********");
                    System.out.println("STATUS  : " + ar.result().statusCode());
                    System.out.println("RESPONSE: " + ar.result().bodyAsString());

                    assertThat(ar.result().statusCode())
                            .isEqualTo(data.expectedStatus());

                    ctx.completeNow();
                });
    }


    /**
     * Verifies that the health endpoint returns HTTP 200
     * and indicates service availability.
     */
    @Test
    void health_endpoint_should_return_service_up_status(
            Vertx vertx,
            Integer serverPort,
            VertxTestContext ctx) {

        WebClient client = WebClient.create(vertx);

        client.get(serverPort, "localhost", "/health")
                .send(ar -> {

                    if (ar.failed()) {
                        ctx.failNow(ar.cause());
                        return;
                    }

                    assertThat(ar.result().statusCode()).isEqualTo(200);

                    JsonObject body = ar.result().bodyAsJsonObject();
                    assertThat(body.getString("status")).isEqualTo("UP");

                    ctx.completeNow();
                });
    }

    /**
     * Ensures the API returns HTTP 404 when
     * an unknown endpoint is requested.
     */
    @Test
    void api_should_return_not_found_when_endpoint_does_not_exist(
            Vertx vertx,
            Integer serverPort,
            VertxTestContext ctx) {

        WebClient client = WebClient.create(vertx);

        client.get(serverPort, "localhost", "/v1/non-existing-route")
                .send(ar -> {

                    if (ar.failed()) {
                        ctx.failNow(ar.cause());
                        return;
                    }

                    assertThat(ar.result().statusCode()).isEqualTo(404);

                    ctx.completeNow();
                });
    }


    /**
     * Ensures the API returns HTTP 405 when
     * an unsupported HTTP method is used on
     * the quote endpoint.
     */
    @Test
    void api_should_reject_get_request_on_quote_endpoint(
            Vertx vertx,
            Integer serverPort,
            VertxTestContext ctx) {

        WebClient client = WebClient.create(vertx);

        client.get(serverPort, "localhost", "/v1/points/quote")
                .send(ar -> {

                    if (ar.failed()) {
                        ctx.failNow(ar.cause());
                        return;
                    }

                    assertThat(ar.result().statusCode()).isEqualTo(405);

                    ctx.completeNow();
                });
    }


    /**
     * Ensures the API returns HTTP 415 when
     * a request is sent without the required
     * application/json Content-Type header.
     */
    @Test
    void api_should_reject_request_without_json_content_type(
            Vertx vertx,
            Integer serverPort,
            VertxTestContext ctx) {

        WebClient client = WebClient.create(vertx);

        JsonObject body = new JsonObject()
                .put("fareAmount", 1000)
                .put("currency", "USD")
                .put("cabinClass", "ECONOMY")
                .put("customerTier", "SILVER");

        client.post(serverPort, "localhost", "/v1/points/quote")
                // intentionally not setting Content-Type header
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer(body.encode()), ar -> {

                    if (ar.failed()) {
                        ctx.failNow(ar.cause());
                        return;
                    }

                    assertThat(ar.result().statusCode()).isEqualTo(415);

                    ctx.completeNow();
                });
    }

    static Stream<QuoteTestDTO> testCases() {
        return QuoteTestDataLoader.loadQuoteTestCases();
    }





}



