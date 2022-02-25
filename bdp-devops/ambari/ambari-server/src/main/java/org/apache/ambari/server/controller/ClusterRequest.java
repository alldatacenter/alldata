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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.state.SecurityType;
import org.apache.commons.lang.StringUtils;

import io.swagger.annotations.ApiModelProperty;

/**
 * Used for create Cluster
 */
public class ClusterRequest {

  private final Long clusterId; // for GET
  private final String clusterName; // for GET/CREATE/UPDATE
  private final String stackVersion; // for CREATE/UPDATE
  private final String provisioningState; // for GET/CREATE/UPDATE
  private SecurityType securityType; // for GET/CREATE/UPDATE
  private Set<String> hostNames; // CREATE/UPDATE
  private List<ConfigurationRequest> configs;
  private ServiceConfigVersionRequest serviceConfigVersionRequest;
  private final Map<String, Object> sessionAttributes;


  // ----- Constructors ------------------------------------------------------

  public ClusterRequest(Long clusterId, String clusterName,
      String stackVersion, Set<String> hostNames) {
    this(clusterId, clusterName, null, null, stackVersion, hostNames);
  }

  public ClusterRequest(Long clusterId, String clusterName,
      String provisioningState, SecurityType securityType, String stackVersion, Set<String> hostNames) {
    this(clusterId, clusterName, provisioningState, securityType, stackVersion, hostNames, null);
  }

  /**
   * @param provisioningState whether the cluster is still initializing or has finished with its deployment requests:
   *                          either {@code INIT} or {@code INSTALLED}, or {@code null} if not set on the request.
   */
  public ClusterRequest(Long clusterId, String clusterName,
                        String provisioningState, SecurityType securityType, String stackVersion,
                        Set<String> hostNames, Map<String, Object> sessionAttributes) {
    this.clusterId         = clusterId;
    this.clusterName       = clusterName;
    this.provisioningState = provisioningState;
    this.securityType      = securityType;
    this.stackVersion      = stackVersion;
    this.hostNames         = hostNames;
    this.sessionAttributes = sessionAttributes;
  }


  // ----- ClusterRequest ----------------------------------------------------

  @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_ID)
  public Long getClusterId() {
    return clusterId;
  }

  @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_NAME)
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @return either {@code INIT} or {@code INSTALLED} or {@code null} if not set
   *         on the request.
   */
  @ApiModelProperty(name = ClusterResourceProvider.PROVISIONING_STATE)
  public String getProvisioningState(){
    return provisioningState;
  }

  /**
   * Gets the cluster's security type.
   *
   * @return a SecurityType declaring the security type; or {@code null} if not set set on the request
   */
  @ApiModelProperty(name = ClusterResourceProvider.SECURITY_TYPE)
  public SecurityType getSecurityType() {
    return securityType;
  }

  @ApiModelProperty(name = ClusterResourceProvider.VERSION)
  public String getStackVersion() {
    return stackVersion;
  }

  @ApiModelProperty(hidden = true)
  public Set<String> getHostNames() {
    return hostNames;
  }

  /**
   * Sets the configs requests (if any).
   *
   * @param configRequests  the list of configuration requests
   */
  public void setDesiredConfig(List<ConfigurationRequest> configRequests) {
    configs = configRequests;
  }

  /**
   * Gets any configuration-based request (if any).
   * @return the list of configuration requests,
   * or <code>null</code> if none is set.
   */
  @ApiModelProperty(name = ClusterResourceProvider.DESIRED_CONFIGS)
  public List<ConfigurationRequest> getDesiredConfig() {
    return configs;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{" + " clusterName=").append(clusterName)
        .append(", clusterId=").append(clusterId)
        .append(", provisioningState=").append(provisioningState)
        .append(", securityType=").append(securityType)
        .append(", stackVersion=").append(stackVersion)
        .append(", desired_scv=").append(serviceConfigVersionRequest)
        .append(", hosts=[")
        .append(hostNames != null ? String.join(",", hostNames) : StringUtils.EMPTY)
        .append("] }");
    return sb.toString();
  }

  @ApiModelProperty(name = ClusterResourceProvider.DESIRED_SERVICE_CONFIG_VERSIONS)
  public ServiceConfigVersionRequest getServiceConfigVersionRequest() {
    return serviceConfigVersionRequest;
  }

  /**
   * Get the session attributes of this request.
   *
   * @return the session attributes; may be null
   */
  @ApiModelProperty(hidden = true)
  public Map<String, Object> getSessionAttributes() {
    return sessionAttributes;
  }

  public void setServiceConfigVersionRequest(ServiceConfigVersionRequest serviceConfigVersionRequest) {
    this.serviceConfigVersionRequest = serviceConfigVersionRequest;
  }

}
