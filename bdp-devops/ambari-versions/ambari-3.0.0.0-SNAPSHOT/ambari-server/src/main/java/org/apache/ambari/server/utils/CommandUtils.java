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

package org.apache.ambari.server.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandUtils {
  private static final Logger LOG = LoggerFactory.getLogger(CommandUtils.class);

  /**
   * Converts a collection of commands to {@code}Map{@code} from command.taskId to command.
   */
  public static Map<Long, HostRoleCommand> convertToTaskIdCommandMap(Collection<HostRoleCommand> commands) {
    if (commands == null || commands.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<Long, HostRoleCommand> result = new HashMap<>();
    for (HostRoleCommand command : commands) {
      result.put(command.getTaskId(), command);
    }
    return result;
  }
}
