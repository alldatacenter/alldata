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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.QueryLexer;
import org.apache.ambari.server.api.predicate.Token;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.TopologyRequest;

/**
 * Provides common cluster request functionality.
 */
public abstract class BaseClusterRequest implements TopologyRequest {
  /**
   * Support for controlling whether Install and Start tasks are created on
   * blueprint deploy by default.
   */
  public static final String PROVISION_ACTION_PROPERTY = "provision_action";
  /**
   * host group info map
   */
  protected final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();

  protected ProvisionAction provisionAction;

  /**
   * cluster id
   */
  protected Long clusterId;

  /**
   * blueprint
   */
  //todo: change interface to only return blueprint name
  protected Blueprint blueprint;

  /**
   * configuration
   */
  protected Configuration configuration;

  /**
   * security configuration
   */
  protected SecurityConfiguration securityConfiguration;

  /**
   * blueprint factory
   */
  protected static BlueprintFactory blueprintFactory;

  /**
   * Lexer used to obtain property names from a predicate string
   */
  private static final QueryLexer queryLexer = new QueryLexer();

  /**
   * host resource provider used to validate predicate properties
   */
  private static ResourceProvider hostResourceProvider;


  /**
   * inject blueprint factory
   * @param factory  blueprint factory
   */
  public static void init(BlueprintFactory factory) {
    blueprintFactory = factory;
  }

  @Override
  public Long getClusterId() {
    return clusterId;
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
    return hostGroupInfoMap;
  }

  /**
   * Validate that all properties specified in the predicate are valid for the Host resource.
   *
   * @param predicate  predicate to validate
   *
   * @throws InvalidTopologyTemplateException  if any of the properties specified in the predicate are invalid
   *                                           for the Host resource type
   */
  protected void validateHostPredicateProperties(String predicate) throws InvalidTopologyTemplateException {
    Token[] tokens;
    try {
      tokens = queryLexer.tokens(predicate);
    } catch (InvalidQueryException e) {
      throw new InvalidTopologyTemplateException(
          String.format("The specified host query is invalid: %s", e.getMessage()));
    }

    Set<String> propertyIds = new HashSet<>();
    for (Token token : tokens) {
      if (token.getType() == Token.TYPE.PROPERTY_OPERAND) {
        propertyIds.add(token.getValue());
      }
    }

    Set<String> invalidProperties = ensureHostProvider().checkPropertyIds(propertyIds);
    if (! invalidProperties.isEmpty()) {
      throw new InvalidTopologyTemplateException(String.format(
          "Invalid Host Predicate.  The following properties are not valid for a host predicate: %s",
          invalidProperties));
    }
  }

  /**
   * Set the request blueprint.
   *
   * @param blueprint blueprint
   */
  protected void setBlueprint(Blueprint blueprint) {
    this.blueprint = blueprint;
  }

  /**
   * Set the request configuration.
   *
   * @param configuration  configuration
   */
  protected void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the blueprint factory.
   */
  protected BlueprintFactory getBlueprintFactory() {
    return blueprintFactory;
  }


  public SecurityConfiguration getSecurityConfiguration() {
    return securityConfiguration;
  }

  /**
   * Get the host resource provider instance.
   *
   * @return host resourece provider instance
   */
  private static synchronized ResourceProvider ensureHostProvider() {
    if (hostResourceProvider == null) {
      hostResourceProvider = ClusterControllerHelper.getClusterController().
          ensureResourceProvider(Resource.Type.Host);
    }
    return hostResourceProvider;
  }

  /**
   * Get requested @ProvisionClusterRequest.ProvisionAction
   */
  public ProvisionAction getProvisionAction() {
    return provisionAction;
  }

  public void setProvisionAction(ProvisionAction provisionAction) {
    this.provisionAction = provisionAction;
  }
}
