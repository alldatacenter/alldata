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

package org.apache.ambari.server.topology.addservice;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.transformValues;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.UnitUpdater;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;

public class StackAdvisorAdapter {

  @Inject
  private AmbariManagementController managementController;

  @Inject
  private StackAdvisorHelper stackAdvisorHelper;

  @Inject
  private org.apache.ambari.server.configuration.Configuration serverConfig;

  @Inject
  private Injector injector;

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorHelper.class);

  /**
   * Recommends component layout for the new services to add. If the request contains explicit layout for some components
   * this will be added to the stack advisor input.
   * @param info
   * @return
   */
  AddServiceInfo recommendLayout(AddServiceInfo info) {
    try {
      // Requested component layout will be added to the StackAdvisor input in addition to existing
      // component layout.
      Map<String, Map<String, Set<String>>> allServices = getAllServices(info);

      Map<String, Set<String>> componentsToHosts = getComponentHostMap(allServices);

      Map<String, Set<String>> hostsToComponents = getHostComponentMap(componentsToHosts);
      List<String> hosts = ImmutableList.copyOf(getCluster(info).getHostNames());
      hosts.forEach( host -> hostsToComponents.putIfAbsent(host, new HashSet<>())); // just in case there are hosts that have no components

      Map<String, Set<String>> hostGroups = getHostGroupStrategy().calculateHostGroups(hostsToComponents);

      StackAdvisorRequest request = StackAdvisorRequest.StackAdvisorRequestBuilder
        .forStack(info.getStack().getStackId())
        .ofType(StackAdvisorRequest.StackAdvisorRequestType.HOST_GROUPS)
        .forHosts(hosts)
        .forServices(allServices.keySet())
        .forHostComponents(hostsToComponents)
        .forHostsGroupBindings(hostGroups)
        .withComponentHostsMap(componentsToHosts)
        .withConfigurations(info.getConfig())
        .withGPLLicenseAccepted(serverConfig.getGplLicenseAccepted())
        .build();
      RecommendationResponse response = stackAdvisorHelper.recommend(request);

      Map<String, Map<String, Set<String>>> recommendedLayout = getRecommendedLayout(
        response.getRecommendations().getBlueprintClusterBinding().getHostgroupHostMap(),
        response.getRecommendations().getBlueprint().getHostgroupComponentMap(),
        info.getStack()::getServiceForComponent);

      // Validate layout
      Map<String, Set<String>> recommendedComponentHosts = getComponentHostMap(recommendedLayout);
      StackAdvisorRequest validationRequest = request.builder()
        .forHostsGroupBindings(response.getRecommendations().getBlueprintClusterBinding().getHostgroupHostMap())
        .withComponentHostsMap(recommendedComponentHosts)
        .forHostComponents(getHostComponentMap(recommendedComponentHosts)).build();
      validate(validationRequest);

      Map<String,Map<String,Set<String>>> newServiceRecommendations = keepNewServicesOnly(recommendedLayout, info.newServices());
      LayoutRecommendationInfo recommendationInfo = new LayoutRecommendationInfo(
        response.getRecommendations().getBlueprintClusterBinding().getHostgroupHostMap(),
        recommendedLayout);
      return info.withLayoutRecommendation(newServiceRecommendations, recommendationInfo);
    }
    catch (AmbariException|StackAdvisorException ex) {
      throw new IllegalArgumentException("Layout recommendation failed.", ex);
    }
  }

  /**
   * Gets all services from the cluster together together with services from AddServiceInfo.
   * @param info
   * @return A map of <i>service name -> component name -> hosts</i> for all services in the cluster and the input
   *  AddServiceInfo combined.
   */
  Map<String, Map<String, Set<String>>> getAllServices(AddServiceInfo info) throws AmbariException {
    Cluster cluster = managementController.getClusters().getCluster(info.clusterName());
    Map<String, Map<String, Set<String>>> clusterServices = transformValues(
      cluster.getServices(),
      service -> transformValues(service.getServiceComponents(), component -> component.getServiceComponentsHosts()));

   return  mergeDisjunctMaps(clusterServices, info.newServices());
  }

  private Cluster getCluster(AddServiceInfo info) throws AmbariException {
    return managementController.getClusters().getCluster(info.clusterName());
  }

  AddServiceInfo recommendConfigurations(AddServiceInfo info) {
    Configuration config = info.getConfig();
    if (info.getRequest().getRecommendationStrategy().shouldUseStackAdvisor()) {
      LayoutRecommendationInfo layoutInfo = getLayoutRecommendationInfo(info);

      Map<String, Set<String>> componentHostMap = getComponentHostMap(layoutInfo.getAllServiceLayouts());
      Map<String, Set<String>> hostComponentMap = getHostComponentMap(componentHostMap);
      StackAdvisorRequest request = StackAdvisorRequest.StackAdvisorRequestBuilder
        .forStack(info.getStack().getStackId())
        .ofType(StackAdvisorRequest.StackAdvisorRequestType.CONFIGURATIONS)
        .forHosts(layoutInfo.getHosts())
        .forServices(layoutInfo.getAllServiceLayouts().keySet())
        .forHostComponents(hostComponentMap)
        .forHostsGroupBindings(layoutInfo.getHostGroups())
        .withComponentHostsMap(componentHostMap)
        .withConfigurations(config)
        .withGPLLicenseAccepted(serverConfig.getGplLicenseAccepted())
        .build();
      RecommendationResponse response;
      try {
        response = stackAdvisorHelper.recommend(request);
      }
      catch (StackAdvisorException|AmbariException ex) {
        throw new IllegalArgumentException("Configuration recommendation failed.", ex);
      }
      Map<String, RecommendationResponse.BlueprintConfigurations> configRecommendations = response.getRecommendations().getBlueprint().getConfigurations();

      // remove recommendations for existing services
      configRecommendations.keySet().removeIf(configType -> !info.newServices().containsKey(info.getStack().getServiceForConfigType(configType)));

      if (info.getRequest().getRecommendationStrategy() == ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY) {
        removeNonStackConfigRecommendations(info.getConfig().getParentConfiguration().getParentConfiguration(), configRecommendations);
      }

      Configuration recommendedConfig = toConfiguration(configRecommendations);

      Configuration userConfig = config;
      Configuration clusterAndStackConfig = userConfig.getParentConfiguration();

      if (info.getRequest().getRecommendationStrategy().shouldOverrideCustomValues()) {
        config = recommendedConfig;
        config.setParentConfiguration(userConfig);
      } else {
        config.setParentConfiguration(recommendedConfig);
        recommendedConfig.setParentConfiguration(clusterAndStackConfig);
      }

      StackAdvisorRequest validationRequest = request.builder().withConfigurations(config).build();
      validate(validationRequest);
    }

    UnitUpdater.updateUnits(config, info.getStack());
    return info.withConfig(config);
  }

  /**
   * Reuse information from layout recommendation if it happened
   */
  LayoutRecommendationInfo getLayoutRecommendationInfo(AddServiceInfo info) {
    if (info.getRecommendationInfo().isPresent()) {
      return info.getRecommendationInfo().get();
    }
    try {
      Map<String, Map<String, Set<String>>> allServices = getAllServices(info);
      Map<String, Set<String>> hostGroups =
        getHostGroupStrategy().calculateHostGroups(getHostComponentMap(getComponentHostMap(allServices)));
      return new LayoutRecommendationInfo(hostGroups, allServices);
    }
    catch (AmbariException ex) {
      throw new IllegalArgumentException("Error gathering host groups and services", ex);
    }
  }

  static void removeNonStackConfigRecommendations(Configuration stackConfig,  Map<String, RecommendationResponse.BlueprintConfigurations> configRecommendations) {
    configRecommendations.keySet().removeIf(configType -> !stackConfig.containsConfigType(configType));
    configRecommendations.entrySet().forEach( e -> {
      String cfgType = e.getKey();
      RecommendationResponse.BlueprintConfigurations cfg = e.getValue();
      cfg.getProperties().keySet().removeIf(propName -> !stackConfig.containsConfig(cfgType, propName));
      if (null != cfg.getPropertyAttributes()) {
        cfg.getPropertyAttributes().keySet().removeIf(propName -> !stackConfig.containsConfig(cfgType, propName));
      }
    });
    configRecommendations.values().removeIf(cfg -> cfg.getProperties().isEmpty() && cfg.getPropertyAttributes().isEmpty());
  }

  private void validate(StackAdvisorRequest request) {
    try {
      Set<ValidationResponse.ValidationItem> items = stackAdvisorHelper.validate(request).getItems();
      if (!items.isEmpty()) {
        LOG.warn("Issues found during recommended {} validation:\n{}", request.getRequestType(), Joiner.on('\n').join(items));
      }
    }
    catch (StackAdvisorException ex) {
      LOG.error(request.getRequestType() + " validation failed", ex);
    }
  }

  static Configuration toConfiguration(Map<String, RecommendationResponse.BlueprintConfigurations> configs) {
    Map<String, Map<String, String>> properties = configs.entrySet().stream()
      .filter( e -> e.getValue().getProperties() != null && !e.getValue().getProperties().isEmpty())
      .map(e -> Pair.of(e.getKey(), e.getValue().getProperties()))
      .collect(toMap(Pair::getKey, Pair::getValue));

    Map<String, Map<String, Map<String, String>>> propertyAttributes = configs.entrySet().stream()
        .filter( e -> e.getValue().getPropertyAttributes() != null && !e.getValue().getPropertyAttributes().isEmpty())
        .map(e -> Pair.of(e.getKey(), e.getValue().getPropertyAttributesAsMap()))
        .collect(toMap(Pair::getKey, Pair::getValue));

    return new Configuration(properties, propertyAttributes);
  }

  static Map<String,Map<String,Set<String>>> keepNewServicesOnly(Map<String,Map<String,Set<String>>> recommendedLayout, Map<String,Map<String,Set<String>>> newServices) {
    HashMap<String, java.util.Map<String, Set<String>>> newServiceRecommendations = new HashMap<>(recommendedLayout);
    newServiceRecommendations.keySet().retainAll(newServices.keySet());
    return newServiceRecommendations;
  }

  static Map<String, Map<String, Set<String>>> getRecommendedLayout(Map<String, Set<String>> hostGroupHosts,
                                                                    Map<String, Set<String>> hostGroupComponents,
                                                                    Function<String, String> componentToService) {
    Map<String, Set<String>> componentHostMap = hostGroupComponents.entrySet().stream()
      .flatMap(entry -> entry.getValue().stream().map(comp -> Pair.of(comp, entry.getKey()))) // component -> hostgroup
      .flatMap(cmpHg -> hostGroupHosts.get(cmpHg.getValue()).stream().map(host -> Pair.of(cmpHg.getKey(), host))) // component -> host
      .collect(groupingBy(Pair::getKey, mapping(Pair::getValue, toSet())));// group by component

    return componentHostMap.entrySet().stream().collect(
      groupingBy(
        cmpHost -> componentToService.apply(cmpHost.getKey()),
        toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }


  /**
   * Transform a map of component -> hosts to a map of hosts -> components
   * @param componentHostMap the map to transform
   * @return the transformed map
   */
  static Map<String, Set<String>> getHostComponentMap(Map<String, Set<String>> componentHostMap) {
    return componentHostMap.entrySet().stream()
      .flatMap(compHosts -> compHosts.getValue().stream().map(host -> Pair.of(host, compHosts.getKey())))
      .collect(groupingBy(Pair::getKey, mapping(Pair::getValue, toSet())));
  }

  /**
   * Extracts a [component -> hosts] map from the [service -> component -> hosts] map. Services
   * with empty component map will be ignored
   * @param serviceComponentHostMap the input map
   * @return the extracted map
   */
  static Map<String, Set<String>> getComponentHostMap(Map<String, Map<String, Set<String>>> serviceComponentHostMap) {
    return serviceComponentHostMap.values().stream()
      .reduce(StackAdvisorAdapter::mergeDisjunctMaps)
      .orElse(new HashMap<>());
  }

  static <S, T> Map<S, T> mergeDisjunctMaps(Map<? extends S, ? extends T> map1, Map<? extends S, ? extends T> map2) {
    Sets.SetView<? extends S> commonKeys = Sets.intersection(map1.keySet(), map2.keySet());
    checkArgument(commonKeys.isEmpty(), "Maps must be disjunct. Common keys: %s", commonKeys);
    Map<S, T> merged = new HashMap<>(map1);
    merged.putAll(map2);
    return merged;
  }

  HostGroupStrategy getHostGroupStrategy() {
    try {
      return injector.getInstance(serverConfig.getAddServiceHostGroupStrategyClass());
    }
    catch (ClassNotFoundException | ClassCastException |ConfigurationException | ProvisionException ex) {
      throw new IllegalStateException("Cannot load host group strategy", ex);
    }
  }

}
