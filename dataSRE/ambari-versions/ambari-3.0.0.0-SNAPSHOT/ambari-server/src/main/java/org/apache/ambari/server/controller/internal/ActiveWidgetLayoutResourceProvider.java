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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.ActiveWidgetLayoutResponse;
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
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

/**
 * Resource provider for widget layout resources.
 */
@StaticallyInject
public class ActiveWidgetLayoutResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String WIDGETLAYOUT_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("WidgetLayoutInfo", "id");
  public static final String WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID                 = PropertyHelper.getPropertyId("WidgetLayoutInfo", "cluster_name");
  public static final String WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID                 = PropertyHelper.getPropertyId("WidgetLayoutInfo", "section_name");
  public static final String WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID                 = PropertyHelper.getPropertyId("WidgetLayoutInfo", "layout_name");
  public static final String WIDGETLAYOUT_SCOPE_PROPERTY_ID                  = PropertyHelper.getPropertyId("WidgetLayoutInfo", "scope");
  public static final String WIDGETLAYOUT_WIDGETS_PROPERTY_ID                   = PropertyHelper.getPropertyId("WidgetLayoutInfo", "widgets");
  public static final String WIDGETLAYOUT_USERNAME_PROPERTY_ID                   = PropertyHelper.getPropertyId("WidgetLayoutInfo", "user_name");
  public static final String WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID                   = PropertyHelper.getPropertyId("WidgetLayoutInfo", "display_name");
  public static final String WIDGETLAYOUT = "WidgetLayouts";
  public static final String ID = "id";

  @SuppressWarnings("serial")
  private static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(WIDGETLAYOUT_ID_PROPERTY_ID)
    .build();

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
    .add(WIDGETLAYOUT)
    .build();

  @SuppressWarnings("serial")
  public static final Map<Type, String> keyPropertyIds = ImmutableMap.<Type, String>builder()
    .put(Type.ActiveWidgetLayout, WIDGETLAYOUT_ID_PROPERTY_ID)
    .put(Type.User, WIDGETLAYOUT_USERNAME_PROPERTY_ID)
    .build();

  @Inject
  private static UserDAO userDAO;

  @Inject
  private static WidgetDAO widgetDAO;

  @Inject
  private static WidgetLayoutDAO widgetLayoutDAO;

  @Inject
  private static Gson gson;

  /**
   * For testing purposes
   */
  public static void init(UserDAO userDAO, WidgetDAO widgetDAO, WidgetLayoutDAO widgetLayoutDAO, Gson gson){
    ActiveWidgetLayoutResourceProvider.userDAO = userDAO;
    ActiveWidgetLayoutResourceProvider.widgetDAO = widgetDAO;
    ActiveWidgetLayoutResourceProvider.widgetLayoutDAO = widgetLayoutDAO;
    ActiveWidgetLayoutResourceProvider.gson = gson;
  }

  /**
   * Create a new resource provider.
   *
   */
  public ActiveWidgetLayoutResourceProvider(AmbariManagementController managementController) {
    super(Type.ActiveWidgetLayout, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    throw new SystemException("The request is not supported");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    List<WidgetLayoutEntity> layoutEntities = new ArrayList<>();

    boolean isUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_USERS);

    for (Map<String, Object> propertyMap: propertyMaps) {
      final String userName = getUserName(propertyMap);

      // Ensure that the authenticated user has authorization to get this information
      if (!isUserAdministrator && !AuthorizationHelper.getAuthenticatedName().equalsIgnoreCase(userName)) {
        throw new AuthorizationException();
      }

      java.lang.reflect.Type type = new TypeToken<Set<Map<String, String>>>(){}.getType();
        Set<Map<String, String>> activeWidgetLayouts = gson.fromJson(userDAO.findUserByName(userName).getActiveWidgetLayouts(), type);
        if (activeWidgetLayouts != null) {
          for (Map<String, String> widgetLayoutId : activeWidgetLayouts) {
            layoutEntities.add(widgetLayoutDAO.findById(Long.parseLong(widgetLayoutId.get(ID))));
          }
        }
    }

    for (WidgetLayoutEntity layoutEntity : layoutEntities) {
      ActiveWidgetLayoutResponse activeWidgetLayoutResponse  = getResponse(layoutEntity);
      Resource resource = new ResourceImpl(Type.ActiveWidgetLayout);
      resource.setProperty(WIDGETLAYOUT_ID_PROPERTY_ID, activeWidgetLayoutResponse.getId());
      resource.setProperty(WIDGETLAYOUT_CLUSTER_NAME_PROPERTY_ID, activeWidgetLayoutResponse.getClusterName());
      resource.setProperty(WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID, activeWidgetLayoutResponse.getLayoutName());
      resource.setProperty(WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID, activeWidgetLayoutResponse.getSectionName());
      resource.setProperty(WIDGETLAYOUT_SCOPE_PROPERTY_ID, activeWidgetLayoutResponse.getScope());
      resource.setProperty(WIDGETLAYOUT_USERNAME_PROPERTY_ID, activeWidgetLayoutResponse.getUserName());
      resource.setProperty(WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID, activeWidgetLayoutResponse.getDisplayName());
      resource.setProperty(WIDGETLAYOUT_WIDGETS_PROPERTY_ID, activeWidgetLayoutResponse.getWidgets());
      resources.add(resource);
    }
    return resources;
  }

  /**
   * Returns the response for the active widget layout that should be returned for the active widget layout REST endpoint
   * @param layoutEntity {@link WidgetLayoutEntity}
   * @return  {@link ActiveWidgetLayoutResponse}
   * @throws SystemException
   */
  private ActiveWidgetLayoutResponse getResponse(WidgetLayoutEntity layoutEntity) throws SystemException {
    String clusterName = null;
    try {
      clusterName = getManagementController().getClusters().getClusterById(layoutEntity.getClusterId()).getClusterName();
    } catch (AmbariException e) {
      throw new SystemException(e.getMessage());
    }
    List<HashMap<String,WidgetResponse>> widgets = new ArrayList<>();
    List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = layoutEntity.getListWidgetLayoutUserWidgetEntity();
    for (WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity : widgetLayoutUserWidgetEntityList) {
      WidgetEntity widgetEntity = widgetLayoutUserWidgetEntity.getWidget();
      HashMap<String, WidgetResponse> widgetInfoMap = new HashMap<>();
      widgetInfoMap.put("WidgetInfo",WidgetResponse.coerce(widgetEntity));
      widgets.add(widgetInfoMap);
    }
   return  new ActiveWidgetLayoutResponse(layoutEntity.getId(), clusterName, layoutEntity.getDisplayName(), layoutEntity.getLayoutName(),
      layoutEntity.getSectionName(), layoutEntity.getScope(), layoutEntity.getUserName(),  widgets);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        boolean isUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
            RoleAuthorization.AMBARI_MANAGE_USERS);

        for (Map<String, Object> propertyMap : propertyMaps) {
          // Ensure that the authenticated user has authorization to get this information
          final String userName = getUserName(propertyMap);
          if (!isUserAdministrator && !AuthorizationHelper.getAuthenticatedName().equalsIgnoreCase(userName)) {
            throw new AuthorizationException();
          }

          Set<HashMap<String, String>> widgetLayouts = (Set) propertyMap.get(WIDGETLAYOUT);
          for (HashMap<String, String> widgetLayout : widgetLayouts) {
            final Long layoutId;
            try {
              layoutId = Long.parseLong(widgetLayout.get(ID));
            } catch (Exception ex) {
              throw new AmbariException("WidgetLayout should have numerical id");
            }
            final WidgetLayoutEntity entity = widgetLayoutDAO.findById(layoutId);
            if (entity == null) {
              throw new AmbariException("There is no widget layout with id " + layoutId);
            }
          }
          UserEntity user = userDAO.findUserByName(userName);
          user.setActiveWidgetLayouts(gson.toJson(propertyMap.get(WIDGETLAYOUT)));
          userDAO.merge(user);
        }
        return null;
      }
    });

    return getRequestStatus(null);
  }

  private String getUserName(Map<String, Object> propertyMap) {
    String userName = propertyMap.get(WIDGETLAYOUT_USERNAME_PROPERTY_ID) == null ? "" : propertyMap.get(WIDGETLAYOUT_USERNAME_PROPERTY_ID).toString();
    if (StringUtils.isBlank(userName)) {
      userName = AuthorizationHelper.getAuthenticatedName();
    }
    return userName;
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("The request is not supported");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
