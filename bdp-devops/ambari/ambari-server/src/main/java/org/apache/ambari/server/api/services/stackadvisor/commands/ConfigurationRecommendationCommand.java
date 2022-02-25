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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.BindingHostGroup;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.HostGroup;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.JsonNode;

/**
 * {@link org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand} implementation for
 * configuration recommendation.
 * <p>
 * One of the following recommandataion types is indicated:
 * <ul>
 * <li>RECOMMEND_CONFIGURATIONS</li>
 * <li>RECOMMEND_CONFIGURATIONS_FOR_SSO</li>
 * <li>RECOMMEND_CONFIGURATIONS_FOR_KERBEROS</li>
 * </ul>
 */
public class ConfigurationRecommendationCommand extends StackAdvisorCommand<RecommendationResponse> {

  private final StackAdvisorCommandType commandType;

  public ConfigurationRecommendationCommand(StackAdvisorCommandType commandType, File recommendationsDir,
                                            String recommendationsArtifactsLifetime,
                                            ServiceInfo.ServiceAdvisorType serviceAdvisorType,
                                            int requestId,
                                            StackAdvisorRunner saRunner,
                                            AmbariMetaInfo metaInfo,
                                            AmbariServerConfigurationHandler ambariServerConfigurationHandler,
                                            Map<String, JsonNode> hostInfoCache) {
    super(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, saRunner, metaInfo,
        ambariServerConfigurationHandler, hostInfoCache);
    this.commandType = commandType;
  }

  @Override
  public StackAdvisorCommandType getCommandType() {
    return commandType;
  }

  @Override
  protected void validate(StackAdvisorRequest request) throws StackAdvisorException {
    if (CollectionUtils.isEmpty(request.getHosts()) || CollectionUtils.isEmpty(request.getServices())) {
      throw new StackAdvisorException("Hosts and services must not be empty");
    }
  }

  @Override
  protected RecommendationResponse updateResponse(StackAdvisorRequest request, RecommendationResponse response) {
    response.getRecommendations().getBlueprint().setHostGroups(processHostGroups(request));
    response.getRecommendations().getBlueprintClusterBinding().setHostGroups(processHostGroupBindings(request));
    return response;
  }

  protected Set<HostGroup> processHostGroups(StackAdvisorRequest request) {
    Set<HostGroup> resultSet = new HashSet<>();
    for (Map.Entry<String, Set<String>> componentHost : request.getHostComponents().entrySet()) {
      String hostGroupName = componentHost.getKey();
      Set<String> components = componentHost.getValue();
      if (hostGroupName != null && components != null) {
        HostGroup hostGroup = new HostGroup();
        Set<Map<String, String>> componentsSet = new HashSet<>();
        for (String component : components) {
          Map<String, String> componentMap = new HashMap<>();
          componentMap.put("name", component);
          componentsSet.add(componentMap);
        }
        hostGroup.setComponents(componentsSet);
        hostGroup.setName(hostGroupName);
        resultSet.add(hostGroup);
      }
    }
    return resultSet;
  }

  private Set<BindingHostGroup> processHostGroupBindings(StackAdvisorRequest request) {
    Set<BindingHostGroup> resultSet = new HashSet<>();
    for (Map.Entry<String, Set<String>> hostBinding : request.getHostGroupBindings().entrySet()) {
      String hostGroupName = hostBinding.getKey();
      Set<String> hosts = hostBinding.getValue();
      if (hostGroupName != null && hosts != null) {
        BindingHostGroup bindingHostGroup = new BindingHostGroup();
        Set<Map<String, String>> hostsSet = new HashSet<>();
        for (String host : hosts) {
          Map<String, String> hostMap = new HashMap<>();
          hostMap.put("name", host);
          hostsSet.add(hostMap);
        }
        bindingHostGroup.setHosts(hostsSet);
        bindingHostGroup.setName(hostGroupName);
        resultSet.add(bindingHostGroup);
      }
    }
    return resultSet;
  }

  @Override
  protected String getResultFileName() {
    return "configurations.json";
  }

}
