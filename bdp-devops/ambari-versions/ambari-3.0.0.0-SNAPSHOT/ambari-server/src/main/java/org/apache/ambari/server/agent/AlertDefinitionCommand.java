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
package org.apache.ambari.server.agent;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * The {@link AlertDefinitionCommand} class is used to encapsulate the
 * {@link AlertDefinition}s that will be returned to an agent given a requested
 * hash.
 * <p/>
 * Commands must have {@link #addConfigs(ConfigHelper, Cluster)} invoked before
 * being sent to the agent so that the definitions will have the required
 * configuration data when they run. Failure to do this will cause the alerts to
 * be scheduled and run, but the result will always be a failure since the
 * parameterized properties they depend on will not be available.
 */
public class AlertDefinitionCommand extends AgentCommand {
  @SerializedName("clusterName")
  private final String m_clusterName;

  @SerializedName("hostName")
  private final String m_hostName;

  @SerializedName("publicHostName")
  private final String m_publicHostName;

  @SerializedName("hash")
  private final String m_hash;

  @SerializedName("alertDefinitions")
  @JsonProperty("alertDefinitions")
  private final List<AlertDefinition> m_definitions;

  @SerializedName("configurations")
  @JsonProperty("configurations")
  private Map<String, Map<String, String>> m_configurations;

  /**
   * Constructor.
   *
   * @param clusterName
   *          the name of the cluster this response is for (
   * @param hostName
   * @param publicHostName
   * @param hash
   * @param definitions
   *
   * @see AlertDefinitionHash
   */
  public AlertDefinitionCommand(String clusterName, String hostName, String publicHostName,
      String hash, List<AlertDefinition> definitions) {
    super(AgentCommandType.ALERT_DEFINITION_COMMAND);

    m_clusterName = clusterName;
    m_hostName = hostName;
    m_publicHostName = publicHostName;
    m_hash = hash;
    m_definitions = definitions;
  }

  /**
   *
   */
  @Override
  public AgentCommandType getCommandType() {
    return AgentCommandType.ALERT_DEFINITION_COMMAND;
  }

  /**
   * Gets the global hash for all alert definitions for a given host.
   *
   * @return the hash (never {@code null}).
   */
  public String getHash() {
    return m_hash;
  }

  /**
   * Gets the alert definitions
   *
   * @return
   */
  public List<AlertDefinition> getAlertDefinitions() {
    return m_definitions;
  }

  /**
   * Gets the name of the cluster.
   *
   * @return the cluster name (not {@code null}).
   */
  @JsonProperty("clusterName")
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * Gets the host name.
   *
   * @return the host name (not {@code null}).
   */
  @JsonProperty("hostName")
  public String getHostName() {
    return m_hostName;
  }

  /**
   * Adds cluster configuration properties as required by commands sent to agent.
   *
   * @param configHelper the helper
   * @param cluster the cluster, matching the cluster name specified by the command
   */
  public void addConfigs(ConfigHelper configHelper, Cluster cluster)
    throws AmbariException {
    Map<String, Map<String, String>> configTags = configHelper.getEffectiveDesiredTags(cluster, m_hostName);
    Map<String, Map<String, String>> configurations = configHelper.getEffectiveConfigProperties(cluster, configTags);
    m_configurations = configurations;
  }
}
