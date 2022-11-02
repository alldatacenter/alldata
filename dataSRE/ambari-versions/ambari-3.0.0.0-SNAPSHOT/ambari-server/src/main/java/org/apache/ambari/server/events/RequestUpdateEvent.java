/**
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about request update. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestUpdateEvent extends STOMPEvent {

  private String clusterName;
  private Long endTime;
  private Long requestId;
  private Double progressPercent;
  private String requestContext;
  private HostRoleStatus requestStatus;
  private Long startTime;
  private String userName;

  @JsonProperty("Tasks")
  private Set<HostRoleCommand> hostRoleCommands = new HashSet<>();

  public RequestUpdateEvent(RequestEntity requestEntity,
                            HostRoleCommandDAO hostRoleCommandDAO,
                            TopologyManager topologyManager,
                            String clusterName,
                            List<HostRoleCommandEntity> hostRoleCommandEntities) {
    super(Type.REQUEST);
    this.clusterName = clusterName;
    this.endTime = requestEntity.getEndTime();
    this.requestId = requestEntity.getRequestId();
    this.progressPercent = CalculatedStatus.statusFromRequest(hostRoleCommandDAO, topologyManager, requestEntity.getRequestId()).getPercent();
    this.requestContext = requestEntity.getRequestContext();
    this.requestStatus = requestEntity.getStatus();
    this.startTime = requestEntity.getStartTime();
    this.userName = requestEntity.getUserName();

    for (HostRoleCommandEntity hostRoleCommandEntity : hostRoleCommandEntities) {
      hostRoleCommands.add(new HostRoleCommand(hostRoleCommandEntity.getTaskId(),
          hostRoleCommandEntity.getRequestId(),
          hostRoleCommandEntity.getStatus(),
          hostRoleCommandEntity.getHostName()));
    }
  }

  public RequestUpdateEvent(Long requestId, HostRoleStatus requestStatus,
                            Set<HostRoleCommand> hostRoleCommands) {
    super(Type.REQUEST);
    this.requestId = requestId;
    this.requestStatus = requestStatus;
    this.hostRoleCommands = hostRoleCommands;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String requestContext) {
    this.requestContext = requestContext;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Double getProgressPercent() {
    return progressPercent;
  }

  public void setProgressPercent(Double progressPercent) {
    this.progressPercent = progressPercent;
  }

  public HostRoleStatus getRequestStatus() {
    return requestStatus;
  }

  public void setRequestStatus(HostRoleStatus requestStatus) {
    this.requestStatus = requestStatus;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public String getUserName() { return userName; }

  public void setUserName(String userName) { this.userName = userName; }

  public Set<HostRoleCommand> getHostRoleCommands() {
    return hostRoleCommands;
  }

  public void setHostRoleCommands(Set<HostRoleCommand> hostRoleCommands) {
    this.hostRoleCommands = hostRoleCommands;
  }

  public static class HostRoleCommand {
    private Long id;
    private Long requestId;
    private HostRoleStatus status;
    private String hostName;

    public HostRoleCommand(Long id, Long requestId, HostRoleStatus status, String hostName) {
      this.id = id;
      this.requestId = requestId;
      this.status = status;
      // AMBARI_SERVER_ACTION does not have attached host, so we use server host
      this.hostName = (hostName == null) ? StageUtils.getHostName() : hostName;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getRequestId() {
      return requestId;
    }

    public void setRequestId(Long requestId) {
      this.requestId = requestId;
    }

    public HostRoleStatus getStatus() {
      return status;
    }

    public void setStatus(HostRoleStatus status) {
      this.status = status;
    }

    public String getHostName() {
      return hostName;
    }

    public void setHostName(String hostName) {
      this.hostName = hostName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      HostRoleCommand that = (HostRoleCommand) o;

      if (!id.equals(that.id)) return false;
      if (!requestId.equals(that.requestId)) return false;
      return hostName != null ? hostName.equals(that.hostName) : that.hostName == null;
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + requestId.hashCode();
      result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
      return result;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestUpdateEvent that = (RequestUpdateEvent) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (progressPercent != null ? !progressPercent.equals(that.progressPercent) : that.progressPercent != null)
      return false;
    if (requestContext != null ? !requestContext.equals(that.requestContext) : that.requestContext != null)
      return false;
    if (requestStatus != that.requestStatus) return false;
    if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;
    if (userName != null ? !userName.equals(that.userName) : that.userName != null ) return false;
    return hostRoleCommands != null ? hostRoleCommands.equals(that.hostRoleCommands) : that.hostRoleCommands == null;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (progressPercent != null ? progressPercent.hashCode() : 0);
    result = 31 * result + (requestContext != null ? requestContext.hashCode() : 0);
    result = 31 * result + (requestStatus != null ? requestStatus.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (userName != null ? userName.hashCode() : 0);
    result = 31 * result + (hostRoleCommands != null ? hostRoleCommands.hashCode() : 0);
    return result;
  }
}
