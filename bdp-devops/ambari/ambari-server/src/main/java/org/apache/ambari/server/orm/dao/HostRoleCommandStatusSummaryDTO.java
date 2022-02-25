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
package org.apache.ambari.server.orm.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleStatus;

/**
 * Summary for a single stage with summary counts for the tasks for that stage.
 */
public class HostRoleCommandStatusSummaryDTO {

  private Long m_stageId = Long.valueOf(0L);
  private Long m_minTime = Long.valueOf(0L);
  private Long m_maxTime = Long.valueOf(Long.MAX_VALUE);
  private boolean m_skippable = false;
  private Map<HostRoleStatus, Integer> m_counts = new HashMap<>();
  private List<HostRoleStatus> m_tasksStatus = new ArrayList<>();

  /**
   * Constructor invoked by JPA.  See {{@link HostRoleCommandDAO#findAggregateCounts(Long)}}
   */
  public HostRoleCommandStatusSummaryDTO(
      Number skippable,
      Number minStartTime,
      Number maxEndTime,
      Number stageId,
      Number aborted,
      Number completed,
      Number failed,
      Number holding,
      Number holdingFailed,
      Number holdingTimedout,
      Number inProgress,
      Number pending,
      Number queued,
      Number timedout,
      Number skippedFailed) {

    m_stageId = Long.valueOf(null == stageId ? 0L : stageId.longValue());
    if (null != skippable) {
      m_skippable = (1 == skippable.intValue());
    }
    if (null != minStartTime) {
      m_minTime = Long.valueOf(minStartTime.longValue());
    }
    if (null != maxEndTime) {
      m_maxTime = Long.valueOf(maxEndTime.longValue());
    }

    put(HostRoleStatus.ABORTED, aborted);
    put(HostRoleStatus.COMPLETED, completed);
    put(HostRoleStatus.FAILED, failed);
    put(HostRoleStatus.HOLDING, holding);
    put(HostRoleStatus.HOLDING_FAILED, holdingFailed);
    put(HostRoleStatus.HOLDING_TIMEDOUT, holdingTimedout);
    put(HostRoleStatus.IN_PROGRESS, inProgress);
    put(HostRoleStatus.PENDING, pending);
    put(HostRoleStatus.QUEUED, queued);
    put(HostRoleStatus.TIMEDOUT, timedout);
    put(HostRoleStatus.SKIPPED_FAILED, skippedFailed);
  }

  @SuppressWarnings("boxing")
  private void put(HostRoleStatus status, Number number) {
    if (null != number) {
      m_counts.put(status, number.intValue());
      for (int i = 0; i < number.intValue(); i++) {
        m_tasksStatus.add(status);
      }
    } else {
      m_counts.put(status, 0);
    }
  }

  /**
   * @return the stage id for this summary
   */
  Long getStageId() {
    return m_stageId;
  }

  /**
   * @return the task result counts, by status
   */
  public Map<HostRoleStatus, Integer> getCounts() {
    return m_counts;
  }

  /**
   * @return the list of tasks status, expanded to cover all tasks for the stage
   */
  public List<HostRoleStatus> getTaskStatuses() {
    return m_tasksStatus;
  }

  /**
   * @return the total number of tasks for the stage
   */
  public int getTaskTotal() {
    return m_tasksStatus.size();
  }

  /**
   * @return {@code true} if the stage is skippable
   */
  public boolean isStageSkippable() {
    return m_skippable;
  }

  /**
   * @return the start time, minimum, for the tasks
   */
  public Long getStartTime() {
    return m_minTime;
  }

  /**
   * @return the end time, maximum, for the tasks
   */
  public Long getEndTime() {
    return m_maxTime;
  }

  /**
   * For testing, create an empty summary.
   */
  public static HostRoleCommandStatusSummaryDTO create() {
    return new HostRoleCommandStatusSummaryDTO(
        0L, // skippable,
        0L, // minStartTime,
        0L, // maxEndTime,
        0L, // stageId,
        0L, // aborted,
        0L, // completed,
        0L, // failed,
        0L, // holding,
        0L, // holdingFailed,
        0L, // holdingTimedout,
        0L, // inProgress,
        0L, // pending,
        0L, // queued,
        0L, // timedout
        0L // skippedFailed
    );
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#COMPLETED} tasks
   */
  public HostRoleCommandStatusSummaryDTO completed(int count) {
    put(HostRoleStatus.COMPLETED, Integer.valueOf(count));
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#FAILED} tasks
   */
  public HostRoleCommandStatusSummaryDTO failed(int count) {
    put(HostRoleStatus.FAILED, Integer.valueOf(count));
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#ABORTED} tasks
   */
  public HostRoleCommandStatusSummaryDTO aborted(int count) {
    put(HostRoleStatus.ABORTED, count);
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#TIMEDOUT} tasks
   */
  public HostRoleCommandStatusSummaryDTO timedout(int count) {
    put(HostRoleStatus.TIMEDOUT, count);
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#IN_PROGRESS} tasks
   */
  public HostRoleCommandStatusSummaryDTO inProgress(int count) {
    put(HostRoleStatus.IN_PROGRESS, count);
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#PENDING} tasks
   */
  public HostRoleCommandStatusSummaryDTO pending(int count) {
    put(HostRoleStatus.PENDING, count);
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#QUEUED} tasks
   */
  public HostRoleCommandStatusSummaryDTO queued(int count) {
    put(HostRoleStatus.QUEUED, count);
    return this;
  }

  /**
   * For testing, set the number of {@link HostRoleStatus#SKIPPED_FAILED} tasks
   */
  public HostRoleCommandStatusSummaryDTO skippedFailed(int count) {
    put(HostRoleStatus.SKIPPED_FAILED, count);
    return this;
  }

}
