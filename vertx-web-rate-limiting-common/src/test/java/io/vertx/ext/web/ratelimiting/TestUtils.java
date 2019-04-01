package io.vertx.ext.web.ratelimiting;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUtils {

  public static Future<HttpServer> startRateLimitedServer(Vertx vertx, RateLimiterHandler rateLimiterHandler) {
    Router router = Router.router(vertx);
    router.get("/hello").handler(rateLimiterHandler).handler(rc -> rc.response().setStatusCode(200).end());
    router.errorHandler(500, rc -> {
      System.err.println(rc.failure().toString());
      rc.response().setStatusCode(500).end();
    });
    Future<HttpServer> fut = Future.future();
    vertx.createHttpServer().requestHandler(router).listen(3000, fut.completer());
    return fut;
  }

  public static void testBurst(Vertx vertx, long succeeding, long failing, VertxTestContext testContext, Checkpoint check) {
    testBurst(vertx, succeeding, failing, null, testContext, check);
  }

  public static void testBurst(Vertx vertx, long succeeding, long failing, Consumer<HttpClientRequest> requestModifier, VertxTestContext testContext, Checkpoint check) {
    testBurst(succeeding, failing, IntStream.range(0, (int) (succeeding + failing)).mapToObj(i -> doDelayedRequest(vertx, requestModifier)), null, testContext, check);
  }

  public static void testBurst(long succeeding, long failing, Stream<Future<Integer>> requestStream, String batchName, VertxTestContext testContext, Checkpoint check) {
    List<Future> results = requestStream.collect(Collectors.toList());

    CompositeFuture
      .all(results)
      .setHandler(cf -> {
        if (cf.failed()) testContext.failNow(cf.cause());
        else
          testContext.verify(() -> {
            long succeeded = cf.result().list().stream().filter(i -> ((Integer) i) == 200).count();
            assertThat(succeeded).as("Succeeding requests of batch %s", batchName).isEqualTo(succeeding);
            long failed = cf.result().list().stream().filter(i -> ((Integer) i) == 429).count();
            assertThat(failed).as("Failing requests of batch %s", batchName).isEqualTo(failing);
            check.flag();
          });
      });
  }

  public static Supplier<Future<Integer>> prepareRequest(Vertx vertx, Consumer<HttpClientRequest> modifyRequest) {
    Future<Integer> fut = Future.future();
    HttpClient client = vertx.createHttpClient();
    HttpClientRequest req = client.getAbs("http://localhost:3000/hello", res -> fut.complete(res.statusCode()));
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

}
