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
package org.apache.ambari.server.controller.metrics.timeline.cache;

import java.util.Set;

import org.apache.ambari.server.controller.spi.TemporalInfo;

/**
 * Cache contents represent metrics for an App / Cluster.
 * This is designed to work on the premise that a client makes same requests
 * over and over, so this caching strategy allows us to send a http request
 * for multiple metrics in the same query and ensure parallelization on the
 * metrics backend side as well.
 */
public class TimelineAppMetricCacheKey {
  private final Set<String> metricNames;
  private final String appId;
  private final String hostNames;
  private String spec;
  private TemporalInfo temporalInfo;

  public TimelineAppMetricCacheKey(Set<String> metricNames, String appId,
                                   String hostNames, TemporalInfo temporalInfo) {
    this.metricNames = metricNames;
    this.appId = appId;
    this.hostNames = hostNames;
    this.temporalInfo = temporalInfo;
  }

  public TimelineAppMetricCacheKey(Set<String> metricNames, String appId,
                                   TemporalInfo temporalInfo) {
    this(metricNames, appId, null, temporalInfo);
  }

  public Set<String> getMetricNames() {
    return metricNames;
  }

  /**
   * Temporal info is used to calculate the next query window,
   * it does not contribute to the key behavior.
   * @return @TemporalInfo
   */
  public TemporalInfo getTemporalInfo() {
    return temporalInfo;
  }

  /**
   * Set temporalInfo to new query window each time.
   * @param temporalInfo @TemporalInfo
   */
  public void setTemporalInfo(TemporalInfo temporalInfo) {
    this.temporalInfo = temporalInfo;
  }

  public String getAppId() {
    return appId;
  }

  public String getHostNames() {
    return hostNames;
  }

  /**
   * Actual http request Uri, this does not contribute to the key behavior,
   * it is used solely for interoperability between @AMSPropertyProvider.MetricsRequest
   * and the Cache.
   * @return Request Uri
   */
  public String getSpec() {
    return spec;
  }

  /**
   * Set spec string.
   * @param spec Request Uri
   */
  public void setSpec(String spec) {
    this.spec = spec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimelineAppMetricCacheKey that = (TimelineAppMetricCacheKey) o;

    if (!metricNames.equals(that.metricNames)) return false;
    if (!appId.equals(that.appId)) return false;
    return !(hostNames != null ? !hostNames.equals(that.hostNames) : that.hostNames != null);

  }

  @Override
  public int hashCode() {
    int result = metricNames.hashCode();
    result = 31 * result + appId.hashCode();
    result = 31 * result + (hostNames != null ? hostNames.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TimelineAppMetricCacheKey{" +
      "metricNames=" + metricNames +
      ", appId='" + appId + '\'' +
      ", hostNames=" + hostNames +
      ", spec='" + spec + '\'' +
      '}';
  }
}
