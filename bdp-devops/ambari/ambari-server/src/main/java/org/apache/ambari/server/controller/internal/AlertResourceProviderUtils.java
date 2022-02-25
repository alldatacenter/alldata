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

package org.apache.ambari.server.controller.internal;

import java.util.EnumSet;
import java.util.Set;

import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.lang.StringUtils;

/**
 * AlertResourceProviderUtils provides utility methods used to help perform tasks within alert-specific
 * resource providers.
 */
public class AlertResourceProviderUtils {

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to view
   * cluster-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_VIEW_CLUSTER_ALERTS = EnumSet.of(
      RoleAuthorization.CLUSTER_VIEW_ALERTS,
      RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
      RoleAuthorization.CLUSTER_MANAGE_ALERTS);

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to view
   * service-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_VIEW_SERVICE_ALERTS = EnumSet.of(
      RoleAuthorization.CLUSTER_VIEW_ALERTS,
      RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
      RoleAuthorization.CLUSTER_MANAGE_ALERTS,
      RoleAuthorization.SERVICE_VIEW_ALERTS,
      RoleAuthorization.SERVICE_TOGGLE_ALERTS,
      RoleAuthorization.SERVICE_MANAGE_ALERTS);

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to execute
   * cluster-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_EXECUTE_CLUSTER_ALERTS = AUTHORIZATIONS_VIEW_CLUSTER_ALERTS;

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to execute
   * service-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_EXECUTE_SERVICE_ALERTS = AUTHORIZATIONS_VIEW_SERVICE_ALERTS;

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to toggle
   * cluster-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_TOGGLE_CLUSTER_ALERTS = EnumSet.of(
      RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
      RoleAuthorization.CLUSTER_MANAGE_ALERTS);

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to toggle
   * service-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_TOGGLE_SERVICE_ALERTS = EnumSet.of(
      RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
      RoleAuthorization.CLUSTER_MANAGE_ALERTS,
      RoleAuthorization.SERVICE_TOGGLE_ALERTS,
      RoleAuthorization.SERVICE_MANAGE_ALERTS);

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to create, update,
   * and delete cluster-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_MANAGE_CLUSTER_ALERTS = EnumSet.of(
      RoleAuthorization.CLUSTER_MANAGE_ALERTS);

  /**
   * A set of RoleAuthorizations where one is required in order to be authorized to create, update,
   * and delete service-level alerts
   */
  private static final Set<RoleAuthorization> AUTHORIZATIONS_MANAGE_SERVICE_ALERTS = EnumSet.of(
      RoleAuthorization.CLUSTER_MANAGE_ALERTS,
      RoleAuthorization.SERVICE_MANAGE_ALERTS);


  /* ------------------------------------------------------------------------------------------
   * Checks for VIEWING Alerts
   * ------------------------------------------------------------------------------------------   */

  /**
   * Tests if the authenticated user is authorized to view the requested alert data
   *
   * @param entity            an AlertGroupEntity
   * @param clusterResourceId the resource id of the relevant cluster
   * @return true if the authenticated user is authorized; otherwise false
   */
  public static boolean hasViewAuthorization(AlertGroupEntity entity, Long clusterResourceId) {
    return (null != entity) &&
        hasViewAuthorization(entity.getServiceName(), clusterResourceId);
  }

  /**
   * Tests if the authenticated user is authorized to view service- or cluster-level alert data
   *
   * @param serviceName       the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId the resource id of the relevant cluster
   * @return true if the authenticated user is authorized; otherwise false
   */
  public static boolean hasViewAuthorization(String serviceName, Long clusterResourceId) {
    return hasAuthorization(serviceName, clusterResourceId,
        AUTHORIZATIONS_VIEW_CLUSTER_ALERTS, AUTHORIZATIONS_VIEW_SERVICE_ALERTS);
  }

  /**
   * Tests if the authenticated user is authorized to view service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity an AlertCurrentEntity
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyViewAuthorization(AlertCurrentEntity entity)
      throws AuthorizationException {
    if (entity != null) {
      verifyViewAuthorization(entity.getAlertHistory());
    }
  }

  /**
   * Tests if the authenticated user is authorized to view service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity an AlertHistoryEntity
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyViewAuthorization(AlertHistoryEntity entity)
      throws AuthorizationException {
    if (entity != null) {
      verifyViewAuthorization(entity.getAlertDefinition());
    }
  }

  /**
   * Tests if the authenticated user is authorized to view service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity            an AlertGroupEntity
   * @param clusterResourceId the resource id of the relevant cluster
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyViewAuthorization(AlertGroupEntity entity, Long clusterResourceId)
      throws AuthorizationException {
    if (entity != null) {
      verifyViewAuthorization(entity.getServiceName(), clusterResourceId);
    }
  }

  /**
   * Tests if the authenticated user is authorized to view service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity an AlertDefinitionEntity
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyViewAuthorization(AlertDefinitionEntity entity) throws AuthorizationException {
    verifyAuthorization(entity, AUTHORIZATIONS_VIEW_CLUSTER_ALERTS, AUTHORIZATIONS_VIEW_SERVICE_ALERTS, "view");
  }

  /**
   * Tests if the authenticated user is authorized to view service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param serviceName       the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId the resource id of the relevant cluster
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyViewAuthorization(String serviceName, Long clusterResourceId) throws AuthorizationException {
    verifyAuthorization(serviceName, clusterResourceId, AUTHORIZATIONS_VIEW_CLUSTER_ALERTS, AUTHORIZATIONS_VIEW_SERVICE_ALERTS, "view");
  }


  /* ------------------------------------------------------------------------------------------
   * Checks for EXECUTING Alerts
   * ------------------------------------------------------------------------------------------   */

  /**
   * Tests if the authenticated user is authorized to execute service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity an AlertDefinitionEntity
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyExecuteAuthorization(AlertDefinitionEntity entity) throws AuthorizationException {
    verifyAuthorization(entity, AUTHORIZATIONS_EXECUTE_CLUSTER_ALERTS, AUTHORIZATIONS_EXECUTE_SERVICE_ALERTS, "execute");
  }

  /**
   * Tests if the authenticated user is authorized to execute service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param serviceName       the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId the resource id of the relevant cluster
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyExecuteAuthorization(String serviceName, Long clusterResourceId) throws AuthorizationException {
    verifyAuthorization(serviceName, clusterResourceId, AUTHORIZATIONS_EXECUTE_CLUSTER_ALERTS, AUTHORIZATIONS_EXECUTE_SERVICE_ALERTS, "execute");
  }


  /* ------------------------------------------------------------------------------------------
   * Checks for TOGGLING Alerts
   * ------------------------------------------------------------------------------------------   */

  /**
   * Tests if the authenticated user is authorized to toggle service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity an AlertDefinitionEntity
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyToggleAuthorization(AlertDefinitionEntity entity) throws AuthorizationException {
    verifyAuthorization(entity, AUTHORIZATIONS_TOGGLE_CLUSTER_ALERTS, AUTHORIZATIONS_TOGGLE_SERVICE_ALERTS, "execute");
  }

  /**
   * Tests if the authenticated user is authorized to toggle service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param serviceName       the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId the resource id of the relevant cluster
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyToggleAuthorization(String serviceName, Long clusterResourceId) throws AuthorizationException {
    verifyAuthorization(serviceName, clusterResourceId, AUTHORIZATIONS_TOGGLE_CLUSTER_ALERTS, AUTHORIZATIONS_TOGGLE_SERVICE_ALERTS, "execute");
  }


  /* ------------------------------------------------------------------------------------------
   * Checks for MANAGING Alerts
   * ------------------------------------------------------------------------------------------   */

  /**
   * Tests if the authenticated user is authorized to manage service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity            an AlertGroupEntity
   * @param clusterResourceId the resource id of the relevant cluster
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyManageAuthorization(AlertGroupEntity entity, Long clusterResourceId) throws AuthorizationException {
    if (entity != null) {
      verifyManageAuthorization(entity.getServiceName(), clusterResourceId);
    }
  }

  /**
   * Tests if the authenticated user is authorized to manage service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param entity an AlertDefinitionEntity
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyManageAuthorization(AlertDefinitionEntity entity) throws AuthorizationException {
    verifyAuthorization(entity, AUTHORIZATIONS_MANAGE_CLUSTER_ALERTS, AUTHORIZATIONS_MANAGE_SERVICE_ALERTS, "manage");
  }

  /**
   * Tests if the authenticated user is authorized to manage service- or cluster-level alert data.
   * An authorization failure results in a thrown {@link AuthorizationException}.
   *
   * @param serviceName       the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId the resource id of the relevant cluster
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyManageAuthorization(String serviceName, Long clusterResourceId) throws AuthorizationException {
    verifyAuthorization(serviceName, clusterResourceId, AUTHORIZATIONS_MANAGE_CLUSTER_ALERTS, AUTHORIZATIONS_MANAGE_SERVICE_ALERTS, "manage");
  }


  /* ------------------------------------------------------------------------------------------
   * Generic checks
   * ------------------------------------------------------------------------------------------   */

  /**
   * Tests if the authenticated user is authorized to for either service- or cluster-level access to
   * alert data.
   * <p/>
   * If the service name is null or empty, the alert is considered to be a cluster-level alert,
   * else it is considered to be a service-level alert.
   * <p/>
   * If the clusterResourceId is null, no cluster is assume and the alert is considered to be an
   * Ambari-level alert.
   *
   * @param serviceName                the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId          the resource id of the relevant cluster
   * @param clusterLevelAuthorizations the set of cluster-level authorizations to check for
   * @param serviceLevelAuthorizations the set of service-level authorizations to check for
   * @return true if the authenticated user is authorized; otherwise false
   */
  public static boolean hasAuthorization(String serviceName, Long clusterResourceId,
                                         Set<RoleAuthorization> clusterLevelAuthorizations,
                                         Set<RoleAuthorization> serviceLevelAuthorizations) {
    if (null == clusterResourceId) {
      // Do not let clusterResourceId be null because that indicates we don't care about which cluster
      // we are checking authorization for, but we do.  Setting this to -1 ensures that no cluster
      // will match will will give only Ambari administrators access to this.
      clusterResourceId = -1L;
    }

    return AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusterResourceId,
        (StringUtils.isEmpty(serviceName) || "AMBARI".equals(serviceName))
            ? clusterLevelAuthorizations
            : serviceLevelAuthorizations);
  }

  /**
   * Tests if the authenticated user is authorized to for either service- or cluster-level access to
   * alert data. An authorization failure results in a thrown {@link AuthorizationException}.
   * <p/>
   * If the service name from the AlertDefinitionEntity is null or empty, the alert is considered to
   * be a cluster-level alert, else it is considered to be a service-level alert.
   *
   * @param entity                     an AlertDefinitionEntity
   * @param clusterLevelAuthorizations the set of cluster-level authorizations to check for
   * @param serviceLevelAuthorizations the set of service-level authorizations to check for
   * @param operation                  the name of the operation being tested for (used in error and logging messages)
   * @throws AuthorizationException if the authenticated user is not authorized
   * @see #verifyAuthorization(String, Long, Set, Set, String)
   */
  public static void verifyAuthorization(AlertDefinitionEntity entity,
                                         Set<RoleAuthorization> clusterLevelAuthorizations,
                                         Set<RoleAuthorization> serviceLevelAuthorizations,
                                         String operation) throws AuthorizationException {
    ClusterEntity clusterEntity = (null == entity) ? null : entity.getCluster();
    ResourceEntity resourceEntity = (null == clusterEntity) ? null : clusterEntity.getResource();
    Long resourceId = (null == resourceEntity) ? null : resourceEntity.getId();

    verifyAuthorization((null == entity) ? null : entity.getServiceName(), resourceId,
        clusterLevelAuthorizations, serviceLevelAuthorizations, operation);
  }

  /**
   * Tests if the authenticated user is authorized to for either service- or cluster-level access to
   * alert data. An authorization failure results in a thrown {@link AuthorizationException}.
   * <p/>
   * If the service name from the AlertDefinitionEntity is null or empty, the alert is considered to
   * be a cluster-level alert, else it is considered to be a service-level alert.
   *
   * @param serviceName                the name of the relevant service - null or empty indicates a cluster-level alert
   * @param clusterResourceId          the resource id of the relevant cluster
   * @param clusterLevelAuthorizations the set of cluster-level authorizations to check for
   * @param serviceLevelAuthorizations the set of service-level authorizations to check for
   * @param operation                  the name of the operation being tested for (used in error and logging messages)
   * @throws AuthorizationException if the authenticated user is not authorized
   */
  public static void verifyAuthorization(String serviceName, Long clusterResourceId,
                                         Set<RoleAuthorization> clusterLevelAuthorizations,
                                         Set<RoleAuthorization> serviceLevelAuthorizations,
                                         String operation) throws AuthorizationException {
    if (null == clusterResourceId) {
      // Do not let clusterResourceId be null because that indicates we don't care about which cluster
      // we are checking authorization for, but we do.  Setting this to -1 ensures that no cluster
      // will match will will give only Ambari administrators access to this.
      clusterResourceId = -1L;
    }

    // If the service name is AMBARI, than the alert is for the cluster
    if (StringUtils.isEmpty(serviceName) || "AMBARI".equals(serviceName)) {
      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusterResourceId,
          clusterLevelAuthorizations)) {
        throw new AuthorizationException(String.format("The authenticated user is not authorized to %s cluster-level alerts", operation));
      }
    } else {
      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, clusterResourceId,
          serviceLevelAuthorizations)) {
        throw new AuthorizationException(String.format("The authenticated user is not authorized to %s service-level alerts", operation));
      }
    }
  }
}

