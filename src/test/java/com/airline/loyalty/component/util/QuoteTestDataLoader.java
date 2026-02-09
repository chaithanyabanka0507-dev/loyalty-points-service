package com.airline.loyalty.component.util;

import com.airline.loyalty.component.models.QuoteTestDTO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class QuoteTestDataLoader {
    public static Stream<QuoteTestDTO> loadQuoteTestCases() {

        try {
            InputStream is = QuoteTestDataLoader.class
                    .getResourceAsStream("/data/TestData.json");
            System.out.println("Loaded test data stream = " + is);

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
                            jsonObj .getInteger("expectedStatus")
                    ));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data", e);
        }
    }
}
