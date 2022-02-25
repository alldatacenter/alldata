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
package org.apache.ambari.server.controller;

import java.util.Map;

import org.apache.ambari.server.controller.internal.HostComponentProcessResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response object for HostComponent processes
 */
public class HostComponentProcessResponse {

  private String cluster;
  private String host;
  private String component;
  private Map<String, String> map;

  /**
   * Constructor
   * @param clusterName the cluster
   * @param hostName the host
   * @param componentName the component
   * @param processValueMap the map of process information
   */
  public HostComponentProcessResponse(String clusterName, String hostName,
      String componentName, Map<String, String> processValueMap) {
    cluster = clusterName;
    host = hostName;
    component = componentName;
    map = processValueMap;
  }
 
  /**
   * @return the cluster
   */
  @ApiModelProperty(name = HostComponentProcessResourceProvider.CLUSTER_NAME_PROPERTY_ID)
  public String getCluster() {
    return cluster;
  }
  
  /**
   * @return the host
   */
  @ApiModelProperty(name = HostComponentProcessResourceProvider.HOST_NAME_PROPERTY_ID)
  public String getHost() {
    return host;
  }
  
  /**
   * @return the component
   */
  @ApiModelProperty(name = HostComponentProcessResourceProvider.COMPONENT_NAME_PROPERTY_ID)
  public String getComponent() {
    return component;
  }
  
  /**
   * @return the map of values
   */
  public Map<String, String> getValueMap() {
    return map;
  }
  
  
}
