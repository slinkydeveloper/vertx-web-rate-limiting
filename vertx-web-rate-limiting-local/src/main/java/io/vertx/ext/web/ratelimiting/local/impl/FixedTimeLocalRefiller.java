package io.vertx.ext.web.ratelimiting.local.impl;

import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.web.ratelimiting.local.LocalRefiller;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class FixedTimeLocalRefiller implements LocalRefiller, Shareable {

  private int capacity;
  private long msRefillTimeout;
  private AtomicLong lastInstant;

  public FixedTimeLocalRefiller(int capacity, Duration refillTimeout) {
    this.capacity = capacity;
    this.msRefillTimeout = refillTimeout.toMillis();
  }

  @Override
  public int getBucketCapacity() {
    return capacity;
  }

  @Override
  public int newTokens() {
    long now = System.currentTimeMillis();
    if (lastInstant == null) {
      lastInstant = new AtomicLong(now);
      return getBucketCapacity();
    }
    long difference = now - lastInstant.get();
    lastInstant.set(now);
    return (int) (difference / msRefillTimeout);
  }
}
