package com.airline.loyalty.points.service;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;

public class FxRateClient {

    private final WebClient client;

    public FxRateClient(WebClient client) {
        this.client = client;
    }

    public Future<Double> getFxRate(String currency) {
        return attempt(currency, 0);
    }

    private Future<Double> attempt(String currency, int retry) {
        return client.get("/fx")
                .addQueryParam("currency", currency)
                .send()
                .compose(resp -> {
                    if (resp.statusCode() != 200) {
                        return Future.failedFuture("FX service error");
                    }
                    return Future.succeededFuture(
                            resp.bodyAsJsonObject().getDouble("rate")
                    );
                })
                .recover(err -> {
                    if (retry < 2) {
                        return attempt(currency, retry + 1);
                    }
                    return Future.failedFuture("FX unavailable after retries");
                });
    }
}
