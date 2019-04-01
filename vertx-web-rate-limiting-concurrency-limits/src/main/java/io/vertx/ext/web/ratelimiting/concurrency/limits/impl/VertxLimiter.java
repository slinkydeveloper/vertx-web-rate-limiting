package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;
import io.vertx.ext.web.RoutingContext;

public class VertxLimiter extends AbstractPartitionedLimiter<RoutingContext> {
  public static class Builder extends AbstractPartitionedLimiter.Builder<Builder, RoutingContext> {
    @Override
    protected Builder self() {
      return this;
    }
  }

  public VertxLimiter(Builder builder) {
    super(builder);
  }
}
