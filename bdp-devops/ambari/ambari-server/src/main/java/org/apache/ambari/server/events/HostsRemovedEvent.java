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

import java.util.Collections;
import java.util.Set;

/**
 * The {@link HostsRemovedEvent} class is fired when the hosts are removed from the
 * cluster.
 */
public class HostsRemovedEvent extends AmbariEvent {

  private final Set<Long> hostIds;
  private final Set<String> hosts;

  public HostsRemovedEvent(Set<String> hosts, Set<Long> hostIds) {
    super(AmbariEventType.HOST_REMOVED);
    this.hostIds = hostIds != null ? hostIds : Collections.emptySet();
    this.hosts = hosts != null ? hosts : Collections.emptySet();
  }

  /**
   * @return names of removed hosts
   */
  public Set<String> getHostNames() {
    return hosts;
  }

  /**
   * @return ids of removed hosts
   */
  public Set<Long> getHostIds() {
    return hostIds;
  }

  @Override
  public String toString() {
    return "HostsRemovedEvent{" + hosts + "}";
  }
}
