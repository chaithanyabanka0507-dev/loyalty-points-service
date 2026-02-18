package com.airline.loyalty.points.model;

/**
 * Represents the loyalty membership tier of a customer.
 *
 * Each tier provides a bonus multiplier that is applied
 * to the calculated base loyalty points.
 *
 * The multiplier represents the percentage bonus added
 * on top of the base points.
 *
 * Example:
 * If base points = 1000 and tier is GOLD (0.30),
 * the tier bonus = 1000 * 0.30 = 300 points.
 */
public enum Tier {

    /**
     * No membership tier. No bonus applied.
     */
    NONE(0.0),

    /**
     * Silver tier with 15% bonus points.
     */
    SILVER(0.15),

    /**
     * Gold tier with 30% bonus points.
     */
    GOLD(0.30),

    /**
     * Platinum tier with 50% bonus points.
     */
    PLATINUM(0.50);

    /**
     * Bonus multiplier applied to base points.
     */
    public final double multiplier;

    /**
     * Creates a tier with the given bonus multiplier.
     *
     * @param multiplier percentage bonus applied to base points
     */
    Tier(double multiplier) {
        this.multiplier = multiplier;
    }
}
