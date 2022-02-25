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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.BlueprintConfigurations;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;

/**
 * Generate advised configurations for blueprint cluster provisioning by the stack advisor.
 */
@Singleton
public class StackAdvisorBlueprintProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorBlueprintProcessor.class);

  private static StackAdvisorHelper stackAdvisorHelper;

  static final String RECOMMENDATION_FAILED = "Configuration recommendation failed.";
  static final String INVALID_RESPONSE = "Configuration recommendation returned with invalid response.";

  public static void init(StackAdvisorHelper instance) {
    stackAdvisorHelper = instance;
  }

  private static final Map<String, String> userContext;
  static
  {
    userContext = new HashMap<>();
    userContext.put("operation", "ClusterCreate");
  }

  /**
   * Recommend configurations by the stack advisor, then store the results in cluster topology.
   * @param clusterTopology cluster topology instance
   * @param userProvidedConfigurations User configurations of cluster provided in Blueprint + Cluster template
   */
  public void adviseConfiguration(ClusterTopology clusterTopology, Map<String, Map<String, String>> userProvidedConfigurations) throws ConfigurationTopologyException {
    StackAdvisorRequest request = createStackAdvisorRequest(clusterTopology, StackAdvisorRequestType.CONFIGURATIONS);
    try {
      RecommendationResponse response = stackAdvisorHelper.recommend(request);
      addAdvisedConfigurationsToTopology(response, clusterTopology, userProvidedConfigurations);
    } catch (StackAdvisorException | AmbariException e) {
      throw new ConfigurationTopologyException(RECOMMENDATION_FAILED, e);
    } catch (IllegalArgumentException e) {
      throw new ConfigurationTopologyException(INVALID_RESPONSE, e);
    }
  }

  private StackAdvisorRequest createStackAdvisorRequest(ClusterTopology clusterTopology, StackAdvisorRequestType requestType) {
    Stack stack = clusterTopology.getBlueprint().getStack();
    Map<String, Set<String>> hgComponentsMap = gatherHostGroupComponents(clusterTopology);
    Map<String, Set<String>> hgHostsMap = gatherHostGroupBindings(clusterTopology);
    Map<String, Set<String>> componentHostsMap = gatherComponentsHostsMap(hgComponentsMap,
        hgHostsMap);
    return StackAdvisorRequest.StackAdvisorRequestBuilder
      .forStack(stack.getName(), stack.getVersion())
      .forServices(new ArrayList<>(clusterTopology.getBlueprint().getServices()))
      .forHosts(gatherHosts(clusterTopology))
      .forHostsGroupBindings(gatherHostGroupBindings(clusterTopology))
      .forHostComponents(gatherHostGroupComponents(clusterTopology))
      .withComponentHostsMap(componentHostsMap)
      .withConfigurations(calculateConfigs(clusterTopology))
      .withUserContext(userContext)
      .ofType(requestType)
      .build();
  }

  private Map<String, Set<String>> gatherHostGroupBindings(ClusterTopology clusterTopology) {
    Map<String, Set<String>> hgBindngs = Maps.newHashMap();
    for (Map.Entry<String, HostGroupInfo> hgEnrty: clusterTopology.getHostGroupInfo().entrySet()) {
      hgBindngs.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getHostNames()));
    }
    return hgBindngs;
  }

  private Map<String, Set<String>> gatherHostGroupComponents(ClusterTopology clusterTopology) {
    Map<String, Set<String>> hgComponentsMap = Maps.newHashMap();
    for (Map.Entry<String, HostGroup> hgEnrty: clusterTopology.getBlueprint().getHostGroups().entrySet()) {
      hgComponentsMap.put(hgEnrty.getKey(), Sets.newCopyOnWriteArraySet(hgEnrty.getValue().getComponentNames()));
    }
    return hgComponentsMap;
  }

  private Map<String, Map<String, Map<String, String>>> calculateConfigs(ClusterTopology clusterTopology) {
    Map<String, Map<String, Map<String, String>>> result = Maps.newHashMap();
    Map<String, Map<String, String>> fullProperties = clusterTopology.getConfiguration().getFullProperties();
    for (Map.Entry<String, Map<String, String>> siteEntry : fullProperties.entrySet()) {
      Map<String, Map<String, String>> propsMap = Maps.newHashMap();
      propsMap.put("properties", siteEntry.getValue());
      result.put(siteEntry.getKey(), propsMap);
    }
    return result;
  }

  private Map<String, Set<String>> gatherComponentsHostsMap(Map<String, Set<String>> hostGroups, Map<String, Set<String>> bindingHostGroups) {
    Map<String, Set<String>> componentHostsMap = new HashMap<>();
    if (null != bindingHostGroups && null != hostGroups) {
      for (Map.Entry<String, Set<String>> hgComponents : hostGroups.entrySet()) {
        String hgName = hgComponents.getKey();
        Set<String> components = hgComponents.getValue();

        Set<String> hosts = bindingHostGroups.get(hgName);
        if (hosts != null) {
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
    }
    return componentHostsMap;
  }

  private List<String> gatherHosts(ClusterTopology clusterTopology) {
    List<String> hosts = Lists.newArrayList();
    for (Map.Entry<String, HostGroupInfo> entry : clusterTopology.getHostGroupInfo().entrySet()) {
      hosts.addAll(entry.getValue().getHostNames());
    }
    return hosts;
  }

  private void addAdvisedConfigurationsToTopology(RecommendationResponse response,
                                                  ClusterTopology topology, Map<String, Map<String, String>> userProvidedConfigurations) {
    Preconditions.checkArgument(response.getRecommendations() != null,
      "Recommendation response is empty.");
    Preconditions.checkArgument(response.getRecommendations().getBlueprint() != null,
      "Blueprint field is missing from the recommendation response.");
    Preconditions.checkArgument(response.getRecommendations().getBlueprint().getConfigurations() != null,
      "Configurations are missing from the recommendation blueprint response.");

    Map<String, BlueprintConfigurations> recommendedConfigurations =
      response.getRecommendations().getBlueprint().getConfigurations();
    Blueprint blueprint = topology.getBlueprint();

    for (Map.Entry<String, BlueprintConfigurations> configEntry : recommendedConfigurations.entrySet()) {
      String configType = configEntry.getKey();
      // add recommended config type only if related service is present in Blueprint
      if (blueprint.isValidConfigType(configType)) {
        BlueprintConfigurations blueprintConfig = filterBlueprintConfig(configType, configEntry.getValue(),
                userProvidedConfigurations, topology);
        topology.getAdvisedConfigurations().put(configType, new AdvisedConfiguration(
                blueprintConfig.getProperties(), blueprintConfig.getPropertyAttributes()));
      }
    }
  }

  /**
   * Remove user defined properties from Stack Advisor output in case of ONLY_STACK_DEFAULTS_APPLY or
   * ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES.
   */
  private BlueprintConfigurations filterBlueprintConfig(String configType, BlueprintConfigurations config,
                                                        Map<String, Map<String, String>> userProvidedConfigurations,
                                                        ClusterTopology topology) {
    if (topology.getConfigRecommendationStrategy() == ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY ||
      topology.getConfigRecommendationStrategy() == ConfigRecommendationStrategy
        .ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES) {
      if (userProvidedConfigurations.containsKey(configType)) {
        BlueprintConfigurations newConfig = new BlueprintConfigurations();
        Map<String, String> filteredProps = Maps.filterKeys(config.getProperties(),
          Predicates.not(Predicates.in(userProvidedConfigurations.get(configType).keySet())));
        newConfig.setProperties(Maps.newHashMap(filteredProps));

        if (config.getPropertyAttributes() != null) {
          Map<String, ValueAttributesInfo> filteredAttributes = Maps.filterKeys(config.getPropertyAttributes(),
            Predicates.not(Predicates.in(userProvidedConfigurations.get(configType).keySet())));
          newConfig.setPropertyAttributes(Maps.newHashMap(filteredAttributes));
        }
        return newConfig;
      }
    }
    return config;
  }
}
