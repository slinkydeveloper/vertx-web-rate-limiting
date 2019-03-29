package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.Limiter;
import io.vertx.ext.web.ratelimiting.concurrency.limits.LimiterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class LimiterListenerDelegate implements LimiterListener {

  private static final Logger log = LoggerFactory.getLogger(LimiterListenerDelegate.class);

  Limiter.Listener listener;
  AtomicBoolean called;

  private LimiterListenerDelegate(Limiter.Listener listener) {
    this.listener = listener;
    called = new AtomicBoolean(false);
  }

  @Override
  public void onSuccess() {
    if (log.isDebugEnabled())
      log.debug("Called onSuccess");
    if (called.compareAndSet(false, true))
      listener.onSuccess();
  }

  @Override
  public void onIgnore() {
    if (log.isDebugEnabled())
      log.debug("Called onIgnore");
    if (called.compareAndSet(false, true))
      listener.onIgnore();
  }

  @Override
  public void onDropped() {
    if (log.isDebugEnabled())
      log.debug("Called onDropped");
    if (called.compareAndSet(false, true))
      listener.onDropped();
  }

  protected static LimiterListener wrap(Limiter.Listener listener) {
    return new LimiterListenerDelegate(listener);
  }
}
