/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.metrics.system.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @{link DatabaseMetricsSource} collects database metrics which is generated through Eclipselink PerformanceMonitor,
 * and publishes to configured Metric Sink.
 **/
public class DatabaseMetricsSource extends AbstractMetricsSource {
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseMetricsSource.class);
  private static String dbMonitorPrefix = "monitor.";
  private ExecutorService executor;
  private MetricsConfiguration configuration;
  private Set<String> includedMetricKeywords = new HashSet<>();
  private Set<Pattern> acceptedEntityPatterns = new HashSet<>();
  private Set<String> acceptedEntities = new HashSet<>();
  private static String TIMER = "Timer.";
  private static String COUNTER = "Counter.";

  @Override
  public void init(MetricsConfiguration metricsConfig, MetricsSink sink) {
    super.init(metricsConfig, sink);
    configuration = metricsConfig;
    initializeFilterSets();
    LOG.info("Initialized Ambari DB Metrics Source...");
  }

  /**
   * Initialize filter sets (Entities and keywords) to know which metrics to track vs drop.
   */
  private void initializeFilterSets() {

    String commaSeparatedValues = configuration.getProperty(dbMonitorPrefix + "query.keywords.include");
    if (StringUtils.isNotEmpty((commaSeparatedValues))) {
      includedMetricKeywords.addAll(Arrays.asList(commaSeparatedValues.split(",")));
    }

    commaSeparatedValues = configuration.getProperty(dbMonitorPrefix + "entities");
    if (StringUtils.isNotEmpty((commaSeparatedValues))) {
      String[] entityPatterns = (commaSeparatedValues.split(","));
      for (String pattern : entityPatterns) {
        acceptedEntityPatterns.add(Pattern.compile(pattern));
      }
    }
  }

  @Override
  public void start() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("DatabaseMetricsSource-%d")
      .build();
    executor = Executors.newSingleThreadExecutor(threadFactory);
    LOG.info("Started Ambari DB Metrics source...");
  }

  /**
   * Method to publish metrics to Sink asynchronously.
   * @param metricsMap Map of metrics to be published to Sink
   */
  public void publish(final Map<String, Long> metricsMap) {
    try {
      executor.submit(new Runnable() {
        @Override
        public void run() {
          long currentTime = System.currentTimeMillis();

          for (Iterator<Map.Entry<String, Long>> it = metricsMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> metricEntry = it.next();
            String metricName = metricEntry.getKey();
            if (!acceptMetric(metricName)) {
              it.remove();
            }
          }
          final List<SingleMetric> metrics = new ArrayList<>();
          for (String metricName : metricsMap.keySet()) {
            double value = metricsMap.get(metricName).doubleValue();
            metrics.add(new SingleMetric(metricName, value, currentTime));

            /*
             * Add computed (Timer/Counter) metric.
             * Example
             * Counter Metric : Counter.ReadAllQuery.HostRoleCommandEntity = 10000
             * Timer Metric : Timer.ReadAllQuery.HostRoleCommandEntity = 50
             * Computed Metric (Avg time for the operation) : ReadAllQuery.HostRoleCommandEntity = 200 (10000 div by 50)
             */

            if (metricName.startsWith(COUNTER)) {
              String baseMetricName = metricName.substring(COUNTER.length());
              if (metricsMap.containsKey(TIMER + baseMetricName)) {
                double timerValue = metricsMap.get(TIMER + baseMetricName).doubleValue();
                if (value != 0.0) {
                  metrics.add(new SingleMetric(baseMetricName, timerValue / value , currentTime));
                }
              }
            }
          }
          sink.publish(metrics);
        }
      });
    } catch (Exception e) {
      LOG.info("Exception when publishing Database metrics to sink", e);
    }
  }

  /**
   * Accept a metric to be passed to Sink or not.
   * @param metricName
   * @return true/false
   */
  public boolean acceptMetric(String metricName) {

    boolean accept = false;

    /*
      Include entities to be tracked.
      source.database.monitor.entities=Cluster(.*)Entity,Host(.*)Entity,ExecutionCommandEntity
     */
    if (acceptedEntities.contains(metricName)) {
      accept = true;
    } else {
      for (Pattern p : acceptedEntityPatterns) {
        Matcher m = p.matcher(metricName);
        if (m.find()) {
          accept = true;
        }
      }
    }

    /*
    Include some metrics which have the keyword even if they are not part of requested Entities.
    source.database.monitor.query.keywords.include=CacheMisses
     */

    for (String keyword : includedMetricKeywords) {
      if (metricName.contains(keyword)) {
        accept = true;
      }
    }

    String[] splits = metricName.split("\\.");
    if (splits.length <= 2) {
      accept = true; //Aggregate Counter metrics are always ok. They are not Entity specific
    }

    if (accept) {
      acceptedEntities.add(metricName);
      return true;
    }

    return false;
  }

  /**
   * Method to get Configuration value given key. An extra prefix is added internally.
   * @param key
   * @param defaultValue
   * @return Value corresponding to key = dbMonitorPrefix + key
   */
  public String getConfigurationValue(String key, String defaultValue) {
    return this.configuration.getProperty(dbMonitorPrefix + key, defaultValue);
  }
}
