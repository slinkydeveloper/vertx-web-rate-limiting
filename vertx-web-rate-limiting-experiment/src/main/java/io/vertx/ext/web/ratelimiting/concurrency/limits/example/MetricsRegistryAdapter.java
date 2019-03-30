package io.vertx.ext.web.ratelimiting.concurrency.limits.example;

import com.netflix.concurrency.limits.MetricRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.Supplier;

public class MetricsRegistryAdapter implements MetricRegistry {

    private final MeterRegistry registry;

    public MetricsRegistryAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public SampleListener registerDistribution(String s, String... strings) {
        return a -> {};
    }

    @Override
    public void registerGauge(String s, Supplier<Number> supplier, String... strings) {
        Gauge.builder(s, supplier).tags(strings).register(registry);
    }
}
