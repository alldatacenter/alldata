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

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.AlertHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The {@link AlertRunnable} class is used to boilerplate the expected
 * functionality of {@link Runnable}s which need to eventually create and fire
 * {@link AlertReceivedEvent}s.
 * <p/>
 * Implementations of this class do not need to concern themselves with checking
 * for whether their alert definition is enabled or with constructing and firing
 * the alert events.
 */
public abstract class AlertRunnable implements Runnable {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AlertRunnable.class);

  /**
   * The alert definition name.
   */
  protected final String m_definitionName;

  /**
   * Used to get alert definitions to use when generating alert instances.
   */
  @Inject
  private Provider<Clusters> m_clustersProvider;

  /**
   * Used for looking up alert definitions.
   */
  @Inject
  private AlertDefinitionDAO m_dao;

  /**
   * Publishes {@link AlertEvent} instances.
   */
  @Inject
  private AlertEventPublisher m_alertEventPublisher;

  @Inject
  protected AlertHelper alertHelper;

  /**
   * Constructor.
   *
   * @param definitionName
   *          the definition name (not {@code null}).
   */
  public AlertRunnable(String definitionName) {
    m_definitionName = definitionName;
  }

  /**
   * Invoked on subclasses when it is time for them to check their specific
   * alert criteria. Implementations must return a list of {@link Alert}
   * instances which will be turned into {@link AlertEvent}s and published.
   * <p/>
   * This method will only be invoked if the definition is enabled for the
   * specified cluster.
   *
   * @param cluster
   *          the cluster for which the alert is executing. If there are
   *          multiple clusters defined, then this method is called once for
   *          each.
   * @param definition
   *          the information about this concrete alert.
   * @return a list of {@link Alert} to be used to create {@link AlertEvent}s
   *         and published with the {@link AlertEventPublisher} (not
   *         {@code null}).
   * @throws AmbariException
   */
  abstract List<Alert> execute(Cluster cluster, AlertDefinitionEntity definition)
      throws AmbariException;

  /**
   * {@inheritDoc}
   */
  @Override
  public final void run() {
    try {
      Map<String, Cluster> clusterMap = m_clustersProvider.get().getClusters();
      for (Cluster cluster : clusterMap.values()) {
        AlertDefinitionEntity definition = m_dao.findByName(cluster.getClusterId(), m_definitionName);

        // skip this cluster if the runnable's alert definition is missing or
        // disabled
        if (null == definition || !definition.getEnabled()) {
          continue;
        }

        // for every alert generated from the implementation, fire an event
        List<Alert> alerts = execute(cluster, definition);
        for (Alert alert : alerts) {
          AlertReceivedEvent event = new AlertReceivedEvent(cluster.getClusterId(), alert);
          m_alertEventPublisher.publish(event);
        }
      }
    } catch (Exception exception) {
      LOG.error("Unable to run the {} alert", m_definitionName, exception);
    }
  }

  /**
   * Builds an {@link Alert} instance.
   *
   * @param cluster
   *          the cluster the alert is for (not {@code null}).
   * @param myDefinition
   *          the alert's definition (not {@code null}).
   * @param alertState
   *          the state of the alert (not {@code null}).
   * @param message
   *          the alert text.
   * @return and alert.
   */
  protected Alert buildAlert(Cluster cluster, AlertDefinitionEntity myDefinition,
      AlertState alertState, String message) {
    Alert alert = new Alert(myDefinition.getDefinitionName(), null, myDefinition.getServiceName(),
        myDefinition.getComponentName(), null, alertState);

    alert.setLabel(myDefinition.getLabel());
    alert.setText(message);
    alert.setTimestamp(System.currentTimeMillis());
    alert.setClusterId(cluster.getClusterId());

    return alert;
  }
}
