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

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.apache.ambari.server.controller.internal.BaseClusterRequest.PROVISION_ACTION_PROPERTY;
import static org.apache.ambari.server.controller.internal.ClusterResourceProvider.CREDENTIALS;
import static org.apache.ambari.server.controller.internal.ClusterResourceProvider.SECURITY;
import static org.apache.ambari.server.controller.internal.ProvisionClusterRequest.CONFIG_RECOMMENDATION_STRATEGY;
import static org.apache.ambari.server.controller.internal.ServiceResourceProvider.OPERATION_TYPE;
import static org.apache.ambari.server.topology.Configurable.CONFIGURATIONS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.ConfigurableHelper;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.Credential;
import org.apache.ambari.server.topology.SecurityConfiguration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Data object representing an add service request.
 */
@ApiModel
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AddServiceRequest {

  private static final String STACK_NAME = "stack_name";
  private static final String STACK_VERSION = "stack_version";
  private static final String SERVICES = "services";
  private static final String COMPONENTS = "components";
  private static final String VALIDATION = "validation";

  public static final Set<String> TOP_LEVEL_PROPERTIES = ImmutableSet.of(
    OPERATION_TYPE, CONFIG_RECOMMENDATION_STRATEGY, PROVISION_ACTION_PROPERTY, VALIDATION,
    STACK_NAME, STACK_VERSION, SERVICES, COMPONENTS, CONFIGURATIONS
  );

  private final OperationType operationType;
  private final ConfigRecommendationStrategy recommendationStrategy;
  private final ProvisionAction provisionAction;
  private final ValidationType validationType;
  private final String stackName;
  private final String stackVersion;
  private final Set<Service> services;
  private final Set<Component> components;
  private final SecurityConfiguration security;
  private final Map<String, Credential> credentials;
  private final Configuration configuration;

  @JsonCreator
  public AddServiceRequest(
    @JsonProperty(OPERATION_TYPE) OperationType operationType,
    @JsonProperty(CONFIG_RECOMMENDATION_STRATEGY) ConfigRecommendationStrategy recommendationStrategy,
    @JsonProperty(PROVISION_ACTION_PROPERTY) ProvisionAction provisionAction,
    @JsonProperty(VALIDATION) ValidationType validationType,
    @JsonProperty(STACK_NAME) String stackName,
    @JsonProperty(STACK_VERSION) String stackVersion,
    @JsonProperty(SERVICES) Set<Service> services,
    @JsonProperty(COMPONENTS) Set<Component> components,
    @JsonProperty(SECURITY) SecurityConfiguration security,
    @JsonProperty(CREDENTIALS) Set<Credential> credentials,
    @JsonProperty(CONFIGURATIONS) Collection<? extends Map<String, ?>> configs
  ) {
    this.operationType = null != operationType ? operationType : OperationType.ADD_SERVICE;
    this.recommendationStrategy = null != recommendationStrategy ? recommendationStrategy : ConfigRecommendationStrategy.defaultForAddService();
    this.provisionAction = null != provisionAction ? provisionAction : ProvisionAction.INSTALL_AND_START;
    this.validationType = validationType != null ? validationType : ValidationType.DEFAULT;
    this.stackName = stackName;
    this.stackVersion = stackVersion;
    this.services = null != services ? services : emptySet();
    this.components = null != components ? components : emptySet();
    this.security = security;
    this.configuration = null != configs ? ConfigurableHelper.parseConfigs(configs) : Configuration.newEmpty();
    this.credentials = null != credentials
      ? credentials.stream().collect(toMap(Credential::getAlias, Function.identity()))
      : ImmutableMap.of();
  }

  // TODO move to JsonUtils -- pick part of 0252c08d86f
  public static AddServiceRequest of(String json) {
    try {
      return new ObjectMapper().readValue(json, AddServiceRequest.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @JsonProperty(OPERATION_TYPE)
  @ApiModelProperty(name = OPERATION_TYPE)
  public OperationType getOperationType() {
    return operationType;
  }

  @JsonProperty(CONFIG_RECOMMENDATION_STRATEGY)
  @ApiModelProperty(name = CONFIG_RECOMMENDATION_STRATEGY)
  public ConfigRecommendationStrategy getRecommendationStrategy() {
    return recommendationStrategy;
  }

  @JsonProperty(PROVISION_ACTION_PROPERTY)
  @ApiModelProperty(name = PROVISION_ACTION_PROPERTY)
  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  @JsonProperty(VALIDATION)
  @ApiModelProperty(name = VALIDATION)
  public ValidationType getValidationType() {
    return validationType;
  }

  @JsonProperty(STACK_NAME)
  @ApiModelProperty(name = STACK_NAME)
  public String getStackName() {
    return stackName;
  }

  @JsonProperty(STACK_VERSION)
  @ApiModelProperty(name = STACK_VERSION)
  public String getStackVersion() {
    return stackVersion;
  }

  @JsonIgnore
  @ApiIgnore
  public Optional<StackId> getStackId() {
    return null != stackName && null != stackVersion
      ? Optional.of(new StackId(stackName, stackVersion)) : Optional.empty();
  }

  @JsonProperty(SERVICES)
  @ApiModelProperty(name = SERVICES)
  public Set<Service> getServices() {
    return services;
  }

  @JsonProperty(COMPONENTS)
  @ApiModelProperty(name = COMPONENTS)
  public Set<Component> getComponents() {
    return components;
  }

  @JsonIgnore
  @ApiIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  @JsonProperty(CONFIGURATIONS)
  @ApiModelProperty(name = CONFIGURATIONS)
  public Collection<Map<String, Map<String, ?>>> getConfigurationContents() {
    return ConfigurableHelper.convertConfigToMap(configuration);
  }

  @JsonIgnore // TODO confirm: credentials shouldn't be serialized
  @ApiIgnore
  public Map<String, Credential> getCredentials() {
    return credentials;
  }

  @JsonIgnore
  @ApiIgnore
  public Optional<SecurityConfiguration> getSecurity() {
    return Optional.ofNullable(security);
  }

  @JsonProperty(SECURITY)
  @ApiModelProperty(name = SECURITY)
  public SecurityConfiguration _getSecurity() {
    return security;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AddServiceRequest other = (AddServiceRequest) obj;

    return Objects.equals(operationType, other.operationType) &&
      Objects.equals(validationType, other.validationType) &&
      Objects.equals(recommendationStrategy, other.recommendationStrategy) &&
      Objects.equals(provisionAction, other.provisionAction) &&
      Objects.equals(stackName, other.stackName) &&
      Objects.equals(stackVersion, other.stackVersion) &&
      Objects.equals(services, other.services) &&
      Objects.equals(components, other.components) &&
      Objects.equals(security, other.security) &&
      Objects.equals(configuration, other.configuration);
    // credentials is ignored for equality, since it's not serialized
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationType, validationType, recommendationStrategy, provisionAction, stackName, stackVersion,
      services, components, configuration, security);
    // credentials is ignored for hashcode, since it's not serialized
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add(OPERATION_TYPE, operationType)
      .add(VALIDATION, validationType)
      .add(CONFIG_RECOMMENDATION_STRATEGY, recommendationStrategy)
      .add(PROVISION_ACTION_PROPERTY, provisionAction)
      .add(STACK_NAME, stackName)
      .add(STACK_VERSION, stackVersion)
      .add(SERVICES, services)
      .add(COMPONENTS, components)
      .add(CONFIGURATIONS, configuration)
      .add(SECURITY, security)
      // credentials is not part of string output
      .toString();
  }

  // ------- inner classes -------

  public enum OperationType {
    ADD_SERVICE, DELETE_SERVICE, MOVE_SERVICE
  }

  public enum ValidationType {
    /**
     * Perform all validation checks.
     */
    STRICT {
      @Override
      public boolean strictValidation() {
        return true;
      }
    },
    /**
     * Skip the parts of validation that are not strictly necessary.
     */
    PERMISSIVE {
      @Override
      public boolean strictValidation() {
        return false;
      }
    },
    ;

    public static final ValidationType DEFAULT = STRICT;

    public abstract boolean strictValidation();
  }

}
