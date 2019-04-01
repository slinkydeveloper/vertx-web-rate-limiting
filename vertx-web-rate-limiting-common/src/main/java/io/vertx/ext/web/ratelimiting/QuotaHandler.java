package io.vertx.ext.web.ratelimiting;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.impl.QuotaHandlerImpl;

public interface QuotaHandler extends Handler<RoutingContext> {

  static QuotaHandler create(Buckets buckets, BucketKeyExtractor bucketKeyExtractor) {
    return new QuotaHandlerImpl(buckets, bucketKeyExtractor);
  }

}
