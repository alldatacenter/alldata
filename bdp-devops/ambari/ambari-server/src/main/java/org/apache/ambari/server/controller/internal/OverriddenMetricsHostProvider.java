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
package org.apache.ambari.server.controller.internal;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;

/**
 * I'm a special {@link MetricHostProvider} that can override default component host names.
 */
public class OverriddenMetricsHostProvider implements MetricHostProvider {
  private final Map<String, String> overriddenHosts;
  private final MetricHostProvider metricHostProvider;
  private final ConfigHelper configHelper;
  private final VariableReplacementHelper variableReplacer = new VariableReplacementHelper();

  public OverriddenMetricsHostProvider(Map<String, String> overriddenHosts, MetricHostProvider metricHostProvider, ConfigHelper configHelper) {
    this.overriddenHosts = overriddenHosts;
    this.metricHostProvider = metricHostProvider;
    this.configHelper = configHelper;
  }

  @Override
  public Optional<String> getExternalHostName(String clusterName, String componentName) {
    return getOverriddenHost(componentName).map(host -> replaceVariables(clusterName, host));
  }

  @Override
  public boolean isCollectorHostExternal(String clusterName) {
    return metricHostProvider.isCollectorHostExternal(clusterName);
  }

  private Optional<String> getOverriddenHost(String componentName) {
    return Optional.ofNullable(overriddenHosts.get(componentName));
  }

  private String replaceVariables(String clusterName, String hostName) {
    try {
      return hostName(variableReplacer.replaceVariables(hostName, config(clusterName)));
    } catch (AmbariException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Map<String, String>> config(String clusterName) throws AmbariException {
    return configHelper.getEffectiveConfigProperties(clusterName, null);
  }

  private String hostName(String resolvedHost) throws AmbariException {
    return hasScheme(resolvedHost)
      ? URI.create(resolvedHost).getHost()
      : URI.create("any://" + resolvedHost).getHost();
  }

  private boolean hasScheme(String host) {
    return host.contains("://");
  }

  @Override
  public String getHostName(String clusterName, String componentName) throws SystemException {
    return metricHostProvider.getHostName(clusterName, componentName);
  }

  @Override
  public String getCollectorHostName(String clusterName, MetricsServiceProvider.MetricsService service) throws SystemException {
    return metricHostProvider.getCollectorHostName(clusterName, service);
  }

  @Override
  public String getCollectorPort(String clusterName, MetricsServiceProvider.MetricsService service) throws SystemException {
    return metricHostProvider.getCollectorPort(clusterName, service);
  }

  @Override
  public boolean isCollectorHostLive(String clusterName, MetricsServiceProvider.MetricsService service) throws SystemException {
    return metricHostProvider.isCollectorHostLive(clusterName, service);
  }

  @Override
  public boolean isCollectorComponentLive(String clusterName, MetricsServiceProvider.MetricsService service) throws SystemException {
    return metricHostProvider.isCollectorComponentLive(clusterName, service);
  }
}
