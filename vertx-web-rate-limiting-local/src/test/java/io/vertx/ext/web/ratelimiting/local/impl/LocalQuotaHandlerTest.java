package io.vertx.ext.web.ratelimiting.local.impl;

import io.vertx.core.Vertx;
import io.vertx.ext.web.ratelimiting.BucketKeyExtractor;
import io.vertx.ext.web.ratelimiting.local.LocalQuotaHandler;
import io.vertx.ext.web.ratelimiting.local.LocalRefiller;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static io.vertx.ext.web.ratelimiting.MyAsserts.testBurst;
import static io.vertx.ext.web.ratelimiting.TestUtils.startRateLimitedServer;
import static io.vertx.ext.web.ratelimiting.TestUtils.waitAsync;

@ExtendWith(VertxExtension.class)
public class LocalQuotaHandlerTest {

  @Test
  public void testPass(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);

    testContext.assertComplete(
      startRateLimitedServer(vertx,
        LocalQuotaHandler.create(
          vertx,
          LocalRefiller.createFixedTimeRefillerSupplier(5, Duration.ofSeconds(1)),
          BucketKeyExtractor.createAddressBucketKeyExtractor()
        )
      )
    ).setHandler(ar ->
      testBurst(vertx, 5, 5, "testPass", testContext, checkpoint)
    );

  }

  @Test
  public void testRefill(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    testContext.assertComplete(
      startRateLimitedServer(vertx,
        LocalQuotaHandler.create(
          vertx,
          LocalRefiller.createFixedTimeRefillerSupplier(5, Duration.ofSeconds(2)),
          BucketKeyExtractor.createAddressBucketKeyExtractor()
        )
      )
    )
      .compose(v -> testBurst(vertx, 5, 5, "firstBatch", testContext, checkpoint))
      .compose(v -> waitAsync(vertx, Duration.ofSeconds(2).plusMillis(400)))
      .compose(v -> testBurst(vertx, 5, 5, "secondBatch", testContext, checkpoint));
  }

}
