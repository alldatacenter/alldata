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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.metrics.system.MetricsService;
import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.MetricsSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MetricsServiceImpl implements MetricsService {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsServiceImpl.class);
  private static Map<String, MetricsSource> sources = new HashMap<>();
  private static MetricsSink sink = null;
  private MetricsConfiguration configuration = null;

  @Inject
  AmbariManagementController amc;
  @Inject
  STOMPUpdatePublisher STOMPUpdatePublisher;

  @Override
  public void start() {
    LOG.info("********* Initializing AmbariServer Metrics Service **********");
    try {
      configuration = MetricsConfiguration.getMetricsConfiguration();
      if (configuration == null) {
        return;
      }
      sink = new AmbariMetricSinkImpl(amc);
      initializeMetricsSink();
      initializeMetricSources();

      if (!sink.isInitialized()) {
        // If Sink is not initialized (say, cluster has not yet been deployed or AMS had not been installed)
        // Service will check for every 5 mins.
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new Runnable() {
          @Override
          public void run() {
            if (!sink.isInitialized()) {
              LOG.info("Attempting to initialize metrics sink");
              initializeMetricsSink();
              if (sink.isInitialized()) {
                LOG.info("Metric sink initialization successful");
              }
            }
          }
        }, 5, 5, TimeUnit.MINUTES);
      }
    } catch (Exception e) {
      LOG.info("Unable to initialize MetricsService : ", e.getMessage());
    }
  }

  private void initializeMetricsSink() {

    LOG.info("********* Configuring Metric Sink **********");
    sink.init(configuration);
  }

  private void initializeMetricSources() {
    try {

      LOG.info("********* Configuring Metric Sources **********");
      String commaSeparatedSources = configuration.getProperty("metric.sources");

      if (StringUtils.isEmpty(commaSeparatedSources)) {
        LOG.info("No metric sources configured.");
        return;
      }

      String[] sourceNames = commaSeparatedSources.split(",");
      for (String sourceName : sourceNames) {

        if (StringUtils.isEmpty(sourceName)) {
          continue;
        }
        sourceName = sourceName.trim();

        String className = configuration.getProperty("source." + sourceName + ".class");
        Class sourceClass;
        try {
          sourceClass = Class.forName(className);
        } catch (ClassNotFoundException ex) {
          LOG.info("Source class not found for source name :" + sourceName);
          continue;
        }
        AbstractMetricsSource src = (AbstractMetricsSource) sourceClass.newInstance();
        src.init(MetricsConfiguration.getSubsetConfiguration(configuration, "source." + sourceName + "."), sink);
        sources.put(sourceName, src);
        if (src instanceof StompEventsMetricsSource) {
          STOMPUpdatePublisher.registerAPI(src);
          STOMPUpdatePublisher.registerAgent(src);
        }
        src.start();
      }

    } catch (Exception e) {
      LOG.error("Error when configuring metric sink and source", e);
    }
  }

  public static MetricsSource getSource(String type) {
    return sources.get(type);
  }

  public static MetricsSink getSink() {
    return sink;
  }
}
