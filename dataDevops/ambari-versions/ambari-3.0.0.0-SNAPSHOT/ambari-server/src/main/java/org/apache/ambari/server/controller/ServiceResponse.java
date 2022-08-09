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

import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;

import io.swagger.annotations.ApiModelProperty;

public class ServiceResponse {

  private Long clusterId;
  private String clusterName;
  private String serviceName;
  private StackId desiredStackId;
  private String desiredRepositoryVersion;
  private Long desiredRepositoryVersionId;
  private RepositoryVersionState repositoryVersionState;
  private String desiredState;
  private String maintenanceState;
  private boolean credentialStoreSupported;
  private boolean credentialStoreEnabled;
  private final boolean ssoIntegrationSupported;
  private final boolean ssoIntegrationDesired;
  private final boolean ssoIntegrationEnabled;
  private final boolean ssoIntegrationRequiresKerberos;
  private final boolean kerberosEnabled;
  private final boolean ldapIntegrationSupported;
  private final boolean ldapIntegrationEnabled;
  private final boolean ldapIntegrationDesired;

  public ServiceResponse(Long clusterId, String clusterName, String serviceName,
                         StackId desiredStackId, String desiredRepositoryVersion,
                         RepositoryVersionState repositoryVersionState, String desiredState,
                         boolean credentialStoreSupported, boolean credentialStoreEnabled, boolean ssoIntegrationSupported,
                         boolean ssoIntegrationDesired, boolean ssoIntegrationEnabled, boolean ssoIntegrationRequiresKerberos,
                         boolean kerberosEnabled, boolean ldapIntegrationSupported,  boolean ldapIntegrationEnabled, boolean ldapIntegrationDesired) {
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.desiredStackId = desiredStackId;
    this.repositoryVersionState = repositoryVersionState;
    this.ssoIntegrationSupported = ssoIntegrationSupported;
    this.ssoIntegrationDesired = ssoIntegrationDesired;
    this.ssoIntegrationEnabled = ssoIntegrationEnabled;
    setDesiredState(desiredState);
    this.desiredRepositoryVersion = desiredRepositoryVersion;
    this.credentialStoreSupported = credentialStoreSupported;
    this.credentialStoreEnabled = credentialStoreEnabled;
    this.ssoIntegrationRequiresKerberos = ssoIntegrationRequiresKerberos;
    this.kerberosEnabled = kerberosEnabled;
    this.ldapIntegrationSupported = ldapIntegrationSupported;
    this.ldapIntegrationEnabled = ldapIntegrationEnabled;
    this.ldapIntegrationDesired = ldapIntegrationDesired;
  }

  /**
   * @return the serviceName
   */
  @ApiModelProperty(name = "service_name")
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName the serviceName to set
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return the clusterId
   */
  @ApiModelProperty(hidden = true)
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * @param clusterId the clusterId to set
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * @return the clusterName
   */
  @ApiModelProperty(name = "cluster_name")
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @return the desiredState
   */
  @ApiModelProperty(name = "state")
  public String getDesiredState() {
    return desiredState;
  }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  /**
   * @return the desired stack ID.
   */
  @ApiModelProperty(hidden = true)
  public String getDesiredStackId() {
    return desiredStackId.getStackId();

  }

  /**
   * Gets the desired repository version.
   *
   * @return the desired repository version.
   */
  public String getDesiredRepositoryVersion() {
    return desiredRepositoryVersion;
  }

  /**
   * Gets the calculated repository version state from the components of this
   * service.
   *
   * @return the desired repository version state
   */
  public RepositoryVersionState getRepositoryVersionState() {
    return repositoryVersionState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceResponse that = (ServiceResponse) o;

    if (clusterId != null ?
        !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }
    if (clusterName != null ?
        !clusterName.equals(that.clusterName) : that.clusterName != null) {
      return false;
    }
    if (serviceName != null ?
        !serviceName.equals(that.serviceName) : that.serviceName != null) {
      return false;
    }

    return true;
  }

  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  @ApiModelProperty(name = "maintenance_state")
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * Get a true or false value indicating if the service supports
   * credential store use or not.
   *
   * @return true or false
   */
  @ApiModelProperty(name = "credential_store_supported")
  public boolean isCredentialStoreSupported() {
    return credentialStoreSupported;
  }

  /**
   * Set a true or false value indicating whether the service
   * supports credential store or not.
   *
   * @param credentialStoreSupported
   */
  public void setCredentialStoreSupported(boolean credentialStoreSupported) {
    this.credentialStoreSupported = credentialStoreSupported;
  }

  /**
   * Get a true or false value indicating if the service is enabled
   * for credential store use or not.
   *
   * @return true or false
   */
  @ApiModelProperty(name = "credential_store_enabled")
  public boolean isCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  /**
   * Set a true or false value indicating whether the service is
   * enabled for credential store use or not.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.intValue() : 0;
    result = 71 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 71 * result + (serviceName != null ? serviceName.hashCode() : 0);
    return result;
  }

  /**
   * Indicates if this service supports single sign-on integration.
   */
  @ApiModelProperty(name = "sso_integration_supported")
  public boolean isSsoIntegrationSupported() {
    return ssoIntegrationSupported;
  }

  /**
   * Indicates whether the service is chosen for SSO integration or not
   */
  @ApiModelProperty(name = "sso_integration_desired")
  public boolean isSsoIntegrationDesired() {
    return ssoIntegrationDesired;
  }

  /**
   * Indicates whether the service is configured for SSO integration or not
   */
  @ApiModelProperty(name = "sso_integration_enabled")
  public boolean isSsoIntegrationEnabled() {
    return ssoIntegrationEnabled;
  }

  /**
   * Indicates if Kerberos is required for SSO integration
   */
  @ApiModelProperty(name = "sso_integration_requires_kerberos")
  public boolean isSsoIntegrationRequiresKerberos() {
    return ssoIntegrationRequiresKerberos;
  }

  /**
   * Indicates whether the service is configured for Kerberos or not
   */
  @ApiModelProperty(name = "kerberos_enabled")
  public boolean isKerberosEnabled() {
    return kerberosEnabled;
  }
  
  /**
   * Indicates if this service supports LDAP integration.
   */
  @ApiModelProperty(name = "ldap_integration_supported")
  public boolean isLdapIntegrationSupported() {
    return ldapIntegrationSupported;
  }

  /**
   * Indicates whether the service is configured for LDAP integration or not
   */
  @ApiModelProperty(name = "ldap_integration_enabled")
  public boolean isLdapIntegrationEnabled() {
    return ldapIntegrationEnabled;
  }

  /**
   * Indicates whether the service is chosen for LDAP integration or not
   */
  @ApiModelProperty(name = "ldap_integration_desired")
  public boolean isLdapIntegrationDesired() {
    return ldapIntegrationDesired;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface ServiceResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "ServiceInfo")
    ServiceResponse getServiceResponse();
  }

  /**
   * @param id the desired repository id
   */
  public void setDesiredRepositoryVersionId(Long id) {
    desiredRepositoryVersionId = id;
  }

  /**
   * @return the desired repository id
   */
  public Long getDesiredRepositoryVersionId() {
    return desiredRepositoryVersionId;
  }

}
