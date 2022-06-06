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
package org.apache.ambari.server.state.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariService;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * Monitors commands during Stack Upgrade that are in a HOLDING_* failed because they failed in order to retry them
 * automatically until they exceed a certain threshold of retry time.
 */
@AmbariService
public class RetryUpgradeActionService extends AbstractScheduledService {

  private final static Logger LOG = LoggerFactory.getLogger(RetryUpgradeActionService.class);

  @Inject
  private Injector m_injector;

  @Inject
  private Provider<Clusters> m_clustersProvider;

  /**
   * Configuration.
   */
  @Inject
  private Configuration m_configuration;

  @Inject
  private HostRoleCommandDAO m_hostRoleCommandDAO;

  private final List<HostRoleStatus> HOLDING_STATUSES = Arrays.asList(HostRoleStatus.HOLDING_FAILED, HostRoleStatus.HOLDING_TIMEDOUT);

  private List<String> CUSTOM_COMMAND_NAMES_TO_IGNORE;
  private List<String> COMMAND_DETAILS_TO_IGNORE;

  /**
   * Tasks will be retried up to this many minutes after their original start time.
   */
  private int MAX_TIMEOUT_MINS;
  private Long MAX_TIMEOUT_MS;

  /**
   * Date formatter to print full dates
   */
  private DateFormat m_fullDateFormat;

  /**
   * Date formatter to print time deltas in HH:MM:ss
   */
  private SimpleDateFormat m_deltaDateFormat;


  public RetryUpgradeActionService() {
    m_fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    TimeZone tz = TimeZone.getTimeZone("UTC");
    m_deltaDateFormat = new SimpleDateFormat("HH:mm:ss");
    m_deltaDateFormat.setTimeZone(tz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Scheduler scheduler() {
    // Suggested every 10-60 secs.
    int secs = m_configuration.getStackUpgradeAutoRetryCheckIntervalSecs();
    return Scheduler.newFixedDelaySchedule(0, secs, TimeUnit.SECONDS);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Only run if the timeout mins is positive.
   */
  @Override
  protected void startUp() throws Exception {
    MAX_TIMEOUT_MINS = m_configuration.getStackUpgradeAutoRetryTimeoutMins();
    MAX_TIMEOUT_MS = MAX_TIMEOUT_MINS * 60000L;

    if (MAX_TIMEOUT_MINS < 1) {
      LOG.info("Will not start service {} used to auto-retry failed actions during " +
          "Stack Upgrade since since the property {} is either invalid/missing or set to {}",
          this.getClass().getSimpleName(), Configuration.STACK_UPGRADE_AUTO_RETRY_TIMEOUT_MINS.getKey(), MAX_TIMEOUT_MINS);
      stopAsync();
    }

    // During Stack Upgrade, some tasks don't make since to auto-retry since they are either
    // running on the server, should only be ran multiple times with human intervention,
    // or are not going to succeed on repeat attempts because they involve DB queries and not necessarily down hosts.
    CUSTOM_COMMAND_NAMES_TO_IGNORE = m_configuration.getStackUpgradeAutoRetryCustomCommandNamesToIgnore();
    COMMAND_DETAILS_TO_IGNORE = m_configuration.getStackUpgradeAutoRetryCommandDetailsToIgnore();
  }

  public void setMaxTimeout(int mins) {
    MAX_TIMEOUT_MINS = mins;
    MAX_TIMEOUT_MS = MAX_TIMEOUT_MINS * 60000L;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Analyze each cluster for any active upgrades and attempt to retry any actions in a HOLDING_* status.
   */
  @Override
  protected void runOneIteration() throws Exception {
    Map<String, Cluster> clusterMap = m_clustersProvider.get().getClusters();
    for (Cluster cluster : clusterMap.values()) {
      try {
        LOG.debug("Analyzing tasks for cluster {} that can be retried during Stack Upgrade.", cluster.getClusterName());
        Long effectiveRequestId = getActiveUpgradeRequestId(cluster);
        if (effectiveRequestId != null) {
          LOG.debug("Upgrade is in-progress with request id {}.", effectiveRequestId);
          retryHoldingCommandsInRequest(effectiveRequestId);
        }
      } catch (Exception e) {
        LOG.error("Unable to analyze commands that may be retried for cluster with id {}. Exception: {}",
            cluster.getClusterId(), e.getMessage());
      }
    }
  }

  /**
   * Get the Request Id of the most recent stack upgrade if it is active.
   * @param cluster Cluster
   * @return Request Id of active stack upgrade.
   */
  private Long getActiveUpgradeRequestId(Cluster cluster) {

    // May be null, and either upgrade or downgrade
    UpgradeEntity currentUpgrade = cluster.getUpgradeInProgress();
    if (currentUpgrade == null) {
      LOG.debug("There is no active upgrade in progress. Skip retrying failed tasks.");
      return null;
    }

    Direction direction = currentUpgrade.getDirection();
    RepositoryVersionEntity repositoryVersion = currentUpgrade.getRepositoryVersion();

    LOG.debug(
        "Found an active upgrade with id: {}, direction: {}, {} {}", currentUpgrade.getId(),
        direction, currentUpgrade.getUpgradeType(), direction.getPreposition(),
        repositoryVersion.getVersion());

    return currentUpgrade.getRequestId();
  }

  /**
   * Retry HOLDING_* tasks for the given Request Id if the tasks meet certain criteria.
   * @param requestId Request Id to search tasks for.
   */
  @Transactional
  public void retryHoldingCommandsInRequest(Long requestId) {
    if (requestId == null) {
      return;
    }
    long now = System.currentTimeMillis();

    List<HostRoleCommandEntity> holdingCommands = m_hostRoleCommandDAO.findByRequestIdAndStatuses(requestId, HOLDING_STATUSES);
    if (holdingCommands.size() > 0) {
      for (HostRoleCommandEntity hrc : holdingCommands) {
        LOG.debug("Comparing taskId: {}, attempt count: {}, original start time: {}, now: {}",
            hrc.getTaskId(), hrc.getAttemptCount(), hrc.getOriginalStartTime(), now);

        /*
        Use-Case 1:
        If the command has been sent to the host before because it was heartbeating, then it does have
        an original start time, so we can attempt to retry on this host even if no longer heartbeating.
        If the host does heartbeat again within the time interval, the command will actually be scheduled by the host.

        Use-Case 2:
        If the host is not heartbeating and the command is scheduled to be ran on it, then it means the following
        is true,
        - does not have original start time
        - does not have start time
        - attempt count is 0
        - status will be HOLDING_TIMEDOUT
        When the host does start heartbeating, we need to schedule this command by changing its state back to PENDING.

        Notes:
        While testing, can update the original_start_time of records in host_role_command table to current epoch time.
        E.g. in postgres,
        SELECT CAST(EXTRACT(EPOCH FROM NOW()) AS BIGINT) * 1000;
        UPDATE host_role_command SET attempt_count = 1, status = 'HOLDING_FAILED', original_start_time = (CAST(EXTRACT(EPOCH FROM NOW()) AS BIGINT) * 1000) WHERE task_id IN (x, y, z);
        */

        if (canRetryCommand(hrc)) {

          boolean allowRetry = false;
          // Use-Case 1
          if (hrc.getOriginalStartTime() != null && hrc.getOriginalStartTime() > 0 && hrc.getOriginalStartTime() < now) {
            Long retryTimeWindow = hrc.getOriginalStartTime() + MAX_TIMEOUT_MS;
            Long deltaMS = retryTimeWindow - now;

            if (deltaMS > 0) {
              String originalStartTimeString = m_fullDateFormat.format(new Date(hrc.getOriginalStartTime()));
              String deltaString = m_deltaDateFormat.format(new Date(deltaMS));
              LOG.info("Retrying task with id: {}, attempts: {}, original start time: {}, time til timeout: {}",
                  hrc.getTaskId(), hrc.getAttemptCount(), originalStartTimeString, deltaString);
              allowRetry = true;
            }
          }

          // Use-Case 2
          if ((hrc.getOriginalStartTime() == null || hrc.getOriginalStartTime() == -1L) &&
              (hrc.getStartTime() == null || hrc.getStartTime() == -1L) &&
              hrc.getAttemptCount() == 0){
            LOG.info("Re-scheduling task with id: {} since it has 0 attempts, and null start_time and " +
                    "original_start_time, which likely means the host was not heartbeating when the command was supposed to be scheduled.",
                hrc.getTaskId());
            allowRetry = true;
          }

          if (allowRetry) {
            retryHostRoleCommand(hrc);
          }
        }
      }
    }
  }

  /**
   * Private method to determine if a Host Role Command can be retried.
   * @param hrc Host Role Command entity.
   * @return True if can be retried, otherwise false.
   */
  private boolean canRetryCommand(HostRoleCommandEntity hrc) {
    if (hrc.isRetryAllowed() && !hrc.isFailureAutoSkipped()) {
      // Important not to retry some steps during RU/EU like "Finalize Upgrade Pre-Check", "Execute HDFS Finalize", and "Save Cluster State".
      // These elements are expected to be in lowercase already
      if (null != hrc.getCustomCommandName()) {
        for (String s : CUSTOM_COMMAND_NAMES_TO_IGNORE) {
          if (hrc.getCustomCommandName().toLowerCase().contains(s)){
            return false;
          }
        }
      }
      if (null != hrc.getCommandDetail()) {
        for (String s : COMMAND_DETAILS_TO_IGNORE) {
          if (hrc.getCommandDetail().toLowerCase().contains(s)) {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Private method to retry a Host Role Command by changing its status back to
   * {@link org.apache.ambari.server.actionmanager.HostRoleStatus#PENDING} so
   * that {@link org.apache.ambari.server.orm.DBAccessorImpl} can retry it.
   * @param hrc Host Role Command entity
   */
  private void retryHostRoleCommand(HostRoleCommandEntity hrc) {
    try {
      hrc.setStatus(HostRoleStatus.PENDING);
      hrc.setStartTime(-1L);
      // Don't change the original start time.
      hrc.setEndTime(-1L);
      hrc.setLastAttemptTime(-1L);
      // This will invalidate the cache, as expected.
      m_hostRoleCommandDAO.merge(hrc);
    } catch (Exception e) {
      LOG.error("Error while updating hostRoleCommand. Entity: {}", hrc, e);
      throw e;
    }
  }
}
