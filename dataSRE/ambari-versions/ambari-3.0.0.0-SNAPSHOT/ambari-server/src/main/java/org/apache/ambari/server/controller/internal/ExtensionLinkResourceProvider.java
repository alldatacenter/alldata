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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExtensionLinkRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
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
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@StaticallyInject
public class ExtensionLinkResourceProvider extends AbstractControllerResourceProvider {

  public static final String LINK_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("ExtensionLink", "link_id");

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper
	      .getPropertyId("ExtensionLink", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("ExtensionLink", "stack_version");

  public static final String EXTENSION_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("ExtensionLink", "extension_name");

  public static final String EXTENSION_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("ExtensionLink", "extension_version");

  /**
   * The key property ids for a ExtensionLink resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.ExtensionLink, LINK_ID_PROPERTY_ID)
      .put(Type.Stack, STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, STACK_VERSION_PROPERTY_ID)
      .put(Type.Extension, EXTENSION_NAME_PROPERTY_ID)
      .put(Type.ExtensionVersion, EXTENSION_VERSION_PROPERTY_ID)
      .build();

  /**
   * The property ids for a ExtensionLink resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      LINK_ID_PROPERTY_ID,
      STACK_NAME_PROPERTY_ID,
      STACK_VERSION_PROPERTY_ID,
      EXTENSION_NAME_PROPERTY_ID,
      EXTENSION_VERSION_PROPERTY_ID);

  @Inject
  private static ExtensionLinkDAO dao;

  protected ExtensionLinkResourceProvider(AmbariManagementController managementController) {
    super(Type.ExtensionLink, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request)
	        throws SystemException, UnsupportedPropertyException,
	        NoSuchParentResourceException, ResourceAlreadyExistsException {

    final Set<ExtensionLinkRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        for (ExtensionLinkRequest extensionLinkRequest : requests) {
          getManagementController().createExtensionLink(extensionLinkRequest);
        }
        return null;
      }
    });

    if (requests.size() > 0) {
      //Need to reread the stacks/extensions directories so the latest information is available
      try {
        getManagementController().updateStacks();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }

      notifyCreate(Resource.Type.ExtensionLink, request);
    }

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException,
        NoSuchResourceException, NoSuchParentResourceException {

    final Set<ExtensionLinkRequest> requests = new HashSet<>();
    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        for (ExtensionLinkRequest extensionLinkRequest : requests) {
          getManagementController().deleteExtensionLink(extensionLinkRequest);
        }
        return null;
      }
    });

    //Need to reread the stacks/extensions directories so the latest information is available
    try {
      getManagementController().updateStacks();
    } catch (AmbariException e) {
      throw new SystemException(e.getMessage(), e);
    }

    notifyDelete(Resource.Type.ExtensionLink, predicate);

    return getRequestStatus(response);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException,
        NoSuchResourceException, NoSuchParentResourceException {

    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    final Set<ExtensionLinkRequest> requests = new HashSet<>();
    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<ExtensionLinkEntity> entities = new HashSet<>();

    for (ExtensionLinkRequest extensionLinkRequest : requests) {
      verifyStackAndExtensionExist(extensionLinkRequest);
      entities.addAll(dao.find(extensionLinkRequest));
    }

    for (ExtensionLinkEntity entity : entities) {
      Resource resource = new ResourceImpl(Resource.Type.ExtensionLink);
      setResourceProperty(resource, LINK_ID_PROPERTY_ID,
		  entity.getLinkId(), requestedIds);
      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
	  entity.getStack().getStackName(), requestedIds);
      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          entity.getStack().getStackVersion(), requestedIds);
      setResourceProperty(resource, EXTENSION_NAME_PROPERTY_ID,
          entity.getExtension().getExtensionName(), requestedIds);
      setResourceProperty(resource, EXTENSION_VERSION_PROPERTY_ID,
          entity.getExtension().getExtensionVersion(), requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException,
        NoSuchResourceException, NoSuchParentResourceException {

    final Set<ExtensionLinkRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        for (ExtensionLinkRequest extensionLinkRequest : requests) {
          getManagementController().updateExtensionLink(extensionLinkRequest);
        }
        return null;
      }
    });

    //Need to reread the stacks/extensions directories so the latest information is available
    try {
      getManagementController().updateStacks();
    } catch (AmbariException e) {
      throw new SystemException(e.getMessage(), e);
    }

    notifyUpdate(Resource.Type.ExtensionLink, request, predicate);
    return getRequestStatus(null);
  }

  private void verifyStackAndExtensionExist(ExtensionLinkRequest request) throws NoSuchParentResourceException {
    try {
      if (request.getStackName() != null && request.getStackVersion() != null) {
        getManagementController().getAmbariMetaInfo().getStack(request.getStackName(), request.getStackVersion());
      }
      if (request.getExtensionName() != null && request.getExtensionVersion() != null) {
        getManagementController().getAmbariMetaInfo().getExtension(request.getExtensionName(), request.getExtensionVersion());
      }
    }
    catch (AmbariException ambariException) {
      throw new NoSuchParentResourceException(ambariException.getMessage());
    }
  }

  private ExtensionLinkRequest getRequest(Map<String, Object> properties) {
    return new ExtensionLinkRequest(
        (String) properties.get(LINK_ID_PROPERTY_ID),
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(EXTENSION_NAME_PROPERTY_ID),
        (String) properties.get(EXTENSION_VERSION_PROPERTY_ID));
  }

  private ExtensionLinkRequest createExtensionLinkRequest(ExtensionLinkEntity entity) {
    if (entity == null) {
      return null;
    }

    return new ExtensionLinkRequest(String.valueOf(entity.getLinkId()),
        entity.getStack().getStackName(),
        entity.getStack().getStackVersion(),
        entity.getExtension().getExtensionName(),
        entity.getExtension().getExtensionVersion());
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }
}
