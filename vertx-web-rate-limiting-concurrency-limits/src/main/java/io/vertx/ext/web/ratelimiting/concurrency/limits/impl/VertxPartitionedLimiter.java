package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;
import io.vertx.ext.web.RoutingContext;

public class VertxPartitionedLimiter extends AbstractPartitionedLimiter<RoutingContext> {
  public static class Builder extends AbstractPartitionedLimiter.Builder<Builder, RoutingContext> {
    @Override
    protected Builder self() {
      return this;
    }
  }

  public VertxPartitionedLimiter(Builder builder) {
    super(builder);
  }
}