package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MyAsserts {

  public static final String TEST_PATH = "/hello";

  public static Future<Void> testBurst(Vertx vertx, long succeeding, long failing, Consumer<HttpClientRequest> requestModifier, VertxTestContext testContext, Checkpoint check) {
    return testBurst(succeeding, failing, IntStream.range(0, (int) (succeeding + failing)).mapToObj(i -> doDelayedRequest(vertx, requestModifier)), null, testContext, check);
  }

  public static Future<Void> testBurst(long succeeding, long failing, Stream<Future<Integer>> requestStream, String batchName, VertxTestContext testContext, Checkpoint check) {
    List<Future> results = requestStream.collect(Collectors.toList());

    return CompositeFuture
      .all(results)
      .compose(cf -> {
        if (cf.failed()) testContext.failNow(cf.cause());
        else
          testContext.verify(() -> {
            long succeeded = cf.result().list().stream().filter(i -> ((Integer) i) == 200).count();
            assertThat(succeeded).as("Succeeding requests of batch %s", batchName).isEqualTo(succeeding);
            long failed = cf.result().list().stream().filter(i -> ((Integer) i) == 429).count();
            assertThat(failed).as("Failing requests of batch %s", batchName).isEqualTo(failing);
            check.flag();
          });
        return Future.succeededFuture();
      });
  }

  public static Supplier<Future<Integer>> prepareRequest(Vertx vertx, Consumer<HttpClientRequest> modifyRequest) {
    Future<Integer> fut = Future.future();
    HttpClient client = vertx.createHttpClient();
    HttpClientRequest req = client.getAbs("http://localhost:3000" + TEST_PATH, res -> fut.complete(res.statusCode()));
    if (modifyRequest != null) modifyRequest.accept(req);
    return () -> {
      req.end();
      client.close();
      return fut;
    };
  }

  public static Future<Integer> doDelayedRequest(Vertx vertx, Consumer<HttpClientRequest> modifyRequest) {
    Future<Integer> fut = Future.future();
    Supplier<Future<Integer>> prepared = prepareRequest(vertx, modifyRequest);
    vertx.setTimer(100, l -> prepared.get().setHandler(fut));
    return fut;
  }

  public static void testRequest(int expectingStatusCode, Vertx vertx, VertxTestContext testContext, Checkpoint check) {
    prepareRequest(vertx, null).get().setHandler(testContext.succeeding(statusCode -> {
      testContext.verify(() -> assertThat(statusCode).isEqualTo(expectingStatusCode));
      check.flag();
    }));
  }

}
