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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionRequest;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
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
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.TopologyRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;


/**
 * Resource provider for cluster resources.
 */
public class ClusterResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  // Clusters
  public static final String RESPONSE_KEY = "Clusters";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String CLUSTER_ID = "cluster_id";
  public static final String CLUSTER_NAME = "cluster_name";
  public static final String VERSION = "version";
  public static final String PROVISIONING_STATE = "provisioning_state";
  public static final String SECURITY_TYPE = "security_type";
  public static final String DESIRED_CONFIGS = "desired_configs";
  public static final String DESIRED_SERVICE_CONFIG_VERSIONS = "desired_service_config_versions";
  public static final String TOTAL_HOSTS = "total_hosts";
  public static final String HEALTH_REPORT = "health_report";
  public static final String CREDENTIAL_STORE_PROPERTIES = "credential_store_properties";
  public static final String REPO_VERSION = "repository_version";
  public static final String CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_ID;
  public static final String CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME;
  public static final String CLUSTER_VERSION_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + VERSION;
  public static final String CLUSTER_PROVISIONING_STATE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + PROVISIONING_STATE;
  public static final String CLUSTER_SECURITY_TYPE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + SECURITY_TYPE;
  public static final String CLUSTER_DESIRED_CONFIGS_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + DESIRED_CONFIGS;
  public static final String CLUSTER_DESIRED_SERVICE_CONFIG_VERSIONS_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + DESIRED_SERVICE_CONFIG_VERSIONS;
  public static final String CLUSTER_TOTAL_HOSTS_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + TOTAL_HOSTS;
  public static final String CLUSTER_HEALTH_REPORT_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + HEALTH_REPORT;
  public static final String CLUSTER_CREDENTIAL_STORE_PROPERTIES_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CREDENTIAL_STORE_PROPERTIES;
  public static final String CLUSTER_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters","state");

  static final String BLUEPRINT = "blueprint";
  public static final String SECURITY = "security";
  public static final String CREDENTIALS = "credentials";
  private static final String QUICKLINKS_PROFILE = "quicklinks_profile";
  private static final String SESSION_ATTRIBUTES = "session_attributes";

  /**
   * The session attributes property prefix.
   */
  private static final String SESSION_ATTRIBUTES_PROPERTY_PREFIX = SESSION_ATTRIBUTES + "/";

  /**
   * Request info property ID.  Allow internal getResources call to bypass permissions check.
   */
  public static final String GET_IGNORE_PERMISSIONS_PROPERTY_ID = "get_resource/ignore_permissions";

  /**
   * topology manager instance
   */
  private static TopologyManager topologyManager;

  /**
   * factory for creating topology requests which are used to provision a cluster via a blueprint
   */
  private static TopologyRequestFactory topologyRequestFactory;

  /**
   * Used to create SecurityConfiguration instances
   */
  private static SecurityConfigurationFactory securityConfigurationFactory;

  /**
   * The key property ids for a cluster resource.
   */
  protected static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, CLUSTER_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a cluster resource.
   */
  protected static final Set<String> propertyIds = ImmutableSet.<String>builder()
    .add(CLUSTER_ID_PROPERTY_ID)
    .add(CLUSTER_NAME_PROPERTY_ID)
    .add(CLUSTER_VERSION_PROPERTY_ID)
    .add(CLUSTER_PROVISIONING_STATE_PROPERTY_ID)
    .add(CLUSTER_SECURITY_TYPE_PROPERTY_ID)
    .add(CLUSTER_DESIRED_CONFIGS_PROPERTY_ID)
    .add(CLUSTER_DESIRED_SERVICE_CONFIG_VERSIONS_PROPERTY_ID)
    .add(CLUSTER_TOTAL_HOSTS_PROPERTY_ID)
    .add(CLUSTER_HEALTH_REPORT_PROPERTY_ID)
    .add(CLUSTER_CREDENTIAL_STORE_PROPERTIES_PROPERTY_ID)
    .add(BLUEPRINT)
    .add(SESSION_ATTRIBUTES)
    .add(SECURITY)
    .add(CREDENTIALS)
    .add(QUICKLINKS_PROFILE)
    .add(CLUSTER_STATE_PROPERTY_ID)
    .build();

  /**
   * Used to serialize to/from json.
   */
  private static Gson jsonSerializer;


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController  the management controller
   */
  ClusterResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Cluster, propertyIds, keyPropertyIds, managementController);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS));
    setRequiredGetAuthorizations(RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER);
    setRequiredUpdateAuthorizations(RoleAuthorization.AUTHORIZATIONS_UPDATE_CLUSTER);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(Collections.singletonList(CLUSTER_ID_PROPERTY_ID));
  }

  /**
   * {@inheritDoc}  Overridden to support configuration.
   */
  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> baseUnsupported = super.checkPropertyIds(propertyIds);

    // extract to own method
    baseUnsupported.remove("blueprint");
    baseUnsupported.remove("host_groups");
    baseUnsupported.remove("default_password");
    baseUnsupported.remove("configurations");
    baseUnsupported.remove("credentials");
    baseUnsupported.remove("config_recommendation_strategy");
    baseUnsupported.remove("provision_action");
    baseUnsupported.remove(ProvisionClusterRequest.REPO_VERSION_PROPERTY);
    baseUnsupported.remove(ProvisionClusterRequest.REPO_VERSION_ID_PROPERTY);
    return checkConfigPropertyIds(baseUnsupported, "Clusters");
  }


  // ----- AbstractAuthorizedResourceProvider ------------------------------------------------

  @Override
  protected boolean isAuthorizedToCreateResources(Authentication authentication, Request request) {
    return AuthorizationHelper.isAuthorized(authentication, ResourceType.AMBARI, null, getRequiredCreateAuthorizations());
  }

  @Override
  protected boolean isAuthorizedToDeleteResources(Authentication authentication, Predicate predicate) throws SystemException {
    return AuthorizationHelper.isAuthorized(authentication, ResourceType.AMBARI, null, getRequiredDeleteAuthorizations());
  }

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    RequestStatusResponse createResponse = null;
    for (final Map<String, Object> properties : request.getProperties()) {
      if (isCreateFromBlueprint(properties)) {
        createResponse = processBlueprintCreate(properties, request.getRequestInfoProperties());
      } else {
        createClusterResource(properties);
      }
    }

    notifyCreate(Resource.Type.Cluster, request);
    return getRequestStatus(createResponse);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ClusterRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    // Authorization checks are performed internally. If the user is not allowed to access a particular
    // cluster, it should not show up in the responses.
    Set<ClusterResponse> responses = getResources(new Command<Set<ClusterResponse>>() {
      @Override
      public Set<ClusterResponse> invoke() throws AmbariException, AuthorizationException {
        return getManagementController().getClusters(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Found clusters matching getClusters request, clusterResponseCount={}", responses.size());
    }

    // Allow internal call to bypass permissions check.
    for (ClusterResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Cluster);
      setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
      setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, CLUSTER_PROVISIONING_STATE_PROPERTY_ID, response.getProvisioningState().name(), requestedIds);
      setResourceProperty(resource, CLUSTER_SECURITY_TYPE_PROPERTY_ID, response.getSecurityType().name(), requestedIds);
      setResourceProperty(resource, CLUSTER_DESIRED_CONFIGS_PROPERTY_ID, response.getDesiredConfigs(), requestedIds);
      setResourceProperty(resource, CLUSTER_DESIRED_SERVICE_CONFIG_VERSIONS_PROPERTY_ID, response.getDesiredServiceConfigVersions(), requestedIds);
      setResourceProperty(resource, CLUSTER_TOTAL_HOSTS_PROPERTY_ID, response.getTotalHosts(), requestedIds);
      setResourceProperty(resource, CLUSTER_HEALTH_REPORT_PROPERTY_ID, response.getClusterHealthReport(), requestedIds);
      setResourceProperty(resource, CLUSTER_CREDENTIAL_STORE_PROPERTIES_PROPERTY_ID, response.getCredentialStoreServiceProperties(), requestedIds);
      setResourceProperty(resource, CLUSTER_VERSION_PROPERTY_ID, response.getDesiredStackVersion(), requestedIds);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding ClusterResponse to resource, clusterResponse={}", response);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ClusterRequest>   requests = new HashSet<>();
    RequestStatusResponse       response;

    for (Map<String, Object> requestPropertyMap : request.getProperties()) {
      Set<Map<String, Object>> propertyMaps = getPropertyMaps(requestPropertyMap, predicate);
      for (Map<String, Object> propertyMap : propertyMaps) {
        ClusterRequest clusterRequest = getRequest(propertyMap);
        requests.add(clusterRequest);
      }
    }
    response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException, AuthorizationException {
        return getManagementController().updateClusters(requests, request.getRequestInfoProperties());
      }
    });
    notifyUpdate(Resource.Type.Cluster, request, predicate);

    Set<Resource> associatedResources = null;
    for (ClusterRequest clusterRequest : requests) {
      ClusterResponse updateResults = getManagementController().getClusterUpdateResults(clusterRequest);
      if (updateResults != null) {
        Map<String, Collection<ServiceConfigVersionResponse>> serviceConfigVersions = updateResults.getDesiredServiceConfigVersions();
        if (serviceConfigVersions != null) {
          associatedResources = new HashSet<>();
          for (Collection<ServiceConfigVersionResponse> scvCollection : serviceConfigVersions.values()) {
            for (ServiceConfigVersionResponse serviceConfigVersionResponse : scvCollection) {
              Resource resource = new ResourceImpl(Resource.Type.ServiceConfigVersion);
              resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_NAME_PROPERTY_ID,
                serviceConfigVersionResponse.getServiceName());
              resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_PROPERTY_ID,
                serviceConfigVersionResponse.getVersion());
              resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID,
                serviceConfigVersionResponse.getNote());
              resource.setProperty(ServiceConfigVersionResourceProvider.GROUP_ID_PROPERTY_ID,
                  serviceConfigVersionResponse.getGroupId());
              resource.setProperty(ServiceConfigVersionResourceProvider.GROUP_NAME_PROPERTY_ID,
                  serviceConfigVersionResponse.getGroupName());
              if (serviceConfigVersionResponse.getConfigurations() != null) {
                resource.setProperty(
                  ServiceConfigVersionResourceProvider.CONFIGURATIONS_PROPERTY_ID,
                  serviceConfigVersionResponse.getConfigurations());
              }
              associatedResources.add(resource);
            }
          }

        }
      }
    }


    return getRequestStatus(response, associatedResources);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final ClusterRequest clusterRequest = getRequest(propertyMap);
      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          getManagementController().deleteCluster(clusterRequest);
          return null;
        }
      });
    }
    notifyDelete(Resource.Type.Cluster, predicate);
    return getRequestStatus(null);
  }


  // ----- ClusterResourceProvider -------------------------------------------

  /**
   * Inject the blueprint data access object which is used to obtain blueprint entities.
   *  @param manager         topology manager
   * @param requestFactory  request factory
   * @param instance
   */
  //todo: proper static injection mechanism
  public static void init(TopologyManager manager, TopologyRequestFactory requestFactory,
                          SecurityConfigurationFactory securityFactory, Gson instance) {
    topologyManager = manager;
    topologyRequestFactory = requestFactory;
    securityConfigurationFactory = securityFactory;
    jsonSerializer = instance;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a cluster request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the cluster request object
   */
  private ClusterRequest getRequest(Map<String, Object> properties) {
    SecurityType securityType;
    String requestedSecurityType = (String) properties.get(CLUSTER_SECURITY_TYPE_PROPERTY_ID);
    if(requestedSecurityType == null)
      securityType = null;
    else {
      try {
        securityType = SecurityType.valueOf(requestedSecurityType.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(String.format("Cannot set cluster security type to invalid value: %s", requestedSecurityType));
      }
    }

    ClusterRequest cr = new ClusterRequest(
        (Long) properties.get(CLUSTER_ID_PROPERTY_ID),
        (String) properties.get(CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(CLUSTER_PROVISIONING_STATE_PROPERTY_ID),
        securityType,
        (String) properties.get(CLUSTER_VERSION_PROPERTY_ID),
        null,
        getSessionAttributes(properties));

    List<ConfigurationRequest> configRequests = getConfigurationRequests(RESPONSE_KEY, properties);
    if (!configRequests.isEmpty()) {
      cr.setDesiredConfig(configRequests);
    }

    ServiceConfigVersionRequest serviceConfigVersionRequest = getServiceConfigVersionRequest(RESPONSE_KEY, properties);
    if (serviceConfigVersionRequest != null) {
      cr.setServiceConfigVersionRequest(serviceConfigVersionRequest);
    }

    return cr;
  }


  /**
   * Get the map of session attributes from the given property map.
   *
   * @param properties  the property map from the request
   *
   * @return the map of session attributes
   */
  private Map<String, Object> getSessionAttributes(Map<String, Object> properties) {
    Map<String, Object> sessionAttributes = new HashMap<>();

    for (Map.Entry<String, Object> entry : properties.entrySet()) {

      String property = entry.getKey();

      if (property.startsWith(SESSION_ATTRIBUTES_PROPERTY_PREFIX)) {
        String attributeName = property.substring(SESSION_ATTRIBUTES_PROPERTY_PREFIX.length());
        sessionAttributes.put(attributeName, entry.getValue());
      }
    }
    return sessionAttributes;
  }

  /**
   * Helper method for creating rollback request
   */
  protected static ServiceConfigVersionRequest getServiceConfigVersionRequest(String parentCategory, Map<String, Object> properties) {
    ServiceConfigVersionRequest serviceConfigVersionRequest = null;

    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String absCategory = PropertyHelper.getPropertyCategory(entry.getKey());
      String propName = PropertyHelper.getPropertyName(entry.getKey());

      if (absCategory != null &&
              absCategory.startsWith(parentCategory + "/desired_service_config_version")) {
        serviceConfigVersionRequest =
            (serviceConfigVersionRequest ==null ) ? new ServiceConfigVersionRequest() : serviceConfigVersionRequest;

        if (propName != null) {
          switch (propName) {
            case "service_name": {
              serviceConfigVersionRequest.setServiceName(entry.getValue().toString());
              break;
            }
            case "service_config_version": {
              serviceConfigVersionRequest.setVersion(Long.valueOf(entry.getValue().toString()));
              break;
            }
            case "service_config_version_note": {
              serviceConfigVersionRequest.setNote(entry.getValue().toString());
              break;
            }
          }
        }
      }
    }
    return serviceConfigVersionRequest;
  }

  /**
   * Determine if the request is a create using a blueprint.
   *
   * @param properties  request properties
   *
   * @return true if request is a create using a blueprint; false otherwise
   */
  private boolean isCreateFromBlueprint(Map<String, Object> properties) {
    return properties.get("blueprint") != null;
  }

  /**
   * Process a create request specifying a blueprint.  This includes creation of all resources,
   * setting of configuration and installing and starting of all services.  The end result of this
   * call will be a running cluster based on the topology and configuration specified in the blueprint.
   *
   * @param properties  request body properties
   *
   * @param requestInfoProperties raw request body
   * @return asynchronous response information
   *
   * @throws ResourceAlreadyExistsException if cluster already exists
   * @throws SystemException                if an unexpected exception occurs
   * @throws UnsupportedPropertyException   if an invalid property is specified in the request
   * @throws NoSuchParentResourceException  if a necessary parent resource doesn't exist
   */
  @SuppressWarnings("unchecked")
  private RequestStatusResponse processBlueprintCreate(Map<String, Object> properties, Map<String, String> requestInfoProperties)
      throws ResourceAlreadyExistsException, SystemException, UnsupportedPropertyException,
      NoSuchParentResourceException {

    LOG.info("Creating Cluster '" + properties.get(CLUSTER_NAME_PROPERTY_ID) +
        "' based on blueprint '" + String.valueOf(properties.get(BLUEPRINT)) + "'.");

    String rawRequestBody = requestInfoProperties.get(Request.REQUEST_INFO_BODY_PROPERTY);
    Map<String, Object> rawBodyMap = jsonSerializer.<Map<String, Object>>fromJson(rawRequestBody, Map.class);
    SecurityConfiguration securityConfiguration =
      securityConfigurationFactory.createSecurityConfigurationFromRequest(rawBodyMap, false);

    ProvisionClusterRequest createClusterRequest;
    try {
      createClusterRequest = topologyRequestFactory.createProvisionClusterRequest(properties, securityConfiguration);
    } catch (InvalidTopologyTemplateException e) {
      throw new IllegalArgumentException("Invalid Cluster Creation Template: " + e, e);
    }

    if (securityConfiguration != null && securityConfiguration.getType() == SecurityType.NONE &&
        createClusterRequest.getBlueprint().getSecurity() != null && createClusterRequest.getBlueprint().getSecurity()
        .getType() == SecurityType.KERBEROS) {
      throw new IllegalArgumentException("Setting security to NONE is not allowed as security type in blueprint is set to KERBEROS!");
    }

    try {
      return topologyManager.provisionCluster(createClusterRequest);
    } catch (InvalidTopologyException e) {
      throw new IllegalArgumentException("Topology validation failed: " + e, e);
    } catch (AmbariException e) {
      throw new SystemException("Unknown exception when asking TopologyManager to provision cluster", e);
    } catch (RuntimeException e) {
      throw new SystemException("An exception occurred during cluster provisioning: " + e.getMessage(), e);
    }
  }

  /**
   * Create the cluster resource.
   *
   * @param properties  cluster resource request properties
   *
   * @throws ResourceAlreadyExistsException  cluster resource already exists
   * @throws SystemException                 an unexpected exception occurred
   * @throws NoSuchParentResourceException   shouldn't be thrown as a cluster doesn't have a parent resource
   */
  private void createClusterResource(final Map<String, Object> properties)
      throws ResourceAlreadyExistsException, SystemException, NoSuchParentResourceException {

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        getManagementController().createCluster(getRequest(properties));
        return null;
      }
    });
  }

}

