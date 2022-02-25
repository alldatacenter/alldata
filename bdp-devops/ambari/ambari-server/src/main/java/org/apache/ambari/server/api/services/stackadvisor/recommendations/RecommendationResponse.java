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

package org.apache.ambari.server.api.services.stackadvisor.recommendations;

import static com.google.common.collect.Maps.transformValues;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.ConfigurableHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.collect.ImmutableMap;

/**
 * Recommendation response POJO.
 */
public class RecommendationResponse extends StackAdvisorResponse {

  @JsonProperty
  private Set<String> hosts;

  @JsonProperty
  private Set<String> services;

  @JsonProperty
  private Recommendation recommendations;

  public Set<String> getHosts() {
    return hosts;
  }

  public void setHosts(Set<String> hosts) {
    this.hosts = hosts;
  }

  public Set<String> getServices() {
    return services;
  }

  public void setServices(Set<String> services) {
    this.services = services;
  }

  public Recommendation getRecommendations() {
    return recommendations;
  }

  public void setRecommendations(Recommendation recommendations) {
    this.recommendations = recommendations;
  }

  public static class Recommendation {
    @JsonProperty
    private Blueprint blueprint;

    @JsonProperty("blueprint_cluster_binding")
    private BlueprintClusterBinding blueprintClusterBinding;

    @JsonProperty("config-groups")
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Set<ConfigGroup> configGroups;

    public Blueprint getBlueprint() {
      return blueprint;
    }

    public void setBlueprint(Blueprint blueprint) {
      this.blueprint = blueprint;
    }

    public BlueprintClusterBinding getBlueprintClusterBinding() {
      return blueprintClusterBinding;
    }

    public void setBlueprintClusterBinding(BlueprintClusterBinding blueprintClusterBinding) {
      this.blueprintClusterBinding = blueprintClusterBinding;
    }

    public Set<ConfigGroup> getConfigGroups() {
      return configGroups;
    }

    public void setConfigGroups(Set<ConfigGroup> configGroups) {
      this.configGroups = configGroups;
    }
  }

  public static class Blueprint {
    @JsonProperty
    private Map<String, BlueprintConfigurations> configurations;

    @JsonProperty("host_groups")
    private Set<HostGroup> hostGroups;

    public Map<String, BlueprintConfigurations> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(Map<String, BlueprintConfigurations> configurations) {
      this.configurations = configurations;
    }

    public Set<HostGroup> getHostGroups() {
      return hostGroups;
    }

    public void setHostGroups(Set<HostGroup> hostGroups) {
      this.hostGroups = hostGroups;
    }

    public Map<String, Set<String>> getHostgroupComponentMap() {
      return hostGroups.stream()
        .flatMap(hg -> hg.getComponentNames().stream().map(comp -> Pair.of(hg.getName(), comp)))
        .collect(groupingBy(Pair::getKey, mapping(Pair::getValue, toSet())));
    }
  }

  public static class BlueprintConfigurations {
    @JsonProperty
    private final Map<String, String> properties = new HashMap<>();

    @JsonProperty("property_attributes")
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Map<String, ValueAttributesInfo> propertyAttributes = null;

    /**
     *
     * @param properties properties in <i>name -> value</i> format
     * @param attributes attributes in <i>attribute name -> property name -> value</i> format
     */
    public static BlueprintConfigurations create(Map<String, String> properties, Map<String, Map<String, String>> attributes) {
      BlueprintConfigurations config = new BlueprintConfigurations();
      config.setProperties(properties);
      if (attributes != null) {
        // transform map to property name -> attribute name -> value format
        Map<String, Map<String, String>> transformedAttributes = ConfigurableHelper.transformAttributesMap(attributes);
        ObjectMapper mapper = new ObjectMapper();
        config.setPropertyAttributes(
          new HashMap<>(transformValues(transformedAttributes, attr -> ValueAttributesInfo.fromMap(attr, Optional.of(mapper)))));
      }
      return config;
    }

    public BlueprintConfigurations() {

    }

    public Map<String, String> getProperties() {
      return properties;
    }

    /**
     * Returns a map of properties for this configuration.
     * <p/>
     * It is expected that a non-null value is always returned.
     *
     * @param properties a map of properties, always non-null
     */
    public void setProperties(Map<String, String> properties) {
      this.properties.clear();
      if(properties != null) {
        this.properties.putAll(properties);
      }
    }

    public Map<String, ValueAttributesInfo> getPropertyAttributes() {
      return propertyAttributes;
    }

    /**
     * @return value attributes in <i>attribute name -> property name -> value</i> format
     */
    @JsonIgnore
    public Map<String, Map<String, String>> getPropertyAttributesAsMap() {
      ObjectMapper mapper = new ObjectMapper();
      return null == propertyAttributes ? null :
        ConfigurableHelper.transformAttributesMap( transformValues(propertyAttributes, vaInfo -> vaInfo.toMap(Optional.of(mapper))) );
    }

    public void setPropertyAttributes(Map<String, ValueAttributesInfo> propertyAttributes) {
      this.propertyAttributes = propertyAttributes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BlueprintConfigurations that = (BlueprintConfigurations) o;
      return Objects.equals(properties, that.properties) &&
          Objects.equals(propertyAttributes, that.propertyAttributes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(properties, propertyAttributes);
    }
  }

  public static class HostGroup {
    @JsonProperty
    private String name;

    @JsonProperty
    private Set<Map<String, String>> components;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Set<Map<String, String>> getComponents() {
      return components;
    }

    public void setComponents(Set<Map<String, String>> components) {
      this.components = components;
    }

    @JsonIgnore
    public Set<String> getComponentNames() {
      return components.stream().map(comp -> comp.get("name")).collect(toSet());
    }

    public static Set<HostGroup> fromHostGroupComponents(Map<String, Set<String>> hostGroupComponents) {
      return hostGroupComponents.entrySet().stream()
        .map(entry -> create(entry.getKey(), entry.getValue()))
        .collect(toSet());
    }

    public static HostGroup create(String name, Set<String> componentNames) {
      HostGroup group = new HostGroup();
      group.setName(name);
      Set<Map<String, String>> components = componentNames.stream().map(comp -> ImmutableMap.of("name", comp)).collect(toSet());
      group.setComponents(components);
      return group;
    }
  }

  public static class BlueprintClusterBinding {
    @JsonProperty("host_groups")
    private Set<BindingHostGroup> hostGroups;

    public Set<BindingHostGroup> getHostGroups() {
      return hostGroups;
    }

    public void setHostGroups(Set<BindingHostGroup> hostGroups) {
      this.hostGroups = hostGroups;
    }

    @JsonIgnore
    public Map<String, Set<String>> getHostgroupHostMap() {
      return hostGroups.stream().collect(toMap(BindingHostGroup::getName, BindingHostGroup::getHostNames));
    }

    public static BlueprintClusterBinding fromHostGroupHostMap(Map<String, Set<String>> hostGroupHosts) {
      Set<BindingHostGroup> hostGroups = hostGroupHosts.entrySet().stream()
        .map(entry -> BindingHostGroup.create(entry.getKey(), entry.getValue()))
        .collect(toSet());
      BlueprintClusterBinding binding = new BlueprintClusterBinding();
      binding.setHostGroups(hostGroups);
      return binding;
    }
  }

  public static class BindingHostGroup {
    @JsonProperty
    private String name;

    @JsonProperty
    private Set<Map<String, String>> hosts;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Set<Map<String, String>> getHosts() {
      return hosts;
    }

    public void setHosts(Set<Map<String, String>> hosts) {
      this.hosts = hosts;
    }

    @JsonIgnore
    public Set<String> getHostNames() {
      return hosts.stream().map(host -> host.get("fqdn")).collect(toSet());
    }

    public static BindingHostGroup create(String name, Set<String> hostNames) {
      BindingHostGroup hostGroup = new BindingHostGroup();
      hostGroup.setName(name);
      Set<Map<String, String>> hosts = hostNames.stream().map(hostName -> ImmutableMap.of("fqdn", hostName)).collect(toSet());
      hostGroup.setHosts(hosts);
      return hostGroup;
    }
  }

  public static class ConfigGroup {

    @JsonProperty
    private List<String> hosts;

    @JsonProperty
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Map<String, BlueprintConfigurations> configurations =
      new HashMap<>();

    @JsonProperty("dependent_configurations")
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Map<String, BlueprintConfigurations> dependentConfigurations =
      new HashMap<>();

    public ConfigGroup() {

    }

    public List<String> getHosts() {
      return hosts;
    }

    public void setHosts(List<String> hosts) {
      this.hosts = hosts;
    }

    public Map<String, BlueprintConfigurations> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(Map<String, BlueprintConfigurations> configurations) {
      this.configurations = configurations;
    }

    public Map<String, BlueprintConfigurations> getDependentConfigurations() {
      return dependentConfigurations;
    }

    public void setDependentConfigurations(Map<String, BlueprintConfigurations> dependentConfigurations) {
      this.dependentConfigurations = dependentConfigurations;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConfigGroup that = (ConfigGroup) o;
      return Objects.equals(hosts, that.hosts) &&
          Objects.equals(configurations, that.configurations) &&
          Objects.equals(dependentConfigurations, that.dependentConfigurations);
    }

    @Override
    public int hashCode() {
      return Objects.hash(hosts, configurations, dependentConfigurations);
    }
  }

}
