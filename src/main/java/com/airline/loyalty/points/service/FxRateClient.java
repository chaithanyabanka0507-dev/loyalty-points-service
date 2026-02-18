package com.airline.loyalty.points.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client responsible for retrieving foreign exchange (FX) rates
 * from an external FX service.
 *
 * This client:
 * - Calls the configured FX endpoint
 * - Validates the HTTP response and JSON payload
 * - Retries failed requests up to a configured maximum
 * - Fails gracefully if the service is unavailable
 *
 * Expected FX service response format:
 * {
 *   "rate": <positive decimal number>
 * }
 *
 * If the FX service returns invalid data or remains unavailable
 * after the configured retry attempts, the returned Future fails.
 */
public class FxRateClient {

    private static final Logger logger = LoggerFactory.getLogger(FxRateClient.class);

    private final WebClient client;
    private final int maxRetries;
    private final String path;

    /**
     * Creates a new FX rate client.
     *
     * @param client the Vert.x WebClient used for HTTP communication
     * @param path the FX endpoint path (e.g. "/fx")
     * @param maxRetries maximum number of retry attempts on failure
     */
    public FxRateClient(WebClient client, String path, int maxRetries) {
        this.client = client;
        this.path = path;
        this.maxRetries = maxRetries;
    }

    /**
     * Retrieves the FX rate for a given currency.
     *
     * The method:
     * 1. Calls the external FX endpoint
     * 2. Validates the HTTP status code
     * 3. Validates that the JSON response contains a positive "rate"
     * 4. Retries if the request fails
     *
     * @param currency the ISO currency code (e.g. "USD")
     * @return a Future containing the FX rate if successful,
     *         or a failed Future if unavailable
     */
    public Future<Double> getFxRate(String currency) {
        return attempt(currency, 0);
    }

    /**
     * Attempts to retrieve the FX rate, retrying on failure.
     *
     * @param currency the currency code
     * @param retry current retry attempt number
     * @return a Future containing the FX rate or failure
     */
    private Future<Double> attempt(String currency, int retry) {

        logger.info("Calling FX endpoint: {}", path);
        return client.get(path)
                .addQueryParam("currency", currency)
                .send()
                .compose(resp -> {

                    if (resp.statusCode() != 200) {
                        logger.warn("FX service returned non-200 status: {}", resp.statusCode());
                        return Future.failedFuture("FX service error");
                    }

                    JsonObject body = resp.bodyAsJsonObject();

                    if (body == null || !body.containsKey("rate")) {
                        logger.error("Invalid FX response: missing 'rate' field");
                        return Future.failedFuture("Invalid FX service response");
                    }

                    Double rate = body.getDouble("rate");

                    if (rate == null || rate <= 0) {
                        logger.error("Invalid FX rate received: {}", rate);
                        return Future.failedFuture("Invalid FX rate");
                    }

                    logger.debug("FX rate retrieved for {}: {}", currency, rate);

                    return Future.succeededFuture(rate);
                })
                .recover(err -> {
                    logger.warn("FX call failed (attempt {}): {}", retry + 1, err.getMessage());

                    if (retry < maxRetries) {
                        return attempt(currency, retry + 1);
                    }

                    logger.error("FX unavailable after retries");
                    return Future.failedFuture("FX unavailable after retries");
                });
    }
}
