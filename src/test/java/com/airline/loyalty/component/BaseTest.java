package com.airline.loyalty.component;

import com.airline.loyalty.points.api.PointsQuoteVerticle;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.extension.*;

/**
 * Base JUnit 5 test extension used for component-level API testing.
 *
 * This class:
 * - Starts a Vert.x instance
 * - Boots the PointsQuoteVerticle with test configuration
 * - Spins up a WireMock server to simulate external FX and Promo services
 * - Injects the dynamically allocated HTTP server port into test classes
 *
 * It ensures:
 * - External dependencies are isolated
 * - Tests are deterministic and repeatable
 * - The application is started only once per test suite
 */
public class BaseTest implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private Vertx vertx;
    private WireMockServer wireMock;
    private int serverPort;

    /**
     * Initializes the test environment before all test cases:
     * - Starts Vert.x
     * - Starts WireMock on a dynamic port
     * - Configures external service stubs
     * - Deploys the PointsQuoteVerticle with test configuration
     * - Captures the dynamically assigned HTTP server port
     */
    @Override
    public void beforeAll(ExtensionContext context) {

        vertx = Vertx.vertx();

        wireMock = new WireMockServer(
                WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMock.start();

        // Stub external services
        ExternalServiceStubs.stubFxUsd(wireMock);
        ExternalServiceStubs.stubPromoSummer25(wireMock);


        JsonObject config = new JsonObject()
                .put("fx", new JsonObject()
                        .put("baseUrl", "http://localhost:" + wireMock.port())
                        .put("maxRetries", 2)
                )
                .put("promo", new JsonObject()
                        .put("baseUrl", "http://localhost:" + wireMock.port())
                        .put("timeoutMs", 500)
                        .put("expiryWarningDays", 3)
                );

        vertx.deployVerticle(
                new PointsQuoteVerticle(),
                new DeploymentOptions().setConfig(config)
        ).toCompletionStage().toCompletableFuture().join();

        var map = vertx.sharedData().getLocalMap("test-data");

        long deadline = System.currentTimeMillis() + 5000;

        while (map.get("http.port") == null) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Timed out waiting for HTTP server port");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }

        serverPort = (int) map.get("http.port");

        if (serverPort <= 0) {
            throw new IllegalStateException("HTTP server port was not initialised");
        }
    }

    /**
     * Cleans up test resources after all test cases:
     * - Stops WireMock
     * - Closes Vert.x instance
     * - Clears shared test data
     */
    @Override
    public void afterAll(ExtensionContext context) {
        if (wireMock != null) {
            wireMock.stop();
        }
        if (vertx != null) {
            vertx.sharedData().getLocalMap("test-data").clear();
            vertx.close();
        }
    }

    /**
     * Determines whether this extension can resolve the requested parameter.
     * Currently supports injection of the HTTP server port (Integer).
     */
    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return pc.getParameter().getType() == Integer.class;
    }

    /**
     * Provides the dynamically allocated HTTP server port
     * to test classes that request it.
     */
    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return serverPort;
    }
}
