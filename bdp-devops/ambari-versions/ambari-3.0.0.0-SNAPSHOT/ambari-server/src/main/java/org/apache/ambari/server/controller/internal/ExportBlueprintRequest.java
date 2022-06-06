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

package org.apache.ambari.server.controller.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintImpl;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupImpl;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.TopologyRequest;

/**
 * Request to export a blueprint from an existing cluster.
 */
public class ExportBlueprintRequest implements TopologyRequest {

  private final AmbariManagementController controller;

  private final String clusterName;
  private final Long clusterId;
  private Blueprint blueprint;
  private final Configuration configuration;
  private final Map<String, HostGroupInfo> hostGroupInfo = new HashMap<>();


  public ExportBlueprintRequest(TreeNode<Resource> clusterNode, AmbariManagementController controller) throws InvalidTopologyTemplateException {
    this.controller = controller;

    Resource clusterResource = clusterNode.getObject();
    Stack stack = parseStack(clusterResource);
    clusterName = String.valueOf(clusterResource.getPropertyValue(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));
    clusterId = Long.valueOf(String.valueOf(clusterResource.getPropertyValue(
            ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID)));


    configuration = createConfiguration(clusterNode);

    Collection<ExportedHostGroup> exportedHostGroups = processHostGroups(clusterNode.getChild("hosts"));
    createHostGroupInfo(exportedHostGroups);
    createBlueprint(exportedHostGroups, stack);
  }

  public String getClusterName() {
    return clusterName;
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  @Override
  public Type getType() {
    return Type.EXPORT;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfo;
  }

  @Override
  public String getDescription() {
    return String.format("Export Command For Cluster '%s'", clusterName);
  }

  // ----- private instance methods ------------------------------------------


  private void createBlueprint(Collection<ExportedHostGroup> exportedHostGroups, Stack stack) {
    String bpName = "exported-blueprint";

    Collection<HostGroup> hostGroups = new ArrayList<>();
    for (ExportedHostGroup exportedHostGroup : exportedHostGroups) {

      // create Component using component name
      List<Component> componentList = new ArrayList<>();
      for (String component : exportedHostGroup.getComponents()) {
        componentList.add(new Component(component));
      }

      hostGroups.add(new HostGroupImpl(exportedHostGroup.getName(), bpName, stack, componentList,
          exportedHostGroup.getConfiguration(), String.valueOf(exportedHostGroup.getCardinality())));
    }
    blueprint = new BlueprintImpl(bpName, hostGroups, stack, configuration, null);
  }

  private void createHostGroupInfo(Collection<ExportedHostGroup> exportedHostGroups) {
    for (ExportedHostGroup exportedGroup : exportedHostGroups) {
      HostGroupInfo groupInfo = new HostGroupInfo(exportedGroup.getName());
      groupInfo.addHosts(exportedGroup.getHostInfo());
      groupInfo.setConfiguration(exportedGroup.getConfiguration());
      hostGroupInfo.put(groupInfo.getHostGroupName(), groupInfo);
    }
  }


  private Stack parseStack(Resource clusterResource) throws InvalidTopologyTemplateException {
    String[] stackTokens = String.valueOf(clusterResource.getPropertyValue(
        ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID)).split("-");

    try {
      return new Stack(stackTokens[0], stackTokens[1], controller);
    } catch (AmbariException e) {
      throw new InvalidTopologyTemplateException(String.format(
          "The specified stack doesn't exist: name=%s version=%s", stackTokens[0], stackTokens[1]));
    }
  }

  /**
   * Process cluster scoped configurations.
   *
   * @param clusterNode  cluster node
   */
  private static Configuration createConfiguration(TreeNode<Resource> clusterNode) {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();

    Map<String, Object> desiredConfigMap = clusterNode.getObject().getPropertiesMap().get("Clusters/desired_configs");
    TreeNode<Resource> configNode = clusterNode.getChild("configurations");
    for (TreeNode<Resource> config : configNode.getChildren()) {
      ExportedConfiguration configuration = new ExportedConfiguration(config);
      DesiredConfig desiredConfig = (DesiredConfig) desiredConfigMap.get(configuration.getType());
      if (desiredConfig != null && desiredConfig.getTag().equals(configuration.getTag())) {
        properties.put(configuration.getType(), configuration.getProperties());
        attributes.put(configuration.getType(), configuration.getPropertyAttributes());
      }
    }

    Configuration configuration = new Configuration(properties, attributes);
    configuration.setParentConfiguration(new Configuration(Collections.emptyMap(), Collections.emptyMap()));

    return configuration;
  }

  /**
   * Process cluster host groups.
   *
   * @param hostNode  host node
   *
   * @return collection of host groups
   */
  private Collection<ExportedHostGroup> processHostGroups(TreeNode<Resource> hostNode) {
    Map<ExportedHostGroup, ExportedHostGroup> mapHostGroups = new HashMap<>();
    int count = 1;
    for (TreeNode<Resource> host : hostNode.getChildren()) {
      ExportedHostGroup group = new ExportedHostGroup(host);
      String hostName = (String) host.getObject().getPropertyValue(
          PropertyHelper.getPropertyId("Hosts", "host_name"));

      if (mapHostGroups.containsKey(group)) {
        ExportedHostGroup hostGroup = mapHostGroups.get(group);
        hostGroup.incrementCardinality();
        hostGroup.addHost(hostName);
      } else {
        mapHostGroups.put(group, group);
        group.setName("host_group_" + count++);
        group.addHost(hostName);
      }
    }

    return mapHostGroups.values();
  }

  // ----- Host Group inner class --------------------------------------------

  /**
   * Host Group representation.
   */
  public class ExportedHostGroup {

    /**
     * Host Group name.
     *
     */
    private String name;

    /**
     * Associated components.
     */
    private final Set<String> components = new HashSet<>();

    /**
     * Host group scoped configurations.
     */
    private final Collection<ExportedConfiguration> configurations = new HashSet<>();

    /**
     * Number of instances.
     */
    private int m_cardinality = 1;

    /**
     * Collection of associated hosts.
     */
    private final Collection<String> hosts = new HashSet<>();

    /**
     * Constructor.
     *
     * @param host  host node
     */
    public ExportedHostGroup(TreeNode<Resource> host) {
      TreeNode<Resource> components = host.getChild("host_components");
      for (TreeNode<Resource> component : components.getChildren()) {
        getComponents().add((String) component.getObject().getPropertyValue(
            "HostRoles/component_name"));
      }
      addAmbariComponentIfLocalhost((String) host.getObject().getPropertyValue(
          PropertyHelper.getPropertyId("Hosts", "host_name")));

      processGroupConfiguration(host);
    }

    public Configuration getConfiguration() {
      Map<String, Map<String, String>> configProperties = new HashMap<>();
      Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();

      for (ExportedConfiguration config : configurations) {
        configProperties.put(config.getType(), config.getProperties());
        configAttributes.put(config.getType(), config.getPropertyAttributes());
      }

      return new Configuration(configProperties, configAttributes);
    }

    /**
     * Process host group configuration.
     *
     * @param host  host node
     */
    private void processGroupConfiguration(TreeNode<Resource> host) {
      Map<String, Object> desiredConfigMap = host.getObject().getPropertiesMap().get("Hosts/desired_configs");
      if (desiredConfigMap != null) {
        for (Map.Entry<String, Object> entry : desiredConfigMap.entrySet()) {
          String type = entry.getKey();
          HostConfig hostConfig = (HostConfig) entry.getValue();
          Map<Long, String> overrides = hostConfig.getConfigGroupOverrides();

          if (overrides != null && ! overrides.isEmpty()) {
            Long version = Collections.max(overrides.keySet());
            String tag = overrides.get(version);
            TreeNode<Resource> clusterNode = host.getParent().getParent();
            TreeNode<Resource> configNode = clusterNode.getChild("configurations");
            for (TreeNode<Resource> config : configNode.getChildren()) {
              ExportedConfiguration configuration = new ExportedConfiguration(config);
              if (type.equals(configuration.getType()) && tag.equals(configuration.getTag())) {
                getConfigurations().add(configuration);
                break;
              }
            }
          }
        }
      }
    }

    public String getName() {
      return name;
    }

    public Set<String> getComponents() {
      return components;
    }

    public Collection<String> getHostInfo() {
      return hosts;
    }


    /**
     * Set the name.
     *
     * @param  name name of host group
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Add a host.
     *
     * @param host  host to add
     */
    public void addHost(String host) {
      hosts.add(host);
    }

    /**
     * Obtain associated host group scoped configurations.
     *
     * @return collection of host group scoped configurations
     */
    public Collection<ExportedConfiguration> getConfigurations() {
      return configurations;
    }

    /**
     * Obtain the number of instances associated with this host group.
     *
     * @return number of hosts associated with this host group
     */
    public int getCardinality() {
      return m_cardinality;
    }

    /**
     * Increment the cardinality count by one.
     */
    public void incrementCardinality() {
      m_cardinality += 1;
    }

    /**
     * Add the AMBARI_SERVER component if the host is the local host.
     *
     * @param hostname  host to check
     */
    private void addAmbariComponentIfLocalhost(String hostname) {
      try {
        InetAddress hostAddress = InetAddress.getByName(hostname);
        try {
          if (hostAddress.equals(InetAddress.getLocalHost())) {
            getComponents().add("AMBARI_SERVER");
          }
        } catch (UnknownHostException e) {
          throw new RuntimeException("Unable to obtain local host name", e);
        }
      } catch (UnknownHostException e) {
        // ignore
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ExportedHostGroup hostGroup = (ExportedHostGroup) o;

      return components.equals(hostGroup.components) &&
          configurations.equals(hostGroup.configurations);
    }

    @Override
    public int hashCode() {
      int result = components.hashCode();
      result = 31 * result + configurations.hashCode();
      return result;
    }
  }

  /**
   * Encapsulates a configuration.
   */
  private static class ExportedConfiguration {
    /**
     * Configuration type such as hdfs-site.
     */
    private String type;

    /**
     * Configuration tag.
     */
    private String tag;

    /**
     * Properties of the configuration.
     */
    private Map<String, String> properties = new HashMap<>();

    /**
     * Attributes for the properties in the cluster configuration.
     */
    private Map<String, Map<String, String>> propertyAttributes = new HashMap<>();

    /**
     * Constructor.
     *
     * @param configNode  configuration node
     */
    @SuppressWarnings("unchecked")
    public ExportedConfiguration(TreeNode<Resource> configNode) {
      Resource configResource = configNode.getObject();
      type = (String) configResource.getPropertyValue("type");
      tag  = (String) configResource.getPropertyValue("tag");

      // property map type is currently <String, Object>
      Map<String, Map<String, Object>> propertiesMap = configNode.getObject().getPropertiesMap();
      if (propertiesMap.containsKey("properties")) {
        properties = (Map) propertiesMap.get("properties");
      }

      // get the property attributes set in this configuration
      if (propertiesMap.containsKey("properties_attributes")) {
        propertyAttributes = (Map) propertiesMap.get("properties_attributes");
      }
    }

    /**
     * Get configuration type.
     *
     * @return configuration type
     */
    public String getType() {
      return type;
    }

    /**
     * Get configuration tag.
     *
     * @return configuration tag
     */
    public String getTag() {
      return tag;
    }

    /**
     * Get configuration properties.
     *
     * @return map of properties and values
     */
    public Map<String, String> getProperties() {
      return properties;
    }

    /**
     * Get property attributes.
     *
     * @return map of property attributes
     */
    public Map<String, Map<String, String>> getPropertyAttributes() {
      return propertyAttributes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ExportedConfiguration that = (ExportedConfiguration) o;
      return tag.equals(that.tag) && type.equals(that.type) && properties.equals(that.properties)
          && propertyAttributes.equals(that.propertyAttributes);
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + tag.hashCode();
      result = 31 * result + properties.hashCode();
      result = 31 * result + propertyAttributes.hashCode();
      return result;
    }
  }

}
