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

import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;

/**
 * The {@link MaintenanceModeEvent} is used to capture a host/service/component
 * maintenance mode change and broadcast it to the system.
 * <p/>
 * Although there are several different states aside from on & off in
 * {@link MaintenanceState}, this event should handle explicit on or off state
 * changes. This event should not be used for implied state changes, such as
 * when a component goes into implied maintenance mode as a result of its host's
 * maintenance mode being explicitely invoked.
 */
public class MaintenanceModeEvent extends AmbariEvent {

  /**
   * The new maintenance state.
   */
  private final MaintenanceState m_state;

  /**
   * The service whose maintenance state changed, or {@code null}.
   */
  private final Service m_service;

  /**
   * The host whose maintenance state changed, or {@code null}.
   */
  private final Host m_host;

  /**
   * The component whose maintenace state changed, or {@code null}.
   */
  private final ServiceComponentHost m_serviceComponentHost;

  /**
   * The cluster to which the host, service or component belongs to.
   */
  private final long m_clusterId;

  /**
   * Constructor.
   *
   * @param state
   *          the new state (not {@code null}).
   * @param service
   *          the service that changed maintenance state (not {@code null})).
   */
  public MaintenanceModeEvent(MaintenanceState state, Service service) {
    this(state, service.getClusterId(), service, null, null);
  }

  /**
   * Constructor.
   *
   * @param state
   *          the new state (not {@code null}).
   * @param clusterId
   *          the cluster to which the host belongs to (not {@code -1}).
   * @param host
   *          the host that changed maintenance state (not {@code null})).
   */
  public MaintenanceModeEvent(MaintenanceState state, long clusterId, Host host) {
    this(state, clusterId, null, host, null);
  }

  /**
   * Constructor.
   *
   * @param state
   *          the new state (not {@code null}).
   * @param serviceComponentHost
   *          the component that changed maintenance state (not {@code null})).
   */
  public MaintenanceModeEvent(MaintenanceState state,
      ServiceComponentHost serviceComponentHost) {
    this(state, serviceComponentHost.getClusterId(), null, null, serviceComponentHost);
  }

  /**
   * Constructor.
   *
   * @param state
   *          the new state (not {@code null}).
   * @param clusterId
   *          the cluster to which the host, service or component belongs to.
   * @param service
   *          the service that changed maintenance state, or {@code null}.
   * @param host
   *          the host that changed maintenance state, or {@code null}.
   * @param serviceComponentHost
   *          the component that changed maintenance state, or {@code null}.
   */
  private MaintenanceModeEvent(MaintenanceState state, long clusterId, Service service,
      Host host, ServiceComponentHost serviceComponentHost) {
    super(AmbariEventType.MAINTENANCE_MODE);
    m_state = state;
    m_clusterId = clusterId;
    m_service = service;
    m_host = host;
    m_serviceComponentHost = serviceComponentHost;
  }

  /**
   * Gets the service that had the direct maintenance mode event, or
   * {@code null} if the event was not directly on a service.
   *
   * @return the service or {@code null}
   */
  public Service getService() {
    return m_service;
  }

  /**
   * Gets the host that had the direct maintenance mode event, or {@code null}
   * if the event was not directly on a host.
   *
   * @return the host or {@code null}
   */
  public Host getHost() {
    return m_host;
  }

  /**
   * Gets the component that had the direct maintenance mode event, or
   * {@code null} if the event was not directly on a component.
   *
   * @return the component or {@code null}
   */
  public ServiceComponentHost getServiceComponentHost() {
    return m_serviceComponentHost;
  }

  /**
   * Gets the new maintenance state for the service/host/component.
   *
   * @return the new maintenance state (never {@code null}).
   */
  public MaintenanceState getMaintenanceState() {
    return m_state;
  }

  /**
   * Gets the cluster id for the host, service or component
   * that had the direct maintenance mode event,.
   *
   * @return the cluster id
   */
  public long getClusterId() {
    return m_clusterId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    Object object = null;
    if (null != m_service) {
      object = m_service;
    } else if (null != m_host) {
      object = m_host;
    } else {
      object = m_serviceComponentHost;
    }

    StringBuilder buffer = new StringBuilder("MaintenanceModeEvent{");
    buffer.append("state=").append(m_state);
    buffer.append(", object=").append(object);
    buffer.append("}");

    return buffer.toString();
  }
}
