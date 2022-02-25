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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.resources.AlertTargetResourceDefinition;
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
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AlertGroup;
import org.apache.ambari.server.state.alert.AlertTarget;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * The {@link AlertTargetResourceProvider} class deals with managing the CRUD
 * operations for {@link AlertTarget}, including property coercion to and from
 * {@link AlertTargetEntity}.
 */
@StaticallyInject
public class AlertTargetResourceProvider extends
 AbstractAuthorizedResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AlertTargetResourceProvider.class);
    
  public static final String ALERT_TARGET = "AlertTarget";

  public static final String ID_PROPERTY_ID = "id";
  public static final String NAME_PROPERTY_ID = "name";
  public static final String DESCRIPTION_PROPERTY_ID = "description";
  public static final String NOTIFICATION_TYPE_PROPERTY_ID = "notification_type";
  public static final String PROPERTIES_PROPERTY_ID = "properties";
  public static final String GROUPS_PROPERTY_ID = "groups";
  public static final String STATES_PROPERTY_ID = "alert_states";
  public static final String GLOBAL_PROPERTY_ID = "global";
  public static final String ENABLED_PROPERTY_ID = "enabled";

  public static final String ALERT_TARGET_ID = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + ID_PROPERTY_ID;
  public static final String ALERT_TARGET_NAME = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + NAME_PROPERTY_ID;
  public static final String ALERT_TARGET_DESCRIPTION = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + DESCRIPTION_PROPERTY_ID;
  public static final String ALERT_TARGET_NOTIFICATION_TYPE = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + NOTIFICATION_TYPE_PROPERTY_ID;
  public static final String ALERT_TARGET_PROPERTIES = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + PROPERTIES_PROPERTY_ID;
  public static final String ALERT_TARGET_GROUPS = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + GROUPS_PROPERTY_ID;
  public static final String ALERT_TARGET_STATES = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + STATES_PROPERTY_ID;
  public static final String ALERT_TARGET_GLOBAL = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + GLOBAL_PROPERTY_ID;
  public static final String ALERT_TARGET_ENABLED = ALERT_TARGET + PropertyHelper.EXTERNAL_PATH_SEP + ENABLED_PROPERTY_ID;

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
      Arrays.asList(ALERT_TARGET_ID, ALERT_TARGET_NAME));

  /**
   * The property ids for an alert target resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for an alert target resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(ALERT_TARGET_ID);
    PROPERTY_IDS.add(ALERT_TARGET_NAME);
    PROPERTY_IDS.add(ALERT_TARGET_DESCRIPTION);
    PROPERTY_IDS.add(ALERT_TARGET_NOTIFICATION_TYPE);
    PROPERTY_IDS.add(ALERT_TARGET_PROPERTIES);
    PROPERTY_IDS.add(ALERT_TARGET_GROUPS);
    PROPERTY_IDS.add(ALERT_TARGET_STATES);
    PROPERTY_IDS.add(ALERT_TARGET_GLOBAL);
    PROPERTY_IDS.add(ALERT_TARGET_ENABLED);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.AlertTarget, ALERT_TARGET_ID);
  }

  /**
   * Target DAO
   */
  @Inject
  private static AlertDispatchDAO s_dao;

  @Inject
  private static DispatchFactory dispatchFactory;

  /**
   * Used for serialization and deserialization of some fields.
   */
  private static final Gson s_gson = new Gson();

  /**
   * Constructor.
   */
  @Inject
  AlertTargetResourceProvider() {
    super(Resource.Type.AlertTarget, PROPERTY_IDS, KEY_PROPERTY_IDS);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.CLUSTER_MANAGE_ALERT_NOTIFICATIONS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createAlertTargets(request.getProperties(), request.getRequestInfoProperties());
        return null;
      }
    });

    notifyCreate(Resource.Type.AlertTarget, request);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    if( null == predicate ){
      List<AlertTargetEntity> entities = s_dao.findAllTargets();
      for (AlertTargetEntity entity : entities) {
        results.add(toResource(entity, requestPropertyIds));
      }
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        String id = (String) propertyMap.get(ALERT_TARGET_ID);
        if (null == id) {
          continue;
        }

        AlertTargetEntity entity = s_dao.findTargetById(Long.parseLong(id));
        if (null != entity) {
          results.add(toResource(entity, requestPropertyIds));
        }
      }
    }

    return results;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        Set<Map<String, Object>> requestMaps = request.getProperties();
        for (Map<String, Object> requestMap : requestMaps) {
          String stringId = (String) requestMap.get(ALERT_TARGET_ID);

          if (StringUtils.isEmpty(stringId)) {
            throw new IllegalArgumentException(
                "The ID of the alert target is required when updating an existing target");
          }

          long alertTargetId = Long.parseLong(stringId);
          updateAlertTargets(alertTargetId, requestMap);
        }
        return null;
      }
    });

    notifyUpdate(Resource.Type.AlertTarget, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(new RequestImpl(null, null, null,
        null), predicate);

    Set<Long> targetIds = new HashSet<>();

    for (final Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ALERT_TARGET_ID);
      targetIds.add(id);
    }

    for (Long targetId : targetIds) {
      LOG.info("Deleting alert target {}", targetId);

      final AlertTargetEntity entity = s_dao.findTargetById(targetId.longValue());

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          s_dao.remove(entity);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.AlertTarget, predicate);
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
   * @param requestInfoProps
   * @throws AmbariException
   */
  @SuppressWarnings("unchecked")
  private void createAlertTargets(Set<Map<String, Object>> requestMaps, Map<String, String> requestInfoProps)
      throws AmbariException {
    for (Map<String, Object> requestMap : requestMaps) {
      String name = (String) requestMap.get(ALERT_TARGET_NAME);
      String description = (String) requestMap.get(ALERT_TARGET_DESCRIPTION);
      String notificationType = (String) requestMap.get(ALERT_TARGET_NOTIFICATION_TYPE);
      Collection<String> alertStates = (Collection<String>) requestMap.get(ALERT_TARGET_STATES);
      String globalProperty = (String) requestMap.get(ALERT_TARGET_GLOBAL);
      String enabledProperty = (String) requestMap.get(ALERT_TARGET_ENABLED);

      if (StringUtils.isEmpty(name)) {
        throw new IllegalArgumentException(
            "The name of the alert target is required.");
      }

      if (StringUtils.isEmpty(notificationType)) {
        throw new IllegalArgumentException(
            "The type of the alert target is required.");
      }

      Map<String, Object> properties = extractProperties(requestMap);

      String propertiesJson = s_gson.toJson(properties);
      if (StringUtils.isEmpty(propertiesJson)) {
        throw new IllegalArgumentException(
            "Alert targets must be created with their connection properties");
      }

      String validationDirective = requestInfoProps.get(AlertTargetResourceDefinition.VALIDATE_CONFIG_DIRECTIVE);
      if (validationDirective != null
          && validationDirective.equalsIgnoreCase("true")) {
        validateTargetConfig(notificationType, properties);
      }

      boolean overwriteExisting = false;
      if (requestInfoProps.containsKey(AlertTargetResourceDefinition.OVERWRITE_DIRECTIVE)) {
        overwriteExisting = true;
      }

      // global not required
      boolean isGlobal = false;
      if (null != globalProperty) {
        isGlobal = Boolean.parseBoolean(globalProperty);
      }

      // enabled not required
      boolean isEnabled = true;
      if (null != enabledProperty) {
        isEnabled = Boolean.parseBoolean(enabledProperty);
      }

      // set the states that this alert target cares about
      final Set<AlertState> alertStateSet;
      if (null != alertStates) {
        alertStateSet = new HashSet<>(alertStates.size());
        for (String state : alertStates) {
          alertStateSet.add(AlertState.valueOf(state));
        }
      } else {
        alertStateSet = EnumSet.allOf(AlertState.class);
      }

      // if we are overwriting an existing, determine if one exists first
      AlertTargetEntity entity = null;
      if( overwriteExisting ) {
        entity = s_dao.findTargetByName(name);
      }

      if (null == entity) {
        entity = new AlertTargetEntity();
      }

      // groups are not required on creation
      if (requestMap.containsKey(ALERT_TARGET_GROUPS)) {
        Collection<Long> groupIds = (Collection<Long>) requestMap.get(ALERT_TARGET_GROUPS);
        if( !groupIds.isEmpty() ){
          Set<AlertGroupEntity> groups = new HashSet<>();
          List<Long> ids = new ArrayList<>(groupIds);
          groups.addAll(s_dao.findGroupsById(ids));
          entity.setAlertGroups(groups);
        }
      }

      entity.setDescription(description);
      entity.setNotificationType(notificationType);
      entity.setProperties(propertiesJson);
      entity.setTargetName(name);
      entity.setAlertStates(alertStateSet);
      entity.setGlobal(isGlobal);
      entity.setEnabled(isEnabled);

      if (null == entity.getTargetId() || 0 == entity.getTargetId()) {
        s_dao.create(entity);
      } else {
        s_dao.merge(entity);
      }
    }
  }

  /**
   * Updates existing {@link AlertTargetEntity}s with the specified properties.
   *
   * @param requestMap
   *          a set of property maps, one map for each entity.
   * @throws AmbariException
   *           if the entity could not be found.
   */
  @Transactional
  @SuppressWarnings("unchecked")
  void updateAlertTargets(long alertTargetId,
      Map<String, Object> requestMap)
      throws AmbariException {

    AlertTargetEntity entity = s_dao.findTargetById(alertTargetId);

    if (null == entity) {
      String message = MessageFormat.format(
          "The alert target with ID {0} could not be found", alertTargetId);

      throw new AmbariException(message);
    }

    String name = (String) requestMap.get(ALERT_TARGET_NAME);
    String description = (String) requestMap.get(ALERT_TARGET_DESCRIPTION);
    String notificationType = (String) requestMap.get(ALERT_TARGET_NOTIFICATION_TYPE);
    Collection<String> alertStates = (Collection<String>) requestMap.get(ALERT_TARGET_STATES);
    Collection<Long> groupIds = (Collection<Long>) requestMap.get(ALERT_TARGET_GROUPS);
    String isGlobal = (String) requestMap.get(ALERT_TARGET_GLOBAL);
    String isEnabled = (String) requestMap.get(ALERT_TARGET_ENABLED);

    if(null != isGlobal){
      entity.setGlobal(Boolean.parseBoolean(isGlobal));
    }

    if (null != isEnabled) {
      entity.setEnabled(Boolean.parseBoolean(isEnabled));
    }

    if (!StringUtils.isBlank(name)) {
      entity.setTargetName(name);
    }

    if (null != description) {
      entity.setDescription(description);
    }

    if (!StringUtils.isBlank(notificationType)) {
      entity.setNotificationType(notificationType);
    }

    Map<String, Object> propertiesMap = extractProperties(requestMap);

    if (propertiesMap != null) {
      String properties = s_gson.toJson(propertiesMap);
      if (!StringUtils.isEmpty(properties)) {
        LOG.debug("Updating Alert Target properties map to: " + properties);
        entity.setProperties(properties);
      }
    }

    // a null alert state implies that the key was not set and no update
    // should occur for this field, while an empty list implies all alert
    // states should be set
    if (null != alertStates) {
      final Set<AlertState> alertStateSet;
      if (alertStates.isEmpty()) {
        alertStateSet = EnumSet.allOf(AlertState.class);
      } else {
        alertStateSet = new HashSet<>(alertStates.size());
        for (String state : alertStates) {
          alertStateSet.add(AlertState.valueOf(state));
        }
      }

      entity.setAlertStates(alertStateSet);
    }

    // if groups were supplied, replace existing
    if (null != groupIds) {
      Set<AlertGroupEntity> groups = new HashSet<>();
      List<Long> ids = new ArrayList<>(groupIds);

      if (ids.size() > 0) {
        groups.addAll(s_dao.findGroupsById(ids));
      }

      entity.setAlertGroups(groups);
    } else if (entity.isGlobal()){
      Set<AlertGroupEntity> groups = new HashSet<>(s_dao.findAllGroups());
      entity.setAlertGroups(groups);
    }

    // merge the entity, cascading the merge to other entities, like groups
    s_dao.merge(entity);
  }

  /**
   * Convert the given {@link AlertTargetEntity} to a {@link Resource}.
   *
   * @param entity
   *          the entity to convert.
   * @param requestedIds
   *          the properties that were requested or {@code null} for all.
   * @return the resource representation of the entity (never {@code null}).
   */
  private Resource toResource(AlertTargetEntity entity, Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.AlertTarget);
    resource.setProperty(ALERT_TARGET_ID, entity.getTargetId());
    resource.setProperty(ALERT_TARGET_NAME, entity.getTargetName());

    resource.setProperty(ALERT_TARGET_DESCRIPTION, entity.getDescription());
    resource.setProperty(ALERT_TARGET_NOTIFICATION_TYPE,
        entity.getNotificationType());

    resource.setProperty(ALERT_TARGET_ENABLED, entity.isEnabled());

    // these are expensive to deserialize; only do it if asked for
    if (requestedIds.contains(ALERT_TARGET_PROPERTIES)) {
      String properties = entity.getProperties();
      Map<String, String> map = s_gson.<Map<String, String>> fromJson(
          properties,
          Map.class);

      resource.setProperty(ALERT_TARGET_PROPERTIES, map);
    }

    setResourceProperty(resource, ALERT_TARGET_STATES, entity.getAlertStates(),
        requestedIds);

    setResourceProperty(resource, ALERT_TARGET_GLOBAL, entity.isGlobal(),
        requestedIds);

    if (BaseProvider.isPropertyRequested(ALERT_TARGET_GROUPS, requestedIds)) {
      Set<AlertGroupEntity> groupEntities = entity.getAlertGroups();
      List<AlertGroup> groups = new ArrayList<>(
        groupEntities.size());

      for (AlertGroupEntity groupEntity : groupEntities) {
        AlertGroup group = new AlertGroup();
        group.setId(groupEntity.getGroupId());
        group.setName(groupEntity.getGroupName());
        group.setClusterName(groupEntity.getClusterId());
        group.setDefault(groupEntity.isDefault());
        groups.add(group);
      }

      resource.setProperty(ALERT_TARGET_GROUPS, groups);
    }

    return resource;
  }

  /**
   * Looks through the flat list of propery keys in the supplied map and builds
   * a JSON string of key:value pairs for the {@link #ALERT_TARGET_PROPERTIES}
   * key.
   *
   * @param requestMap
   *          the map of flattened properties (not {@code null}).
   * @return the JSON representing the key/value pairs of all properties, or
   *         {@code null} if none.
   */
  private Map<String, Object> extractProperties(Map<String, Object> requestMap) {
    Map<String, Object> normalizedMap = new HashMap<>(
      requestMap.size());
    boolean has_properties = false;

    for (Entry<String, Object> entry : requestMap.entrySet()) {
      String key = entry.getKey();
      String propCat = PropertyHelper.getPropertyCategory(key);

      if (propCat.equals(ALERT_TARGET_PROPERTIES)) {
        has_properties = true;
        String propKey = PropertyHelper.getPropertyName(key);
        normalizedMap.put(propKey, entry.getValue());
      }
    }

    if (!has_properties) {
      normalizedMap = null;
    }

    return normalizedMap;
  }

  /**
   * Finds dispatcher for given notification type and validates on it given alert target configuration properties.
   * @param notificationType type of dispatcher
   * @param properties alert target configuration properties
   */
  private void validateTargetConfig(String notificationType, Map<String, Object> properties) {
    NotificationDispatcher dispatcher = dispatchFactory.getDispatcher(notificationType);
    if (dispatcher == null) {
      throw new IllegalArgumentException("Dispatcher for given notification type doesn't exist");
    }
    TargetConfigurationResult validationResult = dispatcher.validateTargetConfig(properties);
    if (validationResult.getStatus() == TargetConfigurationResult.Status.INVALID) {
      throw new IllegalArgumentException(validationResult.getMessage());
    }
  }
}
