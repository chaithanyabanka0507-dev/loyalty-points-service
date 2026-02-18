package com.airline.loyalty.component;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Provides reusable WireMock stubs for external service dependencies
 * used during component testing.
 *
 * This class simulates:
 * - FX rate service responses
 * - Promotion service responses
 *
 * By centralizing stub definitions, tests remain clean,
 * consistent, and easy to extend when new scenarios are added.
 */
public class ExternalServiceStubs {
    /**
     * Stubs the FX service endpoint for USD currency.
     *
     * When the application calls:
     *   GET /fx?currency=USD
     *
     * The stub returns:
     *   {
     *     "rate": 3.67
     *   }
     *
     * This allows deterministic FX conversion during tests.
     *
     * @param wm the active WireMock server instance
     */
    public static void stubFxUsd(WireMockServer wm) {
        wm.stubFor(get(urlPathEqualTo("/fx"))
                .withQueryParam("currency", equalTo("USD"))
                .willReturn(okJson("{\"rate\":3.67}")));
    }

    /**
     * Stubs the Promo service endpoint for the SUMMER25 promo code.
     *
     * When the application calls:
     *   GET /promo?code=SUMMER25
     *
     * The stub returns:
     *   {
     *     "bonusPercentage": 25,
     *     "expiresInDays": 2
     *   }
     *
     * This enables validation of:
     * - Promo bonus calculation
     * - Expiry warning behavior
     *
     * @param wm the active WireMock server instance
     */
    public static void stubPromoSummer25(WireMockServer wm) {
        wm.stubFor(get(urlPathEqualTo("/promo"))
                .withQueryParam("code", equalTo("SUMMER25"))
                .willReturn(okJson("{\"bonusPercentage\":25,\"expiresInDays\":2}")));
    }


    /**
     * Simulates an FX service failure by returning HTTP 500 for the /fx endpoint.
     * Used in component tests to verify retry logic and service unavailability handling.
     *
     * @param wm WireMock server instance
     */
    public static void stubFxFailure(WireMockServer wm) {
        wm.stubFor(get(urlPathEqualTo("/fx"))
                .willReturn(aResponse().withStatus(500)));
    }
}
