package com.airline.loyalty.points;

import com.airline.loyalty.points.api.PointsQuoteVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Calculates loyalty points for a given booking request.
 *
 * Processing flow:
 *
 *     -Validate input request
 *     -Convert currency (if required)
 *     -Calculate base points
 *     -Apply tier bonus
 *     -Apply promo bonus
 *     -Apply maximum cap
 *
 *
 * @param request the incoming quote request containing fare,
 *                currency, cabin class, customer tier and optional promo code
 * @return a {@link Future} containing the computed {@link QuoteResponse}
 *         or a failed Future if validation or external service calls fail
 */
public class LoyaltyApplication {

    private static final Logger logger =
            LoggerFactory.getLogger(LoyaltyApplication.class);

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        try {
            InputStream inputStream =
                    LoyaltyApplication.class.getClassLoader()
                            .getResourceAsStream("config.json");

            if (inputStream == null) {
                throw new IllegalStateException("config.json not found in resources");
            }

            String configContent =
                    new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JsonObject config = new JsonObject(configContent);

            DeploymentOptions options = new DeploymentOptions()
                    .setConfig(config);

            vertx.deployVerticle(new PointsQuoteVerticle(), options, ar -> {
                if (ar.succeeded()) {
                    logger.info("PointsQuoteVerticle deployed successfully");
                } else {
                    logger.error("Failed to deploy PointsQuoteVerticle", ar.cause());
                    vertx.close();
                }
            });

        } catch (Exception e) {
            logger.error("Failed to start LoyaltyApplication", e);
            vertx.close();
        }
    }
}
