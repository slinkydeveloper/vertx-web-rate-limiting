package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.Limiter;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler;
import io.vertx.ext.web.ratelimiting.concurrency.limits.LimiterListener;
import io.vertx.ext.web.ratelimiting.concurrency.limits.RoutingContextPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ConcurrencyLimitsHandlerImpl implements ConcurrencyLimitsHandler {

  private static final Logger log = LoggerFactory.getLogger(ConcurrencyLimitsHandler.class);

  private Limiter<RoutingContext>limiter;
  private int statusCodeExceededLimit;
  private RoutingContextPredicate[] errorPredicates;
  private RoutingContextPredicate[] ignorePredicates;

  public ConcurrencyLimitsHandlerImpl(Limiter<RoutingContext> limiter) {
    this.limiter = limiter;
    this.statusCodeExceededLimit = 429;
    this.errorPredicates = new RoutingContextPredicate[] {};
    this.ignorePredicates = new RoutingContextPredicate[] {};
  }

  @Override
  public void handle(RoutingContext routingContext) {
    Optional<Limiter.Listener> res = this.limiter.acquire(routingContext);
    if (res.isPresent()) {
      LimiterListener listener = LimiterListenerDelegate.wrap(res.get());
      routingContext.response().endHandler(v -> {
        if (checkPredicates(errorPredicates, routingContext)) listener.onDropped();
        else if (checkPredicates(ignorePredicates, routingContext)) listener.onIgnore();
        else listener.onSuccess();
      });
      routingContext.put("limiterListener", listener);
      routingContext.next();
    } else {
      routingContext.fail(statusCodeExceededLimit);
    }
  }

  private boolean checkPredicates(RoutingContextPredicate[] predicates, RoutingContext rc) {
    if (predicates.length == 0) return false;
    boolean b = true;
    for (RoutingContextPredicate p : predicates) {
      b = b && p.test(rc);
    }
    return b;
  }

  @Override
  public ConcurrencyLimitsHandler errorPredicate(RoutingContextPredicate predicate) {
    this.errorPredicates = Stream.concat(Stream.of(errorPredicates), Stream.of(predicate)).toArray(RoutingContextPredicate[]::new);
    return this;
  }

  @Override
  public ConcurrencyLimitsHandler ignorePredicate(RoutingContextPredicate predicate) {
    this.ignorePredicates = Stream.concat(Stream.of(ignorePredicates), Stream.of(predicate)).toArray(RoutingContextPredicate[]::new);
    return this;
  }

  @Override
  public ConcurrencyLimitsHandler statusCodeForExceededLimit(int statusCode) {
    this.statusCodeExceededLimit = statusCode;
    return this;
  }
}
