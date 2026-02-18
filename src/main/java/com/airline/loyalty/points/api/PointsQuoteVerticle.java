package com.airline.loyalty.points.api;

import com.airline.loyalty.points.model.QuoteRequest;
import com.airline.loyalty.points.service.FxRateClient;
import com.airline.loyalty.points.service.PointsCalculator;
import com.airline.loyalty.points.service.PromoClient;
import com.airline.loyalty.points.validation.RequestValidator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * REST API verticle responsible for handling loyalty points quote requests.
 *
 * Exposes:
 *   POST /v1/points/quote  - Calculates loyalty points
 *   GET  /health           - Health check endpoint
 *
 * This verticle:
 * - Initializes external service clients (FX and Promo)
 * - Validates incoming requests
 * - Applies business calculation logic
 * - Returns structured JSON responses with proper HTTP status codes
 */
public class PointsQuoteVerticle extends AbstractVerticle {

    private static final Logger logger =
            LoggerFactory.getLogger(PointsQuoteVerticle.class);


    /**
     * Initializes configuration, sets up routing, and starts the HTTP server.
     *
     * Reads configuration for FX and Promo services,
     * creates required WebClient instances,
     * configures routes and error handling,
     * and deploys the HTTP server on a dynamic port.
     *
     * @param startPromise promise used to signal deployment success or failure
     */
    @Override
    public void start(Promise<Void> startPromise) {

        try {

            // Read Structured Configuration
            JsonObject fxConfig = config().getJsonObject("fx", new JsonObject());
            JsonObject promoConfig = config().getJsonObject("promo", new JsonObject());

            String fxBaseUrl = fxConfig.getString("baseUrl", "http://localhost:8081");
            int fxMaxRetries = fxConfig.getInteger("maxRetries", 2);
            String fxPath = fxConfig.getString("path", "/fx");

            String promoBaseUrl = promoConfig.getString("baseUrl", "http://localhost:8082");
            int promoTimeoutMs = promoConfig.getInteger("timeoutMs", 500);
            int promoExpiryWarningDays = promoConfig.getInteger("expiryWarningDays", 3);



            logger.info("FX Config | baseUrl={} | maxRetries={}", fxBaseUrl, fxMaxRetries);
            logger.info("Promo Config | baseUrl={} | timeoutMs={} | expiryWarningDays={}",
                    promoBaseUrl, promoTimeoutMs, promoExpiryWarningDays);


            // Create WebClients Safely
            WebClient fxClient = createWebClient(fxBaseUrl);
            WebClient promoClient = createWebClient(promoBaseUrl);

            // Initialize Services
            PointsCalculator calculator =
                    new PointsCalculator(
                            new FxRateClient(fxClient,fxPath, fxMaxRetries),
                            new PromoClient(promoClient, promoTimeoutMs, promoExpiryWarningDays)
                    );


            // Router Setup
            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());

            // Global Body Handler
            router.route().handler(BodyHandler.create());

            // Health Endpoint
            router.get("/health").handler(ctx ->
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("status", "UP").encode())
            );


            // Content-Type Enforcement
            router.route("/v1/points/quote")
                    .method(HttpMethod.POST)
                    .handler(BodyHandler.create())
                    .handler(ctx -> {
                        if (!ctx.request().isExpectMultipart() &&
                                (ctx.parsedHeaders().contentType() == null ||
                                        !ctx.parsedHeaders().contentType().value().contains("application/json"))) {

                            sendError(ctx, 415, "Unsupported Media Type");
                            return;
                        }

                        handleQuoteRequest(ctx, calculator);
                    });

            // POST Endpoint
            router.post("/v1/points/quote")
                    .handler(ctx -> handleQuoteRequest(ctx, calculator));

            // Method Not Allowed
            router.route("/v1/points/quote")
                    .handler(ctx -> sendError(ctx, 405, "Method Not Allowed"));


            // Not Found (Fallback)
            router.route().last().handler(ctx ->
                    sendError(ctx, 404, "Endpoint not found")
            );

            // Start HTTP Server
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(0, ar -> {
                        if (ar.succeeded()) {
                            int port = ar.result().actualPort();
                            logger.info("Points Quote API started on port {}", port);

                            vertx.sharedData()
                                    .getLocalMap("test-data")
                                    .put("http.port", port);

                            startPromise.complete();
                        } else {
                            logger.error("Failed to start HTTP server", ar.cause());
                            startPromise.fail(ar.cause());
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed to initialize PointsQuoteVerticle", e);
            startPromise.fail(e);
        }
    }


    /**
     * Processes a loyalty points quote request.
     *
     * Validates the incoming JSON payload,
     * delegates calculation to PointsCalculator,
     * and returns a JSON response.
     *
     * Returns:
     * 200 - Successful calculation
     * 400 - Validation error
     * 503 - External service failure
     * 500 - Unexpected internal error
     *
     * @param ctx routing context
     * @param calculator points calculation service
     */
    // Request Handler
    private void handleQuoteRequest(RoutingContext ctx,
                                    PointsCalculator calculator) {

        try {
            String contentType = ctx.request().getHeader("Content-Type");
            JsonObject body = ctx.body().asJsonObject();

            if (body == null) {
                sendError(ctx, 400, "Request body is required");
                return;
            }

            QuoteRequest request = body.mapTo(QuoteRequest.class);
            RequestValidator.validate(request);

            calculator.calculate(request)
                    .onSuccess(res -> {
                        logger.info(
                                "Points calculated | fare={} {} | cabin={} | tier={} | totalPoints={} | warnings={}",
                                request.fareAmount(),
                                request.currency(),
                                request.cabinClass(),
                                request.customerTier(),
                                res.totalPoints(),
                                res.warnings()
                        );

                        ctx.response()
                                .setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(res));
                    })
                    .onFailure(err -> {
                        logger.error("Calculation failed", err);

                        if (err instanceof IllegalArgumentException) {
                            sendError(ctx, 400, err.getMessage());
                        } else {
                            sendError(ctx, 503, "Service temporarily unavailable");
                        }
                    });

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error: {}", e.getMessage());
            sendError(ctx, 400, e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error while processing request", e);
            sendError(ctx, 500, "Internal server error");
        }
    }


    /**
     * Creates a WebClient instance from a base URL.
     *
     * Validates the URI,
     * extracts host and port,
     * applies default ports if missing,
     * and enables SSL for HTTPS.
     *
     * @param baseUrl service base URL
     * @return configured WebClient
     */
    // Safe WebClient Creator
    private WebClient createWebClient(String baseUrl) {

        URI uri = URI.create(baseUrl);

        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Invalid service URL: " + baseUrl);
        }

        int port = uri.getPort();

        if (port == -1) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }

        WebClientOptions options = new WebClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(port)
                .setSsl("https".equalsIgnoreCase(uri.getScheme()));

        return WebClient.create(vertx, options);
    }

    /**
     * Sends a standardized JSON error response.
     *
     * Response format:
     * {
     *   "status": <code>,
     *   "error": "<message>"
     * }
     *
     * @param ctx routing context
     * @param status HTTP status code
     * @param message error message
     */
    // Standard Error Response
    private void sendError(RoutingContext ctx,
                           int status,
                           String message) {

        JsonObject error = new JsonObject()
                .put("status", status)
                .put("error", message);

        ctx.response()
                .setStatusCode(status)
                .putHeader("Content-Type", "application/json")
                .end(error.encode());
    }
}
