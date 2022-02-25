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

import static java.util.stream.Collectors.joining;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.topology.Configuration;

/**
 * Processed info for adding new services/components to an existing cluster.
 */
public final class AddServiceInfo {

  private final AddServiceRequest request;
  private final String clusterName;
  private final Stack stack;
  private final KerberosDescriptor kerberosDescriptor;
  private final Map<String, Map<String, Set<String>>> newServices;
  private final RequestStageContainer stages;
  private final Configuration config;
  private final LayoutRecommendationInfo recommendationInfo;

  private AddServiceInfo(
    AddServiceRequest request,
    String clusterName,
    RequestStageContainer stages,
    Stack stack,
    Configuration config,
    Map<String, Map<String, Set<String>>> newServices,
    KerberosDescriptor kerberosDescriptor,
    LayoutRecommendationInfo recommendationInfo
  ) {
    this.request = request;
    this.clusterName = clusterName;
    this.stack = stack;
    this.kerberosDescriptor = kerberosDescriptor;
    this.newServices = newServices;
    this.stages = stages;
    this.config = config;
    this.recommendationInfo = recommendationInfo;
  }

  public Builder toBuilder() {
    return new Builder()
      .setRequest(request)
      .setClusterName(clusterName)
      .setStack(stack)
      .setKerberosDescriptor(kerberosDescriptor)
      .setNewServices(newServices)
      .setStages(stages)
      .setConfig(config)
      .setRecommendationInfo(recommendationInfo)
      ;
  }

  public AddServiceInfo withLayoutRecommendation(Map<String, Map<String, Set<String>>> services,
                                                 LayoutRecommendationInfo recommendation) {
    return toBuilder().setNewServices(services).setRecommendationInfo(recommendation).build();
  }

  public AddServiceInfo withConfig(Configuration newConfig) {
    return toBuilder().setConfig(newConfig).build();
  }

  @Override
  public String toString() {
    return "AddServiceRequest(" + stages.getId() + ")";
  }

  public AddServiceRequest getRequest() {
    return request;
  }

  public String clusterName() {
    return clusterName;
  }

  public RequestStageContainer getStages() {
    return stages;
  }

  /**
   * New services to be added to the cluster: service -> component -> host
   * This should include both explicitly requested services, and services of the requested components.
   */
  public Map<String, Map<String, Set<String>>> newServices() {
    return newServices;
  }

  public Stack getStack() {
    return stack;
  }

  public Configuration getConfig() {
    return config;
  }

  public Optional<LayoutRecommendationInfo> getRecommendationInfo() {
    return Optional.ofNullable(recommendationInfo);
  }

  public Optional<KerberosDescriptor> getKerberosDescriptor() {
    return Optional.ofNullable(kerberosDescriptor);
  }

  /**
   * Creates a descriptive label to be displayed in the UI.
   */
  public String describe() {
    int maxServicesToShow = 3;
    StringBuilder sb = new StringBuilder("Add Services: ")
      .append(newServices.keySet().stream().sorted().limit(maxServicesToShow).collect(joining(", ")));
    if (newServices.size() > maxServicesToShow) {
      sb.append(" and ").append(newServices.size() - maxServicesToShow).append(" more");
    }
    sb.append(" to cluster ").append(clusterName);
    return sb.toString();
  }

  public boolean requiresLayoutRecommendation() {
    return !request.getServices().isEmpty();
  }

  public static class Builder {

    private AddServiceRequest request;
    private String clusterName;
    private Stack stack;
    private KerberosDescriptor kerberosDescriptor;
    private Map<String, Map<String, Set<String>>> newServices;
    private RequestStageContainer stages;
    private Configuration config;
    private LayoutRecommendationInfo recommendationInfo;

    public AddServiceInfo build() {
      return new AddServiceInfo(request, clusterName, stages, stack, config, newServices, kerberosDescriptor, recommendationInfo);
    }

    public Builder setRequest(AddServiceRequest request) {
      this.request = request;
      return this;
    }

    public Builder setClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder setStages(RequestStageContainer stages) {
      this.stages = stages;
      return this;
    }

    public Builder setStack(Stack stack) {
      this.stack = stack;
      return this;
    }

    public Builder setConfig(Configuration config) {
      this.config = config;
      return this;
    }

    public Builder setNewServices(Map<String, Map<String, Set<String>>> newServices) {
      this.newServices = newServices;
      return this;
    }

    public Builder setKerberosDescriptor(KerberosDescriptor kerberosDescriptor) {
      this.kerberosDescriptor = kerberosDescriptor;
      return this;
    }

    public Builder setRecommendationInfo(LayoutRecommendationInfo recommendationInfo) {
      this.recommendationInfo = recommendationInfo;
      return this;
    }

  }

}
