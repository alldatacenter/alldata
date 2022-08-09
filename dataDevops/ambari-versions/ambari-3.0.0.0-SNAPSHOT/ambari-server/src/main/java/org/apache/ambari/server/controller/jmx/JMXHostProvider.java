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
package org.apache.ambari.server.controller.jmx;

import java.util.Set;

/**
 * Provider of JMX host information.
 */
public interface JMXHostProvider {

  String getPublicHostName(String clusterName, String hostName);

  /**
   * Get the JMX host names for the given cluster name and component name.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   *
   * @return set of JMX host names
   *
   */
  Set<String> getHostNames(String clusterName, String componentName);

  /**
   * Get the port for the specified cluster name and component.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   * @param hostName       the component hostName
   * @param httpsEnabled   https enabled
   *
   * @return the port for the specified cluster name and component
   */
  String getPort(String clusterName, String componentName, String hostName, boolean httpsEnabled);

  /**
   * Get the protocol for the specified cluster name and component.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   *
   * @return the JMX protocol for the specified cluster name and component, one of http or https
   *
   */
  String getJMXProtocol(String clusterName, String componentName) ;
  
  /**
   * Get the rpc tag for the specified cluster name, component and port number
   *
   * @param clusterName    the cluster name
   * @param componentName  the componentName name
   * @param port           the port number
   *
   * @return the RPC tag for the specified cluster name, component and port number(client/healthcheck/etc.).
   *
   */
  String getJMXRpcMetricTag(String clusterName, String componentName, String port);

}
