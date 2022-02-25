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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.sessions.SessionProfiler;
import org.eclipse.persistence.tools.profiler.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Class to extend the EclipseLink PerformanceMonitor, and dump the collected metrics to the AmbariServer Database Metrics source.
 */
@Singleton
public class AmbariPerformanceMonitor extends PerformanceMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariPerformanceMonitor.class);
  private boolean isInitialized = false;
  private DatabaseMetricsSource metricsSource;
  private static String entityPackagePrefix = "org.apache"; //Can be made into a set later if needed.

  public AmbariPerformanceMonitor() {
    super();
    LOG.info("AmbariPerformanceMonitor instantiated");
    init();
  }

   private void init() {

    if (metricsSource == null) {
      metricsSource = (DatabaseMetricsSource) MetricsServiceImpl.getSource("database");
    }

    if (metricsSource != null) {
      LOG.info("AmbariPerformanceMonitor initialized");

      long interval = Long.parseLong(metricsSource.getConfigurationValue("dumptime", "60000"));
      this.setDumpTime(interval);

      String profileWeight = metricsSource.getConfigurationValue("query.weight", "HEAVY");
      this.setProfileWeight(getWeight(profileWeight));

      isInitialized = true;

    } else {
      LOG.info("AmbariPerformanceMonitor not yet initialized.");
    }
  }

  /**
   * Overridden dump metrics method for dumping Metrics to source rather than writing to Log file.
   */
  @Override
  public void dumpResults() {

    lastDumpTime = System.currentTimeMillis();
    Set<String> operations = new TreeSet<>(this.operationTimings.keySet());
    Map<String, Long> metrics = new HashMap<>();

    for (String operation : operations) {

      String[] splits = operation.split(":");

      Object value = this.operationTimings.get(operation);
      if (value == null) {
        value = Long.valueOf(0);
      }
      //Cleaning up metric names.
      if (value instanceof Long) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < splits.length; i++) {
          //Removing full package paths from Entity names
          if (splits[i].startsWith(entityPackagePrefix)) {
            String[] queryClassSplits = splits[i].split("\\.");
            list.add(queryClassSplits[queryClassSplits.length - 1]);
          } else if (splits[i] != null && !splits[i].equals("null")) {
            //Removing nulls in metric names.
            list.add(splits[i]);
          }
        }
        //Joining metric name portions by "." delimiter.
        metrics.put(StringUtils.join(list, "."), (Long)value);
      }
    }
    if (!metrics.isEmpty()) {
      if (!isInitialized) {
        init();
      }
      if (isInitialized) {
        LOG.debug("Publishing {} metrics to sink.", metrics.size());
        metricsSource.publish(metrics);
      }
    }
  }

  /**
   * Utlity method to get Profiling weight in Integer from String.
   * @param value NONE/HEAVY/ALL/NORMAL
   * @return SessionProfiler.HEAVY/NONE/ALL/NORMAL
   */
  private int getWeight(String value) {

    if (StringUtils.isEmpty(value) || value.equals("NONE")) {
      return SessionProfiler.NONE;
    }

    if (value.equals("ALL")) {
      return SessionProfiler.ALL;
    }

    if (value.equals("NORMAL")) {
      return SessionProfiler.NORMAL;
    }

    return SessionProfiler.HEAVY;
  }
}
