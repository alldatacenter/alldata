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
package org.apache.ambari.server.metric.system.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.apache.ambari.server.metrics.system.impl.MetricsConfiguration;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;

public class TestAmbariMetricsSinkImpl extends AbstractTimelineMetricsSink implements MetricsSink {

  @Override
  public void publish(List<SingleMetric> metrics) {
    LOG.info("Published " + metrics.size() + " metrics.");
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(getCollectorProtocol(), host, getCollectorPort());
  }

  @Override
  protected String getCollectorProtocol() {
    return "http";
  }

  @Override
  protected String getCollectorPort() {
    return "6188";
  }

  @Override
  protected int getTimeoutSeconds() {
    return 1000;
  }

  @Override
  protected String getZookeeperQuorum() {
    return null;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return Collections.singletonList("localhost");
  }

  @Override
  protected String getHostname() {
    return "localhost";
  }

  protected boolean isHostInMemoryAggregationEnabled() {
    return true;
  }

  protected int getHostInMemoryAggregationPort() {
    return 61888;
  }

  @Override
  protected String getHostInMemoryAggregationProtocol() {
    return "http";
  }

  @Override
  public void init(MetricsConfiguration configuration) {

  }
}
