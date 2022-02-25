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
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;


@StaticallyInject
public class StackServiceResourceProvider extends ReadOnlyResourceProvider {

  protected static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "service_name");

  protected static final String SERVICE_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId(
		  "StackServices", "service_type");

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "stack_version");

  private static final String SERVICE_DISPLAY_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "display_name");

  private static final String USER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "user_name");

  private static final String COMMENTS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "comments");

  private static final String SELECTION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "selection");

  private static final String MAINTAINER_PROPERTY_ID = PropertyHelper.getPropertyId(
          "StackServices", "maintainer");

  private static final String VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "service_version");

  private static final String CONFIG_TYPES = PropertyHelper.getPropertyId(
      "StackServices", "config_types");

  private static final String REQUIRED_SERVICES_ID = PropertyHelper.getPropertyId(
      "StackServices", "required_services");

  private static final String SERVICE_CHECK_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "service_check_supported");

  private static final String CUSTOM_COMMANDS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "custom_commands");

  private static final String SERVICE_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "properties");

  private static final String CREDENTIAL_STORE_SUPPORTED = PropertyHelper.getPropertyId(
      "StackServices", "credential_store_supported");

  private static final String CREDENTIAL_STORE_REQUIRED = PropertyHelper.getPropertyId(
      "StackServices", "credential_store_required");

  private static final String CREDENTIAL_STORE_ENABLED = PropertyHelper.getPropertyId(
      "StackServices", "credential_store_enabled");

  private static final String SUPPORT_DELETE_VIA_UI = PropertyHelper.getPropertyId(
      "StackServices", "support_delete_via_ui");

  private static final String SSO_INTEGRATION_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
    "StackServices", "sso_integration_supported");

  private static final String SSO_INTEGRATION_REQUIRES_KERBEROS_PROPERTY_ID = PropertyHelper.getPropertyId(
    "StackServices", "sso_integration_requires_kerberos");

  private static final String LDAP_INTEGRATION_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "ldap_integration_supported");

  private static final String ROLLING_RESTART_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServices", "rolling_restart_supported");

  /**
   * The key property ids for a StackVersion resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Stack, STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, STACK_VERSION_PROPERTY_ID)
      .put(Type.StackService, SERVICE_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a StackVersion resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      SERVICE_NAME_PROPERTY_ID,
      SERVICE_TYPE_PROPERTY_ID,
      STACK_NAME_PROPERTY_ID,
      STACK_VERSION_PROPERTY_ID,
      SERVICE_DISPLAY_NAME_PROPERTY_ID,
      USER_NAME_PROPERTY_ID,
      COMMENTS_PROPERTY_ID,
      SELECTION_PROPERTY_ID,
      MAINTAINER_PROPERTY_ID,
      VERSION_PROPERTY_ID,
      CONFIG_TYPES,
      REQUIRED_SERVICES_ID,
      SERVICE_CHECK_SUPPORTED_PROPERTY_ID,
      CUSTOM_COMMANDS_PROPERTY_ID,
      SERVICE_PROPERTIES_PROPERTY_ID,
      CREDENTIAL_STORE_SUPPORTED,
      CREDENTIAL_STORE_REQUIRED,
      CREDENTIAL_STORE_ENABLED,
      SUPPORT_DELETE_VIA_UI,
      SSO_INTEGRATION_SUPPORTED_PROPERTY_ID,
      SSO_INTEGRATION_REQUIRES_KERBEROS_PROPERTY_ID,
      LDAP_INTEGRATION_SUPPORTED_PROPERTY_ID,
      ROLLING_RESTART_SUPPORTED_PROPERTY_ID);

  /**
   * KerberosServiceDescriptorFactory used to create KerberosServiceDescriptor instances
   */
  @Inject
  private static KerberosServiceDescriptorFactory kerberosServiceDescriptorFactory;

  protected StackServiceResourceProvider(AmbariManagementController managementController) {
    super(Type.StackService, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
    NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackServiceRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackServiceResponse> responses = getResources(new Command<Set<StackServiceResponse>>() {
      @Override
      public Set<StackServiceResponse> invoke() throws AmbariException {
        return getManagementController().getStackServices(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (StackServiceResponse response : responses) {
      Resource resource = createResource(response, requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  private Resource createResource(StackServiceResponse response, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Type.StackService);

    setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
        response.getStackName(), requestedIds);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);

      setResourceProperty(resource, SERVICE_TYPE_PROPERTY_ID,
		  response.getServiceType(), requestedIds);

    setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
        response.getStackVersion(), requestedIds);

    setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
        response.getServiceName(), requestedIds);

    setResourceProperty(resource, SERVICE_DISPLAY_NAME_PROPERTY_ID,
        response.getServiceDisplayName(), requestedIds);

    setResourceProperty(resource, USER_NAME_PROPERTY_ID,
        response.getUserName(), requestedIds);

    setResourceProperty(resource, COMMENTS_PROPERTY_ID,
        response.getComments(), requestedIds);

    setResourceProperty(resource, VERSION_PROPERTY_ID,
        response.getServiceVersion(), requestedIds);

    setResourceProperty(resource, SELECTION_PROPERTY_ID,
        response.getSelection(), requestedIds);

    setResourceProperty(resource, MAINTAINER_PROPERTY_ID,
        response.getMaintainer(), requestedIds);

    setResourceProperty(resource, CONFIG_TYPES,
        response.getConfigTypes(), requestedIds);

    setResourceProperty(resource, REQUIRED_SERVICES_ID,
        response.getRequiredServices(), requestedIds);

    setResourceProperty(resource, SERVICE_CHECK_SUPPORTED_PROPERTY_ID,
        response.isServiceCheckSupported(), requestedIds);

    setResourceProperty(resource, CUSTOM_COMMANDS_PROPERTY_ID,
        response.getCustomCommands(), requestedIds);

    setResourceProperty(resource, SERVICE_PROPERTIES_PROPERTY_ID,
        response.getServiceProperties(), requestedIds);

    setResourceProperty(resource, CREDENTIAL_STORE_SUPPORTED,
        response.isCredentialStoreSupported(), requestedIds);

    setResourceProperty(resource, CREDENTIAL_STORE_REQUIRED,
        response.isCredentialStoreRequired(), requestedIds);

    setResourceProperty(resource, CREDENTIAL_STORE_ENABLED,
        response.isCredentialStoreEnabled(), requestedIds);

    setResourceProperty(resource, SUPPORT_DELETE_VIA_UI,
        response.isSupportDeleteViaUI(), requestedIds);

    setResourceProperty(resource, ROLLING_RESTART_SUPPORTED_PROPERTY_ID,
        response.isRollingRestartSupported(),  requestedIds);


    setResourceProperty(resource, SSO_INTEGRATION_SUPPORTED_PROPERTY_ID, response.isSsoIntegrationSupported(), requestedIds);
    setResourceProperty(resource, SSO_INTEGRATION_REQUIRES_KERBEROS_PROPERTY_ID, response.isSsoIntegrationRequiresKerberos(), requestedIds);
    setResourceProperty(resource, LDAP_INTEGRATION_SUPPORTED_PROPERTY_ID, response.isLdapIntegrationSupported(), requestedIds);

    return resource;
  }

  private StackServiceRequest getRequest(Map<String, Object> properties) {
    return new StackServiceRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(CREDENTIAL_STORE_SUPPORTED),
        (String) properties.get(CREDENTIAL_STORE_ENABLED));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
