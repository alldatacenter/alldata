/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.metrics.system.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

/**
 * @{link JvmMetricsSource} collects JVM Metrics using codahale and publish to Metrics Sink.
 */
public class JvmMetricsSource extends AbstractMetricsSource {
  static final MetricRegistry registry = new MetricRegistry();
  private static final Logger LOG = LoggerFactory.getLogger(JvmMetricsSource.class);
  private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
  private static String JVM_PREFIX = "jvm";
  private int interval = 10;

  @Override
  public void init(MetricsConfiguration configuration, MetricsSink sink) {
    super.init(configuration, sink);
    registerAll(JVM_PREFIX + ".gc", new GarbageCollectorMetricSet(), registry);
    registerAll(JVM_PREFIX + ".buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), registry);
    registerAll(JVM_PREFIX + ".memory", new MemoryUsageGaugeSet(), registry);
    registerAll(JVM_PREFIX + ".threads", new ThreadStatesGaugeSet(), registry);
    registry.register(JVM_PREFIX + ".file.open.descriptor.ratio", new FileDescriptorRatioGauge());
    interval = Integer.parseInt(configuration.getProperty("interval", "10"));
    LOG.info("Initialized JVM Metrics source...");
  }

  @Override
  public void start() {
    try {
      executor.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          try {
            LOG.debug("Publishing JVM metrics to sink");
            sink.publish(getMetrics());
          } catch (Exception e) {
            LOG.debug("Error in publishing JVM metrics to sink.");
          }
        }
      }, interval, interval, TimeUnit.SECONDS);
      LOG.info("Started JVM Metrics source...");
    } catch (Exception e) {
      LOG.info("Throwing exception when starting metric source", e);
    }
  }

  private void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
    for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
      if (entry.getValue() instanceof MetricSet) {
        registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
      } else {
        registry.register(prefix + "." + entry.getKey(), entry.getValue());
      }
    }
  }

  public List<SingleMetric> getMetrics() {

    List<SingleMetric> metrics = new ArrayList<>();
    Map<String, Gauge> gaugeSet = registry.getGauges(new NonNumericMetricFilter());
    for (String metricName : gaugeSet.keySet()) {
      Number value = (Number) gaugeSet.get(metricName).getValue();
      metrics.add(new SingleMetric(metricName, value.doubleValue(), System.currentTimeMillis()));
    }

    return metrics;
  }

  public class NonNumericMetricFilter implements MetricFilter {

    @Override
    public boolean matches(String name, Metric metric) {
      if (name.equalsIgnoreCase("jvm.threads.deadlocks")) {
        return false;
      }
      return true;
    }
  }
}
