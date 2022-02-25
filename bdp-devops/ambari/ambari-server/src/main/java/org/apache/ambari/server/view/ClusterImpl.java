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

package org.apache.ambari.server.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.view.cluster.Cluster;

import com.google.common.collect.ImmutableMap;

/**
 * View associated cluster implementation.
 */
public class ClusterImpl implements Cluster {

  /**
   * The associated Ambari cluster.
   */
  private final org.apache.ambari.server.state.Cluster cluster;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a view associated cluster from an Ambari cluster.
   *
   * @param cluster  the Ambari cluster
   */
  public ClusterImpl(org.apache.ambari.server.state.Cluster cluster) {
    this.cluster = cluster;
  }


  // ----- Cluster -----------------------------------------------------------

  @Override
  public String getName() {
    return cluster.getClusterName();
  }

  @Override
  public String getConfigurationValue(String type, String key) {

    Config config = cluster.getDesiredConfigByType(type);

    return config == null ? null : config.getProperties().get(key);
  }

  @Override
  public Map<String, String> getConfigByType(String type) {
    Config configs = cluster.getDesiredConfigByType(type);
    return ImmutableMap.copyOf(configs.getProperties());
  }

  @Override
  public List<String> getHostsForServiceComponent(String serviceName, String componentName){
    List<ServiceComponentHost> serviceComponentHosts = cluster.getServiceComponentHosts(serviceName, componentName);
    List<String> hosts = new ArrayList<>();
    for (ServiceComponentHost serviceComponentHost : serviceComponentHosts) {
      hosts.add(serviceComponentHost.getHostName());
    }
    return hosts;
  }

}
