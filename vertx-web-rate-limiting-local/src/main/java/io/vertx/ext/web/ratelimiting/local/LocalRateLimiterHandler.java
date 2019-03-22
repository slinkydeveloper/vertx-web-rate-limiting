package io.vertx.ext.web.ratelimiting.local;

import io.vertx.core.Vertx;
import io.vertx.ext.web.ratelimiting.KeyExtractionStrategy;
import io.vertx.ext.web.ratelimiting.RateLimiterHandler;
import io.vertx.ext.web.ratelimiting.impl.RateLimiterHandlerImpl;
import io.vertx.ext.web.ratelimiting.local.impl.LocalBuckets;

import java.util.function.Supplier;

public interface LocalRateLimiterHandler {

  static RateLimiterHandler create(Vertx vertx, Supplier<LocalRefiller> refillerSupplier, KeyExtractionStrategy keyExtractionStrategy) {
    return new RateLimiterHandlerImpl(new LocalBuckets(vertx, refillerSupplier), keyExtractionStrategy);
  }

}
