package com.airline.loyalty.points.model;

public enum Tier {
    NONE(0.0),
    SILVER(0.15),
    GOLD(0.30),
    PLATINUM(0.50);

    public final double multiplier;

    Tier(double multiplier) {
        this.multiplier = multiplier;
    }
}
