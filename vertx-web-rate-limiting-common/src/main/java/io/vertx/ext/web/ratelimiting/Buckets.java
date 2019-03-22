package io.vertx.ext.web.ratelimiting;

import io.vertx.core.Future;

public interface Buckets {

  Future<Integer> availableTokens(String key);

  Future<Integer> tryReserveToken(String key);

}
