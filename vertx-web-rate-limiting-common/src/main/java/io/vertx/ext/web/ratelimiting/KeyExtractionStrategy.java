package io.vertx.ext.web.ratelimiting;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.impl.AddressKeyExtractionStrategy;

@FunctionalInterface
public interface KeyExtractionStrategy {

  String extractKey(RoutingContext routingContext);

  static KeyExtractionStrategy createAddressKeyExtractionStrategy() {
    return new AddressKeyExtractionStrategy();
  }

}
