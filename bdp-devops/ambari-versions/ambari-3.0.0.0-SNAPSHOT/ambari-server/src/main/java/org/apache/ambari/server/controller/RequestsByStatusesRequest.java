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

package org.apache.ambari.server.controller;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.actionmanager.HostRoleStatus;

public class RequestsByStatusesRequest {
  Set<String> statuses;

  public RequestsByStatusesRequest() {
    statuses = new HashSet<>();
    statuses.add(HostRoleStatus.PENDING.toString());
    statuses.add(HostRoleStatus.QUEUED.toString());
    statuses.add(HostRoleStatus.IN_PROGRESS.toString());
  }

  public RequestsByStatusesRequest(Set<String> statuses) {
    this.statuses = statuses;
  }

  public Set<String> getStatuses() {
    return statuses;
  }

  public void setStatuses(Set<String> statuses) {
    this.statuses = statuses;
  }
}
