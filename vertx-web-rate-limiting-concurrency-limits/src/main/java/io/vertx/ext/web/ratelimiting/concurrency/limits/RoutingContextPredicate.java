package io.vertx.ext.web.ratelimiting.concurrency.limits;

import io.vertx.ext.web.RoutingContext;

import java.util.function.Predicate;

@FunctionalInterface
public interface RoutingContextPredicate extends Predicate<RoutingContext> {

  static RoutingContextPredicate createStatusCodePredicate(int statusCode) {
    return ctx -> ctx.response().getStatusCode() == statusCode;
  }

  static RoutingContextPredicate createStatusCodePredicate(int startInclusive, int endExclusive, int... excluded) {
    if (excluded.length == 0)
      return ctx -> ctx.response().getStatusCode() >= startInclusive && ctx.response().getStatusCode() < endExclusive;
    else
      return ctx -> {
        int statusCode = ctx.response().getStatusCode();
        boolean b = statusCode >= startInclusive && statusCode < endExclusive;
        for (int sc : excluded) {
          b = b && statusCode != sc;
        }
        return b;
      };
  }

  static RoutingContextPredicate create4xxErrorPredicate(int... excluded) {
    return createStatusCodePredicate(400, 500, excluded);
  }

  static RoutingContextPredicate create5xxErrorPredicate(int... excluded) {
    return createStatusCodePredicate(500, 600, excluded);
  }

}
