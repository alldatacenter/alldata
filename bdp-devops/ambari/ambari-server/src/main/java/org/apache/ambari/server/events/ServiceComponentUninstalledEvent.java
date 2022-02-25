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

import org.apache.ambari.server.serveraction.kerberos.Component;

/**
 * The {@link ServiceComponentUninstalledEvent} class is fired when a service
 * component is successfully uninstalled.
 */
public class ServiceComponentUninstalledEvent extends ServiceEvent {
  private final String m_componentName;
  private final String m_hostName;
  private final boolean m_recoveryEnabled;
  private final boolean masterComponent;
  private final Long m_hostId;

  public ServiceComponentUninstalledEvent(long clusterId, String stackName,
      String stackVersion, String serviceName, String componentName,
      String hostName, boolean recoveryEnabled, boolean masterComponent, Long hostId) {
    super(AmbariEventType.SERVICE_COMPONENT_UNINSTALLED_SUCCESS, clusterId,
        stackName,
        stackVersion, serviceName);

    m_componentName = componentName;
    m_hostName = hostName;
    m_recoveryEnabled = recoveryEnabled;
    this.masterComponent = masterComponent;
    m_hostId = hostId;
  }

  /**
   * @return the componentName
   */
  public String getComponentName() {
    return m_componentName;
  }

  /**
   * @return the hostName
   */
  public String getHostName() {
    return m_hostName;
  }

  /**
   * @return recovery enabled.
   */
  public boolean isRecoveryEnabled() {
    return m_recoveryEnabled;
  }

  public boolean isMasterComponent() {
    return masterComponent;
  }

  public Long getHostId() {
    return m_hostId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("ServiceComponentUninstalledEvent{");
    buffer.append("clusterId=").append(m_clusterId);
    buffer.append(", stackName=").append(m_stackName);
    buffer.append(", stackVersion=").append(m_stackVersion);
    buffer.append(", serviceName=").append(m_serviceName);
    buffer.append(", componentName=").append(m_componentName);
    buffer.append(", hostName=").append(m_hostName);
    buffer.append(", recoveryEnabled=").append(m_recoveryEnabled);
    buffer.append(", hostId=").append(m_hostId);
    buffer.append("}");
    return buffer.toString();
  }

  public Component getComponent() {
    return new Component(getHostName(), getServiceName(), getComponentName(), getHostId());
  }
}
