package com.airline.loyalty.points.model;

public record QuoteRequest(
        double fareAmount,
        String currency,
        CabinClass cabinClass,
        Tier customerTier,
        String promoCode
) {}
