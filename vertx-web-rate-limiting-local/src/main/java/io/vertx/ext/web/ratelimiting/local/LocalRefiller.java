package io.vertx.ext.web.ratelimiting.local;

import io.vertx.ext.web.ratelimiting.local.impl.FixedTimeLocalRefiller;

import java.time.Duration;
import java.util.function.Supplier;

public interface LocalRefiller {

  int getBucketCapacity();

  int newTokens();

  static Supplier<LocalRefiller> createFixedTimeRefillerSupplier(int initialCapacity, Duration refillTimeout) {
    return () -> new FixedTimeLocalRefiller(initialCapacity, refillTimeout);
  }

}
