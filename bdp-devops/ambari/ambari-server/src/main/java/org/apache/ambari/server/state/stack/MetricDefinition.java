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

package org.apache.ambari.server.state.stack;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.ambari.server.state.UriInfo;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.annotations.SerializedName;

/**
 * Used to represent metrics for a stack component.
 */
public class MetricDefinition {
  private static final String OVERRIDDEN_HOST_PROP = "overridden_host";
  private String type = null;
  private Map<String, String> properties = null;
  private Map<String, Map<String, Metric>> metrics = null;
  @SerializedName("jmx_source_uri")
  private UriInfo jmxSourceUri;

  public MetricDefinition(String type, Map<String, String> properties, Map<String, Map<String, Metric>> metrics) {
    this.type = type;
    this.properties = properties;
    this.metrics = metrics;
  }

  public String getType() {
    return type;
  }
  
  public Map<String, String> getProperties() {
    return properties;
  }

  @JsonProperty("metrics")
  public Map<String, Map<String, Metric>> getMetricsByCategory() {
    return metrics;
  }

  /**
   * Return flat metric map without category
   */
  @JsonIgnore
  public Map<String, Metric> getMetrics() {
    Map<String, Metric> metricMap = new HashMap<>();
    for (Entry<String, Map<String, Metric>> metricMapEntry : metrics.entrySet()) {
      metricMap.putAll(metricMapEntry.getValue());
    }
    return metricMap;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{type=").append(type);
    sb.append(";properties=").append(properties);
    sb.append(";metric_count=").append(getMetrics().size());
    sb.append('}');
    
    return sb.toString();
  }

  public Optional<String> getOverriddenHosts() {
    return properties == null
      ? Optional.empty()
      : Optional.ofNullable(properties.get(OVERRIDDEN_HOST_PROP));
  }

  public Optional<UriInfo> getJmxSourceUri() {
    return !"jmx".equalsIgnoreCase(type) ? Optional.empty() : Optional.ofNullable(jmxSourceUri);
  }
}
