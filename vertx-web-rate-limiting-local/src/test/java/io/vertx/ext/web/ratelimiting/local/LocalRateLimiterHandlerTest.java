package io.vertx.ext.web.ratelimiting.local;

import io.vertx.core.Vertx;
import io.vertx.ext.web.ratelimiting.KeyExtractionStrategy;
import io.vertx.ext.web.ratelimiting.TestUtils;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static io.vertx.ext.web.ratelimiting.TestUtils.*;

@ExtendWith(VertxExtension.class)
public class LocalRateLimiterHandlerTest {

  @Test
  public void test(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);

    testContext.assertComplete(
        startRateLimitedServer(vertx,
            LocalRateLimiterHandler.create(
                vertx,
                LocalRefiller.createFixedTimeRefillerSupplier(5, Duration.ofSeconds(1)),
                KeyExtractionStrategy.createAddressKeyExtractionStrategy()
            )
        )
    ).setHandler(ar ->
        testBurst(
            vertx,
            5,
            5,
            testContext,
            checkpoint
        )
    );

  }

}
