package com.airline.loyalty.component;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ExternalServiceStubs {
    public static void stubFxUsd(WireMockServer wm) {
        wm.stubFor(get(urlPathEqualTo("/fx"))
                .withQueryParam("currency", equalTo("USD"))
                .willReturn(okJson("{\"rate\":3.67}")));
    }

    public static void stubPromoSummer25(WireMockServer wm) {
        wm.stubFor(get(urlPathEqualTo("/promo"))
                .withQueryParam("code", equalTo("SUMMER25"))
                .willReturn(okJson("{\"bonusPercentage\":25,\"expiresInDays\":2}")));
    }
}
