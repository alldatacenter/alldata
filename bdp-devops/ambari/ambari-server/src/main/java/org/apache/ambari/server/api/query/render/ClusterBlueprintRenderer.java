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

package org.apache.ambari.server.api.query.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;
import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.BlueprintExportType;
import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.internal.ExportBlueprintRequest;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.topology.AmbariContext;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.addservice.ResourceProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Renderer which renders a cluster resource as a blueprint.
 */
public class ClusterBlueprintRenderer extends BaseRenderer implements Renderer {

  private final static Logger LOG = LoggerFactory.getLogger(ClusterBlueprintRenderer.class);

  static final String SERVICE_SETTINGS = "service_settings";
  static final String COMPONENT_SETTINGS = "component_settings";
  static final String CREDENTIAL_STORE_ENABLED = "credential_store_enabled";
  static final String RECOVERY_ENABLED = "recovery_enabled";
  static final String TRUE = Boolean.TRUE.toString();
  static final String FALSE = Boolean.FALSE.toString();

  /**
   * Management Controller used to get stack information.
   */
  private final AmbariManagementController controller = AmbariServer.getController();

  private final BlueprintExportType exportType;

  public ClusterBlueprintRenderer(BlueprintExportType exportType) {
    this.exportType = exportType;
  }


  // ----- Renderer ----------------------------------------------------------

  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryProperties, boolean isCollection) {

    Set<String> properties = new HashSet<>(queryProperties.getObject().getProperties());
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<>(
      null, properties, queryProperties.getName());

    copyPropertiesToResult(queryProperties, resultTree);

    String configType = Resource.Type.Configuration.name();
    if (resultTree.getChild(configType) == null) {
      resultTree.addChild(new HashSet<>(), configType);
    }

    String serviceType = Resource.Type.Service.name();
    if (resultTree.getChild(serviceType) == null) {
      resultTree.addChild(new HashSet<>(), serviceType);
    }
    TreeNode<Set<String>> serviceNode = resultTree.getChild(serviceType);
    if (serviceNode == null) {
      serviceNode = resultTree.addChild(new HashSet<>(), serviceType);
    }
    String serviceComponentType = Resource.Type.Component.name();
    TreeNode<Set<String>> serviceComponentNode = resultTree.getChild(
      serviceType + "/" + serviceComponentType);
    if (serviceComponentNode == null) {
      serviceComponentNode = serviceNode.addChild(new HashSet<>(), serviceComponentType);
    }
    serviceComponentNode.getObject().add("ServiceComponentInfo/cluster_name");
    serviceComponentNode.getObject().add("ServiceComponentInfo/service_name");
    serviceComponentNode.getObject().add("ServiceComponentInfo/component_name");
    serviceComponentNode.getObject().add("ServiceComponentInfo/recovery_enabled");

    String hostType = Resource.Type.Host.name();
    String hostComponentType = Resource.Type.HostComponent.name();
    TreeNode<Set<String>> hostComponentNode = resultTree.getChild(
        hostType + "/" + hostComponentType);

    if (hostComponentNode == null) {
      TreeNode<Set<String>> hostNode = resultTree.getChild(hostType);
      if (hostNode == null) {
        hostNode = resultTree.addChild(new HashSet<>(), hostType);
      }
      hostComponentNode = hostNode.addChild(new HashSet<>(), hostComponentType);
    }
    resultTree.getChild(configType).getObject().add("properties");
    hostComponentNode.getObject().add("HostRoles/component_name");

    return resultTree;
  }

  @Override
  public Result finalizeResult(Result queryResult) {
    TreeNode<Resource> resultTree = queryResult.getResultTree();
    Result result = new ResultImpl(true);
    TreeNode<Resource> blueprintResultTree = result.getResultTree();
    if (isCollection(resultTree)) {
      blueprintResultTree.setProperty("isCollection", "true");
    }

    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource blueprintResource = createBlueprintResource(node);
      blueprintResultTree.addChild(new TreeNodeImpl<>(
        blueprintResultTree, blueprintResource, node.getName()));
    }
    return result;
  }

  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    return new BlueprintPostProcessor(request);
  }

  @Override
  public boolean requiresPropertyProviderInput() {
    // the Blueprint-based renderer does not require property provider input
    // this method will help to filter out the un-necessary calls to the AMS
    // and Alerts Property providers, since they are not included in the
    // exported Blueprint
    return false;
  }

  // ----- private instance methods ------------------------------------------

  /**
   * Create a blueprint resource.
   *
   * @param clusterNode  cluster tree node
   * @return a new blueprint resource
   */
  private Resource createBlueprintResource(TreeNode<Resource> clusterNode) {
    Resource blueprintResource = new ResourceImpl(Resource.Type.Cluster);

    ClusterTopology topology;
    try {
      topology = createClusterTopology(clusterNode);
    } catch (InvalidTopologyTemplateException | InvalidTopologyException e) {
      throw new RuntimeException("Unable to process blueprint export request: " + e, e);
    }

    BlueprintConfigurationProcessor configProcessor = new BlueprintConfigurationProcessor(topology);
    configProcessor.doUpdateForBlueprintExport(exportType);

    Stack stack = topology.getBlueprint().getStack();
    blueprintResource.setProperty("Blueprints/stack_name", stack.getName());
    blueprintResource.setProperty("Blueprints/stack_version", stack.getVersion());

    if (topology.isClusterKerberosEnabled()) {
      Map<String, Object> securityConfigMap = new LinkedHashMap<>();
      securityConfigMap.put(SecurityConfigurationFactory.TYPE_PROPERTY_ID, SecurityType.KERBEROS.name());

      try {
        String clusterName = topology.getAmbariContext().getClusterName(topology.getClusterId());
        Map<String, Object> kerberosDescriptor = getKerberosDescriptor(AmbariContext.getClusterController(), clusterName);
        if (kerberosDescriptor != null) {
          securityConfigMap.put(SecurityConfigurationFactory.KERBEROS_DESCRIPTOR_PROPERTY_ID, kerberosDescriptor);
        }
      } catch (AmbariException e) {
        LOG.info("Unable to retrieve kerberos_descriptor: ", e.getMessage());
      }
      blueprintResource.setProperty(BlueprintResourceProvider.BLUEPRINT_SECURITY_PROPERTY_ID, securityConfigMap);
    }

    List<Map<String, Object>> groupList = formatGroupsAsList(topology);
    blueprintResource.setProperty("host_groups", groupList);

    List<Map<String, Map<String, Map<String, ?>>>> configurations = processConfigurations(topology);
    if (exportType.include(configurations)) {
      blueprintResource.setProperty("configurations", configurations);
    }

    //Fetch settings section for blueprint
    Collection<Map<String, Object>> settings = getSettings(clusterNode, stack);
    if (exportType.include(settings)) {
      blueprintResource.setProperty("settings", settings);
    }

    return blueprintResource;
  }

  /***
   * Constructs the Settings object of the following form:
   * "settings": [   {
   "recovery_settings": [
   {
   "recovery_enabled": "true"
   }   ]   },
   {
   "service_settings": [   {
   "name": "HDFS",
   "recovery_enabled": "true",
   "credential_store_enabled": "true"
   },
   {
   "name": "TEZ",
   "recovery_enabled": "false"
   },
   {
   "name": "HIVE",
   "recovery_enabled": "false"
   }   ]   },
   {
   "component_settings": [   {
   "name": "DATANODE",
   "recovery_enabled": "true"
   }   ]   }   ]
   *
   * @return A Collection<Map<String, Object>> which represents the Setting Object
   */
  @VisibleForTesting
  Collection<Map<String, Object>> getSettings(TreeNode<Resource> clusterNode, Stack stack) {
    //Initialize collections to create appropriate json structure
    Collection<Map<String, Object>> blueprintSetting = new ArrayList<>();

    Set<Map<String, String>> serviceSettings = new HashSet<>();
    Set<Map<String, String>> componentSettings = new HashSet<>();

    //Fetch the services, to obtain ServiceInfo and ServiceComponents
    Collection<TreeNode<Resource>> serviceChildren = clusterNode.getChild("services").getChildren();
    for (TreeNode<Resource> serviceNode : serviceChildren) {
      ResourceImpl service = (ResourceImpl) serviceNode.getObject();
      Map<String, Object> serviceInfoMap = service.getPropertiesMap().get("ServiceInfo");

      //service_settings population
      Map<String, String> serviceSetting = new HashMap<>();
      String serviceName = serviceInfoMap.get("service_name").toString();
      ServiceInfo serviceInfo = stack.getServiceInfo(serviceName).orElseThrow(IllegalStateException::new);
      boolean serviceDefaultCredentialStoreEnabled = serviceInfo.isCredentialStoreEnabled();
      String credentialStoreEnabledString = String.valueOf(serviceInfoMap.get(CREDENTIAL_STORE_ENABLED));
      boolean credentialStoreEnabled = Boolean.parseBoolean(credentialStoreEnabledString);
      if (credentialStoreEnabled != serviceDefaultCredentialStoreEnabled) {
        serviceSetting.put(CREDENTIAL_STORE_ENABLED, credentialStoreEnabledString);
        serviceSetting.put("name", serviceName);
        serviceSettings.add(serviceSetting);
      }

      //Fetch the service Components to obtain ServiceComponentInfo
      Collection<TreeNode<Resource>> componentChildren = serviceNode.getChild("components").getChildren();
      for (TreeNode<Resource> componentNode : componentChildren) {
        ResourceImpl component = (ResourceImpl) componentNode.getObject();
        Map<String, Object> serviceComponentInfoMap = component.getPropertiesMap().get("ServiceComponentInfo");
        String componentName = serviceComponentInfoMap.get("component_name").toString();
        boolean componentDefaultRecoveryEnabled = serviceInfo.getComponentByName(componentName).isRecoveryEnabled();

        if (serviceComponentInfoMap.containsKey(RECOVERY_ENABLED)) {
          String recoveryEnabledString = String.valueOf(serviceComponentInfoMap.get(RECOVERY_ENABLED));
          boolean recoveryEnabled = Boolean.parseBoolean(recoveryEnabledString);
          if (recoveryEnabled != componentDefaultRecoveryEnabled) {
            componentSettings.add(ImmutableMap.of(
              "name", componentName,
              RECOVERY_ENABLED, recoveryEnabledString
            ));
          }
        }
      }
    }

    if (exportType.include(serviceSettings)) {
      blueprintSetting.add(ImmutableMap.of(SERVICE_SETTINGS, serviceSettings));
    }
    if (exportType.include(componentSettings)) {
      blueprintSetting.add(ImmutableMap.of(COMPONENT_SETTINGS, componentSettings));
    }

    return blueprintSetting;
  }

  private static Map<String, Object> getKerberosDescriptor(ClusterController clusterController, String clusterName) throws AmbariException {
    Predicate predicate = ResourceProviderAdapter.predicateForKerberosDescriptorArtifact(clusterName);

    ResourceProvider artifactProvider =
       clusterController.ensureResourceProvider(Resource.Type.Artifact);

    org.apache.ambari.server.controller.spi.Request request = new RequestImpl(Collections.emptySet(),
      Collections.emptySet(), Collections.emptyMap(), null);

    Set<Resource> response;
    try {
      response = artifactProvider.getResources(request, predicate);
    } catch (SystemException | UnsupportedPropertyException | NoSuchResourceException | NoSuchParentResourceException
      e) {
      throw new AmbariException("An unknown error occurred while trying to obtain the cluster kerberos descriptor", e);
    }

    if (response != null && !response.isEmpty()) {
      Resource descriptorResource = response.iterator().next();
      Map<String, Map<String, Object>> propertyMap = descriptorResource.getPropertiesMap();

      if (propertyMap != null) {
        Map<String, Object> artifactData = propertyMap.get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY);
        Map<String, Object> artifactDataProperties = propertyMap.get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY + "/properties");
        HashMap<String, Object> data = new HashMap<>();

        if (artifactData != null) {
          data.putAll(artifactData);
        }

        if (artifactDataProperties != null) {
          data.put("properties", artifactDataProperties);
        }
        return data;
      }
    }
    return null;
  }

  /**
   * Process cluster scoped configurations.
   *
   *
   * @return cluster configuration
   */
  private List<Map<String, Map<String, Map<String, ?>>>>  processConfigurations(ClusterTopology topology) {

    List<Map<String, Map<String, Map<String, ?>>>> configList = new ArrayList<>();

    Configuration configuration = topology.getConfiguration();
    Map<String, Map<String, String>> fullProperties = configuration.getFullProperties();
    Map<String, Map<String, Map<String, String>>> fullAttributes = configuration.getFullAttributes();
    Collection<String> allTypes = ImmutableSet.<String>builder()
      .addAll(fullProperties.keySet())
      .addAll(fullAttributes.keySet())
      .build();
    for (String type : allTypes) {
      Map<String, Map<String, ?>> typeMap = new HashMap<>();
      Map<String, String> properties = fullProperties.get(type);
      if (exportType.include(properties)) {
        typeMap.put("properties", properties);
      }
      if (!fullAttributes.isEmpty()) {
        Map<String, Map<String, String>> attributes = fullAttributes.get(type);
        if (exportType.include(attributes)) {
          typeMap.put("properties_attributes", attributes);
        }
      }

      configList.add(Collections.singletonMap(type, typeMap));
    }

    return configList;
  }

  /**
   * Process host group information for all hosts.
   *
   *
   * @return list of host group property maps, one element for each host group
   */
  private List<Map<String, Object>> formatGroupsAsList(ClusterTopology topology) {
    List<Map<String, Object>> listHostGroups = new ArrayList<>();
    for (HostGroupInfo group : topology.getHostGroupInfo().values()) {
      Map<String, Object> mapGroupProperties = new HashMap<>();
      listHostGroups.add(mapGroupProperties);

      String name = group.getHostGroupName();
      mapGroupProperties.put("name", name);
      mapGroupProperties.put("cardinality", String.valueOf(group.getHostNames().size()));
      mapGroupProperties.put("components", processHostGroupComponents(topology.getBlueprint().getHostGroup(name)));

      Configuration configuration = topology.getHostGroupInfo().get(name).getConfiguration();
      List<Map<String, Map<String, String>>> configList = new ArrayList<>();
      for (Map.Entry<String, Map<String, String>> typeEntry : configuration.getProperties().entrySet()) {
        Map<String, Map<String, String>> propertyMap = Collections.singletonMap(
            typeEntry.getKey(), typeEntry.getValue());

        configList.add(propertyMap);
      }
      if (exportType.include(configList)) {
        mapGroupProperties.put("configurations", configList);
      }
    }
    return listHostGroups;
  }


  /**
   * Process host group component information for a specific host.
   *
   * @param group host group instance
   *
   * @return list of component names for the host
   */
  private List<Map<String, String>> processHostGroupComponents(HostGroup group) {
    List<Map<String, String>> listHostGroupComponents = new ArrayList<>();
    for (Component component : group.getComponents()) {
      Map<String, String> mapComponentProperties = new HashMap<>();
      listHostGroupComponents.add(mapComponentProperties);
      mapComponentProperties.put("name", component.getName());
    }
    return listHostGroupComponents;
  }

  protected ClusterTopology createClusterTopology(TreeNode<Resource> clusterNode)
      throws InvalidTopologyTemplateException, InvalidTopologyException {

    return new ClusterTopologyImpl(new AmbariContext(), new ExportBlueprintRequest(clusterNode, controller));
  }

  /**
   * Determine whether a node represents a collection.
   *
   * @param node  node which is evaluated for being a collection
   *
   * @return true if the node represents a collection; false otherwise
   */
  private boolean isCollection(TreeNode<Resource> node) {
    String isCollection = node.getStringProperty("isCollection");
    return isCollection != null && isCollection.equals("true");
  }

  /**
   * Get management controller instance.
   *
   * @return  management controller
   */
  protected AmbariManagementController getController() {
    return controller;
  }

  // ----- Blueprint Post Processor inner class ------------------------------

  /**
   * Post processor that strips href properties
   */
  private static class BlueprintPostProcessor extends ResultPostProcessorImpl {
    private BlueprintPostProcessor(Request request) {
      super(request);
    }

    @Override
    protected void finalizeNode(TreeNode<Resource> node) {
      node.removeProperty("href");
    }
  }
}
