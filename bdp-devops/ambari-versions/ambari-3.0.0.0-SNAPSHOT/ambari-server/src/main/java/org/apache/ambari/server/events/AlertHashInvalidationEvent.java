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

import java.util.Collection;

/**
 * The {@link AlertHashInvalidationEvent} is fired when one of the
 * following conditions is met and the alerts running on an agent need to be
 * recalculated:
 * <ul>
 * <li>An alert definition changes.</li>
 * <li>A host is removed from the cluster.</li>
 * <li>A service host component is added or removed.</li>
 * </ul>
 */
public class AlertHashInvalidationEvent extends ClusterEvent {
  /**
   * The hosts that need to be invalidated.
   */
  private final Collection<String> m_hosts;

  /**
   * Constructor.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is in.
   * @param hosts
   *          the hosts that were invalidated.
   */
  public AlertHashInvalidationEvent(long clusterId,
      Collection<String> hosts) {
    super(AmbariEventType.ALERT_DEFINITION_HASH_INVALIDATION, clusterId);
    m_hosts = hosts;
  }

  /**
   * @return the hosts
   */
  public Collection<String> getHosts() {
    return m_hosts;
  }
}
