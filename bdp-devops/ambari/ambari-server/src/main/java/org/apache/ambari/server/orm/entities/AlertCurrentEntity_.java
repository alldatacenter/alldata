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

import org.apache.ambari.server.controller.internal.AlertResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.state.MaintenanceState;


/**
 * The {@link AlertCurrentEntity_} is a strongly typed metamodel for creating
 * {@link CriteriaQuery} for {@link AlertCurrentEntity}.
 */
@StaticMetamodel(AlertCurrentEntity.class)
public class AlertCurrentEntity_ {
  public static volatile SingularAttribute<AlertCurrentEntity, Long> alertId;
  public static volatile SingularAttribute<AlertCurrentEntity, Long> latestTimestamp;
  public static volatile SingularAttribute<AlertCurrentEntity, Long> originalTimestamp;
  public static volatile SingularAttribute<AlertCurrentEntity, MaintenanceState> maintenanceState;
  public static volatile SingularAttribute<AlertCurrentEntity, String> latestText;
  public static volatile SingularAttribute<AlertCurrentEntity, AlertDefinitionEntity> alertDefinition;
  public static volatile SingularAttribute<AlertCurrentEntity, AlertHistoryEntity> alertHistory;

  /**
   * Gets a mapping of between a resource provider property, like
   * {@link AlertResourceProvider#ALERT_ID} to a metamodel
   * {@link SingularAttribute}.
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

    mapping.put(AlertResourceProvider.ALERT_ID,
        Collections.singletonList(alertId));

    mapping.put(AlertResourceProvider.ALERT_LATEST_TIMESTAMP,
        Collections.singletonList(latestTimestamp));

    mapping.put(AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP,
        Collections.singletonList(originalTimestamp));

    mapping.put(AlertResourceProvider.ALERT_MAINTENANCE_STATE,
        Collections.singletonList(maintenanceState));

    mapping.put(AlertResourceProvider.ALERT_TEXT,
        Collections.singletonList(latestText));

    // AlertCurrentEntity.alertDefinition.definitionId = 1234
    mapping.put(AlertResourceProvider.ALERT_DEFINITION_ID,
        Arrays.asList(alertDefinition, AlertDefinitionEntity_.definitionId));

    // AlertCurrentEntity.alertDefinition.definitionName = foo
    mapping.put(AlertResourceProvider.ALERT_DEFINITION_NAME,
        Arrays.asList(alertDefinition, AlertDefinitionEntity_.definitionName));

    // AlertCurrentEntity.alertDefinition.serviceName = HDFS
    mapping.put(AlertResourceProvider.ALERT_SERVICE,
        Arrays.asList(alertDefinition, AlertDefinitionEntity_.serviceName));

    // AlertCurrentEntity.alertDefinition.componentName = DATANODE
    mapping.put(AlertResourceProvider.ALERT_COMPONENT,
        Arrays.asList(alertDefinition, AlertDefinitionEntity_.componentName));

    // AlertCurrentEntity.alertHistory.hostName = c6401.ambari.apache.org
    mapping.put(AlertResourceProvider.ALERT_HOST,
        Arrays.asList(alertHistory, AlertHistoryEntity_.hostName));

    // AlertCurrentEntity.alertHistory.state = OK
    mapping.put(AlertResourceProvider.ALERT_STATE,
        Arrays.asList(alertHistory, AlertHistoryEntity_.alertState));

    return mapping;
  }
}
