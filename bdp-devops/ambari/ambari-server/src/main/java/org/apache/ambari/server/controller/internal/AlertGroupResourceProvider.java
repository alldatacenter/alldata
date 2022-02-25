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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.agent.stomp.dto.AlertGroupUpdate;
import org.apache.ambari.server.controller.AlertDefinitionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AlertGroupsUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.alert.AlertTarget;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * The {@link AlertGroupResourceProvider} class deals with managing the CRUD
 * operations alert groups, including property coercion to and from
 * {@link AlertGroupEntity}.
 */
@StaticallyInject
public class AlertGroupResourceProvider extends
    AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AlertGroupResourceProvider.class);

  public static final String ALERT_GROUP = "AlertGroup";

  public static final String ID_PROPERTY_ID = "id";
  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String NAME_PROPERTY_ID = "name";
  public static final String DEFAULT_PROPERTY_ID = "default";
  public static final String DEFINITIONS_PROPERTY_ID = "definitions";
  public static final String TARGETS_PROPERTY_ID = "targets";

  public static final String ALERT_GROUP_ID = ALERT_GROUP + PropertyHelper.EXTERNAL_PATH_SEP + ID_PROPERTY_ID;
  public static final String ALERT_GROUP_CLUSTER_NAME = ALERT_GROUP + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME_PROPERTY_ID;
  public static final String ALERT_GROUP_NAME = ALERT_GROUP + PropertyHelper.EXTERNAL_PATH_SEP + NAME_PROPERTY_ID;
  public static final String ALERT_GROUP_DEFAULT = ALERT_GROUP + PropertyHelper.EXTERNAL_PATH_SEP + DEFAULT_PROPERTY_ID;
  public static final String ALERT_GROUP_DEFINITIONS = ALERT_GROUP + PropertyHelper.EXTERNAL_PATH_SEP + DEFINITIONS_PROPERTY_ID;
  public static final String ALERT_GROUP_TARGETS = ALERT_GROUP + PropertyHelper.EXTERNAL_PATH_SEP + TARGETS_PROPERTY_ID;

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
    Arrays.asList(ALERT_GROUP_ID, ALERT_GROUP_CLUSTER_NAME));

  /**
   * The property ids for an alert group resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for an alert group resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(ALERT_GROUP_ID);
    PROPERTY_IDS.add(ALERT_GROUP_CLUSTER_NAME);
    PROPERTY_IDS.add(ALERT_GROUP_NAME);
    PROPERTY_IDS.add(ALERT_GROUP_DEFAULT);
    PROPERTY_IDS.add(ALERT_GROUP_DEFINITIONS);
    PROPERTY_IDS.add(ALERT_GROUP_TARGETS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.AlertGroup, ALERT_GROUP_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, ALERT_GROUP_CLUSTER_NAME);
  }

  /**
   * Group/Target DAO
   */
  @Inject
  private static AlertDispatchDAO s_dao;

  /**
   * Definitions DAO
   */
  @Inject
  private static AlertDefinitionDAO s_definitionDao;

  @Inject
  private static STOMPUpdatePublisher STOMPUpdatePublisher;

  /**
   * Constructor.
   *
   * @param controller
   */
  AlertGroupResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.AlertGroup, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        createAlertGroups(request.getProperties());
        return null;
      }
    });

    notifyCreate(Resource.Type.AlertGroup, request);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(ALERT_GROUP_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("The cluster name is required when retrieving alert groups");
      }

      String id = (String) propertyMap.get(ALERT_GROUP_ID);
      if (null != id) {
        AlertGroupEntity entity = s_dao.findGroupById(Long.parseLong(id));
        if (null != entity) {
          try {
            AlertResourceProviderUtils.verifyViewAuthorization(entity, getClusterResourceId(entity.getClusterId()));
          } catch (AmbariException e) {
            throw new SystemException(e.getMessage(), e);
          }
          results.add(toResource(clusterName, entity, requestPropertyIds));
        }
      } else {
        Cluster cluster = null;

        try {
          cluster = getManagementController().getClusters().getCluster(clusterName);
        } catch (AmbariException ae) {
          throw new NoSuchResourceException("Parent Cluster resource doesn't exist", ae);
        }

        List<AlertGroupEntity> entities = s_dao.findAllGroups(cluster.getClusterId());

        for (AlertGroupEntity entity : entities) {
          try {
            if (AlertResourceProviderUtils.hasViewAuthorization(entity, getClusterResourceId(entity.getClusterId()))) {
              results.add(toResource(clusterName, entity, requestPropertyIds));
            }
          } catch (AmbariException e) {
            throw new SystemException(e.getMessage(), e);
          }
        }
      }
    }

    return results;
  }

  @Override
  public RequestStatus updateResources(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        updateAlertGroups(request.getProperties());
        return null;
      }
    });

    notifyUpdate(Resource.Type.AlertGroup, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(new RequestImpl(null, null, null,
        null), predicate);

    Map<Long, AlertGroupEntity> entities = new HashMap<>();

    for (final Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ALERT_GROUP_ID);
      if (!entities.containsKey(id)) {
        AlertGroupEntity entity = s_dao.findGroupById(id);

        try {
          AlertResourceProviderUtils.verifyManageAuthorization(entity, getClusterResourceId(entity.getClusterId()));
          entities.put(id, entity);
        } catch (AmbariException e) {
          LOG.warn("The default alert group for {} cannot be removed", entity.getServiceName(), e);
        }
      }
    }

    for (final AlertGroupEntity entity : entities.values()) {
      LOG.info("Deleting alert group {}", entity.getGroupId());

      if (entity.isDefault()) {
        // default groups cannot be removed
        LOG.warn("The default alert group for {} cannot be removed",
            entity.getServiceName());

        continue;
      }

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          s_dao.remove(entity);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.AlertGroup, predicate);
    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Create and persist {@link AlertTargetEntity} from the map of properties.
   *
   * @param requestMaps
   * @throws AmbariException
   */
  @SuppressWarnings("unchecked")
  private void createAlertGroups(Set<Map<String, Object>> requestMaps)
      throws AmbariException, AuthorizationException {

    List<AlertGroupEntity> entities = new ArrayList<>();
    for (Map<String, Object> requestMap : requestMaps) {
      AlertGroupEntity entity = new AlertGroupEntity();

      String name = (String) requestMap.get(ALERT_GROUP_NAME);
      String clusterName = (String) requestMap.get(ALERT_GROUP_CLUSTER_NAME);

      if (StringUtils.isEmpty(name)) {
        throw new IllegalArgumentException(
            "The name of the alert group is required.");
      }

      if (StringUtils.isEmpty(clusterName)) {
        throw new IllegalArgumentException(
            "The name of the cluster is required when creating an alert group.");
      }

      Cluster cluster = getManagementController().getClusters().getCluster(
          clusterName);

      entity.setClusterId(cluster.getClusterId());
      entity.setGroupName(name);

      // groups created through the provider are not default service groups
      entity.setDefault(false);

      // targets are not required on creation
      if (requestMap.containsKey(ALERT_GROUP_TARGETS)) {
        List<Long> targetIds = (List<Long>) requestMap.get(ALERT_GROUP_TARGETS);
        Set<AlertTargetEntity> targets = new HashSet<>();
        targets.addAll(s_dao.findTargetsById(targetIds));
        entity.setAlertTargets(targets);
      }

      // definitions are not required on creation
      if (requestMap.containsKey(ALERT_GROUP_DEFINITIONS)) {
        List<Long> definitionIds = (List<Long>) requestMap.get(ALERT_GROUP_DEFINITIONS);
        Set<AlertDefinitionEntity> definitions = new HashSet<>();
        definitions.addAll(s_definitionDao.findByIds(definitionIds));
        entity.setAlertDefinitions(definitions);
      }

      AlertResourceProviderUtils.verifyManageAuthorization(entity, cluster.getResourceId());

      entities.add(entity);
    }

    s_dao.createGroups(entities);
  }

  /**
   * Updates existing {@link AlertGroupEntity}s with the specified properties.
   *
   * @param requestMaps
   *          a set of property maps, one map for each entity.
   * @throws AmbariException
   *           if the entity could not be found.
   */
  @SuppressWarnings("unchecked")
  private void updateAlertGroups(Set<Map<String, Object>> requestMaps)
      throws AmbariException, AuthorizationException {

    for (Map<String, Object> requestMap : requestMaps) {
      String stringId = (String) requestMap.get(ALERT_GROUP_ID);

      if( StringUtils.isEmpty(stringId)){
        throw new IllegalArgumentException("The ID of the alert group is required when updating an existing group");
      }

      long id = Long.parseLong(stringId);
      AlertGroupEntity entity = s_dao.findGroupById(id);

      if( null == entity ){
        String message = MessageFormat.format("The alert group with ID {0} could not be found", id);
        throw new AmbariException(message);
      }

      AlertResourceProviderUtils.verifyManageAuthorization(entity, getClusterResourceId(entity.getClusterId()));

      String name = (String) requestMap.get(ALERT_GROUP_NAME);

      // empty arrays are deserialized as HashSet while populated arrays
      // are deserialized as ArrayList; use Collection for safety
      Collection<Long> targetIds = (Collection<Long>) requestMap.get(ALERT_GROUP_TARGETS);
      Collection<Long> definitionIds = (Collection<Long>) requestMap.get(ALERT_GROUP_DEFINITIONS);

      // if targets were supplied, replace existing
      if (null != targetIds) {
        Set<AlertTargetEntity> targets = new HashSet<>();

        List<Long> ids = new ArrayList<>(targetIds.size());
        ids.addAll(targetIds);

        if (ids.size() > 0) {
          targets.addAll(s_dao.findTargetsById(ids));
        }

        entity.setAlertTargets(targets);
      }

      // only the targets should be updatable on default groups; everything
      // else is valid only on regular groups
      if (!entity.isDefault()) {
        // set the name if supplied
        if (!StringUtils.isBlank(name)) {
          entity.setGroupName(name);
        }

        // if definitions were supplied, replace existing
        Set<AlertDefinitionEntity> definitions = new HashSet<>();
        if (null != definitionIds && definitionIds.size() > 0) {
          List<Long> ids = new ArrayList<>(definitionIds.size());
          ids.addAll(definitionIds);
          definitions.addAll(s_definitionDao.findByIds(ids));

          entity.setAlertDefinitions(definitions);
        } else {
          // empty array supplied, clear out existing definitions
          entity.setAlertDefinitions(definitions);
        }
      }

      s_dao.merge(entity);
      AlertGroupsUpdateEvent alertGroupsUpdateEvent = new AlertGroupsUpdateEvent(Collections.singletonList(
          new AlertGroupUpdate(entity)),
          UpdateEventType.UPDATE);
      STOMPUpdatePublisher.publish(alertGroupsUpdateEvent);
    }
  }

  /**
   * Convert the given {@link AlertGroupEntity} to a {@link Resource}.
   *
   * @param entity
   *          the entity to convert.
   * @param requestedIds
   *          the properties that were requested or {@code null} for all.
   * @return the resource representation of the entity (never {@code null}).
   */
  private Resource toResource(String clusterName, AlertGroupEntity entity,
      Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.AlertGroup);
    resource.setProperty(ALERT_GROUP_ID, entity.getGroupId());
    resource.setProperty(ALERT_GROUP_NAME, entity.getGroupName());
    resource.setProperty(ALERT_GROUP_CLUSTER_NAME, clusterName);

    setResourceProperty(resource, ALERT_GROUP_DEFAULT, entity.isDefault(),
        requestedIds);

    // only set the definitions if requested
    if (BaseProvider.isPropertyRequested(ALERT_GROUP_DEFINITIONS, requestedIds)) {
      Set<AlertDefinitionEntity> definitions = entity.getAlertDefinitions();
      List<AlertDefinitionResponse> definitionList = new ArrayList<>(
        definitions.size());

      for (AlertDefinitionEntity definition : definitions) {
        AlertDefinitionResponse response = AlertDefinitionResponse.coerce(definition);
        definitionList.add(response);
      }

      resource.setProperty(ALERT_GROUP_DEFINITIONS, definitionList);
    }

    if (BaseProvider.isPropertyRequested(ALERT_GROUP_TARGETS, requestedIds)) {
      Set<AlertTargetEntity> targetEntities = entity.getAlertTargets();

      List<AlertTarget> targets = new ArrayList<>(
        targetEntities.size());

      for (AlertTargetEntity targetEntity : targetEntities) {
        AlertTarget target = AlertTarget.coerce(targetEntity);
        targets.add(target);
      }

      resource.setProperty(ALERT_GROUP_TARGETS, targets);
    }

    return resource;
  }
}
