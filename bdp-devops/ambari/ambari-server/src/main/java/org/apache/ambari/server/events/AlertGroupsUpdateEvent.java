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

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.agent.stomp.dto.AlertGroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AlertGroupsUpdateEvent extends STOMPEvent {

  @JsonProperty("groups")
  private List<AlertGroupUpdate> groups;

  @JsonProperty("updateType")
  private UpdateEventType type;

  public AlertGroupsUpdateEvent(List<AlertGroupUpdate> groups, UpdateEventType type) {
    super(Type.ALERT_GROUP);
    this.groups = groups;
    this.type = type;
  }

  public static AlertGroupsUpdateEvent deleteAlertGroupsUpdateEvent(List<Long> alertGroupIdsToDelete) {
    List<AlertGroupUpdate> alertGroupUpdates = new ArrayList<>(alertGroupIdsToDelete.size());
    for (Long alertGroupIdToDelete : alertGroupIdsToDelete) {
      alertGroupUpdates.add(new AlertGroupUpdate(alertGroupIdToDelete));
    }
    return new AlertGroupsUpdateEvent(alertGroupUpdates, UpdateEventType.DELETE);
  }

  public List<AlertGroupUpdate> getGroups() {
    return groups;
  }
}
