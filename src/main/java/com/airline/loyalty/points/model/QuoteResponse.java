package com.airline.loyalty.points.model;

import java.util.List;

/**
 * Represents the calculated loyalty points response returned by the API.
 *
 * Contains the breakdown of points including:
 * - Base points calculated from fare and FX conversion
 * - Tier bonus points based on customer loyalty tier
 * - Promotional bonus points (if applicable)
 * - Final total points after applying cap rules
 * - Effective FX rate used for conversion
 * - Any informational warnings (e.g., promo expiry, service fallback)
 *
 * This object is serialized as JSON and returned
 * from the /v1/points/quote endpoint.
 *
 * @param basePoints       points calculated from fare after FX conversion
 * @param tierBonus        bonus points awarded based on customer tier
 * @param promoBonus       bonus points awarded from promo code
 * @param totalPoints      final total points after applying cap rules
 * @param effectiveFxRate  exchange rate used in the calculation
 * @param warnings         list of informational warnings related to calculation
 */
public record QuoteResponse(
        int basePoints,
        int tierBonus,
        int promoBonus,
        int totalPoints,
        double effectiveFxRate,
        List<String> warnings
) {}
