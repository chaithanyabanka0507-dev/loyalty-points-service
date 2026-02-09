package com.airline.loyalty.points.service;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.List;

public class PromoClient {

    private final WebClient client;

    public PromoClient(WebClient client) {
        this.client = client;
    }

    public Future<PromoResult> getPromoBonus(String code, int basePoints) {

        if (code == null || code.isBlank()) {
            return Future.succeededFuture(new PromoResult(0, List.of()));
        }

        return client.get("/promo")
                .addQueryParam("code", code)
                .timeout(500)
                .send()
                .map(resp -> {
                    int bonusPercent = resp.bodyAsJsonObject().getInteger("bonusPercentage");
                    int expiresInDays = resp.bodyAsJsonObject().getInteger("expiresInDays");

                    int bonus = (basePoints * bonusPercent) / 100;
                    List<String> warnings = new ArrayList<>();

                    if (expiresInDays <= 3) {
                        warnings.add("PROMO_EXPIRES_SOON");
                    }

                    return new PromoResult(bonus, warnings);
                });
    }
}
