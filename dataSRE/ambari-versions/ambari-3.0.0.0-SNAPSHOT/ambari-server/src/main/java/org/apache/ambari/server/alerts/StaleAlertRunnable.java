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
package org.apache.ambari.server.alerts;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ambari.server.agent.stomp.AlertDefinitionsHolder;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.services.AmbariServerAlertService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * The {@link StaleAlertRunnable} is used by the
 * {@link AmbariServerAlertService} to check the last time that an alert was
 * checked and determine if it seems to no longer be running. It will produce a
 * single alert with {@link AlertState#CRITICAL} along with a textual
 * description of the alerts that are stale.
 */
public class StaleAlertRunnable extends AlertRunnable {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StaleAlertRunnable.class);

  /**
   * The message for the alert when all services have run in their designated
   * intervals.
   */
  private static final String ALL_ALERTS_CURRENT_MSG = "All alerts have run within their time intervals.";

  /**
   * The message to use when alerts are detected as stale.
   */
  private static final String STALE_ALERTS_MSG = "There are {0} stale alerts from {1} host(s):\n{2}";

  private static final String TIMED_LABEL_MSG = "{0} ({1})";

  private static final String HOST_LABEL_MSG = "{0}\n  [{1}]";

  /**
   * Convert the minutes for the delay of an alert into milliseconds.
   */
  private static final long MINUTE_TO_MS_CONVERSION = 60L * 1000L;

  private static final long MILLISECONDS_PER_MINUTE = 1000L * 60L;
  private static final int MINUTES_PER_DAY = 24 * 60;
  private static final int MINUTES_PER_HOUR = 60;


  /**
   * Used to get the current alerts and the last time they ran.
   */
  @Inject
  private AlertsDAO m_alertsDao;

  /**
   * Used for converting {@link AlertDefinitionEntity} into
   * {@link AlertDefinition} instances.
   */
  @Inject
  private AlertDefinitionFactory m_definitionFactory;

  @Inject
  private AlertDefinitionsHolder alertDefinitionsHolder;

  /**
   * Constructor.
   *
   * @param definitionName
   */
  public StaleAlertRunnable(String definitionName) {
    super(definitionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  List<Alert> execute(Cluster cluster, AlertDefinitionEntity myDefinition) {
    // get the multiplier
    AlertDefinition alertDefinition = m_definitionFactory.coerce(myDefinition);
    int waitFactor = alertHelper.getWaitFactorMultiplier(alertDefinition);

    // use the uptime of the Ambari Server as a way to determine if we need to
    // give the alert more time to report in
    RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
    long uptime = rb.getUptime();

    int totalStaleAlerts = 0;
    Set<String> staleAlertGroupings = new TreeSet<>();
    Map<String, Set<String>> staleAlertsByHost = new HashMap<>();
    Set<String> hostsWithStaleAlerts = new TreeSet<>();

    // get the cluster's current alerts
    List<AlertCurrentEntity> currentAlerts = m_alertsDao.findCurrentByCluster(
        cluster.getClusterId());

    long now = System.currentTimeMillis();

    Map<Long, List<Long>> alertDefinitionsToHosts = prepareHostDefinitions(cluster.getClusterId());

    // for each current alert, check to see if the last time it ran is
    // more than INTERVAL_WAIT_FACTOR * its interval value (indicating it hasn't
    // run)
    for (AlertCurrentEntity current : currentAlerts) {
      AlertHistoryEntity history = current.getAlertHistory();
      AlertDefinitionEntity currentDefinition = history.getAlertDefinition();

      // skip aggregates as they are special
      if (currentDefinition.getSourceType() == SourceType.AGGREGATE) {
          continue;
        }

      // skip alerts in maintenance mode
      if (current.getMaintenanceState() != MaintenanceState.OFF) {
        continue;
      }

      // skip alerts that have not run yet
      if (current.getLatestTimestamp() == 0) {
        continue;
      }

      // skip this alert (who watches the watchers)
      if (currentDefinition.getDefinitionName().equals(m_definitionName)) {
        continue;
      }

      // convert minutes to milliseconds for the definition's interval
      long intervalInMillis = currentDefinition.getScheduleInterval() * MINUTE_TO_MS_CONVERSION;

      // if the server hasn't been up long enough to consider this alert stale,
      // then don't mark it stale - this is to protect against cases where
      // Ambari was down for a while and after startup it hasn't received the
      // alert because it has a longer interval than this stale alert check:
      //
      // Stale alert check - every 5 minutes
      // Foo alert cehck - every 10 minutes
      // Ambari down for 35 minutes for upgrade
      if (uptime <= waitFactor * intervalInMillis) {
        continue;
      }

      Boolean timedout;
      Long lastCheckTimestamp = 0L;

      // alert history for component or ambari agent should contains the hostname
      // host/hosts for alerts for master component/with host ignoring can be retrieved from current agent's alert definitions

      String currentHostName = history.getHostName();
      List<Host> hosts = new ArrayList<>();
      if (currentHostName != null) {
        hosts.add(cluster.getHost(currentHostName));
      } else if (alertDefinitionsToHosts.containsKey(current.getDefinitionId())) {
        hosts = alertDefinitionsToHosts.get(current.getDefinitionId()).stream()
            .map(i -> cluster.getHost(i)).collect(Collectors.toList());
      }
      if (!hosts.isEmpty()) {

        // in case alert ignores host we should to check alert is stale on all hosts
        timedout = true;
        for (Host host : hosts) {
          if (timedout) {
            // check agent reported about stale alert
            if (alertHelper.getStaleAlerts(host.getHostId()).containsKey(current.getDefinitionId())) {
              lastCheckTimestamp = Math.max(lastCheckTimestamp,
                  alertHelper.getStaleAlerts(host.getHostId()).get(current.getDefinitionId()));
            // check host is in HEARTBEAT_LOST state
            } else if (host.getState().equals(HostState.HEARTBEAT_LOST)) {
              lastCheckTimestamp = Math.max(lastCheckTimestamp, host.getLastHeartbeatTime());
            } else {
              timedout = false;
            }
          }
        }
      } else {
        // Server alerts will be checked by the old way
        long timeDifference = now - current.getLatestTimestamp();
        timedout = timeDifference >= waitFactor * intervalInMillis;
        lastCheckTimestamp = current.getOriginalTimestamp();
      }
      if (timedout) {
        // increase the count
        totalStaleAlerts++;

        // it is technically possible to have a null/blank label; if so,
        // default to the name of the definition
        String label = currentDefinition.getLabel();
        if (StringUtils.isEmpty(label)) {
          label = currentDefinition.getDefinitionName();
        }

        if (null != history.getHostName()) {
          // keep track of the host, if not null
          String hostName = history.getHostName();
          hostsWithStaleAlerts.add(hostName);
          if (!staleAlertsByHost.containsKey(hostName)) {
            staleAlertsByHost.put(hostName, new TreeSet<>());
          }

          long timeDifference = now - lastCheckTimestamp;
          staleAlertsByHost.get(hostName).add(MessageFormat.format(TIMED_LABEL_MSG, label,
              millisToHumanReadableStr(timeDifference)));
        } else {
          // non host alerts
          staleAlertGroupings.add(label);
        }
      }
    }

    for (String host : staleAlertsByHost.keySet()) {
      staleAlertGroupings.add(MessageFormat.format(HOST_LABEL_MSG, host,
          StringUtils.join(staleAlertsByHost.get(host), ",\n  ")));
    }

    AlertState alertState = AlertState.OK;
    String alertText = ALL_ALERTS_CURRENT_MSG;

    // if there are stale alerts, mark as CRITICAL with the list of
    // alerts
    if (!staleAlertGroupings.isEmpty()) {
      alertState = AlertState.CRITICAL;
      alertText = MessageFormat.format(STALE_ALERTS_MSG, totalStaleAlerts,
          hostsWithStaleAlerts.size(), StringUtils.join(staleAlertGroupings, ",\n"));
    }

    Alert alert = new Alert(myDefinition.getDefinitionName(), null, myDefinition.getServiceName(),
        myDefinition.getComponentName(), null, alertState);

    alert.setLabel(myDefinition.getLabel());
    alert.setText(alertText);
    alert.setTimestamp(now);
    alert.setClusterId(cluster.getClusterId());

    return Collections.singletonList(alert);
  }

  /**
   * Retrieves alert definitions sent to agents.
   * @param clusterId cluster id
   * @return map definition id - host ids list
   */
  public Map<Long, List<Long>> prepareHostDefinitions(Long clusterId) {
    Map<Long, List<Long>> alertDefinitionsToHosts = new HashMap<>();

    alertDefinitionsHolder.getData().entrySet().stream()
        .filter(e -> e.getValue().getClusters() != null)
        .filter(e -> e.getValue().getClusters().get(clusterId) != null)
        .forEach(e -> e.getValue().getClusters().get(clusterId).getAlertDefinitions().stream()
            .forEach(l -> {
              alertDefinitionsToHosts.putIfAbsent(l.getDefinitionId(), new ArrayList<>());
              alertDefinitionsToHosts.get(l.getDefinitionId()).add(e.getKey());
            }));
    return alertDefinitionsToHosts;
  }

  /**
   * Converts given {@code milliseconds} to human-readable {@link String} like "1d 2h 3m" or "2h 4m".
   * @param milliseconds milliseconds to convert
   * @return human-readable string
   */
  private static String millisToHumanReadableStr(long milliseconds){
    int min, hour, days;
    min = (int)(milliseconds / MILLISECONDS_PER_MINUTE);
    days = min / MINUTES_PER_DAY;
    min = min % MINUTES_PER_DAY;
    hour = min / MINUTES_PER_HOUR;
    min = min % MINUTES_PER_HOUR;
    String result = "";
    if(days > 0) {
      result += days + "d ";
    }
    if(hour > 0) {
      result += hour + "h ";
    }
    if(min > 0) {
      result += min + "m ";
    }
    return result.trim();
  }
}

