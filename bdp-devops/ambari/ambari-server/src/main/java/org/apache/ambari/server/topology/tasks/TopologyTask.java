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

package org.apache.ambari.server.topology.tasks;

import java.util.Set;

import org.apache.ambari.server.RoleCommand;

import com.google.common.collect.ImmutableSet;

/**
 * Task which is executed by the TopologyManager.
 */
public interface TopologyTask extends Runnable {
  /**
   * Task type.
   */
  enum Type {
    RESOURCE_CREATION,
    CONFIGURE,
    INSTALL,
    START {
      @Override
      public Set<RoleCommand> tasksToAbortOnFailure() {
        return ImmutableSet.of(RoleCommand.START);
      }
    },
    ;

    private static Set<RoleCommand> ALL_TASKS = ImmutableSet.of(RoleCommand.INSTALL, RoleCommand.START);

    public Set<RoleCommand> tasksToAbortOnFailure() {
      return ALL_TASKS;
    }
  }

  /**
   * Get the task type.
   *
   * @return the type of task
   */
  Type getType();
}
