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
 * The {@link AlertDefinitionRegistrationEvent} is used to represent that an
 * {@link AlertDefinition} is now a part of the system. This usually happens at
 * startup when the alerts are being merged from the stack. It also occurs when
 * a new alert is created via the REST APIs or when a new service is installed.
 */
public class AlertDefinitionRegistrationEvent extends ClusterEvent {

  /**
   * The newly registered alert defintiion
   */
  private final AlertDefinition m_definition;

  /**
   * Constructor.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is in.
   * @param definition
   *          the alert definition being registered.
   */
  public AlertDefinitionRegistrationEvent(
      long clusterId, AlertDefinition definition) {
    super(AmbariEventType.ALERT_DEFINITION_REGISTRATION, clusterId);
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
