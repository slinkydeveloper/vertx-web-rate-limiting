package io.vertx.ext.web.ratelimiting.local.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.ratelimiting.local.LocalRefiller;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static io.vertx.ext.web.ratelimiting.TestUtils.waitAsync;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class LocalBucketsTest {

  @Test
  public void testTokenRefillNotExceedTheLimit(Vertx vertx, VertxTestContext testContext) {
    LocalBuckets buckets = new LocalBuckets(vertx, LocalRefiller.createFixedTimeRefillerSupplier(5, Duration.ofSeconds(2)));

    testContext.assertComplete(
      buckets.tryReserveToken("key")
        .compose(v -> buckets.tryReserveToken("key"))
        .compose(v -> buckets.tryReserveToken("key"))
        .compose(v -> buckets.tryReserveToken("key"))
        .compose(tokens -> {
          testContext.verify(() ->
            assertThat(tokens)
              .isEqualTo(1)
          );
          return Future.succeededFuture();
        })
        .compose(v -> waitAsync(vertx, Duration.ofSeconds(2)))
        .compose(v -> buckets.tryReserveToken("127.0.0.1"))
    ).setHandler(ar -> {
      testContext.verify(() ->
        assertThat(ar.result())
          .isEqualTo(4)
      );
      testContext.completeNow();
    });
  }

}
