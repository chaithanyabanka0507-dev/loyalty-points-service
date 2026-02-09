package com.airline.loyalty.points.service;

import com.airline.loyalty.points.model.QuoteRequest;
import com.airline.loyalty.points.model.QuoteResponse;
import io.vertx.core.Future;

public class PointsCalculator {

    private static final int CAP = 50_000;

    private final FxRateClient fxClient;
    private final PromoClient promoClient;

    public PointsCalculator(FxRateClient fxClient, PromoClient promoClient) {
        this.fxClient = fxClient;
        this.promoClient = promoClient;
    }

    public Future<QuoteResponse> calculate(QuoteRequest req) {
        return fxClient.getFxRate(req.currency())
                .compose(rate -> {
                    int basePoints = (int) Math.floor(req.fareAmount() * rate);
                    int tierBonus = (int) (basePoints * req.customerTier().multiplier);

                    return promoClient.getPromoBonus(req.promoCode(), basePoints)
                            .recover(err -> Future.succeededFuture(PromoResult.empty()))
                            .map(promo -> {
                                int total = basePoints + tierBonus + promo.bonus();
                                total = Math.min(total, CAP);

                                return new QuoteResponse(
                                        basePoints,
                                        tierBonus,
                                        promo.bonus(),
                                        total,
                                        rate,
                                        promo.warnings()
                                );
                            });
                });
    }
}

