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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.resources.RepositoryResourceDefinition;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RepositoryRequest;
import org.apache.ambari.server.controller.RepositoryResponse;
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
import org.apache.commons.lang.BooleanUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RepositoryResourceProvider extends AbstractControllerResourceProvider {

  public static final String REPOSITORY_REPO_NAME_PROPERTY_ID             = PropertyHelper.getPropertyId("Repositories", "repo_name");
  public static final String REPOSITORY_STACK_NAME_PROPERTY_ID            = PropertyHelper.getPropertyId("Repositories", "stack_name");
  public static final String REPOSITORY_STACK_VERSION_PROPERTY_ID         = PropertyHelper.getPropertyId("Repositories", "stack_version");
  public static final String REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("Repositories", "cluster_version_id");
  public static final String REPOSITORY_OS_TYPE_PROPERTY_ID               = PropertyHelper.getPropertyId("Repositories", "os_type");
  public static final String REPOSITORY_BASE_URL_PROPERTY_ID              = PropertyHelper.getPropertyId("Repositories", "base_url");
  public static final String REPOSITORY_DISTRIBUTION_PROPERTY_ID          = PropertyHelper.getPropertyId("Repositories", "distribution");
  public static final String REPOSITORY_COMPONENTS_PROPERTY_ID            = PropertyHelper.getPropertyId("Repositories", "components");
  public static final String REPOSITORY_REPO_ID_PROPERTY_ID               = PropertyHelper.getPropertyId("Repositories", "repo_id");
  public static final String REPOSITORY_MIRRORS_LIST_PROPERTY_ID          = PropertyHelper.getPropertyId("Repositories", "mirrors_list");
  public static final String REPOSITORY_DEFAULT_BASE_URL_PROPERTY_ID      = PropertyHelper.getPropertyId("Repositories", "default_base_url");
  public static final String REPOSITORY_VERIFY_BASE_URL_PROPERTY_ID       = PropertyHelper.getPropertyId("Repositories", "verify_base_url");
  public static final String REPOSITORY_REPOSITORY_VERSION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("Repositories", "repository_version_id");
  public static final String REPOSITORY_VERSION_DEFINITION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("Repositories", "version_definition_id");
  public static final String REPOSITORY_UNIQUE_PROPERTY_ID                = PropertyHelper.getPropertyId("Repositories", "unique");
  public static final String REPOSITORY_TAGS_PROPERTY_ID                  = PropertyHelper.getPropertyId("Repositories", "tags");
  @Experimental(feature = ExperimentalFeature.CUSTOM_SERVICE_REPOS,
    comment = "Remove logic for handling custom service repos after enabling multi-mpack cluster deployment")
  public static final String REPOSITORY_APPLICABLE_SERVICES_PROPERTY_ID   = PropertyHelper.getPropertyId("Repositories", "applicable_services");

  @SuppressWarnings("serial")
  private static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(REPOSITORY_STACK_NAME_PROPERTY_ID)
    .add(REPOSITORY_STACK_VERSION_PROPERTY_ID)
    .add(REPOSITORY_OS_TYPE_PROPERTY_ID)
    .add(REPOSITORY_REPO_ID_PROPERTY_ID)
    .build();

  @SuppressWarnings("serial")
  public static final Set<String> propertyIds = ImmutableSet.<String>builder()
    .add(REPOSITORY_REPO_NAME_PROPERTY_ID)
    .add(REPOSITORY_DISTRIBUTION_PROPERTY_ID)
    .add(REPOSITORY_COMPONENTS_PROPERTY_ID)
    .add(REPOSITORY_STACK_NAME_PROPERTY_ID)
    .add(REPOSITORY_STACK_VERSION_PROPERTY_ID)
    .add(REPOSITORY_OS_TYPE_PROPERTY_ID)
    .add(REPOSITORY_BASE_URL_PROPERTY_ID)
    .add(REPOSITORY_REPO_ID_PROPERTY_ID)
    .add(REPOSITORY_MIRRORS_LIST_PROPERTY_ID)
    .add(REPOSITORY_DEFAULT_BASE_URL_PROPERTY_ID)
    .add(REPOSITORY_VERIFY_BASE_URL_PROPERTY_ID)
    .add(REPOSITORY_REPOSITORY_VERSION_ID_PROPERTY_ID)
    .add(REPOSITORY_VERSION_DEFINITION_ID_PROPERTY_ID)
    .add(REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID)
    .add(REPOSITORY_UNIQUE_PROPERTY_ID)
    .add(REPOSITORY_TAGS_PROPERTY_ID)
    .add(REPOSITORY_APPLICABLE_SERVICES_PROPERTY_ID)
    .build();

  @SuppressWarnings("serial")
  public static final Map<Type, String> keyPropertyIds = ImmutableMap.<Type, String>builder()
    .put(Resource.Type.Stack, REPOSITORY_STACK_NAME_PROPERTY_ID)
    .put(Resource.Type.StackVersion, REPOSITORY_STACK_VERSION_PROPERTY_ID)
    .put(Resource.Type.ClusterStackVersion, REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID)
    .put(Resource.Type.OperatingSystem, REPOSITORY_OS_TYPE_PROPERTY_ID)
    .put(Resource.Type.Repository, REPOSITORY_REPO_ID_PROPERTY_ID)
    .put(Resource.Type.RepositoryVersion, REPOSITORY_REPOSITORY_VERSION_ID_PROPERTY_ID)
    .put(Resource.Type.VersionDefinition, REPOSITORY_VERSION_DEFINITION_ID_PROPERTY_ID)
    .build();

  public RepositoryResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Repository, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RepositoryRequest> requestsToVerifyBaseURLs = new HashSet<>();

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        RepositoryRequest rr = getRequest(propertyMap);
        if(rr.isVerifyBaseUrl()) {
          requestsToVerifyBaseURLs.add(rr);
        }
      }
    }

    //Validation only - used by the cluster installation
    try {
      getManagementController().verifyRepositories(requestsToVerifyBaseURLs);
    } catch (AmbariException e) {
      throw new SystemException("", e);
    }

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RepositoryRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RepositoryResponse> responses = getResources(new Command<Set<RepositoryResponse>>() {
      @Override
      public Set<RepositoryResponse> invoke() throws AmbariException {
        return getManagementController().getRepositories(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (RepositoryResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Repository);

        setResourceProperty(resource, REPOSITORY_STACK_NAME_PROPERTY_ID, response.getStackName(), requestedIds);
        setResourceProperty(resource, REPOSITORY_STACK_VERSION_PROPERTY_ID, response.getStackVersion(), requestedIds);
        setResourceProperty(resource, REPOSITORY_REPO_NAME_PROPERTY_ID, response.getRepoName(), requestedIds);
        setResourceProperty(resource, REPOSITORY_DISTRIBUTION_PROPERTY_ID, response.getDistribution(), requestedIds);
        setResourceProperty(resource, REPOSITORY_COMPONENTS_PROPERTY_ID, response.getComponents(), requestedIds);
        setResourceProperty(resource, REPOSITORY_BASE_URL_PROPERTY_ID, response.getBaseUrl(), requestedIds);
        setResourceProperty(resource, REPOSITORY_OS_TYPE_PROPERTY_ID, response.getOsType(), requestedIds);
        setResourceProperty(resource, REPOSITORY_REPO_ID_PROPERTY_ID, response.getRepoId(), requestedIds);
        setResourceProperty(resource, REPOSITORY_MIRRORS_LIST_PROPERTY_ID, response.getMirrorsList(), requestedIds);
        setResourceProperty(resource, REPOSITORY_DEFAULT_BASE_URL_PROPERTY_ID, response.getDefaultBaseUrl(), requestedIds);
        setResourceProperty(resource, REPOSITORY_UNIQUE_PROPERTY_ID, response.isUnique(), requestedIds);
        setResourceProperty(resource, REPOSITORY_TAGS_PROPERTY_ID, response.getTags(), requestedIds);
        setResourceProperty(resource, REPOSITORY_APPLICABLE_SERVICES_PROPERTY_ID, response.getApplicableServices(), requestedIds);
        if (null != response.getClusterVersionId()) {
          setResourceProperty(resource, REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID, response.getClusterVersionId(), requestedIds);
        }

        if (null != response.getRepositoryVersionId()) {
          setResourceProperty(resource, REPOSITORY_REPOSITORY_VERSION_ID_PROPERTY_ID, response.getRepositoryVersionId(), requestedIds);
        }

        if (null != response.getVersionDefinitionId()) {
          setResourceProperty(resource, REPOSITORY_VERSION_DEFINITION_ID_PROPERTY_ID,
              response.getVersionDefinitionId(), requestedIds);
        }

        resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    final String validateOnlyProperty = request.getRequestInfoProperties().get(RepositoryResourceDefinition.VALIDATE_ONLY_DIRECTIVE);
    if (BooleanUtils.toBoolean(validateOnlyProperty)) {
      final Set<RepositoryRequest> requests = new HashSet<>();
      final Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
      if (iterator.hasNext()) {
        for (Map<String, Object> propertyMap : request.getProperties()) {
          requests.add(getRequest(propertyMap));
        }
      }
      createResources(new Command<Void>() {

        @Override
        public Void invoke() throws AmbariException {
          getManagementController().verifyRepositories(requests);
          return null;
        }

      });
      return getRequestStatus(null);
    } else {
      throw new SystemException("Cannot create repositories.", null);
    }
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete repositories.", null);
  }

  private RepositoryRequest getRequest(Map<String, Object> properties) {
    RepositoryRequest request = new RepositoryRequest(
        (String) properties.get(REPOSITORY_STACK_NAME_PROPERTY_ID),
        (String) properties.get(REPOSITORY_STACK_VERSION_PROPERTY_ID),
        (String) properties.get(REPOSITORY_OS_TYPE_PROPERTY_ID),
        (String) properties.get(REPOSITORY_REPO_ID_PROPERTY_ID),
        (String) properties.get(REPOSITORY_REPO_NAME_PROPERTY_ID));

    if (properties.containsKey(REPOSITORY_REPOSITORY_VERSION_ID_PROPERTY_ID)) {
      request.setRepositoryVersionId(Long.parseLong(properties.get(REPOSITORY_REPOSITORY_VERSION_ID_PROPERTY_ID).toString()));
    }

    if (properties.containsKey(REPOSITORY_VERSION_DEFINITION_ID_PROPERTY_ID)) {
      request.setVersionDefinitionId(properties.get(REPOSITORY_VERSION_DEFINITION_ID_PROPERTY_ID).toString());
    }

    if (properties.containsKey(REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID)) {
      request.setClusterVersionId(Long.parseLong(properties.get(REPOSITORY_CLUSTER_STACK_VERSION_PROPERTY_ID).toString()));
    }

    if (properties.containsKey(REPOSITORY_BASE_URL_PROPERTY_ID)) {
      request.setBaseUrl((String) properties.get(REPOSITORY_BASE_URL_PROPERTY_ID));

      if (properties.containsKey(REPOSITORY_VERIFY_BASE_URL_PROPERTY_ID)) {
        request.setVerifyBaseUrl("true".equalsIgnoreCase(properties.get(REPOSITORY_VERIFY_BASE_URL_PROPERTY_ID).toString()));
      }
    }

    if (properties.containsKey(REPOSITORY_MIRRORS_LIST_PROPERTY_ID)) {
      request.setMirrorsList((String) properties.get(REPOSITORY_MIRRORS_LIST_PROPERTY_ID));
    }

    return request;
  }

  @Override
  public Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }
}
