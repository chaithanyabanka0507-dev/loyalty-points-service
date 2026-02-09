package com.airline.loyalty.component;

import com.airline.loyalty.points.api.PointsQuoteVerticle;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.extension.*;

public class BaseTest implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private Vertx vertx;
    private WireMockServer wireMock;
    private int serverPort;

    @Override
    public void beforeAll(ExtensionContext context) {

        vertx = Vertx.vertx();

        wireMock = new WireMockServer(
                WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMock.start();

        ExternalServiceStubs.stubFxUsd(wireMock);
        ExternalServiceStubs.stubPromoSummer25(wireMock);

        JsonObject config = new JsonObject()
                .put("fx.base.url", "http://localhost:" + wireMock.port())
                .put("promo.base.url", "http://localhost:" + wireMock.port());

        vertx.deployVerticle(
                new PointsQuoteVerticle(),
                new DeploymentOptions().setConfig(config)
        ).toCompletionStage().toCompletableFuture().join();

        var map = vertx.sharedData().getLocalMap("test-data");

        long deadline = System.currentTimeMillis() + 5000; // 5s timeout
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

    // 🔽 Allows tests to receive serverPort
    @Override
    public boolean supportsParameter(
            ParameterContext pc,
            ExtensionContext ec) {
        return pc.getParameter().getType() == Integer.class;
    }

    @Override
    public Object resolveParameter(
            ParameterContext pc,
            ExtensionContext ec) {
        return serverPort;
    }


}
