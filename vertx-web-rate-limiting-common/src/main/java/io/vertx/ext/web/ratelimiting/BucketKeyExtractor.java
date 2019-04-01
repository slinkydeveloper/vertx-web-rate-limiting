package io.vertx.ext.web.ratelimiting;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.impl.AddressBucketKeyExtractor;

@FunctionalInterface
public interface BucketKeyExtractor {

  String extractKey(RoutingContext routingContext);

  static BucketKeyExtractor createAddressBucketKeyExtractor() {
    return new AddressBucketKeyExtractor();
  }

}
