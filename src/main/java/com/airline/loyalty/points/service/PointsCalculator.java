package com.airline.loyalty.points.service;

import com.airline.loyalty.points.model.QuoteRequest;
import com.airline.loyalty.points.model.QuoteResponse;
import io.vertx.core.Future;

/**
 * Service responsible for calculating loyalty points for a flight booking.
 *
 * The calculation includes:
 * - FX conversion to base currency
 * - Tier-based bonus multiplier
 * - Promotional bonus (if applicable)
 * - Global points cap enforcement
 *
 * This class is asynchronous and relies on:
 * - FxRateClient for exchange rate retrieval
 * - PromoClient for promotional bonus calculation
 *
 * Business Rules:
 * - Base points = fareAmount × FX rate (rounded down)
 * - Tier bonus = basePoints × tier multiplier
 * - Promo bonus applied on basePoints
 * - Total points capped at 50,000
 */
public class PointsCalculator {

    private static final int CAP = 50_000;

    private final FxRateClient fxClient;
    private final PromoClient promoClient;

    /**
     * Creates a new PointsCalculator.
     *
     * @param fxClient client used to retrieve foreign exchange rates
     * @param promoClient client used to retrieve promotional bonus details
     */
    public PointsCalculator(FxRateClient fxClient, PromoClient promoClient) {
        this.fxClient = fxClient;
        this.promoClient = promoClient;
    }


    /**
     * Calculates total loyalty points for a given quote request.
     *
     * Processing steps:
     * 1. Fetch FX rate for the requested currency
     * 2. Calculate base points from fare amount
     * 3. Apply customer tier bonus
     * 4. Apply promotional bonus (if valid)
     * 5. Enforce maximum cap (50,000 points)
     *
     * If the promo service fails, calculation continues without promo bonus.
     *
     * @param req the incoming quote request containing fare and customer details
     * @return a Future containing the calculated QuoteResponse
     */
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

