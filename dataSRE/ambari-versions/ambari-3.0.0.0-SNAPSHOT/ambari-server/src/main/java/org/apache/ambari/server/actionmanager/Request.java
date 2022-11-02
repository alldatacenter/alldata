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

package org.apache.ambari.server.actionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.RequestOperationLevelEntity;
import org.apache.ambari.server.orm.entities.RequestResourceFilterEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@StaticallyInject
public class Request {
  private static final Logger LOG = LoggerFactory.getLogger(Request.class);

  private final long requestId;
  private long clusterId;
  private String clusterName;
  private Long requestScheduleId;
  private String commandName;
  private String requestContext;
  private long createTime;
  private long startTime;
  private long endTime;
  private String clusterHostInfo;
  private String userName;

  /**
   * If true, this request can not be executed in parallel with any another
   * requests. That is useful when updating MM state, performing
   * decommission etc.
   * Defaults to false.
   */
   private boolean exclusive;

  /**
   * As of now, this field is not used. Request status is
   * calculated at RequestResourceProvider on the fly.
   */
  private HostRoleStatus status = HostRoleStatus.PENDING;
  private HostRoleStatus displayStatus = HostRoleStatus.PENDING;
  private String inputs;
  private List<RequestResourceFilter> resourceFilters;
  private RequestOperationLevel operationLevel;
  private RequestType requestType;

  private Collection<Stage> stages = new ArrayList<>();

  @Inject
  private static HostDAO hostDAO;

  /**
   * Construct new entity
   */
  @AssistedInject
  public Request(@Assisted long requestId, @Assisted("clusterId") Long clusterId, Clusters clusters) {
    this.requestId = requestId;
    this.clusterId = clusterId.longValue();
    this.createTime = System.currentTimeMillis();
    this.startTime = -1;
    this.endTime = -1;
    this.exclusive = false;
    this.clusterHostInfo = "{}";

    if (-1L != this.clusterId) {
      try {
        this.clusterName = clusters.getClusterById(this.clusterId).getClusterName();
      } catch (AmbariException e) {
        LOG.debug("Could not load cluster with id {}, the cluster may have been removed for request {}",
            clusterId, Long.valueOf(requestId));
      }
    }
  }

  /**
   * Construct new entity from stages provided
   */
  @AssistedInject
  //TODO remove when not needed
  public Request(@Assisted Collection<Stage> stages, @Assisted String clusterHostInfo, Clusters clusters){
    if (stages != null && !stages.isEmpty()) {
      this.stages.addAll(stages);
      Stage stage = stages.iterator().next();
      this.requestId = stage.getRequestId();
      this.clusterName = stage.getClusterName();
      try {
        this.clusterId = clusters.getCluster(clusterName).getClusterId();
      } catch (Exception e) {
        if (null != clusterName) {
          String message = String.format("Cluster %s not found", clusterName);
          LOG.error(message);
          throw new RuntimeException(message);
        }
      }
      this.requestContext = stages.iterator().next().getRequestContext();
      this.createTime = System.currentTimeMillis();
      this.startTime = -1;
      this.endTime = -1;
      this.clusterHostInfo = clusterHostInfo;
      this.requestType = RequestType.INTERNAL_REQUEST;
      this.exclusive = false;
    } else {
      String message = "Attempted to construct request from empty stage collection";
      LOG.error(message);
      throw new RuntimeException(message);
    }
  }

  /**
   * Construct new entity from stages provided
   */
  @AssistedInject
  //TODO remove when not needed
  public Request(@Assisted Collection<Stage> stages, @Assisted String clusterHostInfo, @Assisted ExecuteActionRequest actionRequest,
                 Clusters clusters, Gson gson) throws AmbariException {
    this(stages, clusterHostInfo, clusters);
    if (actionRequest != null) {
      this.resourceFilters = actionRequest.getResourceFilters();
      this.operationLevel = actionRequest.getOperationLevel();
      this.inputs = gson.toJson(actionRequest.getParameters());
      this.requestType = actionRequest.isCommand() ? RequestType.COMMAND : RequestType.ACTION;
      this.commandName = actionRequest.isCommand() ? actionRequest.getCommandName() : actionRequest.getActionName();
      this.exclusive = actionRequest.isExclusive();
    }
  }

  /**
   * Load existing request from database
   */
  @AssistedInject
  public Request(@Assisted RequestEntity entity, final StageFactory stageFactory, Clusters clusters){
    if (entity == null) {
      throw new RuntimeException("Request entity cannot be null.");
    }

    this.requestId = entity.getRequestId();
    this.clusterId = entity.getClusterId();

    if (-1L != this.clusterId) {
      try {
        this.clusterName = clusters.getClusterById(this.clusterId).getClusterName();
      } catch (AmbariException e) {
        LOG.debug("Could not load cluster with id {}, the cluster may have been removed for request {}",
            Long.valueOf(clusterId), Long.valueOf(requestId));
      }
    }

    this.createTime = entity.getCreateTime();
    this.startTime = entity.getStartTime();
    this.endTime = entity.getEndTime();
    this.exclusive = entity.isExclusive();
    this.requestContext = entity.getRequestContext();
    this.inputs = entity.getInputs();
    this.clusterHostInfo = entity.getClusterHostInfo();

    this.requestType = entity.getRequestType();
    this.commandName = entity.getCommandName();
    this.status = entity.getStatus();
    this.displayStatus = entity.getDisplayStatus();
    if (entity.getRequestScheduleEntity() != null) {
      this.requestScheduleId = entity.getRequestScheduleEntity().getScheduleId();
    }

    Collection<StageEntity> stageEntities = entity.getStages();
    if(stageEntities == null || stageEntities.isEmpty()) {
      stages = Collections.emptyList();
    } else {
      stages = new ArrayList<>(stageEntities.size());
      for (StageEntity stageEntity : stageEntities) {
        stages.add(stageFactory.createExisting(stageEntity));
      }
    }

    resourceFilters = filtersFromEntity(entity);
    operationLevel = operationLevelFromEntity(entity);
  }

  private static List<String> getHostsList(String hosts) {
    List<String> hostList = new ArrayList<>();
    if (hosts != null && !hosts.isEmpty()) {
      for (String host : hosts.split(",")) {
        if (!host.trim().isEmpty()) {
          hostList.add(host.trim());
        }
      }
    }
    return hostList;
  }

  public Collection<Stage> getStages() {
    return stages;
  }

  public void setStages(Collection<Stage> stages) {
    this.stages = stages;
  }

  public long getRequestId() {
    return requestId;
  }

  public synchronized RequestEntity constructNewPersistenceEntity() {
    RequestEntity requestEntity = new RequestEntity();

    requestEntity.setRequestId(requestId);
    requestEntity.setClusterId(clusterId);
    requestEntity.setCreateTime(createTime);
    requestEntity.setStartTime(startTime);
    requestEntity.setEndTime(endTime);
    requestEntity.setExclusive(exclusive);
    requestEntity.setRequestContext(requestContext);
    requestEntity.setInputs(inputs);
    requestEntity.setRequestType(requestType);
    requestEntity.setRequestScheduleId(requestScheduleId);
    requestEntity.setStatus(status);
    requestEntity.setDisplayStatus(displayStatus);
    requestEntity.setClusterHostInfo(clusterHostInfo);
    requestEntity.setUserName(userName);
    //TODO set all fields

    if (resourceFilters != null) {
      List<RequestResourceFilterEntity> filterEntities = new ArrayList<>();
      for (RequestResourceFilter resourceFilter : resourceFilters) {
        RequestResourceFilterEntity filterEntity = new RequestResourceFilterEntity();
        filterEntity.setServiceName(resourceFilter.getServiceName());
        filterEntity.setComponentName(resourceFilter.getComponentName());
        filterEntity.setHosts(resourceFilter.getHostNames() != null ?
          StringUtils.join(resourceFilter.getHostNames(), ",") : null);
        filterEntity.setRequestEntity(requestEntity);
        filterEntity.setRequestId(requestId);
        filterEntities.add(filterEntity);
      }
      requestEntity.setResourceFilterEntities(filterEntities);
    }


    if (operationLevel != null) {
      HostEntity hostEntity = hostDAO.findByName(operationLevel.getHostName());
      Long hostId = hostEntity != null ? hostEntity.getHostId() : null;

      RequestOperationLevelEntity operationLevelEntity = new RequestOperationLevelEntity();
      operationLevelEntity.setLevel(operationLevel.getLevel().toString());
      operationLevelEntity.setClusterName(operationLevel.getClusterName());
      operationLevelEntity.setServiceName(operationLevel.getServiceName());
      operationLevelEntity.setHostComponentName(operationLevel.getHostComponentName());
      operationLevelEntity.setHostId(hostId);
      operationLevelEntity.setRequestEntity(requestEntity);
      operationLevelEntity.setRequestId(requestId);
      requestEntity.setRequestOperationLevel(operationLevelEntity);
    }

    return requestEntity;
  }

  public String getClusterHostInfo() {
    return clusterHostInfo;
  }

  public void setClusterHostInfo(String clusterHostInfo) {
    this.clusterHostInfo = clusterHostInfo;
  }

  public Long getClusterId() {
    return Long.valueOf(clusterId);
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(String requestContext) {
    this.requestContext = requestContext;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  public List<RequestResourceFilter> getResourceFilters() {
    return resourceFilters;
  }

  public void setResourceFilters(List<RequestResourceFilter> resourceFilters) {
    this.resourceFilters = resourceFilters;
  }

  public RequestOperationLevel getOperationLevel() {
    return operationLevel;
  }

  public void setOperationLevel(RequestOperationLevel operationLevel) {
    this.operationLevel = operationLevel;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public Long getRequestScheduleId() {
    return requestScheduleId;
  }

  public void setRequestScheduleId(Long requestScheduleId) {
    this.requestScheduleId = requestScheduleId;
  }

  public List<HostRoleCommand> getCommands() {
    List<HostRoleCommand> commands = new ArrayList<>();
    for (Stage stage : stages) {
      commands.addAll(stage.getOrderedHostRoleCommands());
    }
    return commands;
  }

  @Override
  public String toString() {
    return "Request{" +
        "requestId=" + requestId +
        ", clusterId=" + clusterId +
        ", clusterName='" + clusterName + '\'' +
        ", requestContext='" + requestContext + '\'' +
        ", createTime=" + createTime +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        ", inputs='" + inputs + '\'' +
        ", status='" + status + '\'' +
        ", displayStatus='" + displayStatus + '\'' +
        ", resourceFilters='" + resourceFilters + '\'' +
        ", operationLevel='" + operationLevel + '\'' +
        ", requestType=" + requestType +
        ", stages=" + stages +
        '}';
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public void setExclusive(boolean isExclusive) {
    exclusive = isExclusive;
  }

  /**
   * Returns the user name associated with the request.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets the user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * @param entity  the request entity
   * @return a list of {@link RequestResourceFilter} from the entity, or {@code null}
   *        if none are defined
   */
  public static List<RequestResourceFilter> filtersFromEntity (RequestEntity entity) {
    List<RequestResourceFilter> resourceFilters = null;

    Collection<RequestResourceFilterEntity> resourceFilterEntities = entity.getResourceFilterEntities();
    if (resourceFilterEntities != null) {
      resourceFilters = new ArrayList<>();
      for (RequestResourceFilterEntity resourceFilterEntity : resourceFilterEntities) {
        RequestResourceFilter resourceFilter =
          new RequestResourceFilter(
            resourceFilterEntity.getServiceName(),
            resourceFilterEntity.getComponentName(),
            getHostsList(resourceFilterEntity.getHosts()));
        resourceFilters.add(resourceFilter);
      }
    }

    return resourceFilters;
  }

  /**
   * @param entity  the request entity
   * @return the {@link RequestOperationLevel} from the entity, or {@code null}
   *        if none is defined
   */
  public static RequestOperationLevel operationLevelFromEntity(RequestEntity entity) {
    RequestOperationLevel level = null;
    RequestOperationLevelEntity operationLevelEntity = entity.getRequestOperationLevel();

    if (operationLevelEntity != null) {
      String hostName = null;
      if (operationLevelEntity.getHostId() != null) {
        HostEntity hostEntity = hostDAO.findById(operationLevelEntity.getHostId());
        hostName = hostEntity.getHostName();
      }

      level = new RequestOperationLevel(
          Resource.Type.valueOf(operationLevelEntity.getLevel()),
          operationLevelEntity.getClusterName(),
          operationLevelEntity.getServiceName(),
          operationLevelEntity.getHostComponentName(),
          hostName);
    }

    return level;
  }
}
