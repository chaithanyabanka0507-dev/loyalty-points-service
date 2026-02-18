package com.airline.loyalty.points.service;

import java.util.List;

/**
 * Represents the result of a promotion calculation.
 *
 * Contains the calculated bonus points derived from a promo code
 * and any warning messages related to the promotion.
 *
 * Warnings may include:
 * - PROMO_EXPIRES_SOON
 * - PROMO_EXPIRED
 * - PROMO_SERVICE_UNAVAILABLE
 *
 * This record is used internally by the promotion service layer
 * before constructing the final QuoteResponse.
 */
public record PromoResult(int bonus, List<String> warnings) {

    /**
     * Returns a fallback promotion result when the promotion service
     * is unavailable or fails.
     *
     * No bonus points are applied, and a warning is added to indicate
     * that the promotion service could not be reached.
     *
     * @return a PromoResult with zero bonus and service-unavailable warning
     */
    public static PromoResult empty() {
        return new PromoResult(0, List.of("PROMO_SERVICE_UNAVAILABLE"));
    }
}
