package com.airline.loyalty.points.api;

import com.airline.loyalty.points.model.QuoteRequest;
import com.airline.loyalty.points.service.FxRateClient;
import com.airline.loyalty.points.service.PointsCalculator;
import com.airline.loyalty.points.service.PromoClient;
import com.airline.loyalty.points.validation.RequestValidator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;

import java.net.URI;

public class PointsQuoteVerticle extends AbstractVerticle {

    @Override
    public void start(io.vertx.core.Promise<Void> startPromise) {

        String fxBaseUrl = config().getString("fx.base.url", "http://localhost:8081");
        String promoBaseUrl = config().getString("promo.base.url", "http://localhost:8082");

        WebClientOptions fxOptions = new WebClientOptions()
                .setDefaultHost(URI.create(fxBaseUrl).getHost())
                .setDefaultPort(URI.create(fxBaseUrl).getPort());

        WebClientOptions promoOptions = new WebClientOptions()
                .setDefaultHost(URI.create(promoBaseUrl).getHost())
                .setDefaultPort(URI.create(promoBaseUrl).getPort());

        WebClient fxClient = WebClient.create(vertx, fxOptions);
        WebClient promoClient = WebClient.create(vertx, promoOptions);

        PointsCalculator calculator =
                new PointsCalculator(
                        new FxRateClient(fxClient),
                        new PromoClient(promoClient)
                );

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/v1/points/quote")
                .handler(ctx -> {
                    try {
                        JsonObject body = ctx.body().asJsonObject();
                        if (body == null) {
                            throw new IllegalArgumentException("Request body is required");
                        }
                        QuoteRequest request = body.mapTo(QuoteRequest.class);
                        RequestValidator.validate(request);

                        calculator.calculate(request)
                                .onSuccess(res ->
                                        ctx.response()
                                                .setStatusCode(200)
                                                .putHeader("Content-Type", "application/json")
                                                .end(Json.encode(res))
                                )
                                .onFailure(err ->
                                        ctx.response()
                                                .setStatusCode(503)
                                                .end(err.getMessage())
                                );

                    } catch (IllegalArgumentException e) {

                        ctx.response()
                                .setStatusCode(400)
                                .end(e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ctx.response()
                                .setStatusCode(400)
                                .end("Invalid JSON payload");
                    }
                });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(0, ar -> {
                    if (ar.succeeded()) {
                        int port = ar.result().actualPort();
                        System.out.println("HTTP server started on port " + port);


                        vertx.sharedData()
                                .getLocalMap("test-data")
                                .put("http.port", port);

                        startPromise.complete();
                    } else {
                        startPromise.fail(ar.cause());
                    }
                });
    }
}

