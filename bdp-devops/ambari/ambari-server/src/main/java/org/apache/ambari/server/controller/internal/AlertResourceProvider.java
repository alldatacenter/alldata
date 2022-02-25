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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AlertCurrentRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.ExtendedResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * ResourceProvider for Alert instances
 */
@StaticallyInject
public class AlertResourceProvider extends ReadOnlyResourceProvider implements
    ExtendedResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AlertResourceProvider.class);

  public static final String ALERT_ID = "Alert/id";
  public static final String ALERT_STATE = "Alert/state";
  public static final String ALERT_ORIGINAL_TIMESTAMP = "Alert/original_timestamp";
  // TODO remove after UI-side fix
  public static final String ALERT_LATEST_TIMESTAMP = "Alert/latest_timestamp";
  public static final String ALERT_MAINTENANCE_STATE = "Alert/maintenance_state";
  public static final String ALERT_DEFINITION_ID = "Alert/definition_id";
  public static final String ALERT_DEFINITION_NAME = "Alert/definition_name";
  public static final String ALERT_TEXT = "Alert/text";

  public static final String ALERT_CLUSTER_NAME = "Alert/cluster_name";
  public static final String ALERT_COMPONENT = "Alert/component_name";
  public static final String ALERT_HOST = "Alert/host_name";
  public static final String ALERT_SERVICE = "Alert/service_name";

  protected static final String ALERT_INSTANCE = "Alert/instance";
  protected static final String ALERT_LABEL = "Alert/label";
  protected static final String ALERT_SCOPE = "Alert/scope";

  protected static final String ALERT_REPEAT_TOLERANCE = "Alert/repeat_tolerance";
  protected static final String ALERT_OCCURRENCES = "Alert/occurrences";
  protected static final String ALERT_REPEAT_TOLERANCE_REMAINING = "Alert/repeat_tolerance_remaining";
  protected static final String ALERT_FIRMNESS = "Alert/firmness";

  private static final Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(ALERT_ID, ALERT_DEFINITION_NAME));

  @Inject
  private static AlertsDAO alertsDAO;

  @Inject
  private static AlertDefinitionDAO alertDefinitionDAO = null;

  @Inject
  private static Provider<Clusters> clusters;

  /**
   * The property ids for an alert defintion resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for an alert definition resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(ALERT_ID);
    PROPERTY_IDS.add(ALERT_STATE);
    PROPERTY_IDS.add(ALERT_ORIGINAL_TIMESTAMP);
    PROPERTY_IDS.add(ALERT_DEFINITION_ID);
    PROPERTY_IDS.add(ALERT_DEFINITION_NAME);
    PROPERTY_IDS.add(ALERT_CLUSTER_NAME);
    // TODO remove after UI-side fix
    PROPERTY_IDS.add(ALERT_LATEST_TIMESTAMP);
    PROPERTY_IDS.add(ALERT_MAINTENANCE_STATE);
    PROPERTY_IDS.add(ALERT_INSTANCE);
    PROPERTY_IDS.add(ALERT_LABEL);
    PROPERTY_IDS.add(ALERT_TEXT);
    PROPERTY_IDS.add(ALERT_COMPONENT);
    PROPERTY_IDS.add(ALERT_HOST);
    PROPERTY_IDS.add(ALERT_SERVICE);
    PROPERTY_IDS.add(ALERT_SCOPE);
    PROPERTY_IDS.add(ALERT_REPEAT_TOLERANCE);
    PROPERTY_IDS.add(ALERT_OCCURRENCES);
    PROPERTY_IDS.add(ALERT_REPEAT_TOLERANCE_REMAINING);
    PROPERTY_IDS.add(ALERT_FIRMNESS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Alert, ALERT_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, ALERT_CLUSTER_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.Service, ALERT_SERVICE);
    KEY_PROPERTY_IDS.put(Resource.Type.Host, ALERT_HOST);
  }

  /**
   * Constructor.
   *
   * @param controller
   */
  AlertResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.Alert, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QueryResponse queryForResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    return new QueryResponseImpl(getResources(request, predicate),
        request.getSortRequest() != null, request.getPageRequest() != null,
        alertsDAO.getCount(predicate));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    // use a collection which preserves order since JPA sorts the results
    Set<Resource> results = new LinkedHashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {

      String clusterName = (String) propertyMap.get(ALERT_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("Invalid argument, cluster name is required");
      }

      String id = (String) propertyMap.get(ALERT_ID);
      if (null != id) {
        AlertCurrentEntity entity = alertsDAO.findCurrentById(Long.parseLong(id));

        if (null != entity) {
          AlertResourceProviderUtils.verifyViewAuthorization(entity);
          results.add(toResource(false, clusterName, entity, requestPropertyIds));
        }

      } else {
        // Verify authorization to retrieve the requested data
        try {
          Long clusterId = getClusterId(clusterName);
          String definitionName = (String) propertyMap.get(ALERT_DEFINITION_NAME);
          String definitionId = (String) propertyMap.get(ALERT_DEFINITION_ID);

          if(clusterId == null)  {
            // Make sure the user has administrative access by using -1 as the cluster id
            AlertResourceProviderUtils.verifyViewAuthorization("", -1L);
          }
          else if(!StringUtils.isEmpty(definitionName)) {
            // Make sure the user has access to the alert
            AlertDefinitionEntity alertDefinition = alertDefinitionDAO.findByName(clusterId, definitionName);
            AlertResourceProviderUtils.verifyViewAuthorization(alertDefinition);
          }
          else if(StringUtils.isNumeric(definitionId)) {
            // Make sure the user has access to the alert
            AlertDefinitionEntity alertDefinition = alertDefinitionDAO.findById(Long.parseLong(definitionId));
            AlertResourceProviderUtils.verifyViewAuthorization(alertDefinition);
          }
          else {
            // Make sure the user has the ability to view cluster-level alerts
            AlertResourceProviderUtils.verifyViewAuthorization("", getClusterResourceId(clusterName));
          }
        } catch (AmbariException e) {
          throw new SystemException(e.getMessage(), e);
        }

        List<AlertCurrentEntity> entities = null;
        AlertCurrentRequest alertCurrentRequest = new AlertCurrentRequest();
        alertCurrentRequest.Predicate = predicate;
        alertCurrentRequest.Pagination = request.getPageRequest();
        alertCurrentRequest.Sort = request.getSortRequest();

        entities = alertsDAO.findAll(alertCurrentRequest);

        if (null == entities) {
          entities = Collections.emptyList();
        }

        for (AlertCurrentEntity entity : entities) {
          results.add(toResource(true, clusterName, entity, requestPropertyIds));
        }
      }
    }

    return results;
  }

  /**
   * Converts an entity to a resource.
   *
   * @param isCollection {@code true} if the response is for a collection
   * @param clusterName the cluster name
   * @param entity the entity
   * @param requestedIds the requested ids
   * @return the resource
   */
  private Resource toResource(boolean isCollection, String clusterName,
      AlertCurrentEntity entity, Set<String> requestedIds) {
    AlertHistoryEntity history = entity.getAlertHistory();
    AlertDefinitionEntity definition = history.getAlertDefinition();

    Resource resource = new ResourceImpl(Resource.Type.Alert);
    setResourceProperty(resource, ALERT_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, ALERT_ID, entity.getAlertId(), requestedIds);
    // TODO remove after UI-side fix
    setResourceProperty(resource, ALERT_LATEST_TIMESTAMP, entity.getLatestTimestamp(), requestedIds);
    setResourceProperty(resource, ALERT_MAINTENANCE_STATE, entity.getMaintenanceState(), requestedIds);
    setResourceProperty(resource, ALERT_ORIGINAL_TIMESTAMP, entity.getOriginalTimestamp(), requestedIds);
    setResourceProperty(resource, ALERT_TEXT, entity.getLatestText(), requestedIds);

    setResourceProperty(resource, ALERT_INSTANCE, history.getAlertInstance(), requestedIds);
    setResourceProperty(resource, ALERT_LABEL, definition.getLabel(), requestedIds);
    setResourceProperty(resource, ALERT_STATE, history.getAlertState(), requestedIds);
    setResourceProperty(resource, ALERT_COMPONENT, history.getComponentName(), requestedIds);
    setResourceProperty(resource, ALERT_HOST, history.getHostName(), requestedIds);
    setResourceProperty(resource, ALERT_SERVICE, history.getServiceName(), requestedIds);

    setResourceProperty(resource, ALERT_DEFINITION_ID, definition.getDefinitionId(),requestedIds);
    setResourceProperty(resource, ALERT_DEFINITION_NAME, definition.getDefinitionName(), requestedIds);
    setResourceProperty(resource, ALERT_SCOPE, definition.getScope(), requestedIds);

    // repeat tolerance values
    int repeatTolerance = getRepeatTolerance(definition, clusterName);
    long occurrences = entity.getOccurrences();
    long remaining = (occurrences > repeatTolerance) ? 0 : (repeatTolerance - occurrences);

    // the OK state is special; when received, we ignore tolerance and assume
    // the alert is HARD
    if (history.getAlertState() == AlertState.OK) {
      remaining = 0;
    }

    setResourceProperty(resource, ALERT_REPEAT_TOLERANCE, repeatTolerance, requestedIds);
    setResourceProperty(resource, ALERT_OCCURRENCES, occurrences, requestedIds);
    setResourceProperty(resource, ALERT_REPEAT_TOLERANCE_REMAINING, remaining, requestedIds);
    setResourceProperty(resource, ALERT_FIRMNESS, entity.getFirmness(), requestedIds);

    if (isCollection) {
      // !!! want name/id to be populated as if it were a PK when requesting the
      // collection
      resource.setProperty(ALERT_DEFINITION_ID, definition.getDefinitionId());
      resource.setProperty(ALERT_DEFINITION_NAME, definition.getDefinitionName());
    }

    return resource;
  }

  /**
   * Gets the repeat tolerance value for the specified definition. This method
   * will return the override from the definition if
   * {@link AlertDefinitionEntity#isRepeatToleranceEnabled()} is {@code true}.
   * Otherwise, it uses {@link ConfigHelper#CLUSTER_ENV_ALERT_REPEAT_TOLERANCE},
   * defaulting to {@code 1} if not found.
   *
   * @param definition
   *          the definition (not {@code null}).
   * @param clusterName
   *          the name of the cluster (not {@code null}).
   * @return the repeat tolerance for the alert
   */
  private int getRepeatTolerance(AlertDefinitionEntity definition, String clusterName ){

    // if the definition overrides the global value, then use that
    if( definition.isRepeatToleranceEnabled() ){
      return definition.getRepeatTolerance();
    }

    int repeatTolerance = 1;
    try {
      Cluster cluster = clusters.get().getCluster(clusterName);
      String value = cluster.getClusterProperty(ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, "1");
      repeatTolerance = NumberUtils.toInt(value, 1);
    } catch (AmbariException ambariException) {
      LOG.warn("Unable to read {}/{} from cluster {}, defaulting to 1", ConfigHelper.CLUSTER_ENV,
          ConfigHelper.CLUSTER_ENV_ALERT_REPEAT_TOLERANCE, clusterName, ambariException);
    }

    return repeatTolerance;
  }
}
