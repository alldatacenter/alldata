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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.events.AggregateAlertRecalculateEvent;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link AlertMaintenanceModeListener} handles events that relate to
 * Maintenance Mode changes.
 */
@Singleton
@EagerSingleton
public class AlertMaintenanceModeListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertMaintenanceModeListener.class);

  /**
   * Publishes {@link AlertEvent} instances.
   */
  @Inject
  private AlertEventPublisher m_alertEventPublisher;

  /**
   * Used for updating the MM of current alerts.
   */
  @Inject
  private AlertsDAO m_alertsDao = null;

  /**
   * Used for quick lookups of aggregate alerts.
   */
  @Inject
  private AggregateDefinitionMapping m_aggregateMapping;

  @Inject
  private STOMPUpdatePublisher  STOMPUpdatePublisher;

  private long clusterId = -1;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public AlertMaintenanceModeListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  /**
   * Handles {@link MaintenanceModeEvent} by performing the following tasks:
   * <ul>
   * <li>Iterates through all {@link AlertNoticeEntity}, updating the MM state</li>
   * </ul>
   *
   * @param event
   *          the event being handled.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onEvent(MaintenanceModeEvent event) {
    LOG.debug("Received event {}", event);

    boolean recalculateAggregateAlert = false;
    List<AlertCurrentEntity> currentAlerts = m_alertsDao.findCurrent();

    MaintenanceState newMaintenanceState = MaintenanceState.OFF;
    if (event.getMaintenanceState() != MaintenanceState.OFF) {
      newMaintenanceState = MaintenanceState.ON;
    }
    Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> alertUpdates = new HashMap<>();
    for( AlertCurrentEntity currentAlert : currentAlerts ){
      AlertHistoryEntity history = currentAlert.getAlertHistory();

      String alertHostName = history.getHostName();
      String alertServiceName = history.getServiceName();
      String alertComponentName = history.getComponentName();

      try {
        Host host = event.getHost();
        Service service = event.getService();
        ServiceComponentHost serviceComponentHost = event.getServiceComponentHost();

        // host level maintenance
        if( null != host ){
          String hostName = host.getHostName();
          if( hostName.equals( alertHostName ) ){
            if (updateMaintenanceStateAndRecalculateAggregateAlert(history, currentAlert, newMaintenanceState, alertUpdates))
              recalculateAggregateAlert = true;
            continue;
          }
        } else if( null != service ){
          // service level maintenance
          String serviceName = service.getName();
          if( serviceName.equals(alertServiceName)){
            if (updateMaintenanceStateAndRecalculateAggregateAlert(history, currentAlert, newMaintenanceState, alertUpdates))
              recalculateAggregateAlert = true;
            continue;
          }
        } else if( null != serviceComponentHost ){
          // component level maintenance
          String hostName = serviceComponentHost.getHostName();
          String serviceName = serviceComponentHost.getServiceName();
          String componentName = serviceComponentHost.getServiceComponentName();

          // match on all 3 for a service component
          if (hostName.equals(alertHostName) && serviceName.equals(alertServiceName)
              && componentName.equals(alertComponentName)) {
            if (updateMaintenanceStateAndRecalculateAggregateAlert(history, currentAlert, newMaintenanceState, alertUpdates))
              recalculateAggregateAlert = true;
            continue;
          }
        }
      } catch (Exception exception) {
        AlertDefinitionEntity definition = history.getAlertDefinition();
        LOG.error("Unable to put alert '{}' for host {} into maintenance mode",
            definition.getDefinitionName(), alertHostName, exception);
      }
    }

    if (!alertUpdates.isEmpty()) {
      STOMPUpdatePublisher.publish(new AlertUpdateEvent(alertUpdates));
    }

    if (recalculateAggregateAlert) {
      // publish the event to recalculate aggregates
      m_alertEventPublisher.publish(new AggregateAlertRecalculateEvent(clusterId));
    }
  }

  /**
   * Updates the maintenance state of the specified alert if different than the
   * supplied maintenance state. And recalcualte aggregates if the maintenance state
   * is changed and there is an aggregate alert for the specified alert if it is
   * in CRITICAL or WARNING state.
   *
   * @param historyAlert
   *          the alert to check if having an aggregate alert associated with it.
   * @param currentAlert
   *          the alert to update (not {@code null}).
   * @param maintenanceState
   *          the maintenance state to change to, either
   *          {@link MaintenanceState#OFF} or {@link MaintenanceState#ON}.
   */
  private boolean updateMaintenanceStateAndRecalculateAggregateAlert (AlertHistoryEntity historyAlert,
      AlertCurrentEntity currentAlert, MaintenanceState maintenanceState, Map<Long, Map<String,
      AlertSummaryGroupedRenderer.AlertDefinitionSummary>> alertUpdates) {

    AlertDefinitionEntity definitionEntity = currentAlert.getAlertHistory().getAlertDefinition();
    // alerts only care about OFF or ON
    if (maintenanceState != MaintenanceState.OFF && maintenanceState != MaintenanceState.ON) {
      LOG.warn("Unable to set invalid maintenance state of {} on the alert {}", maintenanceState,
          definitionEntity.getDefinitionName());

      return false;
    }

    MaintenanceState currentState = currentAlert.getMaintenanceState();
    if (currentState == maintenanceState) {
      return false;
    }

    currentAlert.setMaintenanceState(maintenanceState);
    m_alertsDao.merge(currentAlert);

    AlertState alertState = historyAlert.getAlertState();

    if (!alertUpdates.containsKey(historyAlert.getClusterId())) {
      alertUpdates.put(historyAlert.getClusterId(), new HashMap<>());
    }
    Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary> summaries = alertUpdates.get(historyAlert.getClusterId());

    // alert history doesn't change when MM is turned on/off, so timestamp and text should be empty
    AlertSummaryGroupedRenderer.updateSummary(summaries, definitionEntity.getDefinitionId(),
        definitionEntity.getDefinitionName(), alertState, -1L, maintenanceState, "");

    if (AlertState.RECALCULATE_AGGREGATE_ALERT_STATES.contains(alertState)){
      clusterId = historyAlert.getClusterId();
      String alertName = historyAlert.getAlertDefinition().getDefinitionName();

      if (m_aggregateMapping.getAggregateDefinition(clusterId, alertName) != null){
        return true;
      }
    }
    return false;
  }
}
