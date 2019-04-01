package io.vertx.ext.web.ratelimiting.concurrency.limits.impl;

import com.netflix.concurrency.limits.MetricRegistry;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.Supplier;

public class MicrometerMetricsRegistryAdapter implements MetricRegistry {

  private final MeterRegistry registry;

  public MicrometerMetricsRegistryAdapter(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public SampleListener registerDistribution(String s, String... strings) {
    DistributionSummary summary = registry.summary(s, strings);
    return a -> summary.record(a.longValue());
  }

  @Override
  public void registerGauge(String s, Supplier<Number> supplier, String... strings) {
    Gauge
      .builder(s, supplier)
      .tags(strings)
      .register(registry);
  }

  public static MicrometerMetricsRegistryAdapter create(MeterRegistry registry) {
    return new MicrometerMetricsRegistryAdapter(registry);
  }
}
