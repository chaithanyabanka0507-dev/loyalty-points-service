package com.airline.loyalty.points.model;

/**
 * Represents the available cabin classes for a flight booking.
 *
 * The cabin class determines the travel experience level and
 * may influence loyalty points calculation depending on
 * business rules.
 */
public enum CabinClass {
    ECONOMY,
    PREMIUM_ECONOMY,
    BUSINESS,
    FIRST
}
