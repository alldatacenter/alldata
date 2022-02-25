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

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.events.AggregateAlertRecalculateEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.AlertStateChangeEvent;
import org.apache.ambari.server.events.InitialAlertEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertFirmness;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link AlertAggregateListener} is used to listen for all incoming
 * {@link AlertStateChangeEvent} instances and determine if there exists a
 * {@link SourceType#AGGREGATE} alert that needs to run.
 * <p/>
 * This listener is only needed on state changes as aggregation of alerts is
 * only performed against the state of an alert and not the values that
 * contributed to that state. However, this listener should only be concerned
 * with {@link AlertFirmness#HARD} events as they represent a true change in the
 * state of an alert. Calculations should never be performed on
 * {@link AlertFirmness#SOFT} alerts since they may be false positives.
 */
@Singleton
@EagerSingleton
public class AlertAggregateListener {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertAggregateListener.class);

  @Inject
  private AlertsDAO m_alertsDao = null;

  /**
   * Used for looking cluster name by cluster id
   */
  @Inject
  Provider<Clusters> m_clusters;

  /**
   * The event publisher used to receive incoming events and publish new events
   * when an aggregate alert is run.
   */
  private final AlertEventPublisher m_publisher;

  /**
   * A cache used to store the last state and text of an aggregate alert. We
   * shouldn't need to fire new aggregate alerts unless the state or text has
   * changed.
   */
  private Map<String, Alert> m_alertCache = new ConcurrentHashMap<>();

  /**
   * Used for quick lookups of aggregate alerts.
   */
  @Inject
  private AggregateDefinitionMapping m_aggregateMapping;

  @Inject
  public AlertAggregateListener(AlertEventPublisher publisher) {
    m_publisher = publisher;
    m_publisher.register(this);
  }

  /**
   * Consumes an {@link InitialAlertEvent}.
   */
  @Subscribe
  public void onInitialAlertEvent(InitialAlertEvent event) {
    LOG.debug("Received event {}", event);

    onAlertEvent(event.getClusterId(), event.getAlert().getName());
  }

  /**
   * Consumes an {@link AlertStateChangeEvent}.
   */
  @Subscribe
  public void onAlertStateChangeEvent(AlertStateChangeEvent event) {
    LOG.debug("Received event {}", event);

    // do not recalculate on SOFT events
    AlertCurrentEntity currentEntity = event.getCurrentAlert();
    if (currentEntity.getFirmness() == AlertFirmness.SOFT) {
      return;
    }

    onAlertEvent(event.getClusterId(), event.getAlert().getName());
  }

  /**
   * Consumes an {@link AggregateAlertRecalculateEvent}. When a component is
   * removed, there may be alerts that were removed which have aggregate alerts
   * associated with this. This will ensure that all aggregates recalculate. It
   * can also be used at any point to recalculate all of the aggregates for a
   * cluster.
   *
   */
  @Subscribe
  public void onAlertStateChangeEvent(AggregateAlertRecalculateEvent event) {
    LOG.debug("Received event {}", event);

    List<String> alertNames = m_aggregateMapping.getAlertsWithAggregates(event.getClusterId());
    for (String alertName : alertNames) {
      onAlertEvent(event.getClusterId(), alertName);
    }
  }

  /**
   * Calculates the aggregate alert state if there is an aggregate alert for the
   * specified alert.
   * <p/>
   * This method should not be decoratd with {@link AllowConcurrentEvents} since
   * it would need extra locking around {@link #m_alertCache}.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param alertName
   *          the name of the alert to use when looking up the aggregate.
   */
  private void onAlertEvent(long clusterId, String alertName) {
    AlertDefinition aggregateDefinition = m_aggregateMapping.getAggregateDefinition(clusterId,
        alertName);

    if (null == aggregateDefinition || null == m_alertsDao) {
      return;
    }

    AggregateSource aggregateSource = (AggregateSource) aggregateDefinition.getSource();

    AlertSummaryDTO summary = m_alertsDao.findAggregateCounts(clusterId,
        aggregateSource.getAlertName());

    // OK should be based off of true OKs and those in maintenance mode
    int okCount = summary.getOkCount() + summary.getMaintenanceCount();

    int warningCount = summary.getWarningCount();
    int criticalCount = summary.getCriticalCount();
    int unknownCount = summary.getUnknownCount();
    int totalCount = okCount + warningCount + criticalCount + unknownCount;

    Alert aggregateAlert = new Alert(aggregateDefinition.getName(), null,
        aggregateDefinition.getServiceName(), null, null, AlertState.UNKNOWN);

    aggregateAlert.setLabel(aggregateDefinition.getLabel());
    aggregateAlert.setTimestamp(System.currentTimeMillis());
    aggregateAlert.setClusterId(clusterId);

    if (0 == totalCount) {
      aggregateAlert.setText("There are no instances of the aggregated alert.");
    } else if (summary.getUnknownCount() > 0) {
      aggregateAlert.setText("There are alerts with a state of UNKNOWN.");
    } else {
      Reporting reporting = aggregateSource.getReporting();

      int numerator = summary.getCriticalCount() + summary.getWarningCount();
      int denominator = totalCount;

      double value = ((double) (numerator) / denominator);
      if(Reporting.ReportingType.PERCENT.equals(reporting.getType())) {
        value *= 100;
      }

      if (value >= reporting.getCritical().getValue()) {
        aggregateAlert.setState(AlertState.CRITICAL);
        aggregateAlert.setText(MessageFormat.format(
            reporting.getCritical().getText(),
            denominator, numerator));

      } else if (value >= reporting.getWarning().getValue()) {
        aggregateAlert.setState(AlertState.WARNING);
        aggregateAlert.setText(MessageFormat.format(
            reporting.getWarning().getText(),
            denominator, numerator));

      } else {
        aggregateAlert.setState(AlertState.OK);
        aggregateAlert.setText(MessageFormat.format(
            reporting.getOk().getText(),
            denominator, numerator));
      }
    }

    // now that the alert has been created, see if we need to send it; only send
    // alerts if the state or the text has changed
    boolean sendAlertEvent = true;
    Alert cachedAlert = m_alertCache.get(aggregateAlert.getName());
    if (null != cachedAlert) {
      AlertState cachedState = cachedAlert.getState();
      AlertState alertState = aggregateAlert.getState();
      String cachedText = cachedAlert.getText();
      String alertText = aggregateAlert.getText();

      if (cachedState == alertState && StringUtils.equals(cachedText, alertText)) {
        sendAlertEvent = false;
      }
    }

    // update the cache
    m_alertCache.put(aggregateAlert.getName(), aggregateAlert);

    // make a new event and allow others to consume it, but only if the
    // aggregate has changed
    if (sendAlertEvent) {
      AlertReceivedEvent aggEvent = new AlertReceivedEvent(clusterId, aggregateAlert);
      m_publisher.publish(aggEvent);
    }
  }
}
