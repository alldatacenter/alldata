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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.SettingResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.SettingDAO;
import org.apache.ambari.server.orm.entities.SettingEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * This class deals with managing CRUD operation on {@link SettingEntity}.
 */
@StaticallyInject
public class SettingResourceProvider extends AbstractAuthorizedResourceProvider {

  public static final String RESPONSE_KEY = "Settings";
  protected static final String ID = "id";
  protected static final String SETTING = "Setting";
  public static final String NAME = "name";
  public static final String SETTING_TYPE = "setting_type";
  public static final String CONTENT = "content";
  public static final String UPDATED_BY = "updated_by";
  public static final String UPDATE_TIMESTAMP = "update_timestamp";
  public static final String SETTING_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + NAME;
  public static final String SETTING_SETTING_TYPE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + SETTING_TYPE;
  public static final String SETTING_CONTENT_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CONTENT;
  public static final String SETTING_UPDATED_BY_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + UPDATED_BY;
  public static final String SETTING_UPDATE_TIMESTAMP_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + UPDATE_TIMESTAMP;
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";

  /**
   * The property ids for setting resource.
   */
  private static final Set<String> propertyIds = ImmutableSet.<String>builder()
    .add(SETTING_NAME_PROPERTY_ID)
    .add(SETTING_SETTING_TYPE_PROPERTY_ID)
    .add(SETTING_CONTENT_PROPERTY_ID)
    .add(SETTING_UPDATED_BY_PROPERTY_ID)
    .add(SETTING_UPDATE_TIMESTAMP_PROPERTY_ID)
    .add(SETTING_SETTING_TYPE_PROPERTY_ID)
    .add(SETTING)
    .build();

  /**
   * The key property ids for setting resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
    .put(Resource.Type.Setting, SETTING_NAME_PROPERTY_ID)
    .build();

  private static final Set<String> REQUIRED_PROPERTIES = ImmutableSet.of(
    SETTING_NAME_PROPERTY_ID,
    SETTING_SETTING_TYPE_PROPERTY_ID,
    SETTING_CONTENT_PROPERTY_ID
  );

  @Inject
  private static SettingDAO dao;

  protected SettingResourceProvider() {
    super(Resource.Type.Setting, propertyIds, keyPropertyIds);
    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_SETTINGS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
          throws NoSuchParentResourceException, ResourceAlreadyExistsException, SystemException {
    Set<Resource> associatedResources = new HashSet<>();

    for (Map<String, Object> properties : request.getProperties()) {
      SettingResponse setting = createResources(newCreateCommand(request, properties));
      Resource resource = new ResourceImpl(Resource.Type.Setting);
      resource.setProperty(SETTING_NAME_PROPERTY_ID, setting.getName());
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws NoSuchResourceException {
    List<SettingEntity> entities = new LinkedList<>();
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    if (propertyMaps.isEmpty()) {
      entities = dao.findAll();
    }
    for (Map<String, Object> propertyMap: propertyMaps) {
      if (propertyMap.containsKey(SETTING_NAME_PROPERTY_ID)) {
        String name = propertyMap.get(SETTING_NAME_PROPERTY_ID).toString();
        SettingEntity entity = dao.findByName(name);
        if (entity == null) {
          throw new NoSuchResourceException(String.format("Setting with name %s does not exists", name));
        }
        entities.add(entity);
      } else {
        entities = dao.findAll();
        break;
      }
    }
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();
    for(SettingEntity entity : entities) {
      resources.add(toResource(toResponse(entity), requestedIds));
    }
    return resources;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
          throws NoSuchResourceException, NoSuchParentResourceException, SystemException {
    modifyResources(newUpdateCommand(request));
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) {
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    for (Map<String, Object> propertyMap : propertyMaps) {
      if (propertyMap.containsKey(SETTING_NAME_PROPERTY_ID)) {
        dao.removeByName(propertyMap.get(SETTING_NAME_PROPERTY_ID).toString());
      }
    }
    return getRequestStatus(null);
  }


  private Command<SettingResponse> newCreateCommand(final Request request, final Map<String, Object> properties) {
    return new Command<SettingResponse>() {
      @Override
      public SettingResponse invoke() throws AmbariException, AuthorizationException {
        SettingEntity entity = toEntity(properties);
        if (dao.findByName(entity.getName()) != null) {
          throw new DuplicateResourceException(
                  String.format("Setting already exists. setting name :%s ", entity.getName()));
        }
        dao.create(entity);
        notifyCreate(Resource.Type.Setting, request);
        return toResponse(entity);
      }
    };
  }

  private Command<Void> newUpdateCommand(final Request request) throws NoSuchResourceException, SystemException {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        final Set<Map<String, Object>> propertyMaps = request.getProperties();
        for (Map<String, Object> propertyMap : propertyMaps) {
          if (propertyMap.containsKey(SETTING_NAME_PROPERTY_ID)) {
            String name = propertyMap.get(SETTING_NAME_PROPERTY_ID).toString();
            SettingEntity entity = dao.findByName(name);
            if (entity == null) {
              throw new AmbariException(String.format("There is no setting with name: %s ", name));
            }
            updateEntity(entity, propertyMap);
            dao.merge(entity);
          }
        }
        return null;
      }
    };
  }

  private void updateEntity(SettingEntity entity, Map<String, Object> propertyMap) throws AmbariException {
    String name = propertyMap.get(SETTING_NAME_PROPERTY_ID).toString();
    if (!Objects.equals(name, entity.getName())) {
      throw new AmbariException("Name for Setting is immutable, cannot change name.");
    }

    if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(SETTING_CONTENT_PROPERTY_ID)))) {
      entity.setContent(propertyMap.get(SETTING_CONTENT_PROPERTY_ID).toString());
    }

    if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(SETTING_SETTING_TYPE_PROPERTY_ID)))) {
      entity.setSettingType(propertyMap.get(SETTING_SETTING_TYPE_PROPERTY_ID).toString());
    }

    entity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
    entity.setUpdateTimestamp(System.currentTimeMillis());
  }

  private Resource toResource(final SettingResponse setting, final Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.Setting);
    setResourceProperty(resource, SETTING_NAME_PROPERTY_ID, setting.getName(), requestedIds);
    setResourceProperty(resource, SETTING_SETTING_TYPE_PROPERTY_ID, setting.getSettingType(), requestedIds);
    setResourceProperty(resource, SETTING_CONTENT_PROPERTY_ID, setting.getContent(), requestedIds);
    setResourceProperty(resource, SETTING_UPDATED_BY_PROPERTY_ID, setting.getUpdatedBy(), requestedIds);
    setResourceProperty(resource, SETTING_UPDATE_TIMESTAMP_PROPERTY_ID, setting.getUpdateTimestamp(), requestedIds);
    return resource;
  }

  private SettingEntity toEntity(final Map<String, Object> properties) throws AmbariException {
    for (String propertyName: REQUIRED_PROPERTIES) {
      if (properties.get(propertyName) == null) {
        throw new AmbariException(String.format("Property %s should be provided", propertyName));
      }
    }

    SettingEntity entity = new SettingEntity();
    entity.setName(properties.get(SETTING_NAME_PROPERTY_ID).toString());
    entity.setSettingType(properties.get(SETTING_SETTING_TYPE_PROPERTY_ID).toString());
    entity.setContent(properties.get(SETTING_CONTENT_PROPERTY_ID).toString());
    entity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
    entity.setUpdateTimestamp(System.currentTimeMillis());
    return entity;
  }

  private static SettingResponse toResponse(SettingEntity entity) {
    return new SettingResponse(entity.getName(), entity.getSettingType(), entity.getContent(), entity.getUpdatedBy(), entity.getUpdateTimestamp());
  }
}
