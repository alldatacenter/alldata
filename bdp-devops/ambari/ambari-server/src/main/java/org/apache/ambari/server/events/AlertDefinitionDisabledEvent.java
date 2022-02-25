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
package org.apache.ambari.server.events;

import org.apache.ambari.server.state.alert.AlertDefinition;

/**
 * The {@link AlertDefinitionDisabledEvent} is used to represent that an
 * {@link AlertDefinition} has been disabled.
 */
public class AlertDefinitionDisabledEvent extends ClusterEvent {

  /**
   * The alert definition ID.
   */
  private final long m_definitionId;

  /**
   * The alert definition name.
   */
  private final String definitionName;

  /**
   * Constructor.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is in.
   * @param definitionId
   *          the alert definition being registered.
   */
  public AlertDefinitionDisabledEvent(long clusterId, long definitionId, String definitionName) {
    super(AmbariEventType.ALERT_DEFINITION_DISABLED, clusterId);
    m_definitionId = definitionId;
    this.definitionName = definitionName;
  }

  /**
   * Gets the definition ID.
   *
   * @return the definitionId
   */
  public long getDefinitionId() {
    return m_definitionId;
  }

  /**
   * Gets the definition name.
   *
   * @return the definitionId name
   */
  public String getDefinitionName() {
    return definitionName;
  }
}
