package io.vertx.ext.web.ratelimiting.concurrency.limits;

import com.netflix.concurrency.limits.limit.FixedLimit;
import com.netflix.concurrency.limits.limit.TracingLimitDecorator;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler.*;
import static io.vertx.ext.web.ratelimiting.concurrency.limits.RoutingContextPredicate.*;
import static io.vertx.ext.web.ratelimiting.TestUtils.*;

@ExtendWith(VertxExtension.class)
class ConcurrencyLimitsHandlerTest {

  public static final Logger log = LoggerFactory.getLogger(ConcurrencyLimitsHandlerTest.class);

  @Test
  @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
  public void testSimpleLimiter(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);

    Router router = Router.router(vertx);
    router.get("/hello").handler(
        ConcurrencyLimitsHandler.create(
            newLimiterBuilder()
                .limit(TracingLimitDecorator.wrap(
                    FixedLimit.of(10)
                ))
        )
            .errorPredicate(create5xxErrorPredicate())
            .errorPredicate(createStatusCodePredicate(429))
            .ignorePredicate(create4xxErrorPredicate(429))
    ).handler(rc -> vertx.setTimer(2000, v -> rc.response().setStatusCode(200).end()));

    vertx.createHttpServer(new HttpServerOptions())
        .requestHandler(router)
        .connectionHandler(httpConn -> {
          log.debug("new http connection at time {}: {}", System.currentTimeMillis(), httpConn.remoteAddress());
        })
        .listen(3000, testContext.succeeding(httpServer -> {
          testBurst(vertx, 10, 90, req -> req.putHeader("Rate-limit-id", "1"), testContext, checkpoint);
        }));
  }

  @Test
  @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
  public void testPartitionedLimiter(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Router router = Router.router(vertx);
    router.get("/hello").handler(
        ConcurrencyLimitsHandler.create(
            newLimiterBuilder()
                .limit(TracingLimitDecorator.wrap(FixedLimit.of(20)))
                .partitionResolver(rc -> rc.request().getHeader("Rate-limit-partition"))
                .partition("1", 0.5)
                .partition("2", 0.5)
        )
            .errorPredicate(create5xxErrorPredicate())
            .errorPredicate(createStatusCodePredicate(429))
            .ignorePredicate(create4xxErrorPredicate(429))
    ).handler(rc -> vertx.setTimer(2000, v -> rc.response().setStatusCode(200).end()));

    vertx.createHttpServer(new HttpServerOptions())
        .requestHandler(router)
        .connectionHandler(httpConn -> {
          log.debug("new http connection at time {}: {}", System.currentTimeMillis(), httpConn.remoteAddress());
        })
        .listen(3000, testContext.succeeding(httpServer -> {
          List<Future<Integer>> requestBatch1 = new ArrayList<>();
          List<Future<Integer>> requestBatch2 = new ArrayList<>();
          for (int i = 0; i < 100; i++) {
            if (i % 2 == 0)
              requestBatch1.add(doDelayedRequest(vertx, req -> req.putHeader("Rate-limit-partition", "1")));
            else
              requestBatch2.add(doDelayedRequest(vertx, req -> req.putHeader("Rate-limit-partition", "2")));
          }
          testBurst(10, 40, requestBatch1.stream(), "1", testContext, checkpoint);
          testBurst(10, 4 0, requestBatch2.stream(), "2", testContext, checkpoint);

        }));
  }

}