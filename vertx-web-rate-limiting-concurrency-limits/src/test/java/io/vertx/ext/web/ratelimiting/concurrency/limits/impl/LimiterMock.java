package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.Limiter;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

import java.util.Optional;

public class LimiterMock implements Limiter<RoutingContext> {

  enum LimiterExpectedStatus {
    DROPPING,
    IGNORING,
    SUCCEEDING
  }

  LimiterExpectedStatus status;
  VertxTestContext testContext;
  Checkpoint checkToFlag;

  public LimiterMock(LimiterExpectedStatus status, VertxTestContext testContext, Checkpoint checkToFlag) {
    this.status = status;
    this.testContext = testContext;
    this.checkToFlag = checkToFlag;
  }

  @Override
  public Optional<Listener> acquire(RoutingContext context) {
    return Optional.of(new Listener() {
      @Override
      public void onSuccess() {
        if (status == LimiterExpectedStatus.SUCCEEDING) checkToFlag.flag();
        else testContext.failNow(new IllegalStateException("Listener must be " + status.name()));
      }

      @Override
      public void onIgnore() {
        if (status == LimiterExpectedStatus.IGNORING) checkToFlag.flag();
        else testContext.failNow(new IllegalStateException("Listener must be " + status.name()));
      }

      @Override
      public void onDropped() {
        if (status == LimiterExpectedStatus.DROPPING) checkToFlag.flag();
        else testContext.failNow(new IllegalStateException("Listener must be " + status.name()));
      }
    });
  }
}
