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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.state.ClusterHealthReport;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.State;

import io.swagger.annotations.ApiModelProperty;

public class ClusterResponse {

  private final long clusterId;
  private final String clusterName;
  private final Set<String> hostNames;
  private final String desiredStackVersion;
  private final State provisioningState;
  private final SecurityType securityType;
  private final int totalHosts;

  private Map<String, DesiredConfig> desiredConfigs;
  private Map<String, Collection<ServiceConfigVersionResponse>> desiredServiceConfigVersions;
  private ClusterHealthReport clusterHealthReport;
  private Map<String, String> credentialStoreServiceProperties;

  public ClusterResponse(long clusterId, String clusterName,
                         State provisioningState, SecurityType securityType, Set<String> hostNames, int totalHosts,
                         String desiredStackVersion, ClusterHealthReport clusterHealthReport) {

    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.hostNames = hostNames;
    this.totalHosts = totalHosts;
    this.desiredStackVersion = desiredStackVersion;
    this.clusterHealthReport = clusterHealthReport;

    if (null != provisioningState) {
      this.provisioningState = provisioningState;
    } else {
      this.provisioningState = State.UNKNOWN;
    }

    if (null == securityType) {
      this.securityType = SecurityType.NONE;
    } else {
      this.securityType = securityType;
    }
  }

  /**
   * @return the clusterId
   */
  @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_ID)
  public long getClusterId() {
    return clusterId;
  }

  /**
   * @return the clusterName
   */
  @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_NAME)
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @return either {@code INIT} or {@code INSTALLED}, never {@code null}.
   */
  @ApiModelProperty(name = ClusterResourceProvider.PROVISIONING_STATE)
  public State getProvisioningState() {
    return provisioningState;
  }

  /**
   * Gets the cluster's security type.
   * <p/>
   * See {@link org.apache.ambari.server.state.SecurityType} for relevant values.
   *
   * @return the cluster's security type
   */
  @ApiModelProperty(name = ClusterResourceProvider.SECURITY_TYPE)
  public SecurityType getSecurityType() {
    return securityType;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ clusterName=").append(clusterName)
      .append(", clusterId=").append(clusterId)
      .append(", provisioningState=").append(provisioningState)
      .append(", desiredStackVersion=").append(desiredStackVersion)
      .append(", totalHosts=").append(totalHosts)
      .append(", hosts=[");

    if (hostNames != null) {
      int i = 0;
      for (String hostName : hostNames) {
        if (i != 0) {
          sb.append(",");
        }
        ++i;
        sb.append(hostName);
      }
    }
    sb.append("], clusterHealthReport= ").append(clusterHealthReport).append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterResponse other = (ClusterResponse) o;

    return Objects.equals(clusterId, other.clusterId) &&
      Objects.equals(clusterName, other.clusterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterId, clusterName);
  }

  /**
   * @return the desiredStackVersion
   */
  @ApiModelProperty(name = ClusterResourceProvider.VERSION)
  public String getDesiredStackVersion() {
    return desiredStackVersion;
  }

  public void setDesiredConfigs(Map<String, DesiredConfig> configs) {
    desiredConfigs = configs;
  }

  /**
   * @return the desired configs
   */
  @ApiModelProperty(name = ClusterResourceProvider.DESIRED_CONFIGS)
  public Map<String, DesiredConfig> getDesiredConfigs() {
    return desiredConfigs;
  }

  /**
   * @return total number of hosts in the cluster
   */
  @ApiModelProperty(name = ClusterResourceProvider.TOTAL_HOSTS)
  public int getTotalHosts() {
    return totalHosts;
  }

  /**
   * @return cluster health report
   */
  @ApiModelProperty(name = ClusterResourceProvider.HEALTH_REPORT)
  public ClusterHealthReport getClusterHealthReport() {
    return clusterHealthReport;
  }

  @ApiModelProperty(name = ClusterResourceProvider.DESIRED_SERVICE_CONFIG_VERSIONS)
  public Map<String, Collection<ServiceConfigVersionResponse>> getDesiredServiceConfigVersions() {
    return desiredServiceConfigVersions;
  }

  public void setDesiredServiceConfigVersions(Map<String, Collection<ServiceConfigVersionResponse>> desiredServiceConfigVersions) {
    this.desiredServiceConfigVersions = desiredServiceConfigVersions;
  }

  public void setCredentialStoreServiceProperties(Map<String, String> credentialServiceProperties) {
    this.credentialStoreServiceProperties = credentialServiceProperties;
  }

  @ApiModelProperty(name = ClusterResourceProvider.CREDENTIAL_STORE_PROPERTIES)
  public Map<String, String> getCredentialStoreServiceProperties() {
    return credentialStoreServiceProperties;
  }

  public interface ClusterResponseWrapper extends ApiModel {
    @ApiModelProperty(name = ClusterResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    ClusterResponse getClusterResponse();
  }
}
