/**
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

package org.apache.ambari.server.events.publishers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.events.RequestUpdateEvent;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.topology.TopologyManager;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

@EagerSingleton
public class RequestUpdateEventPublisher extends BufferedUpdateEventPublisher<RequestUpdateEvent> {

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private TopologyManager topologyManager;

  @Inject
  private RequestDAO requestDAO;

  @Inject
  private ClusterDAO clusterDAO;

  @Inject
  public RequestUpdateEventPublisher(STOMPUpdatePublisher stompUpdatePublisher) {
    super(stompUpdatePublisher);
  }

  @Override
  public STOMPEvent.Type getType() {
    return STOMPEvent.Type.REQUEST;
  }

  @Override
  public void mergeBufferAndPost(List<RequestUpdateEvent> events, EventBus m_eventBus) {
    Map<Long, RequestUpdateEvent> filteredRequests = new HashMap<>();
    for (RequestUpdateEvent event : events) {
      Long requestId = event.getRequestId();
      if (filteredRequests.containsKey(requestId)) {
        RequestUpdateEvent filteredRequest = filteredRequests.get(requestId);
        filteredRequest.setEndTime(event.getEndTime());
        filteredRequest.setRequestStatus(event.getRequestStatus());
        filteredRequest.setRequestContext(event.getRequestContext());
        filteredRequest.getHostRoleCommands().removeAll(event.getHostRoleCommands());
        filteredRequest.getHostRoleCommands().addAll(event.getHostRoleCommands());
      } else {
        filteredRequests.put(requestId, event);
      }
    }
    for (RequestUpdateEvent requestUpdateEvent : filteredRequests.values()) {
      RequestUpdateEvent filled = fillRequest(requestUpdateEvent);
      m_eventBus.post(filled);
    }
  }

  private RequestUpdateEvent fillRequest(RequestUpdateEvent event) {
    event.setProgressPercent(
        CalculatedStatus.statusFromRequest(hostRoleCommandDAO, topologyManager, event.getRequestId()).getPercent());
    if (event.getEndTime() == null || event.getStartTime() == null || event.getClusterName() == null
        || event.getRequestContext() == null) {
      RequestEntity requestEntity = requestDAO.findByPK(event.getRequestId());
      event.setStartTime(requestEntity.getStartTime());
      event.setUserName(requestEntity.getUserName());
      event.setEndTime(requestEntity.getEndTime());
      if (requestEntity.getClusterId() != -1) {
        event.setClusterName(clusterDAO.findById(requestEntity.getClusterId()).getClusterName());
      }
      event.setRequestContext(requestEntity.getRequestContext());
      event.setRequestStatus(requestEntity.getStatus());
    }
    return event;
  }
}
