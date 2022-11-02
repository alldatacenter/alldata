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
package org.apache.ambari.server.agent;

import com.google.gson.Gson;

/**
 * The base class for all agent commands. Concrete implementations are
 * serialized by Gson ({@link Gson}) and should be annotated with Gson
 * annotations (not Jackson).
 */
public abstract class AgentCommand {

  private AgentCommandType commandType;

  /**
   * Constructor. Although not required for Gson, it's a good idea to have it so
   * that we don't need to worry about unsafe object construction that bypasses
   * the constructors.
   * <p/>
   * Subclasses should always use {@link #AgentCommand(AgentCommandType)}
   */
  public AgentCommand() {
    commandType = AgentCommandType.STATUS_COMMAND;
  }

  /**
   * Constructor. Must be invoked by all concrete subsclasses to properly set
   * the type.
   */
  public AgentCommand(AgentCommandType type) {
    commandType = type;
  }

  public enum AgentCommandType {
    EXECUTION_COMMAND,
    BACKGROUND_EXECUTION_COMMAND,
    STATUS_COMMAND,
    CANCEL_COMMAND,
    REGISTRATION_COMMAND,

    /**
     * Sends alert definitions to an agent which will refresh all alerts running
     * on that host.
     */
    ALERT_DEFINITION_COMMAND,

    /**
     * A single alert that should be run immediately.
     */
    ALERT_EXECUTION_COMMAND
  }

  public AgentCommandType getCommandType() {
    return commandType;
  }

  public void setCommandType(AgentCommandType commandType) {
    this.commandType = commandType;
  }

  @Override
  public String toString() {
    return "AgentCommand{" +
            "commandType=" + commandType +
            '}';
  }
}
