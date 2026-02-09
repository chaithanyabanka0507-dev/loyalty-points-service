package com.airline.loyalty.component.component;

import com.airline.loyalty.component.BaseTest;
import com.airline.loyalty.component.models.QuoteTestDTO;
import com.airline.loyalty.component.util.QuoteTestDataLoader;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith({
        VertxExtension.class,
        BaseTest.class
})
class PointsQuoteComponentTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void quote_api_behaves_correctly(
            QuoteTestDTO data,
            Vertx vertx,
            Integer serverPort,
            VertxTestContext ctx) {

        WebClient client = WebClient.create(vertx);

        client.post(serverPort, "localhost", "/v1/points/quote")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(data.request(), ar -> {

                    if (ar.failed()) {
                        ctx.failNow(ar.cause());
                        return;
                    }

                    if (data.expectedStatus() == 200) {
                        JsonObject body = ar.result().bodyAsJsonObject();

                        assertThat(body.getInteger("basePoints")).isNotNull();
                        assertThat(body.getInteger("totalPoints")).isGreaterThan(0);
                        assertThat(body.getDouble("effectiveFxRate")).isGreaterThan(0);
                        if(data.name().equalsIgnoreCase("points cap applied"))
                        {
                            assertThat(body.getInteger("totalPoints")).isEqualTo(50000);
                        }
                        else
                        {
                            assertThat(body.getInteger("totalPoints")).isLessThan(50000);
                        }

                    }

                    System.out.println("******** TEST CASE: " + data.name() + "********");
                    System.out.println("STATUS  : " + ar.result().statusCode());
                    System.out.println("RESPONSE: " + ar.result().bodyAsString());

                    assertThat(ar.result().statusCode())
                            .isEqualTo(data.expectedStatus());

                    ctx.completeNow();
                });
    }

    static Stream<QuoteTestDTO> testCases() {
        return QuoteTestDataLoader.loadQuoteTestCases();
    }


}

