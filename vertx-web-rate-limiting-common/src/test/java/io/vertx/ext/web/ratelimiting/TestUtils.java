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
    router.get(MyAsserts.TEST_PATH).handler(rateLimiterHandler).handler(rc -> rc.response().setStatusCode(200).end());
    router.errorHandler(500, rc -> {
      System.err.println(rc.failure().toString());
      rc.response().setStatusCode(500).end();
    });
    Future<HttpServer> fut = Future.future();
    vertx.createHttpServer().requestHandler(router).listen(3000, fut.completer());
    return fut;
  }
}
