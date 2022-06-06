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

package org.apache.ambari.server.orm.entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import org.apache.ambari.server.controller.internal.AlertHistoryResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.state.AlertState;


/**
 * The {@link AlertHistoryEntity_} is a strongly typed metamodel for creating
 * {@link CriteriaQuery} for {@link AlertHistoryEntity}.
 */
@StaticMetamodel(AlertHistoryEntity.class)
public class AlertHistoryEntity_ {
  public static volatile SingularAttribute<AlertHistoryEntity, Long> alertId;
  public static volatile SingularAttribute<AlertHistoryEntity, String> alertInstance;
  public static volatile SingularAttribute<AlertHistoryEntity, String> alertLabel;
  public static volatile SingularAttribute<AlertHistoryEntity, AlertState> alertState;
  public static volatile SingularAttribute<AlertHistoryEntity, String> alertText;
  public static volatile SingularAttribute<AlertHistoryEntity, Long> alertTimestamp;
  public static volatile SingularAttribute<AlertHistoryEntity, Long> clusterId;
  public static volatile SingularAttribute<AlertHistoryEntity, String> componentName;
  public static volatile SingularAttribute<AlertHistoryEntity, String> hostName;
  public static volatile SingularAttribute<AlertHistoryEntity, String> serviceName;
  public static volatile SingularAttribute<AlertHistoryEntity, AlertDefinitionEntity> alertDefinition;

  /**
   * Gets a mapping of between a resource provider property, like
   * {@link AlertHistoryResourceProvider#ALERT_HISTORY_SERVICE_NAME} to a
   * metamodel {@link SingularAttribute}.
   * <p/>
   * This is used when converting an Ambari {@link Predicate} into a JPA
   * {@link javax.persistence.criteria.Predicate} and we need a type-safe
   * conversion between "category/property" and JPA field names.
   * <p/>
   * Multiple {@link SingularAttribute} instances can be chained together in
   * order to provide an {@code entity.subEntity.field} reference.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Map<String, List<? extends SingularAttribute<?, ?>>> getPredicateMapping() {
    Map<String, List<? extends SingularAttribute<?, ?>>> mapping = new HashMap<>();

    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_ID,
        Collections.singletonList(alertId));

    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_SERVICE_NAME,
        Collections.singletonList(serviceName));

    mapping.put(
        AlertHistoryResourceProvider.ALERT_HISTORY_COMPONENT_NAME,
        Collections.singletonList(componentName));

    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_STATE,
        Collections.singletonList(alertState));

    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_TIMESTAMP,
        Collections.singletonList(alertTimestamp));

    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_HOSTNAME,
        Collections.singletonList(hostName));

    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_LABEL,
        Collections.singletonList(alertLabel));

    // AlertHistory.alertDefinition.definitionName = foo
    mapping.put(AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME,
        Arrays.asList(alertDefinition, AlertDefinitionEntity_.definitionName));

    return mapping;
  }
}
