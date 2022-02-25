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

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.apache.ambari.server.serveraction.kerberos.Component;

/**
 * The {@link ServiceRemovedEvent} class is fired when a service is successfully
 * removed.
 */
public class ServiceRemovedEvent extends ServiceEvent {
  private final List<Component> components;

  public ServiceRemovedEvent(long clusterId, String stackName,
                             String stackVersion, String serviceName, List<Component> components) {
    super(AmbariEventType.SERVICE_REMOVED_SUCCESS, clusterId, stackName,
      stackVersion, serviceName);
    this.components = components;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("ServiceRemovedEvent{");
    buffer.append("cluserId=").append(m_clusterId);
    buffer.append(", stackName=").append(m_stackName);
    buffer.append(", stackVersion=").append(m_stackVersion);
    buffer.append(", serviceName=").append(m_serviceName);
    buffer.append("}");
    return buffer.toString();
  }

  public List<Component> getComponents() {
    return components;
  }

  public List<String> getComponentNames() {
    return components.stream().map(Component::getServiceComponentName).collect(toList());
  }
}
