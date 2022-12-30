/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.base.metrics;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;

import com.codahale.metrics.Metric;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ScheduledMetricReporterWrap implements MetricReporter, Scheduled {

  private static final AtomicInteger COUNT = new AtomicInteger();

  private final ScheduledExecutorService executorService;
  @Getter
  private final MetricReporter metricReporter;
  private boolean scheduleReporter = false;

  public ScheduledMetricReporterWrap(MetricReporter metricReporter, boolean daemon) {
    this.metricReporter = metricReporter;
    executorService = Executors.newSingleThreadScheduledExecutor((runnable) -> {
      SecurityManager securityManager = System.getSecurityManager();
      ThreadGroup group = (securityManager != null) ? securityManager.getThreadGroup() :
          Thread.currentThread().getThreadGroup();
      Thread thread = new Thread(group, runnable, "bitsail-metric-scheduled-services-" + COUNT.incrementAndGet());
      thread.setDaemon(daemon);
      return thread;
    });
  }

  @Override
  public void report() {
    try {
      if (metricReporter instanceof Scheduled) {
        Scheduled reporter = (Scheduled) metricReporter;
        log.info("Periodically reporting metrics in intervals of {} {} for reporter {} of type {}.",
            reporter.getDelay(), reporter.getDelayTimeUnit(), metricReporter, metricReporter.getClass().getName());
        executorService.scheduleWithFixedDelay(
            new ReporterTask(metricReporter), reporter.getDelay(), reporter.getDelay(), reporter.getDelayTimeUnit()
        );
        scheduleReporter = true;
      } else {
        log.info("Reporting metrics for reporter {} of type {}.", metricReporter, metricReporter.getClass().getName());
      }
    } catch (Throwable t) {
      log.error("Metric reporter service report {} fail. Metrics might not be exposed/reported", metricReporter, t);
    }
  }

  @Override
  public void notifyOfAddedMetric(Metric metric, String metricName, MetricManager group) {
    if (metricReporter != null) {
      metricReporter.notifyOfAddedMetric(metric, metricName, group);
    }
  }

  @Override
  public void notifyOfRemovedMetric(Metric metric, String metricName, MetricManager group) {
    if (metricReporter != null) {
      this.metricReporter.notifyOfRemovedMetric(metric, metricName, group);
    }
  }

  @Override
  public void open(BitSailConfiguration configuration) {
    if (metricReporter != null) {
      this.metricReporter.open(configuration);
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void close() {
    try {
      log.info("Close metric reporter.");
      if (metricReporter != null) {
        metricReporter.close();
      }
    } catch (Exception e) {
      log.warn("Reporter {} does close properly", metricReporter.getClass().getName(), e);
    } finally {
      try {
        if (scheduleReporter && !executorService.awaitTermination(3, TimeUnit.SECONDS)) {
          log.warn("Metric executor service did not terminate in time, shutting it down now.");
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        log.warn("Interrupted while shutting down executor services. Shutting all " +
            "remaining ExecutorServices down now.", e);
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * This task is explicitly a static class, so that it does not hold any references to the enclosing
   * MetricScheduledService instance.
   *
   * <p>This is a subtle difference, but very important: With this static class, the enclosing class instance
   * may become garbage-collectible, whereas with an anonymous inner class, the timer thread
   * (which is a GC root) will hold a reference via the timer task and its enclosing instance pointer.
   * Making the MetricsRegistry garbage collectible makes the java.util.Timer garbage collectible,
   * which acts as a fail-safe to stop the timer thread and prevents resource leaks.
   */
  private static final class ReporterTask extends TimerTask {
    private final MetricReporter reporter;

    private ReporterTask(MetricReporter reporter) {
      this.reporter = reporter;
    }

    @Override
    public void run() {
      try {
        reporter.report();
      } catch (Throwable t) {
        log.warn("Error while reporting metrics", t);
      }
    }
  }
}
