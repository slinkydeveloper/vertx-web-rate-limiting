package io.vertx.ext.web.ratelimiting.concurrency.limits.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    vertx.setTimer(10000, t -> {
      MeterRegistry registry = BackendRegistries.getDefaultNow();
      Counter counter200Requests = registry.counter("200_requests");
      Counter counter429Requests = registry.counter("429_requests");
      vertx.setPeriodic(3000, l -> {
        i.compareAndSet(10, 0);
        long requests = (long) Math.pow(2, i.getAndIncrement());
        log.info("Running requests {}", requests);
        LongStream.range(0, requests).mapToObj(i -> doDelayedRequest("http://localhost:4000/hello", vertx)).forEach(f -> {
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

  public static Supplier<Future<Integer>> prepareRequest(String url, Vertx vertx) {
    Future<Integer> fut = Future.future();
    HttpClient client = vertx.createHttpClient();
    HttpClientRequest req = client.getAbs(url, res -> fut.complete(res.statusCode()));
    return () -> {
      req.end();
      client.close();
      return fut;
    };
  }

  public static Future<Integer> doDelayedRequest(String url, Vertx vertx) {
    Future<Integer> fut = Future.future();
    Supplier<Future<Integer>> prepared = prepareRequest(url, vertx);
    vertx.setTimer(100, l -> prepared.get().setHandler(fut));
    return fut;
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
