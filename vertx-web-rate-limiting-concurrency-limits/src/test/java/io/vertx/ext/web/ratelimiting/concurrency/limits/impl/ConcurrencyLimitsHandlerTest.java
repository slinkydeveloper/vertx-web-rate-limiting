package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limit.FixedLimit;
import com.netflix.concurrency.limits.limit.TracingLimitDecorator;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler;
import io.vertx.ext.web.ratelimiting.concurrency.limits.LimiterListener;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler.newLimiterBuilder;
import static io.vertx.ext.web.ratelimiting.concurrency.limits.RoutingContextPredicate.*;
import static io.vertx.ext.web.ratelimiting.concurrency.limits.impl.MyAsserts.*;

@ExtendWith(VertxExtension.class)
class ConcurrencyLimitsHandlerTest {

  public static final Logger log = LoggerFactory.getLogger(ConcurrencyLimitsHandlerTest.class);

  @Test
  @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
  public void testSimpleLimiter(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(1);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
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

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(ar -> {
      testBurst(vertx, 10, 90, req -> req.putHeader("Rate-limit-id", "1"), testContext, checkpoint);
    });
  }

  @Test
  @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
  public void testPartitionedLimiter(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
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

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(ar -> {
      List<Future<Integer>> requestBatch1 = new ArrayList<>();
      List<Future<Integer>> requestBatch2 = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        if (i % 2 == 0)
          requestBatch1.add(MyAsserts.doDelayedRequest(vertx, req -> req.putHeader("Rate-limit-partition", "1")));
        else
          requestBatch2.add(MyAsserts.doDelayedRequest(vertx, req -> req.putHeader("Rate-limit-partition", "2")));
      }
      testBurst(10, 40, requestBatch1.stream(), "1", testContext, checkpoint);
      testBurst(10, 40, requestBatch2.stream(), "2", testContext, checkpoint);
    });
  }

  @Test
  public void testLimiterManualFlagSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Limiter<RoutingContext> limiterMock = new LimiterMock(LimiterMock.LimiterExpectedStatus.SUCCEEDING, testContext, checkpoint);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
      new ConcurrencyLimitsHandlerImpl(limiterMock)
        .errorPredicate(create5xxErrorPredicate())
    ).handler(rc -> {
      LimiterListener listener = rc.get("limiterListener");
      listener.onSuccess();
      rc.response().setStatusCode(501).end();
    });

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(rc -> {
      testRequest(501, vertx, testContext, checkpoint);
    });

  }

  @Test
  public void testLimiterManualFlagIgnore(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Limiter<RoutingContext> limiterMock = new LimiterMock(LimiterMock.LimiterExpectedStatus.IGNORING, testContext, checkpoint);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
      new ConcurrencyLimitsHandlerImpl(limiterMock)
        .ignorePredicate(create5xxErrorPredicate())
    ).handler(rc -> {
      LimiterListener listener = rc.get("limiterListener");
      listener.onIgnore();
      rc.response().setStatusCode(501).end();
    });

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(rc ->
      testRequest(501, vertx, testContext, checkpoint)
    );

  }

  @Test
  public void testLimiterManualFlagDropped(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Limiter<RoutingContext> limiterMock = new LimiterMock(LimiterMock.LimiterExpectedStatus.DROPPING, testContext, checkpoint);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
      new ConcurrencyLimitsHandlerImpl(limiterMock)
        .errorPredicate(createStatusCodePredicate(200, 300))
    ).handler(rc -> {
      LimiterListener listener = rc.get("limiterListener");
      listener.onDropped();
      rc.response().setStatusCode(201).end();
    });

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(rc -> {
      testRequest(201, vertx, testContext, checkpoint);
    });

  }

  @Test
  public void testLimiterPredicateFlagSuccess(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Limiter<RoutingContext> limiterMock = new LimiterMock(LimiterMock.LimiterExpectedStatus.SUCCEEDING, testContext, checkpoint);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
      new ConcurrencyLimitsHandlerImpl(limiterMock)
        .errorPredicate(createStatusCodePredicate(0, 500, 201))
        .ignorePredicate(createStatusCodePredicate(0, 500, 201))
    ).handler(rc -> rc.response().setStatusCode(201).end());

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(rc ->
      testRequest(201, vertx, testContext, checkpoint)
    );

  }

  @Test
  public void testLimiterPredicateFlagIgnore(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Limiter<RoutingContext> limiterMock = new LimiterMock(LimiterMock.LimiterExpectedStatus.IGNORING, testContext, checkpoint);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
      new ConcurrencyLimitsHandlerImpl(limiterMock)
        .errorPredicate(createStatusCodePredicate(0, 500, 201))
        .ignorePredicate(createStatusCodePredicate(201))
    ).handler(rc -> rc.response().setStatusCode(201).end());

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(rc ->
      testRequest(201, vertx, testContext, checkpoint)
    );

  }

  @Test
  public void testLimiterPredicateFlagDropped(Vertx vertx, VertxTestContext testContext) {
    Checkpoint checkpoint = testContext.checkpoint(2);

    Limiter<RoutingContext> limiterMock = new LimiterMock(LimiterMock.LimiterExpectedStatus.DROPPING, testContext, checkpoint);

    Router router = Router.router(vertx);
    router.get(TEST_PATH).handler(
      new ConcurrencyLimitsHandlerImpl(limiterMock)
        .ignorePredicate(createStatusCodePredicate(0, 500, 201))
        .errorPredicate(createStatusCodePredicate(201))
    ).handler(rc -> rc.response().setStatusCode(201).end());

    testContext.assertComplete(
      startServer(router, vertx)
    ).setHandler(rc ->
      testRequest(201, vertx, testContext, checkpoint)
    );

  }

  private static Future<HttpServer> startServer(Router router, Vertx vertx) {
    Future<HttpServer> future = Future.future();
    vertx.createHttpServer(new HttpServerOptions())
      .requestHandler(router)
      .connectionHandler(httpConn -> {
        log.debug("new http connection at time {}: {}", System.currentTimeMillis(), httpConn.remoteAddress());
      })
      .listen(3000, future);
    return future;
  }

}
