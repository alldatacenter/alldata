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

import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.spi.upgrade.UpgradeType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains information about cluster upgrade state update. This update will be sent to all subscribed recipients.
 * Used for messaging to UI.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UpgradeUpdateEvent extends STOMPEvent {

  @JsonProperty("associated_version")
  private String associatedVersion;

  @JsonProperty("cluster_id")
  private Long clusterId;

  @JsonProperty("direction")
  private Direction direction;

  @JsonProperty("downgrade_allowed")
  private Boolean downgradeAllowed;

  @JsonProperty("request_id")
  private Long requestId;

  @JsonProperty("request_status")
  private HostRoleStatus requestStatus;

  @JsonProperty("skip_failures")
  private Boolean skipFailures;

  @JsonProperty("skip_service_check_failures")
  private Boolean skipServiceCheckFailures;

  @JsonProperty("upgrade_type")
  private UpgradeType upgradeType;

  @JsonProperty("start_time")
  private Long startTime;

  @JsonProperty("end_time")
  private Long endTime;

  @JsonProperty("upgrade_id")
  private Long upgradeId;

  @JsonProperty("suspended")
  private Boolean suspended;

  @JsonProperty("progress_percent")
  private Double progressPercent;

  @JsonProperty("revert_allowed")
  private Boolean revertAllowed;

  @JsonProperty("type")
  private UpdateEventType type;

  private UpgradeUpdateEvent(UpdateEventType type) {
    super(Type.UPGRADE);
    this.type = type;
  }

  public static UpgradeUpdateEvent formFullEvent(HostRoleCommandDAO hostRoleCommandDAO, RequestDAO requestDAO, UpgradeEntity upgradeEntity,
                                                 UpdateEventType type) {
    UpgradeUpdateEvent upgradeUpdateEvent = new UpgradeUpdateEvent(UpdateEventType.CREATE);
    Map<Long, HostRoleCommandStatusSummaryDTO> summary = hostRoleCommandDAO.findAggregateCounts(
        upgradeEntity.getRequestId());
    CalculatedStatus calc = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());
    double progressPercent;
    if (calc.getStatus() == HostRoleStatus.ABORTED && upgradeEntity.isSuspended()) {
      double percent = UpgradeResourceProvider.calculateAbortedProgress(summary);
      progressPercent = percent*100;
    } else {
      progressPercent = calc.getPercent();
    }

    RequestEntity rentity = requestDAO.findByPK(upgradeEntity.getRequestId());

    upgradeUpdateEvent.setUpgradeId(upgradeEntity.getId());
    upgradeUpdateEvent.setAssociatedVersion(upgradeEntity.getRepositoryVersion().getVersion());
    upgradeUpdateEvent.setClusterId(upgradeEntity.getClusterId());
    upgradeUpdateEvent.setDirection(upgradeEntity.getDirection());
    upgradeUpdateEvent.setDowngradeAllowed(upgradeEntity.isDowngradeAllowed());
    upgradeUpdateEvent.setRequestId(upgradeEntity.getRequestId());
    upgradeUpdateEvent.setRequestStatus(calc.getStatus());
    upgradeUpdateEvent.setSkipFailures(upgradeEntity.isComponentFailureAutoSkipped());
    upgradeUpdateEvent.setSkipServiceCheckFailures(upgradeEntity.isServiceCheckFailureAutoSkipped());
    upgradeUpdateEvent.setUpgradeType(upgradeEntity.getUpgradeType());
    upgradeUpdateEvent.setStartTime(rentity.getStartTime());
    upgradeUpdateEvent.setEndTime(rentity.getEndTime());
    upgradeUpdateEvent.setSuspended(upgradeEntity.isSuspended());
    upgradeUpdateEvent.setProgressPercent(progressPercent);
    upgradeUpdateEvent.setRevertAllowed(upgradeEntity.isRevertAllowed());

    return upgradeUpdateEvent;
  }

  public static UpgradeUpdateEvent formUpdateEvent(HostRoleCommandDAO hostRoleCommandDAO, RequestDAO requestDAO,
                                                   UpgradeEntity upgradeEntity) {
    Map<Long, HostRoleCommandStatusSummaryDTO> summary = hostRoleCommandDAO.findAggregateCounts(
        upgradeEntity.getRequestId());
    CalculatedStatus calc = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());
    double progressPercent;
    if (calc.getStatus() == HostRoleStatus.ABORTED && upgradeEntity.isSuspended()) {
      double percent = UpgradeResourceProvider.calculateAbortedProgress(summary);
      progressPercent = percent*100;
    } else {
      progressPercent = calc.getPercent();
    }
    RequestEntity rentity = requestDAO.findByPK(upgradeEntity.getRequestId());

    UpgradeUpdateEvent upgradeUpdateEvent = new UpgradeUpdateEvent(UpdateEventType.UPDATE);
    upgradeUpdateEvent.setRequestId(upgradeEntity.getRequestId());
    upgradeUpdateEvent.setProgressPercent(progressPercent);
    upgradeUpdateEvent.setSuspended(upgradeEntity.isSuspended());
    upgradeUpdateEvent.setStartTime(rentity.getStartTime());
    upgradeUpdateEvent.setEndTime(rentity.getEndTime());
    upgradeUpdateEvent.setClusterId(upgradeEntity.getClusterId());
    upgradeUpdateEvent.setRequestStatus(calc.getStatus());

    return upgradeUpdateEvent;
  }

  public String getAssociatedVersion() {
    return associatedVersion;
  }

  public void setAssociatedVersion(String associatedVersion) {
    this.associatedVersion = associatedVersion;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public Direction getDirection() {
    return direction;
  }

  public void setDirection(Direction direction) {
    this.direction = direction;
  }

  public Boolean getDowngradeAllowed() {
    return downgradeAllowed;
  }

  public void setDowngradeAllowed(Boolean downgradeAllowed) {
    this.downgradeAllowed = downgradeAllowed;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public HostRoleStatus getRequestStatus() {
    return requestStatus;
  }

  public void setRequestStatus(HostRoleStatus requestStatus) {
    this.requestStatus = requestStatus;
  }

  public Boolean getSkipFailures() {
    return skipFailures;
  }

  public void setSkipFailures(Boolean skipFailures) {
    this.skipFailures = skipFailures;
  }

  public Boolean getSkipServiceCheckFailures() {
    return skipServiceCheckFailures;
  }

  public void setSkipServiceCheckFailures(Boolean skipServiceCheckFailures) {
    this.skipServiceCheckFailures = skipServiceCheckFailures;
  }

  public UpgradeType getUpgradeType() {
    return upgradeType;
  }

  public void setUpgradeType(UpgradeType upgradeType) {
    this.upgradeType = upgradeType;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Long getUpgradeId() {
    return upgradeId;
  }

  public void setUpgradeId(Long upgradeId) {
    this.upgradeId = upgradeId;
  }

  public Boolean getSuspended() {
    return suspended;
  }

  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  public Double getProgressPercent() {
    return progressPercent;
  }

  public void setProgressPercent(Double progressPercent) {
    this.progressPercent = progressPercent;
  }

  public Boolean getRevertAllowed() {
    return revertAllowed;
  }

  public void setRevertAllowed(Boolean revertAllowed) {
    this.revertAllowed = revertAllowed;
  }
}
