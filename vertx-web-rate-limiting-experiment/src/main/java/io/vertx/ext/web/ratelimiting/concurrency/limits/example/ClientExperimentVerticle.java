package io.vertx.ext.web.ratelimiting.concurrency.limits.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class ClientExperimentVerticle extends AbstractVerticle {

  AtomicInteger i = new AtomicInteger(1);

  private final static Logger log = LoggerFactory.getLogger(ClientExperimentVerticle.class);

  @Override
  public void start() {
    HttpClient client = vertx.createHttpClient(new HttpClientOptions().setMaxPoolSize(1000).setIdleTimeout(3).setIdleTimeoutUnit(TimeUnit.SECONDS));
    vertx.setTimer(5000, t -> {
      MeterRegistry registry = BackendRegistries.getDefaultNow();
      Counter counter200Requests = registry.counter("200_requests");
      Counter counter429Requests = registry.counter("429_requests");
      vertx.setPeriodic(1, l -> {
        long requests = (long) i.getAndIncrement();
        log.info("Running requests {}", requests);
        LongStream.range(0, requests).mapToObj(i -> prepareRequest(client, "http://localhost:4000/hello").get()).forEach(f -> {
          f.setHandler(ar -> {
            if (ar.failed()) log.error("Error!", ar.cause());
            else {
              log.debug("request completed {}", ar.result());
              if (ar.result() == 200) counter200Requests.increment();
              else if (ar.result() == 429) counter429Requests.increment();
              else log.error("Received unexpected status code {}", ar.result());
            }
          });
        });
      });
    });
  }

  public static Supplier<Future<Integer>> prepareRequest(HttpClient client, String url) {
    Future<Integer> fut = Future.future();
    HttpClientRequest req = client.getAbs(url, res -> fut.complete(res.statusCode()));
    return () -> {
      req.end();
      return fut;
    };
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
        new MicrometerMetricsOptions()
            .setPrometheusOptions(
                new VertxPrometheusOptions()
                    .setStartEmbeddedServer(true)
                    .setEmbeddedServerOptions(new HttpServerOptions().setPort(5001))
                    .setEnabled(true)
            ).setEnabled(true)
    ));
    vertx.deployVerticle(new ClientExperimentVerticle());
  }
}
