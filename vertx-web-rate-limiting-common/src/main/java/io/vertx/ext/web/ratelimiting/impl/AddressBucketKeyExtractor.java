package io.vertx.ext.web.ratelimiting.impl;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.BucketKeyExtractor;

public class AddressBucketKeyExtractor implements BucketKeyExtractor {
  @Override
  public String extractKey(RoutingContext routingContext) {
    return routingContext.request().remoteAddress().host();
  }
}
