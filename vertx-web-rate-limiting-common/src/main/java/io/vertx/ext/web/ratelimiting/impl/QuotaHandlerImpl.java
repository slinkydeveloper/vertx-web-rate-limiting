package io.vertx.ext.web.ratelimiting.impl;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.BucketKeyExtractor;
import io.vertx.ext.web.ratelimiting.Buckets;
import io.vertx.ext.web.ratelimiting.QuotaHandler;

public class QuotaHandlerImpl implements QuotaHandler {

  BucketKeyExtractor bucketKeyExtractor;
  Buckets buckets;

  public QuotaHandlerImpl(Buckets buckets, BucketKeyExtractor bucketKeyExtractor) {
    this.bucketKeyExtractor = bucketKeyExtractor;
    this.buckets = buckets;
  }

  public void handle(final RoutingContext routingContext) {
    String key = bucketKeyExtractor.extractKey(routingContext);
    buckets.tryReserveToken(key).setHandler(ar -> {
      if (ar.succeeded()) routingContext.next();
      else routingContext.fail(429);
    });
  }
}
