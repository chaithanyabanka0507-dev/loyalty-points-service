package com.airline.loyalty.points.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Client responsible for retrieving promotional bonus details
 * from the external Promotion Service.
 *
 * This client:
 * - Calls the configured promo endpoint
 * - Validates HTTP response and JSON payload structure
 * - Applies expiry and warning rules
 * - Fails gracefully (does NOT break the main flow)
 *
 * Expected Promotion Service response format:
 * {
 *   "bonusPercentage": <non-negative integer>,
 *   "expiresInDays": <integer>
 * }
 *
 * Business Rules:
 * - If the promo code is null or blank → no bonus applied
 * - If the promo is expired → bonus = 0, add "PROMO_EXPIRED"
 * - If promo expires within configured threshold → add "PROMO_EXPIRES_SOON"
 * - If service fails → continue without promo (graceful degradation)
 */
public class PromoClient {

    private static final Logger logger = LoggerFactory.getLogger(PromoClient.class);


    private final WebClient client;
    private final long timeoutMs;
    private final int expiryWarningDays;

    /**
     * Creates a new Promotion service client.
     *
     * @param client the Vert.x WebClient used for HTTP communication
     * @param timeoutMs timeout for promo service calls in milliseconds
     * @param expiryWarningDays threshold (in days) to trigger
     *                          PROMO_EXPIRES_SOON warning
     */
    public PromoClient(WebClient client,long timeoutMs,
                       int expiryWarningDays) {
        this.client = client;
        this.timeoutMs = timeoutMs;
        this.expiryWarningDays = expiryWarningDays;
    }

    /**
     * Retrieves promotional bonus information for a given promo code.
     *
     * The method:
     * 1. Calls the promotion service
     * 2. Validates the HTTP response
     * 3. Validates required JSON fields
     * 4. Calculates bonus points
     * 5. Applies expiry business rules
     * 6. Gracefully degrades on failure
     *
     * @param code the promotional code (may be null or blank)
     * @param basePoints calculated base points before promo
     * @return a Future containing the calculated PromoResult
     */
    public Future<PromoResult> getPromoBonus(String code, int basePoints) {

        if (code == null || code.isBlank()) {
            return Future.succeededFuture(new PromoResult(0, List.of()));
        }

        return client.get("/promo")
                .addQueryParam("code", code)
                .timeout(timeoutMs)
                .send()
                .compose(resp -> {

                    if (resp.statusCode() != 200) {
                        logger.warn("Promo service returned non-200 status: {}", resp.statusCode());
                        return Future.failedFuture("Promo service error");
                    }

                    JsonObject body = resp.bodyAsJsonObject();

                    if (body == null ||
                            !body.containsKey("bonusPercentage") ||
                            !body.containsKey("expiresInDays")) {

                        logger.error("Invalid promo response structure");
                        return Future.failedFuture("Invalid promo service response");
                    }

                    Integer bonusPercent = body.getInteger("bonusPercentage");
                    Integer expiresInDays = body.getInteger("expiresInDays");

                    if (bonusPercent == null || bonusPercent < 0) {
                        logger.error("Invalid bonus percentage: {}", bonusPercent);
                        return Future.failedFuture("Invalid promo bonus percentage");
                    }

                    if (expiresInDays == null) {
                        logger.error("Invalid expiresInDays value");
                        return Future.failedFuture("Invalid promo expiry data");
                    }

                    int bonus = (basePoints * bonusPercent) / 100;

                    List<String> warnings = new ArrayList<>();

                    if (expiresInDays != null
                            && expiresInDays <= expiryWarningDays
                            && expiresInDays > 0) {

                        warnings.add("PROMO_EXPIRES_SOON");
                    }

                    if (expiresInDays <= 0) {
                        warnings.add("PROMO_EXPIRED");
                        logger.info("Promo code {} has expired", code);
                        return Future.succeededFuture(new PromoResult(0, warnings));
                    }

                    logger.debug("Promo bonus calculated: {} ({}%)", bonus, bonusPercent);

                    return Future.succeededFuture(new PromoResult(bonus, warnings));
                })
                .recover(err -> {
                    logger.warn("Promo service failure, continuing without promo: {}", err.getMessage());
                    return Future.succeededFuture(new PromoResult(0, List.of()));
                });
    }
}
