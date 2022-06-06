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


/**
 * The {@link ServiceComponentRecoveryChangedEvent} class is fired when a service
 * component is enabled or disabled for auto start.
 */
public class ServiceComponentRecoveryChangedEvent extends AmbariEvent {
  private final long m_clusterId;
  private final String m_clusterName;
  private final String m_serviceName;
  private final String m_componentName;
  private final boolean m_recoveryEnabled;

  public ServiceComponentRecoveryChangedEvent(
          long clusterId, String clusterName, String serviceName, String componentName, boolean recoveryEnabled) {
    super(AmbariEventType.SERVICE_COMPONENT_RECOVERY_CHANGED);
    m_clusterId = clusterId;
    m_clusterName = clusterName;
    m_serviceName = serviceName;
    m_componentName = componentName;
    m_recoveryEnabled = recoveryEnabled;
  }

  public long getClusterId() {
    return m_clusterId;
  }

  /**
   * Get the cluster name
   *
   * @return
   */
  public String getClusterName() {
    return m_clusterName;
  }

  /**
   * Get the service name
   *
   * @return
   */
  public String getServiceName() {
    return m_serviceName;
  }

  /**
   * Get the component name
   *
   * @return
   */
  public String getComponentName() {
    return m_componentName;
  }

  /**
   * Get recovery enabled
   *
   * @return
   */
  public boolean isRecoveryEnabled() {
    return m_recoveryEnabled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("ServiceComponentRecoveryChangeEvent{");
    buffer.append("clusterId=").append(getClusterId());
    buffer.append(", clusterName=").append(getClusterName());
    buffer.append(", serviceName=").append(getServiceName());
    buffer.append(", componentName=").append(getComponentName());
    buffer.append(", recoveryEnabled=").append(isRecoveryEnabled());
    buffer.append("}");
    return buffer.toString();
  }
}
