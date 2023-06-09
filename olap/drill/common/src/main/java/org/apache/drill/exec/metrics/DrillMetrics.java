/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.metrics;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.apache.drill.exec.util.SystemPropertyUtil;

public final class DrillMetrics {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillMetrics.class);

  public static final boolean METRICS_JMX_OUTPUT_ENABLED =
      SystemPropertyUtil.getBoolean("drill.metrics.jmx.enabled", true);
  public static final boolean METRICS_LOG_OUTPUT_ENABLED =
      SystemPropertyUtil.getBoolean("drill.metrics.log.enabled", false);
  public static final int METRICS_LOG_OUTPUT_INTERVAL =
      SystemPropertyUtil.getInt("drill.metrics.log.interval", 60);

  private static class RegistryHolder {

    private static final MetricRegistry REGISTRY;
    private static final JmxReporter JMX_REPORTER;
    private static final Slf4jReporter LOG_REPORTER;

    static {
      REGISTRY = new MetricRegistry();
      registerSystemMetrics();
      JMX_REPORTER = getJmxReporter();
      LOG_REPORTER = getLogReporter();
    }

    private static void registerSystemMetrics() {
      REGISTRY.registerAll(new GarbageCollectorMetricSet());
      REGISTRY.registerAll(new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
      REGISTRY.registerAll(new MemoryUsageGaugeSet());
      REGISTRY.registerAll(new ThreadStatesGaugeSet());
      REGISTRY.registerAll(new CpuGaugeSet());
      register("fd.usage", new FileDescriptorRatioGauge());
    }

    private static JmxReporter getJmxReporter() {
      if (METRICS_JMX_OUTPUT_ENABLED) {
        JmxReporter reporter = JmxReporter.forRegistry(REGISTRY).build();
        reporter.start();

        return reporter;
      }
      return null;
    }

    private static Slf4jReporter getLogReporter() {
      if (METRICS_LOG_OUTPUT_ENABLED) {
        Slf4jReporter reporter = Slf4jReporter.forRegistry(REGISTRY)
            .outputTo(logger)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
        reporter.start(METRICS_LOG_OUTPUT_INTERVAL, TimeUnit.SECONDS);

        return reporter;
      }
      return null;
    }
  }

  /**
   * Note: For counters, histograms, meters and timers, use get or create methods on {@link #getRegistry the
   * registry} (e.g. {@link MetricRegistry#counter}). For {@link com.codahale.metrics.Gauge gauges} or custom
   * metric implementations use this method. The registry does not allow registering multiple metrics with
   * the same name, which is a problem when multiple drillbits are started in the same JVM (e.g. unit tests).
   *
   * @param name metric name
   * @param metric metric instance
   * @param <T> metric type
   */
  public synchronized static <T extends Metric> void register(String name, T metric) {
    boolean removed = RegistryHolder.REGISTRY.remove(name);
    if (removed) {
      logger.warn("Removing old metric since name matched newly registered metric. Metric name: {}", name);
    }
    RegistryHolder.REGISTRY.register(name, metric);
  }

  public static MetricRegistry getRegistry() {
    return RegistryHolder.REGISTRY;
  }

  public static void resetMetrics() {
    RegistryHolder.REGISTRY.removeMatching(MetricFilter.ALL);
    RegistryHolder.registerSystemMetrics();
  }

  // prevents instantiation
  private DrillMetrics() {
  }
}
