package com.airline.loyalty.component.util;

import com.airline.loyalty.component.models.QuoteTestDTO;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * Utility class responsible for loading API test scenarios
 * from the TestData.json file.
 *
 * This enables parameterized, data-driven component tests
 * by externalizing test cases into JSON rather than hardcoding them.
 *
 * Each JSON entry is mapped into a {@link QuoteTestDTO}
 * containing:
 * - Test case name
 * - Request payload
 * - Expected HTTP status
 * - Optional expected error message
 *
 * This approach improves:
 * - Test readability
 * - Maintainability
 * - Scalability of test scenarios
 */
public class QuoteTestDataLoader {
    private static final Logger logger =
            LoggerFactory.getLogger(QuoteTestDataLoader.class);
    /**
     * Loads all quote API test cases from the TestData.json file
     * located under the test resources directory.
     *
     * @return a Stream of {@link QuoteTestDTO} objects
     *         used by parameterized component tests
     *
     * @throws RuntimeException if the test data file
     *         cannot be found or parsed
     */
    public static Stream<QuoteTestDTO> loadQuoteTestCases() {


        try {
            InputStream is = QuoteTestDataLoader.class
                    .getResourceAsStream("/data/TestData.json");
            logger.debug("Loaded test data stream = "+ is);

            if (is == null) {
                throw new IllegalStateException("Test data file not found");
            }

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonArray array = new JsonArray(json);

            return array.stream()
                    .map(obj -> (JsonObject) obj)
                    .map(jsonObj  -> new QuoteTestDTO(
                            jsonObj .getString("name"),
                            jsonObj .getJsonObject("request"),
                            jsonObj .getInteger("expectedStatus"),
                            jsonObj.getString("expectedError")
                    ));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data", e);
        }
    }
}
