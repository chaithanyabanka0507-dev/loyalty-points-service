package com.airline.loyalty.points.validation;

import com.airline.loyalty.points.model.QuoteRequest;

import java.util.Set;

public class RequestValidator {

    private static final Set<String> SUPPORTED_CURRENCIES =
            Set.of("USD", "EUR", "GBP");

    public static void validate(QuoteRequest req) {

        if (req.fareAmount() <= 0) {
            throw new IllegalArgumentException("Fare amount must be greater than zero");
        }

        if (!SUPPORTED_CURRENCIES.contains(req.currency())) {
            throw new IllegalArgumentException("Unsupported currency");
        }

        if (req.cabinClass() == null) {
            throw new IllegalArgumentException("Invalid cabin class");
        }

        if (req.customerTier() == null) {
            throw new IllegalArgumentException("Invalid customer tier");
        }
    }
}