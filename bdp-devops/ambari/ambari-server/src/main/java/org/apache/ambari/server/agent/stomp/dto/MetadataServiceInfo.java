/**
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

package org.apache.ambari.server.agent.stomp.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;


public class MetadataServiceInfo {
  private String version;
  private Boolean credentialStoreEnabled;

  @JsonProperty("configuration_credentials")
  private Map<String, Map<String, String>> credentialStoreEnabledProperties;

  @JsonProperty("status_commands_timeout")
  private Long statusCommandsTimeout;

  @JsonProperty("service_package_folder")
  private String servicePackageFolder;

  public MetadataServiceInfo(String version, Boolean credentialStoreEnabled,
                             Map<String, Map<String, String>> credentialStoreEnabledProperties,
                             Long statusCommandsTimeout, String servicePackageFolder) {
    this.version = version;
    this.credentialStoreEnabled = credentialStoreEnabled;
    this.credentialStoreEnabledProperties = credentialStoreEnabledProperties;
    this.statusCommandsTimeout = statusCommandsTimeout;
    this.servicePackageFolder = servicePackageFolder;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Boolean getCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  public void setCredentialStoreEnabled(Boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

  public Map<String, Map<String, String>> getCredentialStoreEnabledProperties() {
    return credentialStoreEnabledProperties;
  }

  public void setCredentialStoreEnabledProperties(Map<String, Map<String, String>> credentialStoreEnabledProperties) {
    this.credentialStoreEnabledProperties = credentialStoreEnabledProperties;
  }

  public Long getStatusCommandsTimeout() {
    return statusCommandsTimeout;
  }

  public void setStatusCommandsTimeout(Long statusCommandsTimeout) {
    this.statusCommandsTimeout = statusCommandsTimeout;
  }

  public String getServicePackageFolder() {
    return servicePackageFolder;
  }

  public void setServicePackageFolder(String servicePackageFolder) {
    this.servicePackageFolder = servicePackageFolder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetadataServiceInfo that = (MetadataServiceInfo) o;

    if (version != null ? !version.equals(that.version) : that.version != null) return false;
    if (credentialStoreEnabled != null ? !credentialStoreEnabled.equals(that.credentialStoreEnabled) : that.credentialStoreEnabled != null)
      return false;
    if (credentialStoreEnabledProperties != null ? !credentialStoreEnabledProperties.equals(that.credentialStoreEnabledProperties) : that.credentialStoreEnabledProperties != null)
      return false;
    if (statusCommandsTimeout != null ? !statusCommandsTimeout.equals(that.statusCommandsTimeout) : that.statusCommandsTimeout != null)
      return false;
    return servicePackageFolder != null ? servicePackageFolder.equals(that.servicePackageFolder) : that.servicePackageFolder == null;
  }

  @Override
  public int hashCode() {
    int result = version != null ? version.hashCode() : 0;
    result = 31 * result + (credentialStoreEnabled != null ? credentialStoreEnabled.hashCode() : 0);
    result = 31 * result + (credentialStoreEnabledProperties != null ? credentialStoreEnabledProperties.hashCode() : 0);
    result = 31 * result + (statusCommandsTimeout != null ? statusCommandsTimeout.hashCode() : 0);
    result = 31 * result + (servicePackageFolder != null ? servicePackageFolder.hashCode() : 0);
    return result;
  }
}
