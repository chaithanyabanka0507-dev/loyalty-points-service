package com.airline.loyalty.points.validation;

import com.airline.loyalty.points.model.QuoteRequest;

import java.util.Set;

/**
 * Performs validation on incoming QuoteRequest objects.
 *
 * This class ensures that all mandatory fields are present and
 * that business rules are satisfied before the request is processed.
 *
 * Validation includes:
 * - Fare amount must be greater than zero
 * - Fare amount must not exceed the maximum allowed limit
 * - Currency must be supported and follow ISO format
 * - Cabin class and customer tier must not be null
 * - Promo code must not exceed the allowed length
 *
 * Throws IllegalArgumentException if validation fails.
 */
public class RequestValidator {

    private static final Set<String> SUPPORTED_CURRENCIES =
            Set.of("USD", "EUR", "GBP");

    /**
     * Validates the provided QuoteRequest.
     *
     * @param req the request to validate
     * @throws IllegalArgumentException if any validation rule is violated
     */
    public static void validate(QuoteRequest req) {

        if (req.fareAmount() <= 0) {
            throw new IllegalArgumentException("Fare amount must be greater than zero");
        }

        if (req.fareAmount() > 1_000_000) {
            throw new IllegalArgumentException("Fare amount exceeds maximum allowed");
        }

        if (req.currency() == null || req.currency().isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }

        if (!req.currency().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Invalid currency format");
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

        if (req.promoCode() != null && req.promoCode().length() > 50) {
            throw new IllegalArgumentException("Promo code too long");
        }
    }
}