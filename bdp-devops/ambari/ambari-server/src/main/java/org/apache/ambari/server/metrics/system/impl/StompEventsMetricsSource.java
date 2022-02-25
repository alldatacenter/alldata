/**
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Collects metrics about number of events by types and publishes to configured Metric Sink.
 */
public class StompEventsMetricsSource extends AbstractMetricsSource {
  private static Logger LOG = LoggerFactory.getLogger(StompEventsMetricsSource.class);

  private Map<STOMPEvent.Type, Long> events = new HashMap<>();
  private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

  private final String EVENTS_TOTAL_METRIC = "events.total";
  private final String AVERAGE_METRIC_SUFFIX = ".avg";

  private int interval = 60;

  @Override
  public void init(MetricsConfiguration configuration, MetricsSink sink) {
    super.init(configuration, sink);
    for (STOMPEvent.Type type : STOMPEvent.Type.values()) {
      events.put(type, 0L);
    }

  }

  @Override
  public void start() {
    LOG.info("Starting stomp events source...");
    try {
      executor.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          List<SingleMetric> events = getEvents();
          sink.publish(events);
          LOG.debug("********* Published stomp events metrics to sink **********");
        }
      }, interval, interval, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOG.info("Throwing exception when starting stomp events source", e);
    }
  }

  private List<SingleMetric> getEvents() {
    List<SingleMetric> metrics = new ArrayList<>();
    Long totalEventsCounter = 0L;
    synchronized (events) {
      for (Map.Entry<STOMPEvent.Type, Long> event : events.entrySet()) {
        totalEventsCounter += event.getValue();
        metrics.add(new SingleMetric(event.getKey().getMetricName(), event.getValue(), System.currentTimeMillis()));

        String averageMetricName = event.getKey().getMetricName() + AVERAGE_METRIC_SUFFIX;
        Double eventsPerSecond = event.getValue() == 0 ? -1 : (double) interval / (double) event.getValue();
        metrics.add(new SingleMetric(averageMetricName,
                eventsPerSecond, System.currentTimeMillis()));
        events.put(event.getKey(), 0L);
      }
      metrics.add(new SingleMetric(EVENTS_TOTAL_METRIC, totalEventsCounter, System.currentTimeMillis()));

      String totalAverageMetricName = EVENTS_TOTAL_METRIC + AVERAGE_METRIC_SUFFIX;
      Double eventsPerSecond = totalEventsCounter == 0 ? -1 : (double) interval / (double) totalEventsCounter;
      metrics.add(new SingleMetric(totalAverageMetricName,
              eventsPerSecond, System.currentTimeMillis()));
    }
    return metrics;
  }

  @Subscribe
  public void onUpdateEvent(STOMPEvent STOMPEvent) {
    STOMPEvent.Type metricType = STOMPEvent.getType();
    events.put(metricType, events.get(metricType) + 1);
  }
}
