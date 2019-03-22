package io.vertx.ext.web.ratelimiting;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.impl.RateLimiterHandlerImpl;

public interface RateLimiterHandler extends Handler<RoutingContext> {

  static RateLimiterHandler create(Buckets buckets, KeyExtractionStrategy keyExtractionStrategy) {
    return new RateLimiterHandlerImpl(buckets, keyExtractionStrategy);
  }

}
