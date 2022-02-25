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

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
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
import org.apache.ambari.server.controller.utilities.ServiceCalculatedStateFactory;
import org.apache.ambari.server.controller.utilities.state.ServiceCalculatedState;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.serveraction.kerberos.KerberosAdminAuthenticationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.STOMPComponentsDeleteHandler;
import org.apache.ambari.server.topology.addservice.AddServiceOrchestrator;
import org.apache.ambari.server.topology.addservice.AddServiceRequest;
import org.apache.ambari.server.utils.LoggingPreconditions;
import org.apache.ambari.spi.RepositoryType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for service resources.
 */
public class ServiceResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceResourceProvider.class);
  private static final LoggingPreconditions CHECK = new LoggingPreconditions(LOG);

  public static final String SERVICE_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "cluster_name");

  public static final String SERVICE_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "service_name");

  public static final String SERVICE_SERVICE_STATE_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "state");

  public static final String SERVICE_MAINTENANCE_STATE_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "maintenance_state");

  public static final String SERVICE_CREDENTIAL_STORE_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "credential_store_supported");

  public static final String SERVICE_CREDENTIAL_STORE_ENABLED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "credential_store_enabled");

  public static final String SERVICE_ATTRIBUTES_PROPERTY_ID = PropertyHelper.getPropertyId(
      "Services", "attributes");

  public static final String SERVICE_DESIRED_STACK_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "desired_stack");

  public static final String SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "desired_repository_version_id");

  private static final String SSO_INTEGRATION_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
    "ServiceInfo", "sso_integration_supported");

  private static final String SSO_INTEGRATION_ENABLED_PROPERTY_ID = PropertyHelper.getPropertyId(
    "ServiceInfo", "sso_integration_enabled");

  private static final String SSO_INTEGRATION_DESIRED_PROPERTY_ID = PropertyHelper.getPropertyId(
    "ServiceInfo", "sso_integration_desired");

  private static final String SSO_INTEGRATION_REQUIRES_KERBEROS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "sso_integration_requires_kerberos");

  private static final String KERBEROS_ENABLED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "kerberos_enabled");

  private static final String LDAP_INTEGRATION_SUPPORTED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "ldap_integration_supported");

  private static final String LDAP_INTEGRATION_ENABLED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "ldap_integration_enabled");

  private static final String LDAP_INTEGRATION_DESIRED_PROPERTY_ID = PropertyHelper.getPropertyId(
      "ServiceInfo", "ldap_integration_desired");

  public static final String OPERATION_TYPE = "operation_type";

  protected static final String SERVICE_REPOSITORY_STATE = "ServiceInfo/repository_state";

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID = "params/run_smoke_test";
  private static final String QUERY_PARAMETERS_RECONFIGURE_CLIENT = "params/reconfigure_client";
  private static final String QUERY_PARAMETERS_START_DEPENDENCIES = "params/start_dependencies";

  private static final Set<String> pkPropertyIds =
    new HashSet<>(Arrays.asList(new String[]{
      SERVICE_CLUSTER_NAME_PROPERTY_ID,
      SERVICE_SERVICE_NAME_PROPERTY_ID}));

  /**
   * The property ids for an service resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for an service resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(SERVICE_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_SERVICE_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_SERVICE_STATE_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_MAINTENANCE_STATE_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CREDENTIAL_STORE_SUPPORTED_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CREDENTIAL_STORE_ENABLED_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_ATTRIBUTES_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DESIRED_STACK_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_REPOSITORY_STATE);

    PROPERTY_IDS.add(QUERY_PARAMETERS_RUN_SMOKE_TEST_ID);
    PROPERTY_IDS.add(QUERY_PARAMETERS_RECONFIGURE_CLIENT);
    PROPERTY_IDS.add(QUERY_PARAMETERS_START_DEPENDENCIES);

    PROPERTY_IDS.add(SSO_INTEGRATION_SUPPORTED_PROPERTY_ID);
    PROPERTY_IDS.add(SSO_INTEGRATION_ENABLED_PROPERTY_ID);
    PROPERTY_IDS.add(SSO_INTEGRATION_DESIRED_PROPERTY_ID);
    PROPERTY_IDS.add(SSO_INTEGRATION_REQUIRES_KERBEROS_PROPERTY_ID);
    PROPERTY_IDS.add(KERBEROS_ENABLED_PROPERTY_ID);

    PROPERTY_IDS.add(LDAP_INTEGRATION_SUPPORTED_PROPERTY_ID);
    PROPERTY_IDS.add(LDAP_INTEGRATION_ENABLED_PROPERTY_ID);
    PROPERTY_IDS.add(LDAP_INTEGRATION_DESIRED_PROPERTY_ID);

    PROPERTY_IDS.add(OPERATION_TYPE);
    PROPERTY_IDS.add(ClusterResourceProvider.SECURITY);
    PROPERTY_IDS.add(ClusterResourceProvider.CREDENTIALS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Service, SERVICE_SERVICE_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, SERVICE_CLUSTER_NAME_PROPERTY_ID);
  }

  private MaintenanceStateHelper maintenanceStateHelper;

  /**
   * kerberos helper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private STOMPComponentsDeleteHandler STOMPComponentsDeleteHandler;

  @Inject
  private AddServiceOrchestrator addServiceOrchestrator;

  /**
   * Used to lookup the repository when creating services.
   */
  private final RepositoryVersionDAO repositoryVersionDAO;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController  the management controller
   */
  @AssistedInject
  public ServiceResourceProvider(
      @Assisted AmbariManagementController managementController,
      MaintenanceStateHelper maintenanceStateHelper, RepositoryVersionDAO repositoryVersionDAO) {
    super(Resource.Type.Service, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
    this.maintenanceStateHelper = maintenanceStateHelper;
    this.repositoryVersionDAO = repositoryVersionDAO;

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES));
    setRequiredUpdateAuthorizations(RoleAuthorization.AUTHORIZATIONS_UPDATE_SERVICE);
    setRequiredGetAuthorizations(RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE);
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES));
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    if (request.getProperties().size() == 1) {
      Map<String, Object> requestProperties = request.getProperties().iterator().next();
      if (isAddServiceRequest(requestProperties)) {
        RequestStatusResponse response = processAddServiceRequest(requestProperties, request.getRequestInfoProperties());
        notifyCreate(Resource.Type.Service, request);
        return getRequestStatus(response);
      }
    }

    final Set<ServiceRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        createServices(requests);
        return null;
      }
    });
    notifyCreate(Resource.Type.Service, request);

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceResponse> responses = getResources(new Command<Set<ServiceResponse>>() {
      @Override
      public Set<ServiceResponse> invoke() throws AmbariException {
        return getServices(requests);
      }
    });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    for (ServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Service);
      setResourceProperty(resource, SERVICE_CLUSTER_NAME_PROPERTY_ID,
          response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
      setResourceProperty(resource, SERVICE_SERVICE_STATE_PROPERTY_ID,
          calculateServiceState(response.getClusterName(), response.getServiceName()),
          requestedIds);
      setResourceProperty(resource, SERVICE_MAINTENANCE_STATE_PROPERTY_ID,
          response.getMaintenanceState(), requestedIds);
      setResourceProperty(resource, SERVICE_CREDENTIAL_STORE_SUPPORTED_PROPERTY_ID,
          String.valueOf(response.isCredentialStoreSupported()), requestedIds);
      setResourceProperty(resource, SERVICE_CREDENTIAL_STORE_ENABLED_PROPERTY_ID,
          String.valueOf(response.isCredentialStoreEnabled()), requestedIds);

      RepositoryVersionEntity repoVersion = repositoryVersionDAO.findByPK(response.getDesiredRepositoryVersionId());

      // !!! TODO is the UI using this?
      if (null != repoVersion) {
        setResourceProperty(resource, SERVICE_DESIRED_STACK_PROPERTY_ID, repoVersion.getStackId(), requestedIds);
      }

      setResourceProperty(resource, SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID,
          response.getDesiredRepositoryVersionId(), requestedIds);

      setResourceProperty(resource, SERVICE_REPOSITORY_STATE,
          response.getRepositoryVersionState(), requestedIds);

      setResourceProperty(resource, SSO_INTEGRATION_SUPPORTED_PROPERTY_ID,
        response.isSsoIntegrationSupported(), requestedIds);
      setResourceProperty(resource, SSO_INTEGRATION_ENABLED_PROPERTY_ID,
        response.isSsoIntegrationEnabled(), requestedIds);
      setResourceProperty(resource, SSO_INTEGRATION_DESIRED_PROPERTY_ID,
        response.isSsoIntegrationDesired(), requestedIds);
      setResourceProperty(resource, SSO_INTEGRATION_REQUIRES_KERBEROS_PROPERTY_ID,
        response.isSsoIntegrationRequiresKerberos(), requestedIds);
      setResourceProperty(resource, KERBEROS_ENABLED_PROPERTY_ID,
          response.isKerberosEnabled(), requestedIds);

      setResourceProperty(resource, LDAP_INTEGRATION_SUPPORTED_PROPERTY_ID, response.isLdapIntegrationSupported(), requestedIds);
      setResourceProperty(resource, LDAP_INTEGRATION_ENABLED_PROPERTY_ID, response.isLdapIntegrationEnabled(), requestedIds);
      setResourceProperty(resource, LDAP_INTEGRATION_DESIRED_PROPERTY_ID, response.isLdapIntegrationDesired(), requestedIds);
      

      Map<String, Object> serviceSpecificProperties = getServiceSpecificProperties(
          response.getClusterName(), response.getServiceName(), requestedIds);

      for (Map.Entry<String, Object> entry : serviceSpecificProperties.entrySet()) {
        setResourceProperty(resource, entry.getKey(), entry.getValue(), requestedIds);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    RequestStageContainer requestStages = doUpdateResources(null, request, predicate);

    RequestStatusResponse response = null;
    if (requestStages != null) {
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
      response = requestStages.getRequestStatusResponse();
    }
    notifyUpdate(Resource.Type.Service, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException, AuthorizationException {
        return deleteServices(requests);
      }
    });

    notifyDelete(Resource.Type.Service, predicate);
    return getRequestStatus(response);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<>();

    for (String propertyId : propertyIds) {
      if (!propertyId.equals("config")) {
        String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);
        if (propertyCategory == null || !propertyCategory.equals("config")) {
          unsupportedProperties.add(propertyId);
        }
      }
    }
    unsupportedProperties.removeAll(AddServiceRequest.TOP_LEVEL_PROPERTIES);
    return unsupportedProperties;
  }


  // ----- AbstractResourceProvider ----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  // ----- utility methods -------------------------------------------------

  private RequestStageContainer doUpdateResources(final RequestStageContainer stages, final Request request, Predicate predicate)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<>();
    RequestStageContainer requestStages = null;

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }

      final boolean runSmokeTest = "true".equals(getQueryParameterValue(
          QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate));

      final boolean reconfigureClients = !"false".equals(getQueryParameterValue(
          QUERY_PARAMETERS_RECONFIGURE_CLIENT, predicate));

      final boolean startDependencies = "true".equals(getQueryParameterValue(
          QUERY_PARAMETERS_START_DEPENDENCIES, predicate));

      requestStages = modifyResources(new Command<RequestStageContainer>() {
        @Override
        public RequestStageContainer invoke() throws AmbariException, AuthorizationException {
          return updateServices(stages, requests, request.getRequestInfoProperties(),
              runSmokeTest, reconfigureClients, startDependencies);
        }
      });
    }
    return requestStages;
  }

  /**
   * Get a service request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the service request object
   */
  private ServiceRequest getRequest(Map<String, Object> properties) {

    String desiredRepoId = (String) properties.get(SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID);

    ServiceRequest svcRequest = new ServiceRequest(
        (String) properties.get(SERVICE_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(SERVICE_SERVICE_NAME_PROPERTY_ID),
        null == desiredRepoId ? null : Long.valueOf(desiredRepoId),
        (String) properties.get(SERVICE_SERVICE_STATE_PROPERTY_ID),
        (String) properties.get(SERVICE_CREDENTIAL_STORE_ENABLED_PROPERTY_ID));

    Object o = properties.get(SERVICE_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o) {
      svcRequest.setMaintenanceState(o.toString());
    }

    o = properties.get(SERVICE_CREDENTIAL_STORE_SUPPORTED_PROPERTY_ID);
    if (null != o) {
      svcRequest.setCredentialStoreSupported(o.toString());
    }

    return svcRequest;
  }

  // Create services from the given request.
  public void createServices(Set<ServiceRequest> requests)
      throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters clusters = getManagementController().getClusters();
    // do all validation checks
    validateCreateRequests(requests, clusters);

    for (ServiceRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());

      RepositoryVersionEntity repositoryVersion = request.getResolvedRepository();

      if (null == repositoryVersion) {
        throw new AmbariException("Could not find any repository on the request.");
      }

      if (repositoryVersion.getType() != RepositoryType.STANDARD
          && cluster.getProvisioningState() == State.INIT) {
        throw new AmbariException(String.format(
            "Unable to add %s to %s because the cluster is still being provisioned and the repository for the service is not %s: %s",
            request.getServiceName(), cluster.getClusterName(), RepositoryType.STANDARD,
            repositoryVersion));
      }

      Service s = cluster.addService(request.getServiceName(), repositoryVersion);

      /*
       * Get the credential_store_supported field only from the stack definition.
       * Not possible to update the value through a request.
       */
      StackId stackId = repositoryVersion.getStackId();
      AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
      ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
          stackId.getStackVersion(), request.getServiceName());

      boolean credentialStoreSupported = serviceInfo.isCredentialStoreSupported();
      boolean credentialStoreRequired = serviceInfo.isCredentialStoreRequired();

      LOG.info("Service: {}, credential_store_supported = {} and credential_store_required = {} from stack definition",
               request.getServiceName(), credentialStoreSupported, credentialStoreRequired);
      /*
       * If request does not have credential_store_enabled field,
       * then get the default from the stack definition.
       */
      if (StringUtils.isNotEmpty(request.getCredentialStoreEnabled())) {
        boolean credentialStoreEnabled = Boolean.parseBoolean(request.getCredentialStoreEnabled());
        boolean enableCredStore = credentialStoreSupported && (credentialStoreRequired || credentialStoreEnabled);
        s.setCredentialStoreEnabled(enableCredStore);
        LOG.info("Service: {}, credential_store_enabled = {} from request and resulting" +
                 " credential store enabled status is = {}",
                 request.getServiceName(), credentialStoreEnabled, enableCredStore);
      } else {
        boolean enableCredStore = credentialStoreSupported &&
                                  (credentialStoreRequired || serviceInfo.isCredentialStoreEnabled());
        s.setCredentialStoreEnabled(enableCredStore);
        LOG.info("Service: {}, credential_store_enabled = {} from stack definition and resulting" +
                 " credential store enabled status is = {}",
                 s.getName(), serviceInfo.isCredentialStoreEnabled(), enableCredStore);
      }

      // Initialize service widgets
      getManagementController().initializeWidgetsAndLayouts(cluster, s);
    }
  }

  // Get services from the given set of requests.
  protected Set<ServiceResponse> getServices(Set<ServiceRequest> requests)
      throws AmbariException {
    Set<ServiceResponse> response = new HashSet<>();
    for (ServiceRequest request : requests) {
      try {
        response.addAll(getServices(request));
      } catch (ServiceNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  // Get services from the given request.
  private Set<ServiceResponse> getServices(ServiceRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new AmbariException("Invalid arguments, cluster name"
          + " cannot be null");
    }
    Clusters clusters    = getManagementController().getClusters();
    String   clusterName = request.getClusterName();

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(clusterName);
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    Set<ServiceResponse> response = new HashSet<>();
    if (request.getServiceName() != null) {
      Service s = cluster.getService(request.getServiceName());
      response.add(s.convertToResponse());
      return response;
    }

    // TODO support search on predicates?

    boolean checkDesiredState = false;
    State desiredStateToCheck = null;
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      desiredStateToCheck = State.valueOf(request.getDesiredState());
      if (!desiredStateToCheck.isValidDesiredState()) {
        throw new IllegalArgumentException("Invalid arguments, invalid desired"
            + " state, desiredState=" + desiredStateToCheck);
      }
      checkDesiredState = true;
    }

    for (Service s : cluster.getServices().values()) {
      if (checkDesiredState
          && (desiredStateToCheck != s.getDesiredState())) {
        // skip non matching state
        continue;
      }
      response.add(s.convertToResponse());
    }
    return response;
  }

  // Update services based on the given requests.
  protected RequestStageContainer updateServices(RequestStageContainer requestStages, Set<ServiceRequest> requests,
                                                      Map<String, String> requestProperties, boolean runSmokeTest,
                                                      boolean reconfigureClients, boolean startDependencies) throws AmbariException, AuthorizationException {

    AmbariManagementController controller = getManagementController();

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Map<State, List<Service>> changedServices
        = new EnumMap<>(State.class);
    Map<State, List<ServiceComponent>> changedComps =
      new EnumMap<>(State.class);
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
      new HashMap<>();
    Collection<ServiceComponentHost> ignoredScHosts =
      new ArrayList<>();

    Set<String> clusterNames = new HashSet<>();
    Map<String, Set<String>> serviceNames = new HashMap<>();
    Set<State> seenNewStates = new HashSet<>();
    Map<Service, Boolean> serviceCredentialStoreEnabledMap = new HashMap<>();

    // Determine operation level
    Resource.Type reqOpLvl;
    if (requestProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      RequestOperationLevel operationLevel = new RequestOperationLevel(requestProperties);
      reqOpLvl = operationLevel.getLevel();
    } else {
      String message = "Can not determine request operation level. " +
              "Operation level property should " +
              "be specified for this request.";
      LOG.warn(message);
      reqOpLvl = Resource.Type.Cluster;
    }

    Clusters clusters = controller.getClusters();

    // We don't expect batch requests for different clusters, that's why
    // nothing bad should happen if value is overwritten few times
    for (ServiceRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, cluster name"
            + " and service name should be provided to update services");
      }

      LOG.info("Received a updateService request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", request=" + request);

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        throw new IllegalArgumentException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!serviceNames.containsKey(request.getClusterName())) {
        serviceNames.put(request.getClusterName(), new HashSet<>());
      }

      if (serviceNames.get(request.getClusterName())
          .contains(request.getServiceName())) {
        // TODO throw single exception
        throw new IllegalArgumentException("Invalid request contains duplicate"
            + " service names");
      }
      serviceNames.get(request.getClusterName()).add(request.getServiceName());

      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      State oldState = s.getDesiredState();
      State newState = null;
      if (request.getDesiredState() != null) {
        newState = State.valueOf(request.getDesiredState());
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState);
        }
      }

      // Setting Maintenance state for service
      if (null != request.getMaintenanceState()) {
        if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE)) {
          throw new AuthorizationException("The authenticated user is not authorized to toggle the maintainence state of services");
        }

        MaintenanceState newMaint = MaintenanceState.valueOf(request.getMaintenanceState());
        if (newMaint  != s.getMaintenanceState()) {
          if (newMaint.equals(MaintenanceState.IMPLIED_FROM_HOST)
              || newMaint.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set " +
              "maintenance state to one of " + EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            s.setMaintenanceState(newMaint);
          }
        }
      }

      /*
       * Get the credential_store_supported field only from the stack definition during creation.
       * Not possible to update the value of credential_store_supported through a request.
       */

      /*
       * Gather the credential_store_enabled field per service.
       */
      if (StringUtils.isNotEmpty(request.getCredentialStoreEnabled())) {
        boolean credentialStoreEnabled = Boolean.parseBoolean(request.getCredentialStoreEnabled());
        if (!s.isCredentialStoreSupported() && credentialStoreEnabled) {
          throw new IllegalArgumentException("Invalid arguments, cannot enable credential store " +
              "as it is not supported by the service. Service=" + s.getName());
        }
        if (s.isCredentialStoreRequired() && !credentialStoreEnabled) {
          throw new IllegalArgumentException("Invalid arguments, cannot disable credential store " +
                                             "as it is required by the service. Service=" + s.getName());
        }
        serviceCredentialStoreEnabledMap.put(s, credentialStoreEnabled);
        LOG.info("Service: {}, credential_store_enabled from request: {}", request.getServiceName(),
            credentialStoreEnabled);
      }

      if (StringUtils.isNotEmpty(request.getCredentialStoreSupported())) {
        throw new IllegalArgumentException("Invalid arguments, cannot update credential_store_supported " +
                                           "as it is set only via service definition. Service=" + s.getName());
      }

      if (newState == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateService request, clusterName={}, serviceName={}, newDesiredState=null",
            request.getClusterName(), request.getServiceName());
        }
        continue;
      }

      if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, s)) {
        LOG.info("Operations cannot be applied to service " + s.getName() +
            " in the maintenance state of " + s.getMaintenanceState());
        continue;
      }

      seenNewStates.add(newState);

      if (newState != oldState) {
        // The if user is trying to start or stop the service, ensure authorization
        if (((newState == State.INSTALLED) || (newState == State.STARTED)) &&
            !AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), RoleAuthorization.SERVICE_START_STOP)) {
          throw new AuthorizationException("The authenticated user is not authorized to start or stop services");
        }

        if (!State.isValidDesiredStateTransition(oldState, newState)) {
          throw new AmbariException("Invalid transition for"
              + " service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + s.getName()
              + ", currentDesiredState=" + oldState
              + ", newDesiredState=" + newState);

        }
        if (!changedServices.containsKey(newState)) {
          changedServices.put(newState, new ArrayList<>());
        }
        changedServices.get(newState).add(s);
      }

      // TODO should we check whether all servicecomponents and
      // servicecomponenthosts are in the required desired state?

      updateServiceComponents(requestStages, changedComps, changedScHosts,
        ignoredScHosts, reqOpLvl, s, newState);
    }

    if (startDependencies && changedServices.containsKey(State.STARTED)) {
      HashSet<Service> depServices = new HashSet<>();
      for (Service service : changedServices.get(State.STARTED)) {
        RoleCommandOrder rco = controller.getRoleCommandOrder(service.getCluster());
        Set<Service> dependencies = rco.getTransitiveServices(service, RoleCommand.START);
        for (Service dependency: dependencies) {
          if (!changedServices.get(State.STARTED).contains(dependency)){
            depServices.add(dependency);
          }
        }
      }

      for (Service service : depServices) {
        updateServiceComponents(requestStages, changedComps, changedScHosts,
          ignoredScHosts, reqOpLvl, service, State.STARTED);
        changedServices.get(State.STARTED).add(service);
      }

    }

    if (seenNewStates.size() > 1) {
      // TODO should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired state"
          + " changes for a set of services at the same time");
    }

    // update the credential store enabled information
    for (Map.Entry<Service, Boolean> serviceCredential : serviceCredentialStoreEnabledMap.entrySet()) {
      Service service = serviceCredential.getKey();
      Boolean credentialStoreEnabled = serviceCredential.getValue();
      service.setCredentialStoreEnabled(credentialStoreEnabled);
    }


    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return controller.addStages(requestStages, cluster, requestProperties,
      null, changedServices, changedComps, changedScHosts,
        ignoredScHosts, runSmokeTest, reconfigureClients, false);
  }

  private void updateServiceComponents(RequestStageContainer requestStages,
                                       Map<State, List<ServiceComponent>> changedComps,
                                       Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
                                       Collection<ServiceComponentHost> ignoredScHosts,
                                       Resource.Type reqOpLvl,
                                       Service service, State newState)
    throws AmbariException {

    Cluster cluster = service.getCluster();
    AmbariManagementController controller = getManagementController();
    AmbariMetaInfo ambariMetaInfo = controller.getAmbariMetaInfo();

    for (ServiceComponent sc : service.getServiceComponents().values()) {
      State oldScState = sc.getDesiredState();
      if (newState != oldScState) {
        if (sc.isClientComponent() &&
            !newState.isValidClientComponentState()) {
          continue;
        }
        if (!State.isValidDesiredStateTransition(oldScState, newState)) {
          throw new AmbariException("Invalid transition for"
              + " servicecomponent"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sc.getServiceName()
              + ", componentName=" + sc.getName()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }
        if (!changedComps.containsKey(newState)) {
          changedComps.put(newState, new ArrayList<>());
        }
        changedComps.get(newState).add(sc);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Handling update to ServiceComponent, clusterName={}, serviceName={}, componentName={}, currentDesiredState={}, newDesiredState={}",
          cluster.getClusterName(), service.getName(), sc.getName(), oldScState, newState);
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        State oldSchState = sch.getState();
        if (oldSchState == State.DISABLED || oldSchState == State.UNKNOWN) {
          //Ignore host components updates in this state
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost, clusterName={}, serviceName={}, componentName={}, hostname={}, currentState={}, newDesiredState={}",
              cluster.getClusterName(), service.getName(), sc.getName(), sch.getHostName(), oldSchState, newState);
          }
          continue;
        }
                                         //
        if (newState == oldSchState) {
          ignoredScHosts.add(sch);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost, clusterName={}, serviceName={}, componentName={}, hostname={}, currentState={}, newDesiredState={}",
              cluster.getClusterName(), service.getName(), sc.getName(), sch.getHostName(), oldSchState, newState);
          }
          continue;
        }

        if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, sch)) {
          ignoredScHosts.add(sch);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost, clusterName={}, serviceName={}, componentName={}, hostname={}",
              cluster.getClusterName(), service.getName(), sc.getName(), sch.getHostName());
          }
          continue;
        }

        if (sc.isClientComponent() &&
            !newState.isValidClientComponentState()) {
          continue;
        }
        /*
         * This is hack for now wherein we don't fail if the
         * sch is in INSTALL_FAILED
         */
        if (! isValidStateTransition(requestStages, oldSchState, newState, sch)) {
          String error = "Invalid transition for"
              + " servicecomponenthost"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sch.getServiceName()
              + ", componentName=" + sch.getServiceComponentName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState;
          StackId sid = service.getDesiredStackId();

          if ( ambariMetaInfo.getComponent(
              sid.getStackName(), sid.getStackVersion(), sc.getServiceName(),
              sch.getServiceComponentName()).isMaster()) {
            throw new IllegalArgumentException(error);
          } else {
            LOG.warn("Ignoring: " + error);
            continue;
          }
        }
        if (!changedScHosts.containsKey(sc.getName())) {
          changedScHosts.put(sc.getName(), new EnumMap<>(State.class));
        }
        if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
          changedScHosts.get(sc.getName()).put(newState, new ArrayList<>());
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponentHost, clusterName={}, serviceName={}, componentName={}, hostname={}, currentState={}, newDesiredState={}",
            cluster.getClusterName(), service.getName(), sc.getName(), sch.getHostName(), oldSchState, newState);
        }
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }
  }

  // Delete services based on the given set of requests
  protected RequestStatusResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException, AuthorizationException {

    Clusters clusters    = getManagementController().getClusters();

    Set<Service> removable = new HashSet<>();

    for (ServiceRequest serviceRequest : request) {
      if (StringUtils.isEmpty(serviceRequest.getClusterName()) || StringUtils.isEmpty(serviceRequest.getServiceName())) {
        // FIXME throw correct error
        throw new AmbariException("invalid arguments");
      } else {

        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, getClusterResourceId(serviceRequest.getClusterName()), RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
          throw new AuthorizationException("The user is not authorized to delete services");
        }

        Service service = clusters.getCluster(
            serviceRequest.getClusterName()).getService(
                serviceRequest.getServiceName());

        //
        // Run through the list of service component hosts. If all host components are in removable state,
        // the service can be deleted, irrespective of it's state.
        //
        List<ServiceComponentHost> nonRemovableComponents =  service.getServiceComponents().values().stream()
          .flatMap(sch -> sch.getServiceComponentHosts().values().stream())
          .filter(sch -> !sch.canBeRemoved())
          .collect(Collectors.toList());

        if (!nonRemovableComponents.isEmpty()) {
          for (ServiceComponentHost sch: nonRemovableComponents){
            String msg = String.format("Cannot remove %s/%s. %s on %s is in %s state.",
              serviceRequest.getClusterName(), serviceRequest.getServiceName(), sch.getServiceComponentName(),
              sch.getHost(), String.valueOf(sch.getState()));

            LOG.error(msg);
          }

          throw new AmbariException ("Cannot remove " +
                  serviceRequest.getClusterName() + "/" + serviceRequest.getServiceName() +
                    ". " + "One or more host components are in a non-removable state.");
        }

        removable.add(service);
      }
    }

    DeleteHostComponentStatusMetaData deleteMetaData = new DeleteHostComponentStatusMetaData();
    for (Service service : removable) {
      service.getCluster().deleteService(service.getName(), deleteMetaData);
      STOMPComponentsDeleteHandler.processDeleteByMetaDataException(deleteMetaData);
    }
    STOMPComponentsDeleteHandler.processDeleteByMetaData(deleteMetaData);

    return null;
  }

  // calculate the service state, accounting for the state of the host components
  private String calculateServiceState(String clusterName, String serviceName) {
    ServiceCalculatedState serviceCalculatedState = ServiceCalculatedStateFactory.getServiceStateProvider(serviceName);
    return serviceCalculatedState.getState(clusterName, serviceName).toString();
  }

  /**
   * Determine whether a service state change is valid.
   * Looks at projected state from the current stages associated with the request.
   *
   *
   * @param stages        request stages
   * @param startState    service start state
   * @param desiredState  service desired state
   * @param host          host where state change is occurring
   *
   * @return whether the state transition is valid
   */
  private boolean isValidStateTransition(RequestStageContainer stages, State startState,
                                         State desiredState, ServiceComponentHost host) {

    if (stages != null) {
      State projectedState = stages.getProjectedState(host.getHostName(), host.getServiceComponentName());
      startState = projectedState == null ? startState : projectedState;
    }

    return State.isValidStateTransition(startState, desiredState);
  }

  /**
   * Get any service specific properties for the request.
   *
   * @param clusterName  cluster name
   * @param serviceName  service name
   * @param requestedIds relevant request property ids
   */
  private Map<String, Object> getServiceSpecificProperties(String clusterName, String serviceName, Set<String> requestedIds) {
    Map<String, Object> serviceSpecificProperties = new HashMap<>();
    if (serviceName.equals("KERBEROS")) {
      // Only include details on whether the KDC administrator credentials are set and correct if
      // implicitly (Service/attributes) or explicitly (Service/attributes/kdc_...) queried
      if (requestedIds.contains(SERVICE_ATTRIBUTES_PROPERTY_ID) ||
          isPropertyCategoryRequested(SERVICE_ATTRIBUTES_PROPERTY_ID, requestedIds) ||
          isPropertyEntryRequested(SERVICE_ATTRIBUTES_PROPERTY_ID, requestedIds)) {
        Map<String, String> kerberosAttributes = new HashMap<>();
        String kdcValidationResult = "OK";
        String failureDetails = "";
        try {
          kerberosHelper.validateKDCCredentials(
              getManagementController().getClusters().getCluster(clusterName));

        } catch (KerberosInvalidConfigurationException e) {
          kdcValidationResult = "INVALID_CONFIGURATION";
          failureDetails = e.getMessage();
        } catch (KerberosAdminAuthenticationException e) {
          kdcValidationResult = "INVALID_CREDENTIALS";
          failureDetails = e.getMessage();
        } catch (KerberosMissingAdminCredentialsException e) {
          kdcValidationResult = "MISSING_CREDENTIALS";
          failureDetails = e.getMessage();
        } catch (AmbariException e) {
          kdcValidationResult = "VALIDATION_ERROR";
          failureDetails = e.getMessage();
        }

        kerberosAttributes.put("kdc_validation_result", kdcValidationResult);
        kerberosAttributes.put("kdc_validation_failure_details", failureDetails);
        serviceSpecificProperties.put(SERVICE_ATTRIBUTES_PROPERTY_ID, kerberosAttributes);
      }
    }

    return serviceSpecificProperties;
  }

  private void validateCreateRequests(Set<ServiceRequest> requests, Clusters clusters)
          throws AuthorizationException, AmbariException {

    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
    Map<String, Set<String>> serviceNames = new HashMap<>();
    Set<String> duplicates = new HashSet<>();

    for (ServiceRequest request : requests) {
      final String clusterName = request.getClusterName();
      final String serviceName = request.getServiceName();
      Validate.notEmpty(clusterName, "Cluster name should be provided when creating a service");
      Validate.notEmpty(serviceName, "Service name should be provided when creating a service");

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createService request, clusterName={}, serviceName={}, request={}", clusterName, serviceName, request);
      }

      if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, getClusterResourceId(clusterName), RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
        throw new AuthorizationException("The user is not authorized to create services");
      }

      if (!serviceNames.containsKey(clusterName)) {
        serviceNames.put(clusterName, new HashSet<>());
      }

      if (serviceNames.get(clusterName).contains(serviceName)) {
        // throw error later for dup
        duplicates.add(serviceName);
        continue;
      }
      serviceNames.get(clusterName).add(serviceName);

      if (StringUtils.isNotEmpty(request.getDesiredState())) {
        State state = State.valueOf(request.getDesiredState());
        if (!state.isValidDesiredState() || state != State.INIT) {
          throw new IllegalArgumentException("Invalid desired state"
                  + " only INIT state allowed during creation"
                  + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(clusterName);
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a service to a cluster which doesn't exist", e);
      }
      try {
        Service s = cluster.getService(serviceName);
        if (s != null) {
          // throw error later for dup
          duplicates.add(serviceName);
          continue;
        }
      } catch (ServiceNotFoundException e) {
        // Expected
      }

      Long desiredRepositoryVersion = request.getDesiredRepositoryVersionId();

      if (null == desiredRepositoryVersion) {
        Set<Long> repoIds = new HashSet<>();

        for (Service service : cluster.getServices().values()) {
          RepositoryVersionEntity serviceRepo = service.getDesiredRepositoryVersion();
          if (null != serviceRepo.getParentId()) {
            repoIds.add(serviceRepo.getParentId());
          } else {
            repoIds.add(serviceRepo.getId());
          }
        }

        LOG.info("{} was not specified; the following repository ids were found: {}",
            SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID, StringUtils.join(repoIds, ','));

        if (CollectionUtils.isEmpty(repoIds)) {
          throw new IllegalArgumentException("No repositories were found for service installation");
        } else if (repoIds.size() > 1) {
          throw new IllegalArgumentException(String.format("%s was not specified, and the cluster " +
              "contains more than one standard-type repository", SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID));
        } else {
          desiredRepositoryVersion = repoIds.iterator().next();
        }
      }

      if (null == desiredRepositoryVersion) {
        throw new IllegalArgumentException(String.format("%s is required when adding a service.", SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID));
      }

      RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByPK(desiredRepositoryVersion);

      if (null == repositoryVersion) {
        throw new IllegalArgumentException(String.format("Could not find any repositories defined by %d", desiredRepositoryVersion));
      }

      StackId stackId = repositoryVersion.getStackId();

      request.setResolvedRepository(repositoryVersion);

      if (!ambariMetaInfo.isValidService(stackId.getStackName(),
              stackId.getStackVersion(), request.getServiceName())) {
        throw new IllegalArgumentException("Unsupported or invalid service in stack, clusterName=" + clusterName
                + ", serviceName=" + serviceName + ", stackInfo=" + stackId.getStackId());
      }

      // validate the credential store input provided
      ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
          stackId.getStackVersion(), request.getServiceName());

      if (StringUtils.isNotEmpty(request.getCredentialStoreEnabled())) {
        boolean credentialStoreEnabled = Boolean.parseBoolean(request.getCredentialStoreEnabled());
        if (!serviceInfo.isCredentialStoreSupported() && credentialStoreEnabled) {
          throw new IllegalArgumentException("Invalid arguments, cannot enable credential store " +
              "as it is not supported by the service. Service=" + request.getServiceName());
        }
      }
    }
    // ensure only a single cluster update
    if (serviceNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments, updates allowed"
              + "on only one cluster at a time");
    }

    // Validate dups
    if (!duplicates.isEmpty()) {
      String clusterName = requests.iterator().next().getClusterName();
      String msg = "Attempted to create a service which already exists: "
              + ", clusterName=" + clusterName  + " serviceName=" + StringUtils.join(duplicates, ",");

      throw new DuplicateResourceException(msg);
    }

  }

  private static boolean isAddServiceRequest(Map<String, Object> properties) {
    return AddServiceRequest.OperationType.ADD_SERVICE.name().equals(properties.get(OPERATION_TYPE));
  }

  private RequestStatusResponse processAddServiceRequest(Map<String, Object> requestProperties, Map<String, String> requestInfoProperties) throws NoSuchParentResourceException {
    AddServiceRequest request = createAddServiceRequest(requestInfoProperties);
    String clusterName = String.valueOf(requestProperties.get(SERVICE_CLUSTER_NAME_PROPERTY_ID));
    try {
      return addServiceOrchestrator.processAddServiceRequest(getManagementController().getClusters().getCluster(clusterName), request);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    }
  }

  private static AddServiceRequest createAddServiceRequest(Map<String, String> requestInfoProperties) {
    try {
      return AddServiceRequest.of(requestInfoProperties.get(Request.REQUEST_INFO_BODY_PROPERTY));
    } catch (UncheckedIOException e) {
      return CHECK.wrapInUnchecked(e, IllegalArgumentException::new, "Could not parse input as valid Add Service request: " + e.getCause().getMessage());
    }
  }

}
