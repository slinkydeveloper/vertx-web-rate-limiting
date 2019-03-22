package io.vertx.ext.web.ratelimiting.local.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.ratelimiting.Buckets;
import io.vertx.ext.web.ratelimiting.local.LocalRefiller;

import java.util.Map;
import java.util.function.Supplier;

public class LocalBuckets implements Buckets {

  Map<String, Integer> buckets;
  Map<String, LocalRefiller> refillProviders;
  Supplier<LocalRefiller> refillProviderFactory;

  public LocalBuckets(Vertx vertx, Supplier<LocalRefiller> refillProviderFactory) {
    this.buckets = vertx.sharedData().getLocalMap("rate-limiting-buckets");
    this.refillProviders = vertx.sharedData().getLocalMap("rate-limiting-refill-providers");
    this.refillProviderFactory = refillProviderFactory;
  }

  @Override
  public Future<Integer> availableTokens(String key) {
    return Future.succeededFuture(this.buckets.get(key));
  }

  @Override
  public Future<Integer> tryReserveToken(String key) {
    Integer result = this.buckets.merge(
        key,
        refillProviders.computeIfAbsent(key, k -> refillProviderFactory.get()).newTokens(),
        (oldValue, newTokens) -> {
          int remainingTokens = oldValue + newTokens - 1;
          return remainingTokens > 0 ? remainingTokens : 0;
        }
    );
    if (result == 0) return Future.failedFuture((String)null);
    else return Future.succeededFuture(result);
  }
}
