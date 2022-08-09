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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequestException;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse.ValidationItem;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class ValidationResourceProvider extends StackAdvisorResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ValidationResourceProvider.class);

  protected static final String VALIDATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId(
      "Validation", "id");
  protected static final String VALIDATE_PROPERTY_ID = "validate";

  protected static final String ITEMS_PROPERTY_ID = "items";
  protected static final String TYPE_PROPERTY_ID = "type";
  protected static final String LEVE_PROPERTY_ID = "level";
  protected static final String MESSAGE_PROPERTY_ID = "message";
  protected static final String COMPONENT_NAME_PROPERTY_ID = "component-name";
  protected static final String HOST_PROPERTY_ID = "host";
  protected static final String CONFIG_TYPE_PROPERTY_ID = "config-type";
  protected static final String CONFIG_NAME_PROPERTY_ID = "config-name";
  protected static final String HOST_GROUP_PROPERTY_ID = "host-group";
  protected static final String HOSTS_PROPERTY_ID = "hosts";
  protected static final String SERVICES_PROPERTY_ID = "services";
  protected static final String RECOMMENDATIONS_PROPERTY_ID = "recommendations";

  protected static final String ITEMS_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, TYPE_PROPERTY_ID);
  protected static final String ITEMS_LEVE_PROPERTY_ID =  PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, LEVE_PROPERTY_ID);
  protected static final String ITEMS_MESSAGE_PROPERTY_ID =  PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, MESSAGE_PROPERTY_ID);
  protected static final String ITEMS_COMPONENT_NAME_PROPERTY_ID =  PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, COMPONENT_NAME_PROPERTY_ID);
  protected static final String ITEMS_HOST_PROPERTY_ID =  PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, HOST_PROPERTY_ID);
  protected static final String ITEMS_CONFIG_TYPE_PROPERTY_ID =  PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, CONFIG_TYPE_PROPERTY_ID);
  protected static final String ITEMS_CONFIG_NAME_PROPERTY_ID =  PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, CONFIG_NAME_PROPERTY_ID);
  protected static final String ITEMS_HOST_GROUP_PROPERTY_ID = PropertyHelper.getPropertyId(ITEMS_PROPERTY_ID, HOST_GROUP_PROPERTY_ID);

  /**
   * The key property ids for a Validation resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Validation, VALIDATION_ID_PROPERTY_ID)
      .put(Type.Stack, STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, STACK_VERSION_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Validation resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      VALIDATION_ID_PROPERTY_ID,
      VALIDATE_PROPERTY_ID,
      ITEMS_PROPERTY_ID,
      STACK_NAME_PROPERTY_ID,
      STACK_VERSION_PROPERTY_ID,
      ITEMS_TYPE_PROPERTY_ID,
      ITEMS_LEVE_PROPERTY_ID,
      ITEMS_MESSAGE_PROPERTY_ID,
      ITEMS_COMPONENT_NAME_PROPERTY_ID,
      ITEMS_HOST_PROPERTY_ID,
      ITEMS_CONFIG_TYPE_PROPERTY_ID,
      ITEMS_CONFIG_NAME_PROPERTY_ID,
      ITEMS_HOST_GROUP_PROPERTY_ID,
      HOSTS_PROPERTY_ID,
      SERVICES_PROPERTY_ID,
      RECOMMENDATIONS_PROPERTY_ID);

  protected ValidationResourceProvider(AmbariManagementController managementController) {
    super(Type.Validation, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  protected String getRequestTypePropertyId() {
    return VALIDATE_PROPERTY_ID;
  }

  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    StackAdvisorRequest validationRequest = prepareStackAdvisorRequest(request);

    final ValidationResponse response;
    try {
      response = saHelper.validate(validationRequest);
    } catch (StackAdvisorRequestException e) {
      LOG.warn("Error occurred during validation", e);
      throw new IllegalArgumentException(e.getMessage(), e);
    } catch (StackAdvisorException e) {
      LOG.warn("Error occurred during validation", e);
      throw new SystemException(e.getMessage(), e);
    }

    Resource validation = createResources(new Command<Resource>() {
      @Override
      public Resource invoke() throws AmbariException {

        Resource resource = new ResourceImpl(Resource.Type.Validation);
        setResourceProperty(resource, VALIDATION_ID_PROPERTY_ID, response.getId(), getPropertyIds());
        setResourceProperty(resource, STACK_NAME_PROPERTY_ID, response.getVersion().getStackName(), getPropertyIds());
        setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, response.getVersion().getStackVersion(), getPropertyIds());

        List<Map<String, Object>> listItemProps = new ArrayList<>();

        Set<ValidationItem> items = response.getItems();
        for (ValidationItem item : items) {
          Map<String, Object> mapItemProps = new HashMap<>();
          mapItemProps.put(TYPE_PROPERTY_ID, item.getType());
          mapItemProps.put(LEVE_PROPERTY_ID, item.getLevel());
          mapItemProps.put(MESSAGE_PROPERTY_ID, item.getMessage());

          if (item.getComponentName() != null) {
            mapItemProps.put(COMPONENT_NAME_PROPERTY_ID, item.getComponentName());
          }
          if (item.getHost() != null) {
            mapItemProps.put(HOST_PROPERTY_ID, item.getHost());
          }
          if (item.getConfigType() != null) {
            mapItemProps.put(CONFIG_TYPE_PROPERTY_ID, item.getConfigType());
            mapItemProps.put(CONFIG_NAME_PROPERTY_ID, item.getConfigName());
          }
          listItemProps.add(mapItemProps);
        }
        setResourceProperty(resource, ITEMS_PROPERTY_ID, listItemProps, getPropertyIds());

        return resource;
      }
    });
    notifyCreate(Resource.Type.Validation, request);

    Set<Resource> resources = new HashSet<>(Arrays.asList(validation));
    return new RequestStatusImpl(null, resources);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
