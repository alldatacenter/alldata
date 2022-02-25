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

import org.apache.ambari.server.controller.internal.AlertNoticeResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.state.NotificationState;


/**
 * The {@link AlertNoticeEntity_} is a strongly typed metamodel for creating
 * {@link CriteriaQuery} for {@link AlertNoticeEntity}.
 */
@StaticMetamodel(AlertNoticeEntity.class)
public class AlertNoticeEntity_ {
  public static volatile SingularAttribute<AlertNoticeEntity, Long> notificationId;
  public static volatile SingularAttribute<AlertNoticeEntity, NotificationState> notifyState;
  public static volatile SingularAttribute<AlertNoticeEntity, String> uuid;
  public static volatile SingularAttribute<AlertNoticeEntity, AlertNoticeEntity> alertHistory;
  public static volatile SingularAttribute<AlertNoticeEntity, AlertTargetEntity> alertTarget;

  /**
   * Gets a mapping of between a resource provider property, like
   * {@link AlertNoticeResourceProvider#ALERT_NOTICE_STATE} to a metamodel
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

    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_ID,
        Collections.singletonList(notificationId));

    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_STATE,
        Collections.singletonList(notifyState));

    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_UUID,
        Collections.singletonList(uuid));

    // AlertNotice.alertTarget.targetId = 123
    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_ID,
        Arrays.asList(alertTarget, AlertTargetEntity_.targetId));

    // AlertNotice.alertTarget.targetName = foo
    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME,
        Arrays.asList(alertTarget, AlertTargetEntity_.targetName));

    // AlertNotice.alertHistory.alertId = 123
    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_HISTORY_ID,
        Arrays.asList(alertHistory, AlertHistoryEntity_.alertId));

    // AlertNotice.alertHistory.serviceName = 123
    mapping.put(AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME,
        Arrays.asList(alertHistory, AlertHistoryEntity_.serviceName));

    return mapping;
  }
}
