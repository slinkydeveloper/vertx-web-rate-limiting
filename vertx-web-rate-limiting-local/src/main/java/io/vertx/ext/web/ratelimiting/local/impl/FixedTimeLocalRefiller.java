package io.vertx.ext.web.ratelimiting.local.impl;

import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.web.ratelimiting.local.LocalRefiller;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class FixedTimeLocalRefiller implements LocalRefiller, Shareable {

  private int windowCapacity;
  private long msOneRefillTimeout;
  private AtomicLong lastInstant;

  public FixedTimeLocalRefiller(int windowCapacity, Duration refillTimeout) {
    this.windowCapacity = windowCapacity;
    this.msOneRefillTimeout = refillTimeout.toMillis() / this.windowCapacity;
  }

  @Override
  public int getBucketCapacity() {
    return windowCapacity;
  }

  @Override
  public int newTokens() {
    long now = System.currentTimeMillis();
    if (lastInstant == null) {
      lastInstant = new AtomicLong(now);
      return getBucketCapacity();
    }
    long difference = now - lastInstant.get();
    int newTokens = (int) (difference / msOneRefillTimeout);
    if (newTokens > 0) lastInstant.set(now);
    return newTokens;
  }
}
