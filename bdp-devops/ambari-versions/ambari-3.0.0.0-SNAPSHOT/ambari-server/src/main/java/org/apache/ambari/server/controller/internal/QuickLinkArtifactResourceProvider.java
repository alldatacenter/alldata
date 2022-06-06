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
import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.quicklinks.Link;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinkVisibilityController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class QuickLinkArtifactResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(QuickLinkArtifactResourceProvider.class);

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("QuickLinkInfo", "stack_name");
  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("QuickLinkInfo", "stack_version");
  public static final String STACK_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("QuickLinkInfo", "service_name");
  public static final String QUICKLINK_DEFAULT_PROPERTY_ID = PropertyHelper.getPropertyId("QuickLinkInfo", "default");
  public static final String QUICKLINK_FILE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("QuickLinkInfo", "file_name");
  public static final String QUICKLINK_DATA_PROPERTY_ID = PropertyHelper.getPropertyId("QuickLinkInfo", "quicklink_data");

  /**
   * primary key fields
   */
  public static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(QUICKLINK_FILE_NAME_PROPERTY_ID)
    .build();

  /**
   * map of resource type to fk field
   */
  public static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
    .put(Resource.Type.QuickLink, QUICKLINK_FILE_NAME_PROPERTY_ID)
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
    .add(QUICKLINK_FILE_NAME_PROPERTY_ID)
    .add(QUICKLINK_DATA_PROPERTY_ID)
    .add(QUICKLINK_DEFAULT_PROPERTY_ID)
    .build();

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  protected QuickLinkArtifactResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.QuickLink, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException("Creating of quick links is not supported");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Resource> resources = new LinkedHashSet<>();

    resources.addAll(getQuickLinks(request, predicate));
    // add other artifacts types here

    if (resources.isEmpty()) {
      throw new NoSuchResourceException(
        "The requested resource doesn't exist: QuickLink not found, " + predicate);
    }

    return resources;

  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException("Updating of quick links is not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Deleting of quick links is not supported");
  }

  private Set<Resource> getQuickLinks(Request request, Predicate predicate) throws NoSuchParentResourceException,
    NoSuchResourceException, UnsupportedPropertyException, SystemException {

    Set<Resource> resources = new LinkedHashSet<>();
    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      String quickLinksFileName = (String) properties.get(QUICKLINK_FILE_NAME_PROPERTY_ID);
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
        List<QuickLinksConfigurationInfo> serviceQuickLinks = new ArrayList<>();
        if (quickLinksFileName != null) {
          LOG.debug("Getting quick links from service {}, quick links = {}", serviceInfo.getName(), serviceInfo.getQuickLinksConfigurationsMap());
          serviceQuickLinks.add(serviceInfo.getQuickLinksConfigurationsMap().get(quickLinksFileName));
        } else {
          for (QuickLinksConfigurationInfo quickLinksConfigurationInfo : serviceInfo.getQuickLinksConfigurationsMap().values()) {
            if (quickLinksConfigurationInfo.getIsDefault()) {
              serviceQuickLinks.add(0, quickLinksConfigurationInfo );
            } else {
              serviceQuickLinks.add(quickLinksConfigurationInfo );
            }
          }
        }

        setVisibilityAndOverrides(serviceInfo.getName(), serviceQuickLinks);

        List<Resource> serviceResources = new ArrayList<>();
        for (QuickLinksConfigurationInfo quickLinksConfigurationInfo : serviceQuickLinks) {
          Resource resource = new ResourceImpl(Resource.Type.QuickLink);
          Set<String> requestedIds = getRequestPropertyIds(request, predicate);
          setResourceProperty(resource, QUICKLINK_FILE_NAME_PROPERTY_ID, quickLinksConfigurationInfo.getFileName(), requestedIds);
          setResourceProperty(resource, QUICKLINK_DATA_PROPERTY_ID, quickLinksConfigurationInfo.getQuickLinksConfigurationMap(), requestedIds);
          setResourceProperty(resource, QUICKLINK_DEFAULT_PROPERTY_ID, quickLinksConfigurationInfo.getIsDefault(), requestedIds);
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

  /**
   * Sets the visibility flag of the links based on the actual quicklinks profile (if exists).
   * @param serviceName the name of the service
   * @param serviceQuickLinks the links
   */
  private void setVisibilityAndOverrides(String serviceName, List<QuickLinksConfigurationInfo> serviceQuickLinks) {
    QuickLinkVisibilityController visibilityController = getManagementController().getQuicklinkVisibilityController();

    for(QuickLinksConfigurationInfo configurationInfo: serviceQuickLinks) {
      for (QuickLinks links: configurationInfo.getQuickLinksConfigurationMap().values()) {
        for(Link link: links.getQuickLinksConfiguration().getLinks()) {
          link.setVisible(visibilityController.isVisible(serviceName, link));
          visibilityController.getUrlOverride(serviceName, link).ifPresent(link::setUrl);
        }
      }
    }
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return null;
  }
}
