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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
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
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ThemeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ThemeArtifactResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ThemeArtifactResourceProvider.class);

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("ThemeInfo", "stack_name");
  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("ThemeInfo", "stack_version");
  public static final String STACK_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("ThemeInfo", "service_name");
  public static final String THEME_FILE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("ThemeInfo", "file_name");
  public static final String THEME_DEFAULT_PROPERTY_ID = PropertyHelper.getPropertyId("ThemeInfo", "default");
  public static final String THEME_DATA_PROPERTY_ID = PropertyHelper.getPropertyId("ThemeInfo", "theme_data");

  /**
   * primary key fields
   */
  public static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(THEME_FILE_NAME_PROPERTY_ID)
    .build();
  /**
   * map of resource type to fk field
   */
  public static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
    .put(Resource.Type.Theme, THEME_FILE_NAME_PROPERTY_ID)
    .put(Resource.Type.Stack, STACK_NAME_PROPERTY_ID)
    .put(Resource.Type.StackVersion, STACK_VERSION_PROPERTY_ID)
    .put(Resource.Type.StackService, STACK_SERVICE_NAME_PROPERTY_ID)
    .build();

  /**
   * resource properties
   */
  public static final Set<String> propertyIds = ImmutableSet.<String>builder()
    .add(STACK_NAME_PROPERTY_ID)
    .add(STACK_VERSION_PROPERTY_ID)
    .add(STACK_SERVICE_NAME_PROPERTY_ID)
    .add(THEME_FILE_NAME_PROPERTY_ID)
    .add(THEME_DEFAULT_PROPERTY_ID)
    .add(THEME_DATA_PROPERTY_ID)
    .build();

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  protected ThemeArtifactResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Theme, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException("Creating of themes is not supported");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Resource> resources = new LinkedHashSet<>();

    resources.addAll(getThemes(request, predicate));
    // add other artifacts types here

    if (resources.isEmpty()) {
      throw new NoSuchResourceException(
        "The requested resource doesn't exist: Themes not found, " + predicate);
    }

    return resources;

  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException("Updating of themes is not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Deleting of themes is not supported");
  }

  private Set<Resource> getThemes(Request request, Predicate predicate) throws NoSuchParentResourceException,
    NoSuchResourceException, UnsupportedPropertyException, SystemException {

    Set<Resource> resources = new LinkedHashSet<>();
    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      String themeFileName = (String) properties.get(THEME_FILE_NAME_PROPERTY_ID);


      String stackName = (String) properties.get(STACK_NAME_PROPERTY_ID);
      String stackVersion = (String) properties.get(STACK_VERSION_PROPERTY_ID);
      String stackService = (String) properties.get(STACK_SERVICE_NAME_PROPERTY_ID);

      StackInfo stackInfo;
      try {
        stackInfo = getManagementController().getAmbariMetaInfo().getStack(stackName, stackVersion);
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format(
          "Parent stack resource doesn't exist: stackName='%s', stackVersion='%s'", stackName, stackVersion));
      }

      List<ServiceInfo> serviceInfoList = new ArrayList<>();

      if (stackService == null) {
        serviceInfoList.addAll(stackInfo.getServices());
      } else {
        ServiceInfo service = stackInfo.getService(stackService);
        if (service == null) {
          throw new NoSuchParentResourceException(String.format(
            "Parent stack/service resource doesn't exist: stackName='%s', stackVersion='%s', serviceName='%s'",
            stackName, stackVersion, stackService));
        }
        serviceInfoList.add(service);
      }

      for (ServiceInfo serviceInfo : serviceInfoList) {
        List<ThemeInfo> serviceThemes = new ArrayList<>();
        if (themeFileName != null) {
          LOG.debug("Getting themes from service {}, themes = {}", serviceInfo.getName(), serviceInfo.getThemesMap());
          serviceThemes.add(serviceInfo.getThemesMap().get(themeFileName));
        } else {
          for (ThemeInfo themeInfo : serviceInfo.getThemesMap().values()) {
            if (themeInfo.getIsDefault()) {
              serviceThemes.add(0, themeInfo);
            } else {
              serviceThemes.add(themeInfo);
            }
          }
        }

        List<Resource> serviceResources = new ArrayList<>();
        for (ThemeInfo themeInfo : serviceThemes) {
          Resource resource = new ResourceImpl(Resource.Type.Theme);
          Set<String> requestedIds = getRequestPropertyIds(request, predicate);
          setResourceProperty(resource, THEME_FILE_NAME_PROPERTY_ID, themeInfo.getFileName(), requestedIds);
          setResourceProperty(resource, THEME_DEFAULT_PROPERTY_ID, themeInfo.getIsDefault(), requestedIds);
          setResourceProperty(resource, THEME_DATA_PROPERTY_ID, themeInfo.getThemeMap(), requestedIds);
          setResourceProperty(resource, STACK_NAME_PROPERTY_ID, stackName, requestedIds);
          setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, stackVersion, requestedIds);
          setResourceProperty(resource, STACK_SERVICE_NAME_PROPERTY_ID, serviceInfo.getName(), requestedIds);
          serviceResources.add(resource);
        }

        resources.addAll(serviceResources);

      }
    }
    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return null;
  }
}
