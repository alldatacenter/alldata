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

package org.apache.ambari.server.state.host;

import org.apache.ambari.server.state.HostEvent;
import org.apache.ambari.server.state.HostEventType;
import org.apache.ambari.server.state.HostHealthStatus;

public class HostUnhealthyHeartbeatEvent extends HostEvent {

  private final long heartbeatTime;

  private final HostHealthStatus healthStatus;

  public HostUnhealthyHeartbeatEvent(String hostName, long heartbeatTime,
      HostHealthStatus healthStatus) {
    super(hostName, HostEventType.HOST_HEARTBEAT_UNHEALTHY);
    this.heartbeatTime = heartbeatTime;
    this.healthStatus = healthStatus;
  }

  /**
   * @return the heartbeatTime
   */
  public long getHeartbeatTime() {
    return heartbeatTime;
  }

  /**
   * @return the healthStatus
   */
  public HostHealthStatus getHealthStatus() {
    return healthStatus;
  }

}
