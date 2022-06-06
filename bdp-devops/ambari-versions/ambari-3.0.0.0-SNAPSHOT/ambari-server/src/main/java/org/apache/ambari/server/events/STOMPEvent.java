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

import java.beans.Transient;

/**
 * Update data from server side, will be sent as STOMP message to recipients from all hosts.
 */
public abstract class STOMPEvent {

  /**
   * Update type.
   */
  protected final Type type;

  public STOMPEvent(Type type) {
    this.type = type;
  }

  @Transient
  public Type getType() {
    return type;
  }

  @Transient
  public String getMetricName() {
    return type.getMetricName();
  }

  public enum Type {
    ALERT("events.alerts"),
    ALERT_GROUP("events.alert_group"),
    METADATA("events.metadata"),
    HOSTLEVELPARAMS("events.hostlevelparams"),
    UI_TOPOLOGY("events.topology_update"),
    AGENT_TOPOLOGY("events.topology_update"),
    AGENT_CONFIGS("events.agent.configs"),
    CONFIGS("events.configs"),
    HOSTCOMPONENT("events.hostcomponents"),
    NAMEDHOSTCOMPONENT("events.hostrolecommands.named"),
    NAMEDTASK("events.tasks.named"),
    REQUEST("events.requests"),
    SERVICE("events.services"),
    HOST("events.hosts"),
    UI_ALERT_DEFINITIONS("events.alert_definitions"),
    ALERT_DEFINITIONS("alert_definitions"),
    UPGRADE("events.upgrade"),
    COMMAND("events.commands"),
    AGENT_ACTIONS("events.agentactions"),
    ENCRYPTION_KEY_UPDATE("events.encryption_key_update");

    /**
     * Is used to collect info about event appearing frequency.
     */
    private String metricName;

    Type(String metricName) {
      this.metricName = metricName;
    }

    public String getMetricName() {
      return metricName;
    }
  }

  public String completeDestination(String destination) {
    return destination;
  }
}
