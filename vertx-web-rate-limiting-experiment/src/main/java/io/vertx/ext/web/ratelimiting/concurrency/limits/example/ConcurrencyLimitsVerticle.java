package io.vertx.ext.web.ratelimiting.concurrency.limits.example;

import com.netflix.concurrency.limits.MetricRegistry;
import com.netflix.concurrency.limits.limit.TracingLimitDecorator;
import com.netflix.concurrency.limits.limit.VegasLimit;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler;
import io.vertx.ext.web.ratelimiting.concurrency.limits.impl.MicrometerMetricsRegistryAdapter;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vertx.ext.web.ratelimiting.concurrency.limits.ConcurrencyLimitsHandler.newLimiterBuilder;
import static io.vertx.ext.web.ratelimiting.concurrency.limits.RoutingContextPredicate.*;

public class ConcurrencyLimitsVerticle extends AbstractVerticle {

    private final static Logger log = LoggerFactory.getLogger(ConcurrencyLimitsVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        MeterRegistry registry = BackendRegistries.getDefaultNow();

        final long startup = System.currentTimeMillis();

        MetricRegistry concurrencyLimitsMetricsRegistry = MicrometerMetricsRegistryAdapter.create(registry);

        Router router = Router.router(vertx);
        router.get("/hello").handler(
                ConcurrencyLimitsHandler.create(
                        newLimiterBuilder()
                                .limit(TracingLimitDecorator.wrap(
                                        VegasLimit
                                            .newBuilder()
                                            .metricRegistry(concurrencyLimitsMetricsRegistry)
                                            .build()
                                ))
                                .metricRegistry(concurrencyLimitsMetricsRegistry)
                )
                        .errorPredicate(create5xxErrorPredicate())
                        .errorPredicate(createStatusCodePredicate(429))
                        .ignorePredicate(create4xxErrorPredicate(429))
        ).handler(rc -> {
            long waitTime = (System.currentTimeMillis() - startup) / 200L; // Increment 5ms/s
            vertx.setTimer(waitTime, v -> rc.response().setStatusCode(200).end());
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
