package io.vertx.ext.web.ratelimiting.concurrency.limits;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.concurrency.limits.impl.ConcurrencyLimitsHandlerImpl;
import io.vertx.ext.web.ratelimiting.concurrency.limits.impl.VertxLimiter;

public interface ConcurrencyLimitsHandler extends Handler<RoutingContext> {

  @Fluent
  ConcurrencyLimitsHandler errorPredicate(RoutingContextPredicate predicate);

  @Fluent
  ConcurrencyLimitsHandler ignorePredicate(RoutingContextPredicate predicate);

  @Fluent
  ConcurrencyLimitsHandler statusCodeForExceededLimit(int statusCode);

  static ConcurrencyLimitsHandler create(VertxLimiter.Builder limiterBuilder) {
    return new ConcurrencyLimitsHandlerImpl(limiterBuilder.build());
  }

  static VertxLimiter.Builder newLimiterBuilder() {
    return new VertxLimiter.Builder();
  }

}
