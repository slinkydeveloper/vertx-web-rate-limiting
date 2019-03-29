package io.vertx.ext.web.ratelimiting.concurrency.limits.example;

import com.netflix.concurrency.limits.limit.FixedLimit;
import com.netflix.concurrency.limits.limit.TracingLimitDecorator;
import com.netflix.concurrency.limits.limit.VegasLimit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler;
import io.vertx.ext.web.ratelimiting.concurrency.limits.impl.VertxPartitionedLimiter;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.micrometer.impl.MicrometerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler.newLimiterBuilder;
import static io.vertx.ext.web.ratelimiting.concurrency.limits.RoutingContextPredicate.*;

public class ConcurrencyLimitsVerticle extends AbstractVerticle {

  private final static Logger log = LoggerFactory.getLogger(ConcurrencyLimitsVerticle.class);

  private AtomicInteger requestsCount = new AtomicInteger();

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    Gauge.builder("requests_count", requestsCount::get);

    Router router = Router.router(vertx);
    router.get("/hello").handler(
        ConcurrencyLimitsHandler.create(
            newLimiterBuilder()
                .limit(TracingLimitDecorator.wrap(
                    VegasLimit.newDefault()
                ))
        )
            .errorPredicate(create5xxErrorPredicate())
            .errorPredicate(createStatusCodePredicate(429))
            .ignorePredicate(create4xxErrorPredicate(429))
    ).handler(rc -> {
      int val = requestsCount.get();
      if (val > 120) {
        val = 0;
        requestsCount.set(0);
      }
      if (val > 100)
        vertx.setTimer(4000, v -> rc.response().setStatusCode(200).end());
      else
        vertx.setTimer(2000, v -> rc.response().setStatusCode(200).end());
    });

    vertx.createHttpServer(new HttpServerOptions())
        .requestHandler(router)
        .listen(4000, ar -> {
          if (ar.succeeded()) startFuture.complete();
          else startFuture.fail(ar.cause());
        });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
        new MicrometerMetricsOptions()
        .setPrometheusOptions(
            new VertxPrometheusOptions()
                .setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new HttpServerOptions().setPort(5000))
                .setEnabled(true)
        ).setEnabled(true)
    ));
    vertx.deployVerticle(new ConcurrencyLimitsVerticle());
  }
}
