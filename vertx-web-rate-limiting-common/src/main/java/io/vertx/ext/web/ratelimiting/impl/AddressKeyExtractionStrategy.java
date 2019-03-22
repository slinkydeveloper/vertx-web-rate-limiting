package io.vertx.ext.web.ratelimiting.impl;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.KeyExtractionStrategy;

public class AddressKeyExtractionStrategy implements KeyExtractionStrategy {
  @Override
  public String extractKey(RoutingContext routingContext) {
    return routingContext.request().remoteAddress().host();
  }
}
