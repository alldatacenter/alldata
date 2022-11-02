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

import org.apache.ambari.server.state.alert.AlertDefinition;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link AlertExecutionCommand} is used to instruct an agent to run an
 * alert immediately and return the result in the next heartbeat. This does not
 * affect any existing jobs and does cause the job for the executed alert to
 * reset its interval.
 */
public class AlertExecutionCommand extends AgentCommand {

  /**
   * The name of the cluster.
   */
  @SerializedName("clusterName")
  @com.fasterxml.jackson.annotation.JsonProperty("clusterName")
  private final String m_clusterName;

  /**
   * The agent hostname.
   */
  @SerializedName("hostName")
  @com.fasterxml.jackson.annotation.JsonProperty("hostName")
  private final String m_hostName;

  /**
   * The definition to run.
   */
  @SerializedName("alertDefinition")
  @com.fasterxml.jackson.annotation.JsonProperty("alertDefinition")
  private final AlertDefinition m_definition;

  /**
   * Constructor.
   *
   * @param clusterName
   *          the name of the cluster (not {@code null}).
   * @param hostName
   *          the name of the host (not {@code null}).
   * @param definition
   *          the definition to run (not {@code null}).
   */
  public AlertExecutionCommand(String clusterName, String hostName, AlertDefinition definition) {
    super(AgentCommandType.ALERT_EXECUTION_COMMAND);
    m_clusterName = clusterName;
    m_hostName = hostName;
    m_definition = definition;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AgentCommandType getCommandType() {
    return AgentCommandType.ALERT_EXECUTION_COMMAND;
  }
}
