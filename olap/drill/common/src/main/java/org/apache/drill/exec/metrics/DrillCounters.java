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

import com.codahale.metrics.Counter;

/**
 * Holder containing query state counter metrics.
 */
public class DrillCounters {

  private static final DrillCounters INSTANCE = new DrillCounters();

  private static final String QUERIES_METRICS_PREFIX = "drill.queries.";

  private final Counter planningQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "planning");
  private final Counter enqueuedQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "enqueued");
  private final Counter runningQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "running");
  private final Counter completedQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "completed");
  private final Counter succeededQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "succeeded");
  private final Counter failedQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "failed");
  private final Counter canceledQueries = DrillMetrics.getRegistry().counter(QUERIES_METRICS_PREFIX + "canceled");

  private DrillCounters() {
  }

  public static DrillCounters getInstance() {
    return INSTANCE;
  }

  public Counter getPlanningQueries() {
    return planningQueries;
  }

  public Counter getEnqueuedQueries() {
    return enqueuedQueries;
  }

  public Counter getRunningQueries() {
    return runningQueries;
  }

  public Counter getCompletedQueries() {
    return completedQueries;
  }

  public Counter getSucceededQueries() {
    return succeededQueries;
  }

  public Counter getFailedQueries() {
    return failedQueries;
  }

  public Counter getCanceledQueries() {
    return canceledQueries;
  }
}
