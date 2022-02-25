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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.events.InitialAlertEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.alert.AlertHelper;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertReceivedListener} class handles {@link AlertReceivedEvent}
 * and updates the appropriate DAOs. It may also fire new
 * {@link AlertStateChangeEvent} when an {@link AlertState} change is detected.
 */
@Singleton
@EagerSingleton
public class AlertReceivedListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertReceivedListener.class);

  @Inject
  Configuration m_configuration;

  @Inject
  AlertsDAO m_alertsDao;

  @Inject
  AlertDefinitionDAO m_definitionDao;

  /**
   * Used for looking up whether an alert has a valid service/component/host
   */
  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  /**
   * Used to calculate the maintenance state of new alerts being created.
   * Consider the case where you have disabled alerts for a component in MM.
   * This means that there are no current alerts in the system since disabling
   * them removes all current instances. New alerts being created for the
   * component in MM must reflect the correct MM.
   */
  @Inject
  private Provider<MaintenanceStateHelper> m_maintenanceStateHelper;

  @Inject
  private AlertHelper alertHelper;

  /**
   * Receives and publishes {@link AlertEvent} instances.
   */
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Used for ensuring that creation of {@link AlertCurrentEntity} instances has fine-grain
   * locks to prevent duplicates.
   */
  private Striped<Lock> creationLocks = Striped.lazyWeakLock(100);

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertReceivedListener(AlertEventPublisher publisher) {
    m_alertEventPublisher = publisher;
    m_alertEventPublisher.register(this);
  }

  /**
   * Adds an alert. Checks for a new state before creating a new history record.
   *
   * @param event
   *          the event to handle.
   */
  @Subscribe
  @AllowConcurrentEvents
  @RequiresSession
  public void onAlertEvent(AlertReceivedEvent event) throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    // process the list of alerts inside of a single transaction to prevent too
    // many transactions/commits
    List<Alert> alerts = event.getAlerts();

    // these can be wrapped in their own transaction
    List<AlertCurrentEntity> toMerge = new ArrayList<>();
    List<AlertCurrentEntity> toCreateHistoryAndMerge = new ArrayList<>();

    List<AlertEvent> alertEvents = new ArrayList<>(20);
    Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> alertUpdates = new HashMap<>();

    for (Alert alert : alerts) {
      Long clusterId = alert.getClusterId();
      if (clusterId == null) {
        // check event
        clusterId = event.getClusterId();
      }

      AlertDefinitionEntity definition = m_definitionDao.findByName(clusterId, alert.getName());

      if (null == definition) {
        LOG.warn(
          "Received an alert for {} which is a definition that does not exist in cluster id={}",
          alert.getName(), clusterId);

        continue;
      }

      alert.setComponent(definition.getComponentName());
      alert.setLabel(definition.getComponentName());
      alert.setService(definition.getServiceName());

      // jobs that were running when a service/component/host was changed
      // which invalidate the alert should not be reported
      if (!isValid(alert)) {
        continue;
      }
      // it's possible that a definition which is disabled will still have a
      // running alert returned; this will ensure we don't record it
      if (!definition.getEnabled()) {
        LOG.debug(
          "Received an alert for {} which is disabled. No more alerts should be received for this definition.",
          alert.getName());

        continue;
      }

      updateAlertDetails(alert, definition);

      // jobs that were running when a service/component/host was changed
      // which invalidate the alert should not be reported
      if (!isValid(alert)) {
        continue;
      }

      AlertCurrentEntity current;
      AlertState alertState = alert.getState();

      // attempt to lookup the current alert
      current = getCurrentEntity(clusterId, alert, definition);

      // if it doesn't exist then we must create it, ensuring that two or more
      // aren't created from other threads
      if( null == current ){

        // if there is no current alert and the state is skipped, then simply
        // skip over this one as there is nothing to update in the databse
        if (alertState == AlertState.SKIPPED) {
          continue;
        }

        // create a key out of the cluster/definition name/host (possibly null)
        int key = Objects.hash(clusterId, alert.getName(), alert.getHostName());
        Lock lock = creationLocks.get(key);
        lock.lock();

        // attempt to lookup the current alert again to ensure that a previous
        // thread didn't already create it
        try {
          // if it's not null anymore, then there's no work to do here
          current = getCurrentEntity(clusterId, alert, definition);
          if( null != current ) {
            continue;
          }

          // the current alert is still null, so go through and create it
          AlertHistoryEntity history = createHistory(clusterId, definition, alert);

          // this new alert must reflect the correct MM state for the
          // service/component/host
          MaintenanceState maintenanceState = getMaintenanceState(alert, clusterId);

          current = new AlertCurrentEntity();
          current.setMaintenanceState(maintenanceState);
          current.setAlertHistory(history);
          current.setLatestTimestamp(alert.getTimestamp());
          current.setOriginalTimestamp(alert.getTimestamp());
          clearStaleAlerts(alert.getHostName(), definition.getDefinitionId());

          // brand new alert instances being received are always HARD
          current.setFirmness(AlertFirmness.HARD);

          m_alertsDao.create(current);

          // create the event to fire later
          alertEvents.add(new InitialAlertEvent(clusterId, alert, current));

          if (!alertUpdates.containsKey(clusterId)) {
            alertUpdates.put(clusterId, new HashMap<>());
          }
          Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary> summaries = alertUpdates.get(clusterId);

          AlertSummaryGroupedRenderer.updateSummary(summaries, definition.getDefinitionId(),
              definition.getDefinitionName(), alertState, alert.getTimestamp(), maintenanceState, alert.getText());
        } finally {
          // release the lock for this alert
          lock.unlock();
        }
      } else if (alertState == current.getAlertHistory().getAlertState()
          || alertState == AlertState.SKIPPED) {

        // update the timestamp no matter what
        current.setLatestTimestamp(alert.getTimestamp());
        clearStaleAlerts(alert.getHostName(), definition.getDefinitionId());

        // only update some fields if the alert isn't SKIPPED
        if (alertState != AlertState.SKIPPED) {
          current.setLatestText(alert.getText());

          // ++ the occurrences (should be safe enough since we should ever only
          // be handling unique alert events concurrently
          long occurrences = current.getOccurrences() + 1;
          current.setOccurrences(occurrences);

          // ensure that if we've met the repeat tolerance and the alert is
          // still SOFT, then we transition it to HARD - we also need to fire an
          // event
          AlertFirmness firmness = current.getFirmness();
          int repeatTolerance = getRepeatTolerance(definition, clusterId);
          if (firmness == AlertFirmness.SOFT && occurrences >= repeatTolerance) {
            current.setFirmness(AlertFirmness.HARD);

            // create the event to fire later
            AlertStateChangeEvent stateChangedEvent = new AlertStateChangeEvent(clusterId, alert,
                current, alertState, firmness);

            alertEvents.add(stateChangedEvent);
          }
        }

        // some special cases for SKIPPED alerts
        if (alertState == AlertState.SKIPPED) {
          // set the text on a SKIPPED alert IFF it's not blank; a blank text
          // field means that the alert doesn't want to change the existing text
          String alertText = alert.getText();
          if (StringUtils.isNotBlank(alertText)) {
            current.setLatestText(alertText);
          }
        }

        // store the entity for merging later
        toMerge.add(current);
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug(
            "Alert State Changed: CurrentId {}, CurrentTimestamp {}, HistoryId {}, HistoryState {}",
            current.getAlertId(), current.getLatestTimestamp(),
            current.getAlertHistory().getAlertId(),
            current.getAlertHistory().getAlertState());
        }

        AlertHistoryEntity oldHistory = current.getAlertHistory();
        AlertState oldState = oldHistory.getAlertState();
        AlertFirmness oldFirmness = current.getFirmness();

        // insert history, update current
        AlertHistoryEntity history = createHistory(clusterId,
          oldHistory.getAlertDefinition(), alert);

        current.setLatestTimestamp(alert.getTimestamp());
        current.setOriginalTimestamp(alert.getTimestamp());
        current.setLatestText(alert.getText());

        clearStaleAlerts(alert.getHostName(), definition.getDefinitionId());

        current.setAlertHistory(history);

        // figure out how to set the occurrences correctly
        switch (alertState) {
          // an OK state always resets, regardless of what the old one was
          case OK:
            current.setOccurrences(1);
            break;
          case CRITICAL:
          case SKIPPED:
          case UNKNOWN:
          case WARNING:
            // OK -> non-OK is a reset
            if (oldState == AlertState.OK) {
              current.setOccurrences(1);
            } else {
              // non-OK -> non-OK is a continuation
              current.setOccurrences(current.getOccurrences() + 1);
            }
            break;
          default:
            break;
        }

        // set the firmness of the new alert state based on the state, type,
        // occurrences, and repeat tolerance
        AlertFirmness firmness = calculateFirmnessForStateChange(clusterId, definition,
            alertState, current.getOccurrences());

        current.setFirmness(firmness);

        // store the entity for merging later
        toCreateHistoryAndMerge.add(current);

        // create the event to fire later
        alertEvents.add(new AlertStateChangeEvent(clusterId, alert, current, oldState, oldFirmness));

        // create alert update to fire event to UI
        MaintenanceState maintenanceState = getMaintenanceState(alert, clusterId);

        if (!alertUpdates.containsKey(clusterId)) {
          alertUpdates.put(clusterId, new HashMap<>());
        }
        Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary> summaries = alertUpdates.get(clusterId);

        AlertSummaryGroupedRenderer.updateSummary(summaries, definition.getDefinitionId(),
            definition.getDefinitionName(), alertState, alert.getTimestamp(), maintenanceState, alert.getText());
      }
    }

    // invokes the EntityManager create/merge on various entities in a single
    // transaction
    saveEntities(toMerge, toCreateHistoryAndMerge);

    // broadcast events
    for (AlertEvent eventToFire : alertEvents) {
      m_alertEventPublisher.publish(eventToFire);
    }
    if (!alertUpdates.isEmpty()) {
      STOMPUpdatePublisher.publish(new AlertUpdateEvent(alertUpdates));
    }
  }

  private void clearStaleAlerts(String hostName, Long definitionId) throws AmbariException {
    if (StringUtil.isNotBlank(hostName)) {
      Host host = m_clusters.get().getHosts().stream().filter(h -> h.getHostName().equals(hostName))
          .findFirst().orElse(null);
      if (host != null) {
        alertHelper.clearStaleAlert(host.getHostId(), definitionId);
      }
    } else {
      alertHelper.clearStaleAlert(definitionId);
    }
  }

  private void updateAlertDetails(Alert alert, AlertDefinitionEntity definition) {
    if (alert.getService() == null) {
      alert.setService(definition.getServiceName());
    }
    if (alert.getComponent() == null) {
      alert.setComponent(definition.getComponentName());
    }
  }

  private MaintenanceState getMaintenanceState(Alert alert, Long clusterId) {
    MaintenanceState maintenanceState = MaintenanceState.OFF;
    try {
      maintenanceState = m_maintenanceStateHelper.get().getEffectiveState(clusterId, alert);
    } catch (Exception exception) {
      LOG.error("Unable to determine the maintenance mode state for {}, defaulting to OFF",
          alert, exception);
    }
    return maintenanceState;
  }

  /**
   * Gets the {@link AlertCurrentEntity} which cooresponds to the new alert being received, if any.
   *
   * @param clusterId the ID of the cluster.
   * @param alert the alert being received (not {@code null}).
   * @param definition  the {@link AlertDefinitionEntity} for the alert being received (not {@code null}).
   * @return  the existing current alert or {@code null} for none.
   */
  private AlertCurrentEntity getCurrentEntity(long clusterId, Alert alert, AlertDefinitionEntity definition){
    if (StringUtils.isBlank(alert.getHostName()) || definition.isHostIgnored()) {
      return m_alertsDao.findCurrentByNameNoHost(clusterId, alert.getName());
    } else {
      return m_alertsDao.findCurrentByHostAndName(clusterId, alert.getHostName(),
        alert.getName());
    }
  }

  /**
   * Saves alert and alert history entities in single transaction
   * @param toMerge - merge alert only
   * @param toCreateHistoryAndMerge - create new history, merge alert
   */
  @Transactional
  void saveEntities(List<AlertCurrentEntity> toMerge,
      List<AlertCurrentEntity> toCreateHistoryAndMerge) {
    for (AlertCurrentEntity entity : toMerge) {
      m_alertsDao.merge(entity, m_configuration.isAlertCacheEnabled());
    }

    for (AlertCurrentEntity entity : toCreateHistoryAndMerge) {
      m_alertsDao.create(entity.getAlertHistory());
      m_alertsDao.merge(entity);

      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Alert State Merged: CurrentId {}, CurrentTimestamp {}, HistoryId {}, HistoryState {}",
            entity.getAlertId(), entity.getLatestTimestamp(),
            entity.getAlertHistory().getAlertId(),
            entity.getAlertHistory().getAlertState());
      }
    }
  }

  /**
   * Gets whether the specified alert is valid for its reported cluster,
   * service, component, and host. This method is necessary for the following
   * cases
   * <ul>
   * <li>A service/component is removed, but an alert queued for reporting is
   * received after that event.</li>
   * <li>A host is removed from the cluster but the agent is still running and
   * reporting</li>
   * <li>A cluster is renamed</li>
   * </ul>
   *
   * @param alert
   *          the alert.
   * @return {@code true} if the alert is for a valid combination of
   *         cluster/service/component/host.
   */
  private boolean isValid(Alert alert) {
    Long clusterId = alert.getClusterId();
    String serviceName = alert.getService();
    String componentName = alert.getComponent();
    String hostName = alert.getHostName();

    // AMBARI/AMBARI_SERVER is always a valid service/component combination
    String ambariServiceName = RootService.AMBARI.name();
    String ambariServerComponentName = RootComponent.AMBARI_SERVER.name();
    String ambariAgentComponentName = RootComponent.AMBARI_AGENT.name();
    if (ambariServiceName.equals(serviceName) && ambariServerComponentName.equals(componentName)) {
      return true;
    }

    // if the alert is not bound to a cluster, then it's most likely a
    // host alert and is always valid as long as the host exists
    Clusters clusters = m_clusters.get();
    if (clusterId == null) {
      // no cluster, no host; return true out of respect for the unknown alert
      if (StringUtils.isBlank(hostName)) {
        return true;
      }

      // if a host is reported, it must be registered to some cluster somewhere
      if (!clusters.hostExists(hostName)) {
        LOG.error("Unable to process alert {} for an invalid host named {}",
            alert.getName(), hostName);
        return false;
      }

      // no cluster, valid host; return true
      return true;
    }

    // at this point the following criteria is guaranteed, so get the cluster
    // - a cluster exists
    // - this is not for AMBARI_SERVER component
    final Cluster cluster;
    try {
      cluster = clusters.getCluster(clusterId);
      if (null == cluster) {
        LOG.error("Unable to process alert {} for cluster id={}", alert.getName(), clusterId);
        return false;
      }
    } catch (AmbariException ambariException) {
      String msg = String.format("Unable to process alert %s for cluster id=%s", alert.getName(), clusterId);
      LOG.error(msg, ambariException);
      return false;
    }

    // at this point the following criteria is guaranteed
    // - a cluster exists
    // - this is not for AMBARI_SERVER component
    //
    // if the alert is for AMBARI/AMBARI_AGENT, then it's valid IFF
    // the agent's host is still a part of the reported cluster
    if (ambariServiceName.equals(serviceName) && ambariAgentComponentName.equals(componentName)) {
      // agents MUST report a hostname
      if (StringUtils.isBlank(hostName) || !clusters.hostExists(hostName)
          || !clusters.isHostMappedToCluster(clusterId, hostName)) {
        LOG.warn(
            "Unable to process alert {} for cluster {} and host {} because the host is not a part of the cluster.",
            alert.getName(), hostName);

        return false;
      }

      // AMBARI/AMBARI_AGENT and valid host; return true
      return true;
    }

    // at this point the following criteria is guaranteed
    // - a cluster exists
    // - not for the AMBARI service
    if (StringUtils.isNotBlank(hostName)) {
      // if valid hostname
      if (!clusters.hostExists(hostName)) {
        LOG.warn("Unable to process alert {} for an invalid host named {}",
            alert.getName(), hostName);
        return false;
      }

      if (!cluster.getServices().containsKey(serviceName)) {
        LOG.warn("Unable to process alert {} for an invalid service named {}",
            alert.getName(), serviceName);

        return false;
      }

      // if the alert is for a host/component then verify that the component
      // is actually installed on that host
      if (null != componentName &&
          !cluster.getHosts(serviceName, componentName).contains(hostName)) {
        LOG.warn(
            "Unable to process alert {} for an invalid service {} and component {} on host {}",
            alert.getName(), serviceName, componentName, hostName);
        return false;
      }
    }

    return true;
  }

  /**
   * Convenience method to create a new historical alert.
   *
   * @param clusterId
   *          the cluster id
   * @param definition
   *          the definition
   * @param alert
   *          the alert data
   * @return the new history record
   */
  private AlertHistoryEntity createHistory(long clusterId,
      AlertDefinitionEntity definition, Alert alert) {
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setAlertDefinitionId(definition.getDefinitionId());
    history.setAlertLabel(definition.getLabel());
    history.setAlertInstance(alert.getInstance());
    history.setAlertState(alert.getState());
    history.setAlertText(alert.getText());
    history.setAlertTimestamp(alert.getTimestamp());
    history.setClusterId(clusterId);
    history.setComponentName(alert.getComponent());
    history.setServiceName(alert.getService());

    // only set a host for the history item if the alert definition says to
    if (definition.isHostIgnored()) {
      history.setHostName(null);
    } else {
      history.setHostName(alert.getHostName());
    }

    return history;
  }

  /**
   * Gets the firmness for an {@link AlertCurrentEntity}. The following rules
   * apply:
   * <ul>
   * <li>If an alert is {@link AlertState#OK}, then the firmness is always
   * {@link AlertFirmness#HARD}.</li>
   * <li>If an alert is {@link SourceType#AGGREGATE}, then the firmness is
   * always {@link AlertFirmness#HARD}.</li>
   * <li>Otherwise, the firmness will be {@link AlertFirmness#SOFT} unless the
   * repeat tolerance has been met.</li>
   * </ul>
   *
   * @param definition
   *          the definition to read any repeat tolerance overrides from.
   * @param state
   *          the state of the {@link AlertCurrentEntity}.
   * @param occurrences the
   *          occurrences of the alert in the current state (used for
   *          calculation firmness when moving between non-OK states)
   * @return
   */
  private AlertFirmness calculateFirmnessForStateChange(Long clusterId, AlertDefinitionEntity definition,
      AlertState state, long occurrences) {
    // OK is always HARD since the alert has fulfilled the conditions
    if (state == AlertState.OK) {
      return AlertFirmness.HARD;
    }

    // aggregate alerts are always HARD since they only react to HARD alerts
    if (definition.getSourceType() == SourceType.AGGREGATE) {
      return AlertFirmness.HARD;
    }

    int tolerance = getRepeatTolerance(definition, clusterId);
    if (tolerance <= 1) {
      return AlertFirmness.HARD;
    }

    if (tolerance <= occurrences) {
      return AlertFirmness.HARD;
    }

    return AlertFirmness.SOFT;
  }

  /**
   * Gets the repeat tolerance value for the specified definition. This method
   * will return the override from the definition if
   * {@link AlertDefinitionEntity#isRepeatToleranceEnabled()} is {@code true}.
   * Otherwise, it uses {@link ConfigHelper#CLUSTER_ENV_ALERT_REPEAT_TOLERANCE},
   * defaulting to {@code 1} if not found.
   *
   * @param definition the definition (not {@code null}).
   * @param clusterId the ID of the cluster (not {@code null}).
   * @return the repeat tolerance for the alert
   */
  private int getRepeatTolerance(AlertDefinitionEntity definition, Long clusterId) {

    // if the definition overrides the global value, then use that
    if (definition.isRepeatToleranceEnabled()) {
      return definition.getRepeatTolerance();
    }

    int repeatTolerance = 1;
    try {
      Cluster cluster = m_clusters.get().getCluster(clusterId);
      String value = cluster.getClusterProperty(ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, "1");
      repeatTolerance = NumberUtils.toInt(value, 1);
    } catch (AmbariException ambariException) {
      String msg = String.format("Unable to read %s/%s from cluster %s, defaulting to 1",
        ConfigHelper.CLUSTER_ENV, ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, clusterId
      );
      LOG.warn(msg, ambariException);
    }

    return repeatTolerance;
  }
}
