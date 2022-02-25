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

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.state.HostEvent;
import org.apache.ambari.server.state.HostEventType;

public class HostHealthyHeartbeatEvent extends HostEvent {

  private final long heartbeatTime;
  private AgentEnv agentEnv = null;
  private List<DiskInfo> mounts = new ArrayList<>();;

  public HostHealthyHeartbeatEvent(String hostName, long heartbeatTime, AgentEnv env, List<DiskInfo> mounts) {
    super(hostName, HostEventType.HOST_HEARTBEAT_HEALTHY);
    this.heartbeatTime = heartbeatTime;
    agentEnv = env;
    this.mounts = mounts;
  }

  /**
   * @return the heartbeatTime
   */
  public long getHeartbeatTime() {
    return heartbeatTime;
  }
  
  /**
   * @return the heartbeatinfo, if present.  Can return <code>null</code> if
   * there was no new status.
   */
  public AgentEnv getAgentEnv() {
    return agentEnv;
  }

  /**
   * @return the disks info, if present.  Can return <code>null</code> if
   * there was no new info.
   */
  public List<DiskInfo> getMounts() {
    return mounts;
  }
}
