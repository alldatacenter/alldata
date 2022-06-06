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
package org.apache.ambari.server.events.listeners.alerts;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.NotificationState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertStateChangedListener} class response to
 * {@link AlertStateChangeEvent} and creates {@link AlertNoticeEntity} instances
 * in the database.
 * <p/>
 * {@link AlertNoticeEntity} instances will only be updated if the firmness of
 * the alert is {@link AlertFirmness#HARD}. In the case of {@link AlertState#OK}
 * (which is always {@link AlertFirmness#HARD}), then the prior alert must be
 * {@link AlertFirmness#HARD} for any notifications to be created. This is
 * because a SOFT non-OK alert (such as CRITICAL) would not have caused a
 * notification, so changing back from this SOFT state should not either.
 * <p/>
 * This class will not create {@link AlertNoticeEntity}s in the following cases:
 * <ul>
 * <li>If {@link AlertTargetEntity#isEnabled()} is {@code false}
 * <li>If the cluster is upgrading or the upgrade is suspended, only
 * {@link RootService#AMBARI} alerts will be dispatched.
 * </ul>
 */
@Singleton
@EagerSingleton
public class AlertStateChangedListener {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertStateChangedListener.class);

  /**
   * A logger that is only for logging alert state changes so that there is an
   * audit trail in the event that definitions/history are removed.
   */
  private static final Logger ALERT_LOG = LoggerFactory.getLogger("alerts");

  /**
   * [CRITICAL] [HARD] [HDFS] [namenode_hdfs_blocks_health] (NameNode Blocks
   * Health) Total Blocks:[100], Missing Blocks:[6]
   */
  private static final String ALERT_LOG_MESSAGE = "[{}] [{}] [{}] [{}] ({}) {}";

  /**
   * Used for looking up groups and targets.
   */
  @Inject
  private AlertDispatchDAO m_alertsDispatchDao;

  /**
   * Used to retrieve a cluster and check for upgrades in progress.
   */
  @Inject
  private Provider<Clusters> m_clusters;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertStateChangedListener(AlertEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Listens for when an alert's state has changed and creates
   * {@link AlertNoticeEntity} instances when appropriate to notify
   * {@link AlertTargetEntity}.
   * <p/>
   * {@link AlertNoticeEntity} are only created when the target has the
   * {@link AlertState} in its list of states.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onAlertEvent(AlertStateChangeEvent event) {
    LOG.debug("Received event {}", event);

    Alert alert = event.getAlert();
    AlertCurrentEntity current = event.getCurrentAlert();
    AlertHistoryEntity history = event.getNewHistoricalEntry();
    AlertDefinitionEntity definition = history.getAlertDefinition();

    // log to the alert audit log so there is physical record even if
    // definitions and historical enties are removed
    ALERT_LOG.info(ALERT_LOG_MESSAGE, alert.getState(), current.getFirmness(),
        definition.getServiceName(), definition.getDefinitionName(),
        definition.getLabel(), alert.getText());

    // do nothing if the firmness is SOFT
    if (current.getFirmness() == AlertFirmness.SOFT) {
      return;
    }

    // OK alerts are always HARD, so we need to catch the case where we are
    // coming from a SOFT non-OK to an OK; in these cases we should not alert
    //
    // New State = OK
    // Old Firmness = SOFT
    if (history.getAlertState() == AlertState.OK && event.getFromFirmness() == AlertFirmness.SOFT) {
      return;
    }

    // don't create any outbound alert notices if in MM
    AlertCurrentEntity currentAlert = event.getCurrentAlert();
    if (null != currentAlert
        && currentAlert.getMaintenanceState() != MaintenanceState.OFF) {
      return;
    }

    List<AlertGroupEntity> groups = m_alertsDispatchDao.findGroupsByDefinition(definition);
    List<AlertNoticeEntity> notices = new LinkedList<>();

    // for each group, determine if there are any targets that need to receive
    // a notification about the alert state change event
    for (AlertGroupEntity group : groups) {
      Set<AlertTargetEntity> targets = group.getAlertTargets();
      if (null == targets || targets.size() == 0) {
        continue;
      }

      for (AlertTargetEntity target : targets) {
        if (!canDispatch(target, history, definition)) {
          continue;
        }

        AlertNoticeEntity notice = new AlertNoticeEntity();
        notice.setUuid(UUID.randomUUID().toString());
        notice.setAlertTarget(target);
        notice.setAlertHistory(event.getNewHistoricalEntry());
        notice.setNotifyState(NotificationState.PENDING);
        notices.add(notice);
      }
    }

    // create notices if there are any to create
    if (!notices.isEmpty()) {
      m_alertsDispatchDao.createNotices(notices);
    }
  }

  /**
   * Gets whether an {@link AlertNoticeEntity} should be created for the
   * {@link AlertHistoryEntity} and {@link AlertTargetEntity}. Reasons this
   * would be false include:
   * <ul>
   * <li>The target is disabled
   * <li>The target is not configured for the state of the alert
   * <li>The cluster is upgrading and the alert is cluster-related
   * </ul>
   *
   * @param target
   *          the target (not {@code null}).
   * @param history
   *          the history entry that represents the state change (not
   *          {@code null}).
   * @return {@code true} if a notification should be dispatched for the target,
   *         {@code false} otherwise.
   * @see AlertTargetEntity#isEnabled()
   */
  private boolean canDispatch(AlertTargetEntity target,
      AlertHistoryEntity history, AlertDefinitionEntity definition) {

    // disable alert targets should be skipped
    if (!target.isEnabled()) {
      return false;
    }

    Set<AlertState> alertStates = target.getAlertStates();
    if (null != alertStates && alertStates.size() > 0) {
      if (!alertStates.contains(history.getAlertState())) {
        return false;
      }
    }

    // check if in an upgrade
    Long clusterId = history.getClusterId();
    try {
      Cluster cluster = m_clusters.get().getClusterById(clusterId);
      if (null != cluster.getUpgradeInProgress()) {
        // only send AMBARI alerts if in an upgrade
        String serviceName = definition.getServiceName();
        if (!StringUtils.equals(serviceName, RootService.AMBARI.name())) {
          LOG.debug(
              "Skipping alert notifications for {} because the cluster is upgrading",
              definition.getDefinitionName(), target);

          return false;
        }
      }
    } catch (AmbariException ambariException) {
      LOG.warn(
          "Unable to process an alert state change for cluster with ID {} because it does not exist",
          clusterId);

      return false;
    }

    return true;
  }
}
