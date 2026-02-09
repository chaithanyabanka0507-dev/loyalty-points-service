package com.airline.loyalty.points;

import com.airline.loyalty.points.api.PointsQuoteVerticle;
import io.vertx.core.Vertx;

public class LoyaltyApplication {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new PointsQuoteVerticle());
    }
}
