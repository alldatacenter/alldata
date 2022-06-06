/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.controller.internal;

import static java.util.Collections.singleton;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.UriInfo;

/**
 * A special {@link JMXHostProvider} that resolves JMX URIs based on cluster configuration.
 */
public class ConfigBasedJmxHostProvider implements JMXHostProvider {
  private final Map<String, UriInfo> overriddenJmxUris;
  private final JMXHostProvider defaultProvider;
  private final ConfigHelper configHelper;

  public ConfigBasedJmxHostProvider(Map<String, UriInfo> overriddenJmxUris, JMXHostProvider defaultProvider, ConfigHelper configHelper) {
    this.overriddenJmxUris = overriddenJmxUris;
    this.defaultProvider = defaultProvider;
    this.configHelper = configHelper;
  }

  @Override
  public String getPublicHostName(String clusterName, String hostName) {
    return defaultProvider.getPublicHostName(clusterName, hostName);
  }

  @Override
  public Set<String> getHostNames(String clusterName, String componentName) {
    return overridenJmxUri(componentName)
      .map(uri -> singleton(resolve(uri, clusterName).getHost()))
      .orElseGet(() -> defaultProvider.getHostNames(clusterName, componentName));
  }

  private URI resolve(UriInfo uri, String clusterName) {
    try {
      return uri.resolve(config(clusterName));
    } catch (AmbariException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getPort(String clusterName, String componentName, String hostName, boolean httpsEnabled) {
    return overridenJmxUri(componentName)
      .map(uri -> String.valueOf(resolve(uri, clusterName).getPort()))
      .orElseGet(() -> defaultProvider.getPort(clusterName, componentName, hostName, httpsEnabled));
  }

  @Override
  public String getJMXProtocol(String clusterName, String componentName) {
    return overridenJmxUri(componentName)
      .map(uri -> resolve(uri, clusterName).getScheme())
      .orElseGet(() -> defaultProvider.getJMXProtocol(clusterName, componentName));
  }

  @Override
  public String getJMXRpcMetricTag(String clusterName, String componentName, String port) {
    return defaultProvider.getJMXRpcMetricTag(clusterName, componentName, port);
  }

  private Optional<UriInfo> overridenJmxUri(String component) {
    return Optional.ofNullable(overriddenJmxUris.get(component));
  }

  private Map<String, Map<String, String>> config(String clusterName) throws AmbariException {
    return configHelper.getEffectiveConfigProperties(clusterName, null);
  }
}
