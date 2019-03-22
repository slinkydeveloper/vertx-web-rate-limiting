package io.vertx.ext.web.ratelimiting.impl;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.Buckets;
import io.vertx.ext.web.ratelimiting.KeyExtractionStrategy;
import io.vertx.ext.web.ratelimiting.RateLimiterHandler;

public class RateLimiterHandlerImpl implements RateLimiterHandler {

  KeyExtractionStrategy keyExtractionStrategy;
  Buckets buckets;

  public RateLimiterHandlerImpl(Buckets buckets, KeyExtractionStrategy keyExtractionStrategy) {
    this.keyExtractionStrategy = keyExtractionStrategy;
    this.buckets = buckets;
  }

  public void handle(final RoutingContext routingContext) {
    String key = keyExtractionStrategy.extractKey(routingContext);
    buckets.tryReserveToken(key).setHandler(ar -> {
      if (ar.succeeded()) routingContext.next();
      else routingContext.fail(429);
    });
  }
}
