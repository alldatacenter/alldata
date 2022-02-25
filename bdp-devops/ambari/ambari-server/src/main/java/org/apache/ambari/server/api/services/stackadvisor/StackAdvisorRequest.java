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

package org.apache.ambari.server.api.services.stackadvisor;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.state.ChangedConfigInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Stack advisor request.
 */
public class StackAdvisorRequest {

  private Long clusterId;
  private String serviceName;
  private String stackName;
  private String stackVersion;
  private StackAdvisorRequestType requestType;
  private List<String> hosts = new ArrayList<>();
  private Collection<String> services = new ArrayList<>();
  private Map<String, Set<String>> componentHostsMap = new HashMap<>();
  private Map<String, Set<String>> hostComponents = new HashMap<>();
  private Map<String, Set<String>> hostGroupBindings = new HashMap<>();
  private Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
  private List<ChangedConfigInfo> changedConfigurations = new LinkedList<>();
  private Set<RecommendationResponse.ConfigGroup> configGroups;
  private Map<String, String> userContext = new HashMap<>();
  private Map<String, Object> ldapConfig = new HashMap<>();
  private Boolean gplLicenseAccepted;
  private Boolean configsResponse = false;

  public String getStackName() {
    return stackName;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public StackAdvisorRequestType getRequestType() {
    return requestType;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public Collection<String> getServices() {
    return services;
  }

  public Map<String, Set<String>> getComponentHostsMap() {
    return componentHostsMap;
  }

  public String getHostsCommaSeparated() {
    return StringUtils.join(hosts, ",");
  }

  public String getServicesCommaSeparated() {
    return StringUtils.join(services, ",");
  }

  public Map<String, Set<String>> getHostComponents() {
    return hostComponents;
  }

  public Map<String, Set<String>> getHostGroupBindings() {
    return hostGroupBindings;
  }

  public Map<String, Map<String, Map<String, String>>> getConfigurations() {
    return configurations;
  }

  public Map<String, Object> getLdapConfig() { return ldapConfig; }

  public List<ChangedConfigInfo> getChangedConfigurations() {
    return changedConfigurations;
  }

  public void setChangedConfigurations(List<ChangedConfigInfo> changedConfigurations) {
    this.changedConfigurations = changedConfigurations;
  }

  public Map<String, String> getUserContext() {
    return this.userContext;
  }

  public void setUserContext(Map<String, String> userContext) {
    this.userContext = userContext;
  }

  public Set<RecommendationResponse.ConfigGroup> getConfigGroups() {
    return configGroups;
  }

  public void setConfigGroups(Set<RecommendationResponse.ConfigGroup> configGroups) {
    this.configGroups = configGroups;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public String getServiceName() {
    return serviceName;
  }

  /**
   * @return true if GPL license is accepted, false otherwise
   */
  public Boolean getGplLicenseAccepted() {
    return gplLicenseAccepted;
  }

  public Boolean getConfigsResponse() {
    return configsResponse;
  }

  private StackAdvisorRequest(String stackName, String stackVersion) {
    this.stackName = stackName;
    this.stackVersion = stackVersion;
  }

  public StackAdvisorRequestBuilder builder() {
    return StackAdvisorRequestBuilder.forStack(stackName, stackVersion)
      .ofType(requestType)
      .forHosts(hosts)
      .forServices(services)
      .forHostComponents(hostComponents)
      .forHostsGroupBindings(hostGroupBindings)
      .withComponentHostsMap(componentHostsMap)
      .withConfigurations(configurations)
      .withChangedConfigurations(changedConfigurations)
      .withConfigGroups(configGroups)
      .withUserContext(userContext)
      .withGPLLicenseAccepted(gplLicenseAccepted)
      .withLdapConfig(ldapConfig);
  }

  public static class StackAdvisorRequestBuilder {
    StackAdvisorRequest instance;

    private StackAdvisorRequestBuilder(String stackName, String stackVersion) {
      this.instance = new StackAdvisorRequest(stackName, stackVersion);
    }

    public static StackAdvisorRequestBuilder forStack(StackId stackId) {
      return forStack(stackId.getStackName(), stackId.getStackVersion());
    }

    public static StackAdvisorRequestBuilder forStack(String stackName, String stackVersion) {
      return new StackAdvisorRequestBuilder(stackName, stackVersion);
    }

    public StackAdvisorRequestBuilder ofType(StackAdvisorRequestType requestType) {
      this.instance.requestType = requestType;
      return this;
    }

    public StackAdvisorRequestBuilder forHosts(List<String> hosts) {
      this.instance.hosts = hosts;
      return this;
    }

    public StackAdvisorRequestBuilder forServices(Collection<String> services) {
      this.instance.services = services;
      return this;
    }

    public StackAdvisorRequestBuilder withComponentHostsMap(
        Map<String, Set<String>> componentHostsMap) {
      this.instance.componentHostsMap = componentHostsMap;
      return this;
    }

    public StackAdvisorRequestBuilder forHostComponents(Map<String, Set<String>> hostComponents) {
      this.instance.hostComponents = hostComponents;
      return this;
    }

    public StackAdvisorRequestBuilder forHostsGroupBindings(
        Map<String, Set<String>> hostGroupBindings) {
      this.instance.hostGroupBindings = hostGroupBindings;
      return this;
    }

    public StackAdvisorRequestBuilder withConfigurations(
        Map<String, Map<String, Map<String, String>>> configurations) {
      this.instance.configurations = configurations;
      return this;
    }

    public StackAdvisorRequestBuilder withConfigurations(Configuration configuration) {
      Map<String, Map<String, String>> properties = configuration.getFullProperties();
      this.instance.configurations = properties.entrySet().stream()
        .map( e -> Pair.of(e.getKey(), ImmutableMap.of("properties", e.getValue())))
        .collect(toMap(Pair::getKey, Pair::getValue));
      return this;
    }


    public StackAdvisorRequestBuilder withChangedConfigurations(
      List<ChangedConfigInfo> changedConfigurations) {
      this.instance.changedConfigurations = changedConfigurations;
      return this;
    }

    public StackAdvisorRequestBuilder withUserContext(
        Map<String, String> userContext) {
      this.instance.userContext = userContext;
      return this;
    }

    public StackAdvisorRequestBuilder withConfigGroups(
        Set<RecommendationResponse.ConfigGroup> configGroups) {
      this.instance.configGroups = configGroups;
      return this;
    }

    /**
     * Set GPL license acceptance parameter to request.
     * @param gplLicenseAccepted is GPL license accepted.
     * @return stack advisor request builder.
     */
    public StackAdvisorRequestBuilder withGPLLicenseAccepted(
        Boolean gplLicenseAccepted) {
      this.instance.gplLicenseAccepted = gplLicenseAccepted;
      return this;
    }

    public StackAdvisorRequestBuilder withLdapConfig(Map<String, Object> ldapConfig) {
      Preconditions.checkNotNull(ldapConfig);
      this.instance.ldapConfig = ldapConfig;
      return this;
    }

    public StackAdvisorRequestBuilder withClusterId(Long clusterId) {
      this.instance.clusterId = clusterId;
      return this;
    }

    public StackAdvisorRequestBuilder withServiceName(String serviceName) {
      this.instance.serviceName = serviceName;
      return this;
    }

    public StackAdvisorRequestBuilder withConfigsResponse(
        Boolean configsResponse) {
      this.instance.configsResponse = configsResponse;
      return this;
    }

    public StackAdvisorRequest build() {
      return this.instance;
    }
  }

  public enum StackAdvisorRequestType {
    HOST_GROUPS("host_groups"),
    CONFIGURATIONS("configurations"),
    LDAP_CONFIGURATIONS("ldap-configurations"),
    SSO_CONFIGURATIONS("sso-configurations"),
    KERBEROS_CONFIGURATIONS("kerberos-configurations"),
    CONFIGURATION_DEPENDENCIES("configuration-dependencies");

    private String type;

    StackAdvisorRequestType(String type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return type;
    }

    public static StackAdvisorRequestType fromString(String text) throws StackAdvisorException {
      if (text != null) {
        for (StackAdvisorRequestType next : StackAdvisorRequestType.values()) {
          if (text.equalsIgnoreCase(next.type)) {
            return next;
          }
        }
      }
      throw new StackAdvisorException(String.format(
          "Unknown request type: %s, possible values: %s", text,
          Arrays.toString(StackAdvisorRequestType.values())));
    }
  }
}
