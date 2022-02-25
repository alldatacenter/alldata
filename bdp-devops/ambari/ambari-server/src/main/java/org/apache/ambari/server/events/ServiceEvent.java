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
 * The {@link ServiceEvent} class is the base for all service events in Ambari.
 */
public abstract class ServiceEvent extends ClusterEvent {

  /**
   * The name of the service.
   */
  protected final String m_serviceName;

  /**
   * The name of the services' stack.
   */
  protected final String m_stackName;

  /**
   * The version of the services' stack.
   */
  protected final String m_stackVersion;

  /**
   * Constructor.
   *
   * @param eventType
   * @param clusterId
   */
  public ServiceEvent(AmbariEventType eventType, long clusterId,
      String stackName, String stackVersion, String serviceName) {
    super(eventType, clusterId);
    m_stackName = stackName;
    m_stackVersion = stackVersion;
    m_serviceName = serviceName;
  }

  /**
   * @return the serviceName (never {@code null}).
   */
  public String getServiceName() {
    return m_serviceName;
  }

  /**
   * @return the stackName (never {@code null}).
   */
  public String getStackName() {
    return m_stackName;
  }

  /**
   * @return the stackVersion (never {@code null}).
   */
  public String getStackVersion() {
    return m_stackVersion;
  }
}
