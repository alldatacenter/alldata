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
 * The {@link AlertDefinitionChangedEvent} is used to represent that an
 * {@link AlertDefinition} has been changed.
 */
public class AlertDefinitionChangedEvent extends ClusterEvent {

  /**
   * The changed alert defintiion
   */
  private final AlertDefinition m_definition;

  /**
   * Constructor.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is in.
   * @param definition
   *          the alert definition that was changed.
   */
  public AlertDefinitionChangedEvent(long clusterId, AlertDefinition definition) {
    super(AmbariEventType.ALERT_DEFINITION_CHANGED, clusterId);
    m_definition = definition;
  }

  /**
   * Get the registered alert definition.
   *
   * @return the alert definition (not {@code null}).
   */
  public AlertDefinition getDefinition() {
    return m_definition;
  }
}
