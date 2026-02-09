package com.airline.loyalty.component.models;


import io.vertx.core.json.JsonObject;

public record QuoteTestDTO(
        String name,
        JsonObject request,
        int expectedStatus
) {}
