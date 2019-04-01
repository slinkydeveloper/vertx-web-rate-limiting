package io.vertx.ext.web.ratelimiting.local;

import io.vertx.core.Vertx;
import io.vertx.ext.web.ratelimiting.BucketKeyExtractor;
import io.vertx.ext.web.ratelimiting.QuotaHandler;
import io.vertx.ext.web.ratelimiting.impl.QuotaHandlerImpl;
import io.vertx.ext.web.ratelimiting.local.impl.LocalBuckets;

import java.util.function.Supplier;

public interface LocalQuotaHandler {

  static QuotaHandler create(Vertx vertx, Supplier<LocalRefiller> refillSupplier, BucketKeyExtractor bucketKeyExtractor) {
    return new QuotaHandlerImpl(new LocalBuckets(vertx, refillSupplier), bucketKeyExtractor);
  }

}
