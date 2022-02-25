/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

import java.util.List;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.commons.lang.StringUtils;

/**
 * {@link TaskEvent} is the base for all events related to create/update of tasks
 * that might result in update of stage/request status
 */
public class TaskEvent {
  /**
   * List of {@link HostRoleCommand}
   */
  private List<HostRoleCommand> hostRoleCommands;

  /**
   * Constructor.
   *
   * @param hostRoleCommands
   *          list of HRCs which have been reported back by the agents.
   */
  public TaskEvent(List<HostRoleCommand> hostRoleCommands) {
    this.hostRoleCommands = hostRoleCommands;
  }

  /**
   *  Gets hostRoleCommands that created event
   * @return List of {@link HostRoleCommand}
   */
  public List<HostRoleCommand> getHostRoleCommands() {
    return hostRoleCommands;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    String hostRoleCommands = StringUtils.join(this.hostRoleCommands, ", ");
    StringBuilder buffer = new StringBuilder("TaskEvent{");
    buffer.append("hostRoleCommands=").append(hostRoleCommands);
    buffer.append("}");
    return buffer.toString();
  }

}
