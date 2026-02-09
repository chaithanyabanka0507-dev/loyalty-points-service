package com.airline.loyalty.points.service;

import java.util.List;

public record PromoResult(int bonus, List<String> warnings) {

    public static PromoResult empty() {
        return new PromoResult(0, List.of("PROMO_SERVICE_UNAVAILABLE"));
    }
}
