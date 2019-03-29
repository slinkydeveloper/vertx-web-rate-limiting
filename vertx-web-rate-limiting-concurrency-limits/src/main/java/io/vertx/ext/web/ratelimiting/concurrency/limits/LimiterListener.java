package io.vertx.ext.web.ratelimiting.concurrency.limits;

/**
 * Interface that delegates to {@link com.netflix.concurrency.limits.Limiter.Listener}
 *
 */
public interface LimiterListener {

  /**
   * Notification that the operation succeeded and internally measured latency should be
   * used as an RTT sample
   */
  void onSuccess();

  /**
   * The operation failed before any meaningful RTT measurement could be made and should
   * be ignored to not introduce an artificially low RTT
   */
  void onIgnore();

  /**
   * The request failed and was dropped due to being rejected by an external limit or hitting
   * a timeout.  Loss based {@link com.netflix.concurrency.limits.Limit} implementations will likely do an aggressive
   * reducing in limit when this happens.
   */
  void onDropped();

}
