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
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.GroupRequest;
import org.apache.ambari.server.controller.GroupResponse;
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
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for group resources.
 */
public class GroupResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GroupResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  // Groups
  public static final String GROUP_GROUPNAME_PROPERTY_ID  = PropertyHelper.getPropertyId("Groups", "group_name");
  public static final String GROUP_LDAP_GROUP_PROPERTY_ID = PropertyHelper.getPropertyId("Groups", "ldap_group");
  public static final String GROUP_GROUPTYPE_PROPERTY_ID  = PropertyHelper.getPropertyId("Groups", "group_type");


  /**
   * The key property ids for a Group resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Group, GROUP_GROUPNAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Group resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      GROUP_GROUPNAME_PROPERTY_ID,
      GROUP_LDAP_GROUP_PROPERTY_ID,
      GROUP_GROUPTYPE_PROPERTY_ID);

  /**
   * Create a new resource provider for the given management controller.
   *
   * @param managementController  the management controller
   */
  GroupResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Group, propertyIds, keyPropertyIds, managementController);

    EnumSet<RoleAuthorization> manageUserAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS);
    setRequiredCreateAuthorizations(manageUserAuthorizations);
    setRequiredGetAuthorizations(manageUserAuthorizations);
    setRequiredUpdateAuthorizations(manageUserAuthorizations);
    setRequiredDeleteAuthorizations(manageUserAuthorizations);
  }

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    final Set<GroupRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createGroups(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<GroupRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(null));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<GroupResponse> responses = getResources(new Command<Set<GroupResponse>>() {
      @Override
      public Set<GroupResponse> invoke() throws AmbariException {
        return getManagementController().getGroups(requests);
      }
    });

    LOG.debug("Found group responses matching get group request, groupRequestSize={}, groupResponseSize={}", requests.size(), responses.size());

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    for (GroupResponse groupResponse : responses) {
      ResourceImpl resource = new ResourceImpl(Resource.Type.Group);

      setResourceProperty(resource, GROUP_GROUPNAME_PROPERTY_ID,
          groupResponse.getGroupName(), requestedIds);

      setResourceProperty(resource, GROUP_LDAP_GROUP_PROPERTY_ID,
          groupResponse.isLdapGroup(), requestedIds);

      setResourceProperty(resource, GROUP_GROUPTYPE_PROPERTY_ID,
          groupResponse.getGroupType(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<GroupRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      final GroupRequest req = getRequest(propertyMap);
      requests.add(req);
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().updateGroups(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<GroupRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final GroupRequest req = getRequest(propertyMap);
      requests.add(req);
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().deleteGroups(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  private GroupRequest getRequest(Map<String, Object> properties) {
    if (properties == null) {
      return new GroupRequest(null);
    }

    final GroupRequest request = new GroupRequest((String) properties.get(GROUP_GROUPNAME_PROPERTY_ID));
    return request;
  }
}
