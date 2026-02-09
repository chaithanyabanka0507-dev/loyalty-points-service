package com.airline.loyalty.points.model;

import java.util.List;

public record QuoteResponse(
        int basePoints,
        int tierBonus,
        int promoBonus,
        int totalPoints,
        double effectiveFxRate,
        List<String> warnings
) {}
