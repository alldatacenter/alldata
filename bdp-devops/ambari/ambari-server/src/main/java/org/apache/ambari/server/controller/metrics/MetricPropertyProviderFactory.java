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
package org.apache.ambari.server.controller.metrics;

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.utilities.StreamProvider;

import com.google.inject.assistedinject.Assisted;

/**
 * The {@link MetricPropertyProviderFactory} is used to provide injected
 * instances of {@link PropertyProvider}s which retrieve metrics.
 */
public interface MetricPropertyProviderFactory {

  /**
   * Gets a Guice-inject instance of a {@link JMXPropertyProvider}.
   *
   * @param componentMetrics
   *          the map of supported metrics
   * @param streamProvider
   *          the stream provider
   * @param jmxHostProvider
   *          the JMX host mapping
   * @param metricHostProvider
   *          the host mapping
   * @param clusterNamePropertyId
   *          the cluster name property id
   * @param hostNamePropertyId
   *          the host name property id
   * @param componentNamePropertyId
   *          the component name property id
   * @param statePropertyId
   *          the state property id
   * @return the instantiated and injected {@link JMXPropertyProvider}.
   */
  JMXPropertyProvider createJMXPropertyProvider(
      @Assisted("componentMetrics") Map<String, Map<String, PropertyInfo>> componentMetrics,
      @Assisted("streamProvider") StreamProvider streamProvider,
      @Assisted("jmxHostProvider") JMXHostProvider jmxHostProvider,
      @Assisted("metricHostProvider") MetricHostProvider metricHostProvider,
      @Assisted("clusterNamePropertyId") String clusterNamePropertyId,
      @Assisted("hostNamePropertyId") @Nullable String hostNamePropertyId,
      @Assisted("componentNamePropertyId") String componentNamePropertyId,
      @Assisted("statePropertyId") @Nullable String statePropertyId);

  /**
   * /** Create a REST property provider.
   *
   * @param metricsProperties
   *          the map of per-component metrics properties
   * @param componentMetrics
   *          the map of supported metrics for component
   * @param streamProvider
   *          the stream provider
   * @param metricHostProvider
   *          metricsHostProvider instance
   * @param clusterNamePropertyId
   *          the cluster name property id
   * @param hostNamePropertyId
   *          the host name property id, or {@code null} if none.
   * @param componentNamePropertyId
   *          the component name property id
   * @param statePropertyId
   *          the state property id or {@code null} if none.
   * @param componentName
   *          the name of the component which the metric is for, or {@code null}
   *          if none.
   * @return the instantiated and injected {@link RestMetricsPropertyProvider}.
   */
  RestMetricsPropertyProvider createRESTMetricsPropertyProvider(
      @Assisted("metricsProperties") Map<String, String> metricsProperties,
      @Assisted("componentMetrics") Map<String, Map<String, PropertyInfo>> componentMetrics,
      @Assisted("streamProvider") StreamProvider streamProvider,
      @Assisted("metricHostProvider") MetricHostProvider metricHostProvider,
      @Assisted("clusterNamePropertyId") String clusterNamePropertyId,
      @Assisted("hostNamePropertyId") @Nullable String hostNamePropertyId,
      @Assisted("componentNamePropertyId") String componentNamePropertyId,
      @Assisted("statePropertyId") @Nullable String statePropertyId,
      @Assisted("componentName") @Nullable String componentName);
}
