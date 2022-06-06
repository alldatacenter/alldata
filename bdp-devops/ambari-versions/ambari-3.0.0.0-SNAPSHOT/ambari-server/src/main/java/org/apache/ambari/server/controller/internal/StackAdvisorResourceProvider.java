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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.ChangedConfigInfo;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Abstract superclass for recommendations and validations.
 */
public abstract class StackAdvisorResourceProvider extends ReadOnlyResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorResourceProvider.class);

  protected static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Versions",
      "stack_name");
  protected static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "Versions", "stack_version");


  private static final String CLUSTER_ID_PROPERTY = "clusterId";
  private static final String SERVICE_NAME_PROPERTY = "serviceName";
  private static final String AUTO_COMPLETE_PROPERTY = "autoComplete";
  private static final String CONFIGS_RESPONSE_PROPERTY = "configsResponse";
  private static final String CONFIG_GROUPS_GROUP_ID_PROPERTY = "group_id";
  private static final String HOST_PROPERTY = "hosts";
  private static final String SERVICES_PROPERTY = "services";

  private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed_configurations";
  private static final String OPERATION_PROPERTY = "operation";
  private static final String OPERATION_DETAILS_PROPERTY = "operation_details";


  private static final String BLUEPRINT_HOST_GROUPS_PROPERTY = "recommendations/blueprint/host_groups";
  private static final String BINDING_HOST_GROUPS_PROPERTY = "recommendations/blueprint_cluster_binding/host_groups";

  private static final String BLUEPRINT_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY = "components";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY = "name";

  private static final String BINDING_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BINDING_HOST_GROUPS_HOSTS_PROPERTY = "hosts";
  private static final String BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY = "fqdn";

  private static final String CONFIG_GROUPS_PROPERTY = "recommendations/config_groups";
  private static final String CONFIG_GROUPS_CONFIGURATIONS_PROPERTY = "configurations";
  private static final String CONFIG_GROUPS_HOSTS_PROPERTY = "hosts";

  protected static StackAdvisorHelper saHelper;
  private static Configuration configuration;
  private static Clusters clusters;
  private static AmbariMetaInfo ambariMetaInfo;
  protected static final String USER_CONTEXT_OPERATION_PROPERTY = "user_context/operation";
  protected static final String USER_CONTEXT_OPERATION_DETAILS_PROPERTY = "user_context/operation_details";

  @Inject
  public static void init(StackAdvisorHelper instance, Configuration serverConfig, Clusters clusters,
                          AmbariMetaInfo ambariMetaInfo) {
    saHelper = instance;
    configuration = serverConfig;
    StackAdvisorResourceProvider.clusters = clusters;
    StackAdvisorResourceProvider.ambariMetaInfo = ambariMetaInfo;
  }

  protected StackAdvisorResourceProvider(Resource.Type type, Set<String> propertyIds, Map<Type, String> keyPropertyIds,
                                         AmbariManagementController managementController) {
    super(type, propertyIds, keyPropertyIds, managementController);
  }

  protected abstract String getRequestTypePropertyId();

  @SuppressWarnings("unchecked")
  protected StackAdvisorRequest prepareStackAdvisorRequest(Request request) {
    try {
      String clusterIdProperty = (String) getRequestProperty(request, CLUSTER_ID_PROPERTY);
      Long clusterId = clusterIdProperty == null ? null : Long.valueOf(clusterIdProperty);

      String serviceName = (String) getRequestProperty(request, SERVICE_NAME_PROPERTY);

      String autoCompleteProperty = (String) getRequestProperty(request, AUTO_COMPLETE_PROPERTY);
      Boolean autoComplete = autoCompleteProperty == null ? false : Boolean.valueOf(autoCompleteProperty);

      String stackName = (String) getRequestProperty(request, STACK_NAME_PROPERTY_ID);
      String stackVersion = (String) getRequestProperty(request, STACK_VERSION_PROPERTY_ID);
      StackAdvisorRequestType requestType = StackAdvisorRequestType
          .fromString((String) getRequestProperty(request, getRequestTypePropertyId()));

      List<String> hosts;
      List<String> services;
      Map<String, Set<String>> hgComponentsMap;
      Map<String, Set<String>> hgHostsMap;
      Map<String, Set<String>> componentHostsMap;
      Map<String, Map<String, Map<String, String>>> configurations;
      Set<RecommendationResponse.ConfigGroup> configGroups;

      // In auto complete case all required fields will be filled will cluster current info
      if (autoComplete) {
        if (clusterId == null || serviceName == null) {
          throw new Exception(
              String.format("Incomplete request, clusterId and/or serviceName are not valid, clusterId=%s, serviceName=%s",
                  clusterId, serviceName));
        }
        Cluster cluster = clusters.getCluster(clusterId);
        List<Host> hostObjects = new ArrayList<>(cluster.getHosts());
        Map<String, Service> serviceObjects = cluster.getServices();

        hosts = hostObjects.stream().map(h -> h.getHostName()).collect(Collectors.toList());
        services = new ArrayList<>(serviceObjects.keySet());
        hgComponentsMap = calculateHostGroupComponentsMap(cluster);
        hgHostsMap = calculateHostGroupHostsMap(cluster);
        componentHostsMap = calculateComponentHostsMap(cluster);
        configurations = calculateConfigurations(cluster, serviceName);

        configGroups = calculateConfigGroups(cluster, request);
      } else {
        /*
       * ClassCastException will occur if hosts or services are empty in the
       * request.
       *
       * @see JsonRequestBodyParser for arrays parsing
       */
        Object hostsObject = getRequestProperty(request, HOST_PROPERTY);
        if (hostsObject instanceof LinkedHashSet) {
          if (((LinkedHashSet)hostsObject).isEmpty()) {
            throw new Exception("Empty host list passed to recommendation service");
          }
        }
        hosts = (List<String>) hostsObject;

        Object servicesObject = getRequestProperty(request, SERVICES_PROPERTY);
        if (servicesObject instanceof LinkedHashSet) {
          if (((LinkedHashSet)servicesObject).isEmpty()) {
            throw new Exception("Empty service list passed to recommendation service");
          }
        }
        services = (List<String>) servicesObject;

        hgComponentsMap = calculateHostGroupComponentsMap(request);
        hgHostsMap = calculateHostGroupHostsMap(request);
        componentHostsMap = calculateComponentHostsMap(hgComponentsMap, hgHostsMap);
        configurations = calculateConfigurations(request);
        configGroups = calculateConfigGroups(request);
      }
      Map<String, String> userContext = readUserContext(request);
      Boolean gplLicenseAccepted = configuration.getGplLicenseAccepted();
      List<ChangedConfigInfo> changedConfigurations =
        requestType == StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES ?
          calculateChangedConfigurations(request) : Collections.emptyList();

      String configsResponseProperty = (String) getRequestProperty(request, CONFIGS_RESPONSE_PROPERTY);
      Boolean configsResponse = configsResponseProperty == null ? false : Boolean.valueOf(configsResponseProperty);

      return StackAdvisorRequestBuilder.
        forStack(stackName, stackVersion).ofType(requestType).forHosts(hosts).
        forServices(services).forHostComponents(hgComponentsMap).
        forHostsGroupBindings(hgHostsMap).
        withComponentHostsMap(componentHostsMap).
        withConfigurations(configurations).
        withConfigGroups(configGroups).
        withChangedConfigurations(changedConfigurations).
        withUserContext(userContext).
        withGPLLicenseAccepted(gplLicenseAccepted).
        withClusterId(clusterId).
        withServiceName(serviceName).
        withConfigsResponse(configsResponse).build();
    } catch (Exception e) {
      LOG.warn("Error occurred during preparation of stack advisor request", e);
      Response response = Response.status(Status.BAD_REQUEST)
          .entity(String.format("Request body is not correct, error: %s", e.getMessage())).build();
      throw new WebApplicationException(response);
    }
  }

  /**
   * Will prepare host-group names to components names map from the
   * recommendation blueprint host groups.
   * 
   * @param request stack advisor request
   * @return host-group to components map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupComponentsMap(Request request) {
    Set<Map<String, Object>> hostGroups = (Set<Map<String, Object>>) getRequestProperty(request,
        BLUEPRINT_HOST_GROUPS_PROPERTY);
    Map<String, Set<String>> map = new HashMap<>();
    if (hostGroups != null) {
      for (Map<String, Object> hostGroup : hostGroups) {
        String hostGroupName = (String) hostGroup.get(BLUEPRINT_HOST_GROUPS_NAME_PROPERTY);

        Set<Map<String, Object>> componentsSet = (Set<Map<String, Object>>) hostGroup
            .get(BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY);

        Set<String> components = new HashSet<>();
        for (Map<String, Object> component : componentsSet) {
          components.add((String) component.get(BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY));
        }

        map.put(hostGroupName, components);
      }
    }

    return map;
  }

  /**
   * Retrieves component names mapped by host groups, host name is used as host group identifier
   * @param cluster cluster for calculating components mapping by host groups
   * @return map "host group name" -> ["component name1", "component name 2", ...]
   */
  private Map<String, Set<String>> calculateHostGroupComponentsMap(Cluster cluster) {
    Map<String, Set<String>> map = new HashMap<>();
    List<Host> hosts = new ArrayList<>(cluster.getHosts());
    if (!hosts.isEmpty()) {
      for (Host host : hosts) {
        String hostGroupName = host.getHostName();

        Set<String> components = new HashSet<>();
        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host.getHostName())) {
          components.add(sch.getServiceComponentName());
        }
        map.put(hostGroupName, components);
      }
    }
    return map;
  }

  /**
   * Will prepare host-group names to hosts names map from the recommendation
   * binding host groups.
   * 
   * @param request stack advisor request
   * @return host-group to hosts map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupHostsMap(Request request) {
    Set<Map<String, Object>> bindingHostGroups = (Set<Map<String, Object>>) getRequestProperty(
        request, BINDING_HOST_GROUPS_PROPERTY);
    Map<String, Set<String>> map = new HashMap<>();
    if (bindingHostGroups != null) {
      for (Map<String, Object> hostGroup : bindingHostGroups) {
        String hostGroupName = (String) hostGroup.get(BINDING_HOST_GROUPS_NAME_PROPERTY);

        Set<Map<String, Object>> hostsSet = (Set<Map<String, Object>>) hostGroup
            .get(BINDING_HOST_GROUPS_HOSTS_PROPERTY);

        Set<String> hosts = new HashSet<>();
        for (Map<String, Object> host : hostsSet) {
          hosts.add((String) host.get(BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY));
        }

        map.put(hostGroupName, hosts);
      }
    }

    return map;
  }

  /**
   * Retrieves hosts names mapped by host groups, host name is used as host group identifier
   * @param cluster cluster for calculating hosts mapping by host groups
   * @return map "host group name" -> ["host name 1"]
   */
  private Map<String, Set<String>> calculateHostGroupHostsMap(Cluster cluster) {
    Map<String, Set<String>> map = new HashMap<>();

    List<Host> hosts = new ArrayList<>(cluster.getHosts());
    if (!hosts.isEmpty()) {
      for (Host host : hosts) {
        map.put(host.getHostName(), Collections.singleton(host.getHostName()));
      }
    }

    return map;
  }

  protected List<ChangedConfigInfo> calculateChangedConfigurations(Request request) {
    List<ChangedConfigInfo> configs =
      new LinkedList<>();
    HashSet<HashMap<String, String>> changedConfigs =
      (HashSet<HashMap<String, String>>) getRequestProperty(request, CHANGED_CONFIGURATIONS_PROPERTY);
    for (HashMap<String, String> props: changedConfigs) {
      configs.add(new ChangedConfigInfo(props.get("type"), props.get("name"), props.get("old_value")));
    }

    return configs;
  }

  protected Set<RecommendationResponse.ConfigGroup> calculateConfigGroups(Request request) {

    Set<RecommendationResponse.ConfigGroup> configGroups =
      new HashSet<>();

    Set<HashMap<String, Object>> configGroupsProperties =
      (HashSet<HashMap<String, Object>>) getRequestProperty(request, CONFIG_GROUPS_PROPERTY);
    if (configGroupsProperties != null) {
      for (HashMap<String, Object> props : configGroupsProperties) {
        RecommendationResponse.ConfigGroup configGroup = new RecommendationResponse.ConfigGroup();
        configGroup.setHosts((List<String>) props.get(CONFIG_GROUPS_HOSTS_PROPERTY));

        for (Map<String, String> property : (Set<Map<String, String>>) props.get(CONFIG_GROUPS_CONFIGURATIONS_PROPERTY)) {
          for (Map.Entry<String, String> entry : property.entrySet()) {
            String[] propertyPath = entry.getKey().split("/"); // length == 3
            String siteName = propertyPath[0];
            String propertyName = propertyPath[2];

            if (!configGroup.getConfigurations().containsKey(siteName)) {
              RecommendationResponse.BlueprintConfigurations configurations =
                new RecommendationResponse.BlueprintConfigurations();
              configGroup.getConfigurations().put(siteName, configurations);
              configGroup.getConfigurations().get(siteName).setProperties(new HashMap<>());
            }
            configGroup.getConfigurations().get(siteName).getProperties().put(propertyName, entry.getValue());
          }
        }
        configGroups.add(configGroup);
      }
    }

    return configGroups;
  }

  protected Set<RecommendationResponse.ConfigGroup> calculateConfigGroups(Cluster cluster, Request request) {

    Set<RecommendationResponse.ConfigGroup> configGroups = new HashSet<>();

    Set<HashMap<String, Object>> configGroupsProperties =
      (HashSet<HashMap<String, Object>>) getRequestProperty(request, CONFIG_GROUPS_PROPERTY);
    if (configGroupsProperties != null) {
      for (HashMap<String, Object> props : configGroupsProperties) {
        RecommendationResponse.ConfigGroup configGroup = new RecommendationResponse.ConfigGroup();
        Object groupIdObject = props.get(CONFIG_GROUPS_GROUP_ID_PROPERTY);
        if (groupIdObject != null) {
          Long groupId = Long.valueOf((String) groupIdObject);
          ConfigGroup clusterConfigGroup = cluster.getConfigGroupsById(groupId);

          // convert configs
          Map<String, RecommendationResponse.BlueprintConfigurations> typedConfiguration = new HashMap<>();
          for (Map.Entry<String, Config> config : clusterConfigGroup.getConfigurations().entrySet()) {
            RecommendationResponse.BlueprintConfigurations blueprintConfiguration = new RecommendationResponse.BlueprintConfigurations();
            blueprintConfiguration.setProperties(config.getValue().getProperties());
            typedConfiguration.put(config.getKey(), blueprintConfiguration);
          }

          configGroup.setConfigurations(typedConfiguration);

          configGroup.setHosts(clusterConfigGroup.getHosts().values().stream().map(h -> h.getHostName()).collect(Collectors.toList()));
          configGroups.add(configGroup);
        }
      }
    }

    return configGroups;
  }

  /**
   * Parse the user contex for the call. Typical structure
   * { "operation" : "createCluster" }
   * { "operation" : "addService", "services" : "Atlas,Ranger" }
   * @param request
   * @return
   */
  protected Map<String, String> readUserContext(Request request) {
    Map<String, String> userContext = new HashMap<>();
    if (null != getRequestProperty(request, USER_CONTEXT_OPERATION_PROPERTY)) {
      userContext.put(OPERATION_PROPERTY,
                      (String) getRequestProperty(request, USER_CONTEXT_OPERATION_PROPERTY));
    }
    if (null != getRequestProperty(request, USER_CONTEXT_OPERATION_DETAILS_PROPERTY)) {
      userContext.put(OPERATION_DETAILS_PROPERTY,
                      (String) getRequestProperty(request, USER_CONTEXT_OPERATION_DETAILS_PROPERTY));
    }
    return userContext;
  }

  protected static final String CONFIGURATIONS_PROPERTY_ID = "recommendations/blueprint/configurations/";

  protected Map<String, Map<String, Map<String, String>>> calculateConfigurations(Request request) {
    Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
    Map<String, Object> properties = request.getProperties().iterator().next();
    for (String property : properties.keySet()) {
      if (property.startsWith(CONFIGURATIONS_PROPERTY_ID)) {
        try {
          String propertyEnd = property.substring(CONFIGURATIONS_PROPERTY_ID.length()); // mapred-site/properties/yarn.app.mapreduce.am.resource.mb
          String[] propertyPath = propertyEnd.split("/"); // length == 3
          String siteName = propertyPath[0];
          String propertiesProperty = propertyPath[1];
          String propertyName = propertyPath[2];

          Map<String, Map<String, String>> siteMap = configurations.get(siteName);
          if (siteMap == null) {
            siteMap = new HashMap<>();
            configurations.put(siteName, siteMap);
          }

          Map<String, String> propertiesMap = siteMap.get(propertiesProperty);
          if (propertiesMap == null) {
            propertiesMap = new HashMap<>();
            siteMap.put(propertiesProperty, propertiesMap);
          }

          Object propVal = properties.get(property);
          if (propVal != null)
            propertiesMap.put(propertyName, propVal.toString());
          else
            LOG.info(String.format("No value specified for configuration property, name = %s ", property));

        } catch (Exception e) {
          LOG.debug(String.format("Error handling configuration property, name = %s", property), e);
          // do nothing
        }
      }
    }
    return configurations;
  }

  protected Map<String, Map<String, Map<String, String>>> calculateConfigurations(Cluster cluster, String serviceName)
      throws AmbariException {
    Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
    Service service = cluster.getService(serviceName);

    StackId stackId = service.getDesiredStackId();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);

    List<String> requiredConfigTypes = serviceInfo.getConfigDependenciesWithComponents();
    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    Map<String, DesiredConfig> requiredDesiredConfigs = new HashMap<>();
    for (String requiredConfigType : requiredConfigTypes) {
      if (desiredConfigs.containsKey(requiredConfigType)) {
        requiredDesiredConfigs.put(requiredConfigType, desiredConfigs.get(requiredConfigType));
      }
    }
    for (Map.Entry<String, DesiredConfig> requiredDesiredConfigEntry : requiredDesiredConfigs.entrySet()) {
      Config config = cluster.getConfig(requiredDesiredConfigEntry.getKey(), requiredDesiredConfigEntry.getValue().getTag());
      configurations.put(requiredDesiredConfigEntry.getKey(), Collections.singletonMap("properties", config.getProperties()));
    }
    return configurations;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateComponentHostsMap(Map<String, Set<String>> hostGroups,
                                                              Map<String, Set<String>> bindingHostGroups) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */

    Map<String, Set<String>> componentHostsMap = new HashMap<>();
    if (null != bindingHostGroups && null != hostGroups) {
      for (Map.Entry<String, Set<String>> hgComponents : hostGroups.entrySet()) {
        String hgName = hgComponents.getKey();
        Set<String> components = hgComponents.getValue();

        Set<String> hosts = bindingHostGroups.get(hgName);
        for (String component : components) {
          Set<String> componentHosts = componentHostsMap.get(component);
          if (componentHosts == null) { // if was not initialized
            componentHosts = new HashSet<>();
            componentHostsMap.put(component, componentHosts);
          }
          componentHosts.addAll(hosts);
        }
      }
    }

    return componentHostsMap;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateComponentHostsMap(Cluster cluster) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */

    Map<String, Set<String>> componentHostsMap = new HashMap<>();
    List<ServiceComponentHost> schs = cluster.getServiceComponentHosts();
    for (ServiceComponentHost sch : schs) {
      componentHostsMap.putIfAbsent(sch.getServiceComponentName(), new HashSet<>());
      componentHostsMap.get(sch.getServiceComponentName()).add(sch.getHostName());
    }

    return componentHostsMap;
  }

  protected Object getRequestProperty(Request request, String propertyName) {
    for (Map<String, Object> propertyMap : request.getProperties()) {
      if (propertyMap.containsKey(propertyName)) {
        return propertyMap.get(propertyName);
      }
    }
    return null;
  }

}
