package io.vertx.ext.web.ratelimiting;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

import java.time.Duration;
import java.util.function.Consumer;

public class TestUtils {

  public static Future<HttpServer> startRateLimitedServer(Vertx vertx, QuotaHandler quotaHandler) {
    Router router = Router.router(vertx);
    router.get(MyAsserts.TEST_PATH).handler(quotaHandler).handler(rc -> rc.response().setStatusCode(200).end());
    router.errorHandler(500, rc -> {
      System.err.println(rc.failure().toString());
      rc.response().setStatusCode(500).end();
    });
    Future<HttpServer> fut = Future.future();
    vertx.createHttpServer().requestHandler(router).listen(3000, fut.completer());
    return fut;
  }

  public static <T> Future<T> futurifyAr(Consumer<Handler<AsyncResult<T>>> c) {
    Future<T> f = Future.future();
    c.accept(f);
    return f;
  }

  public static Future<Void> waitAsync(Vertx vertx, Duration duration) {
    Future<Void> f = Future.future();
    vertx.setTimer(duration.toMillis(), l -> f.complete());
    return f;
  }
}
