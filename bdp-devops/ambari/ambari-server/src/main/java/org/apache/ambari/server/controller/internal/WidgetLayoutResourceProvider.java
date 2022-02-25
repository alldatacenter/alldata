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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.WidgetResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Resource provider for widget layout resources.
 */
@StaticallyInject
public class WidgetLayoutResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String WIDGETLAYOUT_ID_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "id");
  public static final String WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "cluster_name");
  public static final String WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "section_name");
  public static final String WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "layout_name");
  public static final String WIDGETLAYOUT_SCOPE_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "scope");
  public static final String WIDGETLAYOUT_WIDGETS_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "widgets");
  public static final String WIDGETLAYOUT_USERNAME_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "user_name");
  public static final String WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("WidgetLayoutInfo", "display_name");
  public enum SCOPE {
    CLUSTER,
    USER
  }

  @SuppressWarnings("serial")
  private static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(WIDGETLAYOUT_ID_PROPERTY_ID)
    .build();

  @SuppressWarnings("serial")
  private static final ReadWriteLock lock = new ReentrantReadWriteLock();

  @SuppressWarnings("serial")
  public static final Set<String> propertyIds = ImmutableSet.<String>builder()
    .add(WIDGETLAYOUT_ID_PROPERTY_ID)
    .add(WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID)
    .add(WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID)
    .add(WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID)
    .add(WIDGETLAYOUT_WIDGETS_PROPERTY_ID)
    .add(WIDGETLAYOUT_SCOPE_PROPERTY_ID)
    .add(WIDGETLAYOUT_USERNAME_PROPERTY_ID)
    .add(WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID)
    .build();

  @SuppressWarnings("serial")
  public static final Map<Type, String> keyPropertyIds = ImmutableMap.<Type, String>builder()
    .put(Type.WidgetLayout, WIDGETLAYOUT_ID_PROPERTY_ID)
    .put(Type.Cluster, WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID)
    .put(Type.User, WIDGETLAYOUT_USERNAME_PROPERTY_ID)
    .build();

  @Inject
  private static WidgetDAO widgetDAO;

  @Inject
  private static WidgetLayoutDAO widgetLayoutDAO;

  /**
   * Create a new resource provider.
   *
   */
  public WidgetLayoutResourceProvider(AmbariManagementController managementController) {
    super(Type.WidgetLayout, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Resource> associatedResources = new HashSet<>();

    for (final Map<String, Object> properties : request.getProperties()) {
      WidgetLayoutEntity widgetLayoutEntity = createResources(new Command<WidgetLayoutEntity>() {

        @Override
        public WidgetLayoutEntity invoke() throws AmbariException {
          final String[] requiredProperties = {
              WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID,
              WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID,
              WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID,
              WIDGETLAYOUT_SCOPE_PROPERTY_ID,
              WIDGETLAYOUT_WIDGETS_PROPERTY_ID,
              WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID,
              WIDGETLAYOUT_USERNAME_PROPERTY_ID
          };
          for (String propertyName : requiredProperties) {
            if (properties.get(propertyName) == null) {
              throw new AmbariException("Property " + propertyName + " should be provided");
            }
          }
          final WidgetLayoutEntity entity = new WidgetLayoutEntity();
          String userName = getUserName(properties);

          Set widgetsSet = (LinkedHashSet) properties.get(WIDGETLAYOUT_WIDGETS_PROPERTY_ID);

          String clusterName = properties.get(WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID).toString();
          entity.setLayoutName(properties.get(WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID).toString());
          entity.setClusterId(getManagementController().getClusters().getCluster(clusterName).getClusterId());
          entity.setSectionName(properties.get(WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID).toString());
          entity.setScope(properties.get(WIDGETLAYOUT_SCOPE_PROPERTY_ID).toString());
          entity.setUserName(userName);
          entity.setDisplayName(properties.get(WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID).toString());

          List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = new LinkedList<>();
          int order=0;
          for (Object widgetObject : widgetsSet) {
            HashMap<String, Object> widget = (HashMap) widgetObject;
            long id = Integer.parseInt(widget.get("id").toString());
            WidgetEntity widgetEntity = widgetDAO.findById(id);
            if (widgetEntity == null) {
              throw new AmbariException("Widget with id " + widget.get("id") + " does not exists");
            }
            WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity = new WidgetLayoutUserWidgetEntity();

            widgetLayoutUserWidgetEntity.setWidget(widgetEntity);
            widgetLayoutUserWidgetEntity.setWidgetOrder(order++);
            widgetLayoutUserWidgetEntity.setWidgetLayout(entity);
            widgetLayoutUserWidgetEntityList.add(widgetLayoutUserWidgetEntity);
            widgetEntity.getListWidgetLayoutUserWidgetEntity().add(widgetLayoutUserWidgetEntity);
          }

          entity.setListWidgetLayoutUserWidgetEntity(widgetLayoutUserWidgetEntityList);
          widgetLayoutDAO.create(entity);
          notifyCreate(Type.WidgetLayout, request);

          return entity;
        }
      });
      Resource resource = new ResourceImpl(Type.WidgetLayout);
      resource.setProperty(WIDGETLAYOUT_ID_PROPERTY_ID, widgetLayoutEntity.getId());
      associatedResources.add(resource);

    }

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    List<WidgetLayoutEntity> layoutEntities = new ArrayList<>();

    for (Map<String, Object> propertyMap: propertyMaps) {
      if (propertyMap.get(WIDGETLAYOUT_ID_PROPERTY_ID) != null) {
        final Long id;
        try {
          id = Long.parseLong(propertyMap.get(WIDGETLAYOUT_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("WidgetLayout should have numerical id");
        }
        final WidgetLayoutEntity entity = widgetLayoutDAO.findById(id);
        if (entity == null) {
          throw new NoSuchResourceException("WidgetLayout with id " + id + " does not exists");
        }
        layoutEntities.add(entity);
      } else {
        layoutEntities.addAll(widgetLayoutDAO.findAll());
      }
    }

    for (WidgetLayoutEntity layoutEntity : layoutEntities) {
      Resource resource = new ResourceImpl(Type.WidgetLayout);
      resource.setProperty(WIDGETLAYOUT_ID_PROPERTY_ID, layoutEntity.getId());
      String clusterName;
      try {
        clusterName = getManagementController().getClusters().getClusterById(layoutEntity.getClusterId()).getClusterName();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage());
      }
      resource.setProperty(WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID, clusterName);
      resource.setProperty(WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID, layoutEntity.getLayoutName());
      resource.setProperty(WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID, layoutEntity.getSectionName());
      resource.setProperty(WIDGETLAYOUT_SCOPE_PROPERTY_ID, layoutEntity.getScope());
      resource.setProperty(WIDGETLAYOUT_USERNAME_PROPERTY_ID, layoutEntity.getUserName());
      resource.setProperty(WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID, layoutEntity.getDisplayName());

      List<HashMap> widgets = new ArrayList<>();
      List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = layoutEntity.getListWidgetLayoutUserWidgetEntity();
      for (WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity : widgetLayoutUserWidgetEntityList) {
        WidgetEntity widgetEntity = widgetLayoutUserWidgetEntity.getWidget();
        HashMap<String, Object> widgetInfoMap = new HashMap<>();
        widgetInfoMap.put("WidgetInfo",WidgetResponse.coerce(widgetEntity));
        widgets.add(widgetInfoMap);
      }
      resource.setProperty(WIDGETLAYOUT_WIDGETS_PROPERTY_ID, widgets);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        for (Map<String, Object> propertyMap : propertyMaps) {
          final Long layoutId;
          try {
            layoutId = Long.parseLong(propertyMap.get(WIDGETLAYOUT_ID_PROPERTY_ID).toString());
          } catch (Exception ex) {
            throw new AmbariException("WidgetLayout should have numerical id");
          }
          lock.writeLock().lock();
          try {
            final WidgetLayoutEntity entity = widgetLayoutDAO.findById(layoutId);
            if (entity == null) {
              throw new ObjectNotFoundException("There is no widget layout with id " + layoutId);
            }
            if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID)))) {
              entity.setLayoutName(propertyMap.get(WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID).toString());
            }
            if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID)))) {
              entity.setSectionName(propertyMap.get(WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID).toString());
            }
            if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID)))) {
              entity.setDisplayName(propertyMap.get(WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID).toString());
            }
            if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(WIDGETLAYOUT_SCOPE_PROPERTY_ID)))) {
              entity.setScope(propertyMap.get(WIDGETLAYOUT_SCOPE_PROPERTY_ID).toString());
            }

            Set widgetsSet = (LinkedHashSet) propertyMap.get(WIDGETLAYOUT_WIDGETS_PROPERTY_ID);

            //Remove old relations from widget entities
            for (WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity : entity.getListWidgetLayoutUserWidgetEntity()) {
              widgetLayoutUserWidgetEntity.getWidget().getListWidgetLayoutUserWidgetEntity()
                      .remove(widgetLayoutUserWidgetEntity);
            }
            entity.setListWidgetLayoutUserWidgetEntity(new LinkedList<>());

            List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = new LinkedList<>();
            int order = 0;
            for (Object widgetObject : widgetsSet) {
              HashMap<String, Object> widget = (HashMap) widgetObject;
              long id = Integer.parseInt(widget.get("id").toString());
              WidgetEntity widgetEntity = widgetDAO.findById(id);
              if (widgetEntity == null) {
                throw new AmbariException("Widget with id " + widget.get("id") + " does not exists");
              }
              WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity = new WidgetLayoutUserWidgetEntity();

              widgetLayoutUserWidgetEntity.setWidget(widgetEntity);
              widgetLayoutUserWidgetEntity.setWidgetOrder(order++);
              widgetLayoutUserWidgetEntity.setWidgetLayout(entity);
              widgetLayoutUserWidgetEntityList.add(widgetLayoutUserWidgetEntity);
              widgetEntity.getListWidgetLayoutUserWidgetEntity().add(widgetLayoutUserWidgetEntity);
              entity.getListWidgetLayoutUserWidgetEntity().add(widgetLayoutUserWidgetEntity);
            }


            widgetLayoutDAO.mergeWithFlush(entity);
          } finally {
            lock.writeLock().unlock();
          }
        }
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    final List<WidgetLayoutEntity> entitiesToBeRemoved = new ArrayList<>();
    for (Map<String, Object> propertyMap : propertyMaps) {
      final Long id;
      try {
        id = Long.parseLong(propertyMap.get(WIDGETLAYOUT_ID_PROPERTY_ID).toString());
      } catch (Exception ex) {
        throw new SystemException("WidgetLayout should have numerical id");
      }

      final WidgetLayoutEntity entity = widgetLayoutDAO.findById(id);
      if (entity == null) {
        throw new NoSuchResourceException("There is no widget layout with id " + id);
      }

      entitiesToBeRemoved.add(entity);
    }

    for (WidgetLayoutEntity entity: entitiesToBeRemoved) {
      if (entity.getListWidgetLayoutUserWidgetEntity() != null) {
        for (WidgetLayoutUserWidgetEntity layoutUserWidgetEntity : entity.getListWidgetLayoutUserWidgetEntity()) {
          if (layoutUserWidgetEntity.getWidget().getListWidgetLayoutUserWidgetEntity() != null) {
            layoutUserWidgetEntity.getWidget().getListWidgetLayoutUserWidgetEntity().remove(layoutUserWidgetEntity);
          }
        }
      }
      widgetLayoutDAO.remove(entity);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private String getUserName(Map<String, Object> properties) {
    if (properties.get(WIDGETLAYOUT_USERNAME_PROPERTY_ID) != null) {
      return properties.get(WIDGETLAYOUT_USERNAME_PROPERTY_ID).toString();
    }
    return getManagementController().getAuthName();
  }
}
