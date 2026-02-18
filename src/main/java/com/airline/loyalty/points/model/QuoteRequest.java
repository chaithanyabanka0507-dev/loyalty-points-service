package com.airline.loyalty.points.model;

/**
 * Represents an incoming loyalty points quote request.
 *
 * Contains the required booking and customer information used to
 * calculate loyalty points, including fare amount, currency,
 * cabin class, customer tier, and optional promo code.
 *
 * This object is typically received as a JSON payload
 * in the /v1/points/quote API endpoint.
 *
 * @param fareAmount   the flight fare amount provided by the client
 * @param currency     the 3-letter ISO currency code (e.g., USD, EUR, GBP)
 * @param cabinClass   the selected cabin class for the booking
 * @param customerTier the loyalty tier of the customer
 * @param promoCode    optional promotional code for bonus points
 */
public record QuoteRequest(
        double fareAmount,
        String currency,
        CabinClass cabinClass,
        Tier customerTier,
        String promoCode
) {}
