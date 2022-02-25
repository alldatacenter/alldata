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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.ActionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
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
import org.apache.ambari.server.customactions.ActionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class ActionResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ActionResourceProvider.class);

  public static final String ACTION_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "action_name");
  public static final String ACTION_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "action_type");
  public static final String INPUTS_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "inputs");
  public static final String TARGET_SERVICE_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "target_service");
  public static final String TARGET_COMPONENT_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "target_component");
  public static final String DESCRIPTION_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "description");
  public static final String TARGET_HOST_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "target_type");
  public static final String DEFAULT_TIMEOUT_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "default_timeout");

  /**
   * The key property ids for a Action resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Action, ACTION_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Action resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      ACTION_NAME_PROPERTY_ID,
      ACTION_TYPE_PROPERTY_ID,
      INPUTS_PROPERTY_ID,
      TARGET_SERVICE_PROPERTY_ID,
      TARGET_COMPONENT_PROPERTY_ID,
      DESCRIPTION_PROPERTY_ID,
      TARGET_HOST_PROPERTY_ID,
      DEFAULT_TIMEOUT_PROPERTY_ID);

  public ActionResourceProvider(AmbariManagementController managementController) {
    super(Type.Action, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    throw new UnsupportedOperationException("Not currently supported.");

  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new UnsupportedOperationException("Not currently supported.");

  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<ActionRequest> requests = new HashSet<>();
    if (predicate != null) {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        ActionRequest actionReq = getRequest(propertyMap);
        LOG.debug("Received a get request for Action with, actionName = {}", actionReq.getActionName());
        requests.add(actionReq);
      }
    } else {
      LOG.debug("Received a get request for all Actions");
      requests.add(ActionRequest.getAllRequest());
    }

    Set<ActionResponse> responses = getResources(new Command<Set<ActionResponse>>() {
      @Override
      public Set<ActionResponse> invoke() throws AmbariException {
        return getActionDefinitions(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    for (ActionResponse response : responses) {
      Resource resource = new ResourceImpl(Type.Action);
      setResourceProperty(resource, ACTION_NAME_PROPERTY_ID,
          response.getActionName(), requestedIds);
      setResourceProperty(resource, ACTION_TYPE_PROPERTY_ID,
          response.getActionType(), requestedIds);
      setResourceProperty(resource, INPUTS_PROPERTY_ID,
          response.getInputs(), requestedIds);
      setResourceProperty(resource, TARGET_SERVICE_PROPERTY_ID,
          response.getTargetService(), requestedIds);
      setResourceProperty(resource, TARGET_COMPONENT_PROPERTY_ID,
          response.getTargetComponent(), requestedIds);
      setResourceProperty(resource, DESCRIPTION_PROPERTY_ID,
          response.getDescription(), requestedIds);
      setResourceProperty(resource, TARGET_HOST_PROPERTY_ID,
          response.getTargetType(), requestedIds);
      setResourceProperty(resource, DEFAULT_TIMEOUT_PROPERTY_ID,
          response.getDefaultTimeout(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    throw new UnsupportedOperationException("Not currently supported.");
  }

  private ActionRequest getRequest(Map<String, Object> properties) {
    ActionRequest ar = new ActionRequest(
        (String) properties.get(ACTION_NAME_PROPERTY_ID),
        (String) properties.get(ACTION_TYPE_PROPERTY_ID),
        (String) properties.get(INPUTS_PROPERTY_ID),
        (String) properties.get(TARGET_SERVICE_PROPERTY_ID),
        (String) properties.get(TARGET_COMPONENT_PROPERTY_ID),
        (String) properties.get(DESCRIPTION_PROPERTY_ID),
        (String) properties.get(TARGET_HOST_PROPERTY_ID),
        (String) properties.get(DEFAULT_TIMEOUT_PROPERTY_ID));

    return ar;
  }

  @Override
  public Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  private ActionManager getActionManager() {
    return getManagementController().getActionManager();
  }

  private AmbariMetaInfo getAmbariMetaInfo() {
    return getManagementController().getAmbariMetaInfo();
  }

  protected synchronized Set<ActionResponse> getActionDefinitions(Set<ActionRequest> requests)
      throws AmbariException {
    Set<ActionResponse> responses = new HashSet<>();
    for (ActionRequest request : requests) {
      if (request.getActionName() == null) {
        List<ActionDefinition> ads = getAmbariMetaInfo().getAllActionDefinition();
        for (ActionDefinition ad : ads) {
          responses.add(ad.convertToResponse());
        }
      } else {
        ActionDefinition ad = getAmbariMetaInfo().getActionDefinition(request.getActionName());
        if (ad != null) {
          responses.add(ad.convertToResponse());
        }
      }
    }

    return responses;
  }
}
