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
package org.apache.ambari.server.state.alert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;

/**
 * The {@link AggregateDefinitionMapping} is used to keep an in-memory mapping
 * of all of the {@link AlertDefinition}s that have aggregate definitions
 * associated with them.
 */
@Singleton
public class AggregateDefinitionMapping {
  /**
   * In-memory mapping of cluster ID to definition name / aggregate definition.
   * This is used for fast lookups when receiving events.
   */
  private Map<Long, Map<String, AlertDefinition>> m_aggregateMap =
    new ConcurrentHashMap<>();

  /**
   * Constructor.
   *
   */
  public AggregateDefinitionMapping() {
  }

  /**
   * Gets an aggregate definition based on a given alert definition name.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is bound to.
   * @param name
   *          the unique name of the definition.
   * @return the aggregate definition, or {@code null} if none.
   */
  public AlertDefinition getAggregateDefinition(long clusterId, String name) {
    Long id = Long.valueOf(clusterId);
    if (!m_aggregateMap.containsKey(id)) {
      return null;
    }

    if (!m_aggregateMap.get(id).containsKey(name)) {
      return null;
    }

    return m_aggregateMap.get(id).get(name);
  }

  /**
   * Adds a mapping for a new aggregate definition.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is bound to.
   * @param definition
   *          the aggregate definition to register (not {@code null}).
   */
  public void registerAggregate(long clusterId, AlertDefinition definition) {
    Long id = Long.valueOf(clusterId);

    if (!m_aggregateMap.containsKey(id)) {
      m_aggregateMap.put(id, new HashMap<>());
    }

    Map<String, AlertDefinition> map = m_aggregateMap.get(id);

    AggregateSource as = (AggregateSource) definition.getSource();

    map.put(as.getAlertName(), definition);
  }

  /**
   * Removes the associated aggregate for the specified aggregated definition.
   *
   * @param clusterId
   *          the ID of the cluster that the definition is bound to.
   * @param aggregatedDefinitonName
   *          the unique name of the definition for which aggregates should be
   *          unassociated (not {@code null}).
   */
  public void removeAssociatedAggregate(long clusterId,
      String aggregatedDefinitonName) {
    Long id = Long.valueOf(clusterId);

    if (!m_aggregateMap.containsKey(id)) {
      return;
    }

    Map<String, AlertDefinition> map = m_aggregateMap.get(id);
    map.remove(aggregatedDefinitonName);
  }

  /**
   * Gets a copy of all of the aggregate definitions for the specified cluster.
   *
   * @param clusterId
   *          the cluster ID
   * @return the list of all aggregate definitions
   */
  public List<AlertDefinition> getAggregateDefinitions(long clusterId) {
    if (!m_aggregateMap.containsKey(clusterId)) {
      return Collections.emptyList();
    }

    Map<String, AlertDefinition> map = m_aggregateMap.get(clusterId);
    return new ArrayList<>(map.values());
  }

  /**
   * Gets a copy of all of the alerts that have aggregates defined for them.
   *
   * @param clusterId
   *          the cluster ID
   * @return the list of all alerts with aggregate definitions
   */
  public List<String> getAlertsWithAggregates(long clusterId) {
    if (!m_aggregateMap.containsKey(clusterId)) {
      return Collections.emptyList();
    }

    Map<String, AlertDefinition> map = m_aggregateMap.get(clusterId);
    return new ArrayList<>(map.keySet());
  }
}
