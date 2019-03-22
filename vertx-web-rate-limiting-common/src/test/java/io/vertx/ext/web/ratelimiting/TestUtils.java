package io.vertx.ext.web.ratelimiting;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    HttpClient client = vertx.createHttpClient();

    long reqNumber = succeeding + failing;

    List<Future> results = IntStream.range(0, (int) reqNumber).mapToObj(i -> doRequest(client)).collect(Collectors.toList());

    CompositeFuture
        .all(results)
        .setHandler(cf -> {
          if (cf.failed()) testContext.failNow(cf.cause());
          else
            testContext.verify(() -> {
              long succeeded = cf.result().list().stream().filter(i -> ((Integer)i) == 200).count();
              assertThat(succeeded).isEqualTo(succeeding);
              long failed = cf.result().list().stream().filter(i -> ((Integer)i) == 429).count();
              assertThat(failed).isEqualTo(failing);
              check.flag();
            });
        });
  }

  private static Future<Integer> doRequest(HttpClient client) {
    Future<Integer> fut = Future.future();
    client.getAbs("http://localhost:3000/hello", res -> fut.complete(res.statusCode())).end();
    return fut;
  }

}
