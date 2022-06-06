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

import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_INITIAL_INSTALL;
import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_INITIAL_START;
import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_PROPERTY;
import static org.apache.ambari.server.controller.internal.RequestResourceProvider.CONTEXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
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
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostDisableEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostRestoreEvent;
import org.apache.ambari.server.topology.Setting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for host component resources.
 */
public class HostComponentResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HostComponentResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  public static final String HOST_ROLES = "HostRoles";
  public static final String HOST = "host";
  public static final String SERVICE_COMPONENT_INFO = "ServiceComponentInfo";

  public static final String ROLE_ID_PROPERTY_ID = "role_id";
  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String SERVICE_NAME_PROPERTY_ID = "service_name";
  public static final String COMPONENT_NAME_PROPERTY_ID = "component_name";
  public static final String DISPLAY_NAME_PROPERTY_ID = "display_name";
  public static final String HOST_NAME_PROPERTY_ID = "host_name";
  public static final String PUBLIC_HOST_NAME_PROPERTY_ID = "public_host_name";
  public static final String STATE_PROPERTY_ID = "state";
  public static final String DESIRED_STATE_PROPERTY_ID = "desired_state";
  public static final String VERSION_PROPERTY_ID = "version";
  public static final String DESIRED_STACK_ID_PROPERTY_ID = "desired_stack_id";
  public static final String DESIRED_REPOSITORY_VERSION_PROPERTY_ID = "desired_repository_version";
  public static final String ACTUAL_CONFIGS_PROPERTY_ID = "actual_configs";
  public static final String STALE_CONFIGS_PROPERTY_ID = "stale_configs";
  public static final String RELOAD_CONFIGS_PROPERTY_ID = "reload_configs";
  public static final String DESIRED_ADMIN_STATE_PROPERTY_ID = "desired_admin_state";
  public static final String MAINTENANCE_STATE_PROPERTY_ID = "maintenance_state";
  public static final String UPGRADE_STATE_PROPERTY_ID = "upgrade_state";
  public static final String HOST_PROPERTY_ID = "host";
  public static final String HREF_PROPERTY_ID = "href";
  public static final String COMPONENT_PROPERTY_ID = "component";
  public static final String METRICS_PROPERTY_ID = "metrics";
  public static final String PROCESSES_PROPERTY_ID = "processes";

  public static final String ROLE_ID = PropertyHelper.getPropertyId(HOST_ROLES, ROLE_ID_PROPERTY_ID);
  public static final String CLUSTER_NAME = PropertyHelper.getPropertyId(HOST_ROLES, CLUSTER_NAME_PROPERTY_ID);
  public static final String SERVICE_NAME = PropertyHelper.getPropertyId(HOST_ROLES, SERVICE_NAME_PROPERTY_ID);
  public static final String COMPONENT_NAME = PropertyHelper.getPropertyId(HOST_ROLES, COMPONENT_NAME_PROPERTY_ID);
  public static final String DISPLAY_NAME = PropertyHelper.getPropertyId(HOST_ROLES, DISPLAY_NAME_PROPERTY_ID);
  public static final String HOST_NAME = PropertyHelper.getPropertyId(HOST_ROLES, HOST_NAME_PROPERTY_ID);
  public static final String PUBLIC_HOST_NAME = PropertyHelper.getPropertyId(HOST_ROLES, PUBLIC_HOST_NAME_PROPERTY_ID);
  public static final String STATE = PropertyHelper.getPropertyId(HOST_ROLES, STATE_PROPERTY_ID);
  public static final String DESIRED_STATE = PropertyHelper.getPropertyId(HOST_ROLES, DESIRED_STATE_PROPERTY_ID);
  public static final String VERSION = PropertyHelper.getPropertyId(HOST_ROLES, VERSION_PROPERTY_ID);
  public static final String DESIRED_STACK_ID = PropertyHelper.getPropertyId(HOST_ROLES, DESIRED_STACK_ID_PROPERTY_ID);
  public static final String DESIRED_REPOSITORY_VERSION = PropertyHelper.getPropertyId(HOST_ROLES, DESIRED_REPOSITORY_VERSION_PROPERTY_ID);
  public static final String ACTUAL_CONFIGS = PropertyHelper.getPropertyId(HOST_ROLES, ACTUAL_CONFIGS_PROPERTY_ID);
  public static final String STALE_CONFIGS = PropertyHelper.getPropertyId(HOST_ROLES, STALE_CONFIGS_PROPERTY_ID);
  public static final String RELOAD_CONFIGS = PropertyHelper.getPropertyId(HOST_ROLES, RELOAD_CONFIGS_PROPERTY_ID);
  public static final String DESIRED_ADMIN_STATE = PropertyHelper.getPropertyId(HOST_ROLES, DESIRED_ADMIN_STATE_PROPERTY_ID);
  public static final String MAINTENANCE_STATE = PropertyHelper.getPropertyId(HOST_ROLES, MAINTENANCE_STATE_PROPERTY_ID);
  public static final String UPGRADE_STATE = PropertyHelper.getPropertyId(HOST_ROLES, UPGRADE_STATE_PROPERTY_ID);

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID = "params/run_smoke_test";

  /**
   * The key property ids for a HostComponent resource.
   */
  public static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, CLUSTER_NAME)
      .put(Resource.Type.Host, HOST_NAME)
      .put(Resource.Type.HostComponent, COMPONENT_NAME)
      .put(Resource.Type.Component, COMPONENT_NAME)
      .build();

  /**
   * The property ids for a HostComponent resource.
   */
  protected static final Set<String> propertyIds = ImmutableSet.of(
      ROLE_ID,
      CLUSTER_NAME,
      SERVICE_NAME,
      COMPONENT_NAME,
      DISPLAY_NAME,
      HOST_NAME,
      PUBLIC_HOST_NAME,
      STATE,
      DESIRED_STATE,
      VERSION,
      DESIRED_STACK_ID,
      DESIRED_REPOSITORY_VERSION,
      ACTUAL_CONFIGS,
      STALE_CONFIGS,
      RELOAD_CONFIGS,
      DESIRED_ADMIN_STATE,
      MAINTENANCE_STATE,
      UPGRADE_STATE,
      QUERY_PARAMETERS_RUN_SMOKE_TEST_ID);

  public static final String SKIP_INSTALL_FOR_COMPONENTS = "skipInstallForComponents";
  public static final String DO_NOT_SKIP_INSTALL_FOR_COMPONENTS = "dontSkipInstallForComponents";
  public static final String ALL_COMPONENTS = "ALL";
  public static final String FOR_ALL_COMPONENTS = joinComponentList(ImmutableSet.of(ALL_COMPONENTS));
  public static final String FOR_NO_COMPONENTS = joinComponentList(ImmutableSet.of());

  /**
   * maintenance state helper
   */
  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  private HostVersionDAO hostVersionDAO;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  @AssistedInject
  public HostComponentResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.HostComponent, propertyIds, keyPropertyIds, managementController);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,RoleAuthorization.HOST_ADD_DELETE_COMPONENTS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,RoleAuthorization.HOST_ADD_DELETE_COMPONENTS));
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(changeRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        getManagementController().createHostComponents(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.HostComponent, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    return findResources(request, predicate, requests);
  }

  private Set<Resource> getResourcesForUpdate(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    return findResources(request, predicate, requests);
  }


  private Set<Resource> findResources(Request request, final Predicate predicate,
                                      final Set<ServiceComponentHostRequest> requests)
          throws SystemException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources = new HashSet<>();
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    // We always need host_name for sch
    requestedIds.add(HOST_NAME);

    Set<ServiceComponentHostResponse> responses = getResources(new Command<Set<ServiceComponentHostResponse>>() {
      @Override
      public Set<ServiceComponentHostResponse> invoke() throws AmbariException {
        return getManagementController().getHostComponents(requests);
      }
    });

    for (ServiceComponentHostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);
      setResourceProperty(resource, CLUSTER_NAME,
              response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_NAME,
              response.getServiceName(), requestedIds);
      setResourceProperty(resource, COMPONENT_NAME,
              response.getComponentName(), requestedIds);
      setResourceProperty(resource, DISPLAY_NAME,
              response.getDisplayName(), requestedIds);
      setResourceProperty(resource, HOST_NAME,
              response.getHostname(), requestedIds);
      setResourceProperty(resource, PUBLIC_HOST_NAME,
          response.getPublicHostname(), requestedIds);
      setResourceProperty(resource, STATE,
              response.getLiveState(), requestedIds);
      setResourceProperty(resource, DESIRED_STATE,
              response.getDesiredState(), requestedIds);
      setResourceProperty(resource, VERSION, response.getVersion(),
          requestedIds);
      setResourceProperty(resource, DESIRED_STACK_ID,
              response.getDesiredStackVersion(), requestedIds);
      setResourceProperty(resource, ACTUAL_CONFIGS,
              response.getActualConfigs(), requestedIds);
      setResourceProperty(resource, STALE_CONFIGS,
              response.isStaleConfig(), requestedIds);
      setResourceProperty(resource, RELOAD_CONFIGS,
              response.isReloadConfig(), requestedIds);
      setResourceProperty(resource, UPGRADE_STATE,
              response.getUpgradeState(), requestedIds);
      setResourceProperty(resource, DESIRED_REPOSITORY_VERSION,
          response.getDesiredRepositoryVersion(), requestedIds);

      if (response.getAdminState() != null) {
        setResourceProperty(resource, DESIRED_ADMIN_STATE,
                response.getAdminState(), requestedIds);
      }

      if (null != response.getMaintenanceState()) {
        setResourceProperty(resource, MAINTENANCE_STATE,
                response.getMaintenanceState(), requestedIds);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    if (request.getProperties().isEmpty()) {
      throw new IllegalArgumentException("Received an update request with no properties");
    }

    RequestStageContainer requestStages = doUpdateResources(null, request, predicate, false, false, false);

    RequestStatusResponse response = null;
    if (requestStages != null) {
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
      response = requestStages.getRequestStatusResponse();
      notifyUpdate(Resource.Type.HostComponent, request, predicate);
    }

    return getRequestStatus(response);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceComponentHostRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(changeRequest(propertyMap));
    }
    DeleteStatusMetaData deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
      @Override
      public DeleteStatusMetaData invoke() throws AmbariException, AuthorizationException {
        return getManagementController().deleteHostComponents(requests);
      }
    });

    notifyDelete(Resource.Type.HostComponent, predicate);

    return getRequestStatus(null, null, deleteStatusMetaData);
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
    return unsupportedProperties;
  }

  public RequestStatusResponse install(String cluster, String hostname, Collection<String> skipInstallForComponents,
                                       Collection<String> dontSkipInstallForComponents,
                                       boolean skipFailure, boolean useClusterHostInfo) throws  SystemException,
      UnsupportedPropertyException, NoSuchParentResourceException {

    RequestStageContainer requestStages;
    //for (String host : hosts) {

    Map<String, Object> installProperties = new HashMap<>();

    installProperties.put(DESIRED_STATE, "INSTALLED");
    Map<String, String> requestInfo = new HashMap<>();
    requestInfo.put(CONTEXT, String.format("Install components on host %s", hostname));
    requestInfo.put(CLUSTER_PHASE_PROPERTY, CLUSTER_PHASE_INITIAL_INSTALL);
    // although the operation is really for a specific host, the level needs to be set to HostComponent
    // to make sure that any service in maintenance mode does not prevent install/start on the new host during scale-up
    requestInfo.putAll(RequestOperationLevel.propertiesFor(Resource.Type.HostComponent, cluster));
    addProvisionActionProperties(skipInstallForComponents, dontSkipInstallForComponents, requestInfo);

    Request installRequest = PropertyHelper.getUpdateRequest(installProperties, requestInfo);

    Predicate statePredicate = new EqualsPredicate<>(STATE, "INIT");
    Predicate clusterPredicate = new EqualsPredicate<>(CLUSTER_NAME, cluster);
    // single host
    Predicate hostPredicate = new EqualsPredicate<>(HOST_NAME, hostname);
    //Predicate hostPredicate = new OrPredicate(hostPredicates.toArray(new Predicate[hostPredicates.size()]));
    Predicate hostAndStatePredicate = new AndPredicate(statePredicate, hostPredicate);
    Predicate installPredicate = new AndPredicate(hostAndStatePredicate, clusterPredicate);

    try {
      LOG.info("Installing all components on host: " + hostname);

      // we need send special parameters to send install/start commands with configs
      requestStages = doUpdateResources(null, installRequest, installPredicate, true,
          true, useClusterHostInfo);
      notifyUpdate(Resource.Type.HostComponent, installRequest, installPredicate);
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
    } catch (NoSuchResourceException e) {
      // shouldn't encounter this exception here
      throw new SystemException("An unexpected exception occurred while processing install hosts",  e);
      }

    return requestStages.getRequestStatusResponse();
  }

  @VisibleForTesting
  static void addProvisionActionProperties(Collection<String> skipInstallForComponents, Collection<String> dontSkipInstallForComponents, Map<String, String> requestInfo) {
    requestInfo.put(SKIP_INSTALL_FOR_COMPONENTS, joinComponentList(skipInstallForComponents));
    requestInfo.put(DO_NOT_SKIP_INSTALL_FOR_COMPONENTS, joinComponentList(dontSkipInstallForComponents));
  }

  public static String joinComponentList(Collection<String> components) {
    return components != null
      ? ";" + String.join(";", components) + ";"
      : "";
  }

  public static boolean shouldSkipInstallTaskForComponent(String componentName, boolean isClientComponent, Map<String, String> requestProperties) {
    // Skip INSTALL for service components if START_ONLY is set for component, or if START_ONLY is set on cluster
    // level and no other provision action is specified for component
    String skipInstallForComponents = requestProperties.get(SKIP_INSTALL_FOR_COMPONENTS);
    String searchString = joinComponentList(ImmutableSet.of(componentName));
    return !isClientComponent &&
      CLUSTER_PHASE_INITIAL_INSTALL.equals(requestProperties.get(CLUSTER_PHASE_PROPERTY)) &&
      skipInstallForComponents != null &&
      (skipInstallForComponents.contains(searchString) ||
        (skipInstallForComponents.equals(FOR_ALL_COMPONENTS) &&
          !requestProperties.get(DO_NOT_SKIP_INSTALL_FOR_COMPONENTS).contains(searchString))
      );
  }

  // TODO, revisit this extra method, that appears to be used during Add Hosts
  // TODO, How do we determine the component list for INSTALL_ONLY during an Add Hosts operation? rwn
  public RequestStatusResponse start(String cluster, String hostName) throws  SystemException,
    UnsupportedPropertyException, NoSuchParentResourceException {

    return this.start(cluster, hostName, Collections.emptySet(), false, false);
  }

  public RequestStatusResponse start(String cluster, String hostName, Collection<String> installOnlyComponents,
                                     boolean skipFailure, boolean useClusterHostInfo) throws  SystemException,
      UnsupportedPropertyException, NoSuchParentResourceException {

    Map<String, String> requestInfo = new HashMap<>();
    requestInfo.put(CONTEXT, String.format("Start components on host %s", hostName));
    requestInfo.put(CLUSTER_PHASE_PROPERTY, CLUSTER_PHASE_INITIAL_START);
    // see rationale for marking the operation as HostComponent-level at "Install components on host"
    requestInfo.putAll(RequestOperationLevel.propertiesFor(Resource.Type.HostComponent, cluster));
    requestInfo.put(Setting.SETTING_NAME_SKIP_FAILURE, Boolean.toString(skipFailure));

    Predicate clusterPredicate = new EqualsPredicate<>(CLUSTER_NAME, cluster);
    Predicate hostPredicate = new EqualsPredicate<>(HOST_NAME, hostName);
    //Predicate hostPredicate = new OrPredicate(hostPredicates.toArray(new Predicate[hostPredicates.size()]));

    RequestStageContainer requestStages;
    try {
      Map<String, Object> startProperties = new HashMap<>();
      startProperties.put(DESIRED_STATE, "STARTED");
      Request startRequest = PropertyHelper.getUpdateRequest(startProperties, requestInfo);
      // Important to query against desired_state as this has been updated when install stage was created
      // If I query against state, then the getRequest compares predicate prop against desired_state and then when the predicate
      // is later applied explicitly, it gets compared to live_state. Since live_state == INSTALLED == INIT at this point and
      // desired_state == INSTALLED, we will always get 0 matches since both comparisons can't be true :(
      Predicate installedStatePredicate = new EqualsPredicate<>(DESIRED_STATE, "INSTALLED");
      Predicate notClientPredicate = new NotPredicate(new ClientComponentPredicate());
      Predicate clusterAndClientPredicate = new AndPredicate(clusterPredicate, notClientPredicate);
      Predicate hostAndStatePredicate = new AndPredicate(installedStatePredicate, hostPredicate);
      Predicate startPredicate;

      if (installOnlyComponents.isEmpty()) {
        // all installed components should be started
        startPredicate = new AndPredicate(clusterAndClientPredicate, hostAndStatePredicate);
        LOG.info("Starting all non-client components on host: " + hostName);
      } else {
        // any INSTALL_ONLY components should not be started
        List<Predicate> listOfComponentPredicates =
          new ArrayList<>();

        for (String installOnlyComponent : installOnlyComponents) {
          Predicate componentNameEquals = new EqualsPredicate<>(COMPONENT_NAME, installOnlyComponent);
          // create predicate to filter out the install only component
          listOfComponentPredicates.add(new NotPredicate(componentNameEquals));
        }

        Predicate[] arrayOfInstallOnlyPredicates = new Predicate[listOfComponentPredicates.size()];
        // aggregate Predicate of all INSTALL_ONLY component names
        Predicate installOnlyComponentsPredicate = new AndPredicate(listOfComponentPredicates.toArray(arrayOfInstallOnlyPredicates));

        // start predicate must now include the INSTALL_ONLY component predicates, in
        // order to filter out those components for START attempts
        startPredicate = new AndPredicate(clusterAndClientPredicate, hostAndStatePredicate, installOnlyComponentsPredicate);
        LOG.info("Starting all non-client components on host: " + hostName + ", except for the INSTALL_ONLY components specified: " + installOnlyComponents);
      }


      requestStages = doUpdateResources(null, startRequest, startPredicate, true,
          true, useClusterHostInfo);
      notifyUpdate(Resource.Type.HostComponent, startRequest, startPredicate);
      try {
        requestStages.persist();
      } catch (AmbariException e) {
        throw new SystemException(e.getMessage(), e);
      }
    } catch (NoSuchResourceException e) {
      // shouldn't encounter this exception here
      throw new SystemException("An unexpected exception occurred while processing start hosts",  e);
    }

    return requestStages.getRequestStatusResponse();
  }

  /**
   * Update the host component identified by the given request object with the
   * values carried by the given request object.
   *
   * @param stages             stages of the associated request
   * @param requests           the request object which defines which host component to
   *                           update and the values to set
   * @param requestProperties  the request properties
   * @param runSmokeTest       indicates whether or not to run a smoke test
   *
   * @return a track action response
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  //todo: This was moved from AmbariManagementController and needs a lot of refactoring.
  //todo: Look into using the predicate instead of Set<ServiceComponentHostRequest>
  //todo: change to private access when all AMC tests have been moved.
  protected RequestStageContainer updateHostComponents(RequestStageContainer stages,
                                                                    Set<ServiceComponentHostRequest> requests,
                                                                    Map<String, String> requestProperties,
                                                                    boolean runSmokeTest, boolean useGeneratedConfigs,
                                                                    boolean useClusterHostInfo) throws AmbariException, AuthorizationException {

    Clusters clusters = getManagementController().getClusters();


    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts = new HashMap<>();
    Collection<ServiceComponentHost> ignoredScHosts = new ArrayList<>();
    Set<String> clusterNames = new HashSet<>();
    Map<String, Map<String, Map<String, Set<String>>>> requestClusters = new HashMap<>();
    Map<ServiceComponentHost, State> directTransitionScHosts = new HashMap<>();

    Resource.Type reqOpLvl = determineOperationLevel(requestProperties);

    String clusterName = requestProperties.get(RequestOperationLevel.OPERATION_CLUSTER_ID);
    if (clusterName != null && !clusterName.isEmpty()) {
      clusterNames.add(clusterName);
    }

    for (ServiceComponentHostRequest request : requests) {
      validateServiceComponentHostRequest(request);

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if(runSmokeTest) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), RoleAuthorization.SERVICE_RUN_SERVICE_CHECK)) {
          throw new AuthorizationException("The authenticated user is not authorized to run service checks");
        }
      }

      if (StringUtils.isEmpty(request.getServiceName())) {
        request.setServiceName(getManagementController().findServiceName(cluster, request.getComponentName()));
      }

      ServiceComponent sc = getServiceComponent(
          request.getClusterName(), request.getServiceName(), request.getComponentName());

      logRequestInfo("Received a updateHostComponent request", request);

      if((clusterName == null || clusterName.isEmpty())
              && (request.getClusterName() != null
              && !request.getClusterName().isEmpty())) {
        clusterNames.add(request.getClusterName());
      }

      if (clusterNames.size() > 1) {
        throw new IllegalArgumentException("Updates to multiple clusters is not"
            + " supported");
      }

      // maps of cluster->services, services->components, components->hosts
      Map<String, Map<String, Set<String>>> clusterServices = requestClusters.get(request.getClusterName());
      if (clusterServices == null) {
        clusterServices = new HashMap<>();
        requestClusters.put(request.getClusterName(), clusterServices);
      }

      Map<String, Set<String>> serviceComponents = clusterServices.get(request.getServiceName());
      if (serviceComponents == null) {
        serviceComponents = new HashMap<>();
        clusterServices.put(request.getServiceName(), serviceComponents);
      }

      Set<String> componentHosts = serviceComponents.get(request.getComponentName());
      if (componentHosts == null) {
        componentHosts = new HashSet<>();
        serviceComponents.put(request.getComponentName(), componentHosts) ;
      }

      if (componentHosts.contains(request.getHostname())) {
        throw new IllegalArgumentException("Invalid request contains duplicate hostcomponents");
      }

      componentHosts.add(request.getHostname());


      ServiceComponentHost sch = sc.getServiceComponentHost(request.getHostname());
      State oldState = sch.getState();
      State newState = null;
      if (request.getDesiredState() != null) {
        // set desired state on host component
        newState = State.valueOf(request.getDesiredState());

        // throw exception if desired state isn't a valid desired state (static check)
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState);
        }
      }

      // Setting Maintenance state for host component
      if (null != request.getMaintenanceState()) {
        MaintenanceState newMaint = MaintenanceState.valueOf(request.getMaintenanceState());
        MaintenanceState oldMaint = maintenanceStateHelper.getEffectiveState(sch);

        if (newMaint != oldMaint) {
          if (sc.isClientComponent()) {
            throw new IllegalArgumentException("Invalid arguments, cannot set maintenance state on a client component");
          } else if (newMaint.equals(MaintenanceState.IMPLIED_FROM_HOST)  || newMaint.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set maintenance state to one of " +
                EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            sch.setMaintenanceState(newMaint);
          }
        }
      }

      if (newState == null) {
        LOG.info(getServiceComponentRequestInfoLogMessage("Nothing to do for new updateServiceComponentHost", request, oldState, null));
        continue;
      }

      if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
          EnumSet.of(RoleAuthorization.SERVICE_START_STOP, RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,
              RoleAuthorization.HOST_ADD_DELETE_COMPONENTS, RoleAuthorization.HOST_ADD_DELETE_HOSTS))) {
        throw new AuthorizationException("The authenticated user is not authorized to change the state of service components");
      }

      // STARTED state is invalid for the client component, but this shouldn't cancel the whole stage
      if (sc.isClientComponent() && newState == State.STARTED &&
            !requestProperties.containsKey(sch.getServiceComponentName().toLowerCase())) {
        ignoredScHosts.add(sch);
        LOG.info(getServiceComponentRequestInfoLogMessage("Ignoring ServiceComponentHost as STARTED new desired state for client components is not valid", request, sch.getState(), newState));
        continue;
      }

      if (sc.isClientComponent() &&
          !newState.isValidClientComponentState()) {
        throw new IllegalArgumentException("Invalid desired state for a client"
            + " component");
      }

      State oldSchState = sch.getState();
      // Client component reinstall allowed
      if (newState == oldSchState && !sc.isClientComponent() &&
          !requestProperties.containsKey(sch.getServiceComponentName().toLowerCase())) {

        ignoredScHosts.add(sch);
        LOG.info(getServiceComponentRequestInfoLogMessage("Ignoring ServiceComponentHost as the current state matches the new desired state", request, oldState, newState));
        continue;
      }

      if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, sch)) {
        ignoredScHosts.add(sch);
        LOG.info(getServiceComponentRequestInfoLogMessage("Ignoring ServiceComponentHost as operation is not allowed", request, oldState, newState));
        continue;
      }

      if (! isValidStateTransition(stages, oldSchState, newState, sch)) {
        throw new AmbariException("Invalid state transition for host component"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + sch.getServiceName()
            + ", componentName=" + sch.getServiceComponentName()
            + ", hostname=" + sch.getHostName()
            + ", currentState=" + oldSchState
            + ", newDesiredState=" + newState);
      }

      if (isDirectTransition(oldSchState, newState)) {
        LOG.info(getServiceComponentRequestInfoLogMessage("Handling direct transition update to host component", request, oldState, newState));
        directTransitionScHosts.put(sch, newState);
      } else {
        if (!changedScHosts.containsKey(sc.getName())) {
          changedScHosts.put(sc.getName(), new EnumMap<>(State.class));
        }
        if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
          changedScHosts.get(sc.getName()).put(newState, new ArrayList<>());
        }
        LOG.info(getServiceComponentRequestInfoLogMessage("Handling update to host component", request, oldState, newState));
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }

    doDirectTransitions(directTransitionScHosts);

    // just getting the first cluster
    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return getManagementController().addStages(
        stages, cluster, requestProperties, null, null, null,
        changedScHosts, ignoredScHosts, runSmokeTest, false, useGeneratedConfigs, useClusterHostInfo);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- utility methods -------------------------------------------------

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties the predicate
   * @return the component request object
   */
  private ServiceComponentHostRequest getRequest(Map<String, Object> properties) {
    ServiceComponentHostRequest serviceComponentHostRequest = new ServiceComponentHostRequest(
        (String) properties.get(CLUSTER_NAME),
        (String) properties.get(SERVICE_NAME),
        (String) properties.get(COMPONENT_NAME),
        (String) properties.get(HOST_NAME),
        (String) properties.get(DESIRED_STATE));
    serviceComponentHostRequest.setState((String) properties.get(STATE));
    if (properties.get(STALE_CONFIGS) != null) {
      serviceComponentHostRequest.setStaleConfig(
          properties.get(STALE_CONFIGS).toString().toLowerCase());
    }

    if (properties.get(DESIRED_ADMIN_STATE) != null) {
      serviceComponentHostRequest.setAdminState(
          properties.get(DESIRED_ADMIN_STATE).toString());
    }
    if (properties.get(PUBLIC_HOST_NAME) != null) {
      serviceComponentHostRequest.setPublicHostname(
          properties.get(PUBLIC_HOST_NAME).toString());
    }


    Object o = properties.get(MAINTENANCE_STATE);
    if (null != o) {
      serviceComponentHostRequest.setMaintenanceState (o.toString());
    }

    return serviceComponentHostRequest;
  }

  /**
   * Put changes to component request object from a map of property values.
   *
   * @param properties the predicate
   * @return the component request object
   */
  private ServiceComponentHostRequest changeRequest(Map<String, Object> properties) {
    ServiceComponentHostRequest serviceComponentHostRequest = new ServiceComponentHostRequest(
            (String) properties.get(CLUSTER_NAME),
            (String) properties.get(SERVICE_NAME),
            (String) properties.get(COMPONENT_NAME),
            (String) properties.get(HOST_NAME),
            (String) properties.get(STATE));
    if (properties.get(DESIRED_STATE) != null) {
      serviceComponentHostRequest.setDesiredState((String)properties.get(DESIRED_STATE));
    }
    if (properties.get(STALE_CONFIGS) != null) {
      serviceComponentHostRequest.setStaleConfig(
              properties.get(STALE_CONFIGS).toString().toLowerCase());
    }

    if (properties.get(DESIRED_ADMIN_STATE) != null) {
      serviceComponentHostRequest.setAdminState(
              properties.get(DESIRED_ADMIN_STATE).toString());
    }

    Object o = properties.get(MAINTENANCE_STATE);
    if (null != o) {
      serviceComponentHostRequest.setMaintenanceState (o.toString());
    }

    return serviceComponentHostRequest;
  }

  /**
   * Update resources.
   *
   * @param stages                  request stage container
   * @param request                 request
   * @param predicate               request predicate
   * @param performQueryEvaluation  should query be evaluated for matching resource set
   * @param useGeneratedConfigs     should update request contains actual configs
   * @param useClusterHostInfo      should update request contain cluster topology info
   * @return
   * @throws UnsupportedPropertyException   an unsupported property was specified in the request
   * @throws SystemException                an unknown exception occurred
   * @throws NoSuchResourceException        the query didn't match any resources
   * @throws NoSuchParentResourceException  a specified parent resource doesn't exist
   */
  public RequestStageContainer doUpdateResources(final RequestStageContainer stages, final Request request,
                                                  Predicate predicate, boolean performQueryEvaluation,
                                                  boolean useGeneratedConfigs, boolean useClusterHostInfo)
                                                  throws UnsupportedPropertyException,
                                                         SystemException,
                                                         NoSuchResourceException,
                                                         NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<>();

    final boolean runSmokeTest = "true".equals(getQueryParameterValue(
        QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate));

    Set<String> queryIds = Collections.singleton(COMPONENT_NAME);

    Request queryRequest = PropertyHelper.getReadRequest(queryIds);
    // will take care of 404 exception

    Set<Resource> matchingResources = getResourcesForUpdate(queryRequest, predicate);

    for (Resource queryResource : matchingResources) {
      //todo: predicate evaluation was removed for BUG-28737 and the removal of this breaks
      //todo: the new "add hosts" api.  BUG-4818 is the root cause and needs to be addressed
      //todo: and then this predicate evaluation should always be performed and the
      //todo: temporary performQueryEvaluation flag hack should be removed.
      if (! performQueryEvaluation || predicate.evaluate(queryResource)) {
        Map<String, Object> updateRequestProperties = new HashMap<>();

        // add props from query resource
        updateRequestProperties.putAll(PropertyHelper.getProperties(queryResource));

        // add properties from update request
        //todo: should we flag value size > 1?
        if (request.getProperties() != null && request.getProperties().size() != 0) {
          updateRequestProperties.putAll(request.getProperties().iterator().next());
        }
        requests.add(changeRequest(updateRequestProperties));
      }
    }

    if (requests.isEmpty()) {
      String msg = String.format("Skipping updating hosts: no matching requests for %s", predicate);
      LOG.info(msg);
      throw new NoSuchResourceException(msg);
    }

    RequestStageContainer requestStages = modifyResources(new Command<RequestStageContainer>() {
      @Override
      public RequestStageContainer invoke() throws AmbariException {
        RequestStageContainer stageContainer = null;
        try {
          stageContainer = updateHostComponents(stages, requests, request.getRequestInfoProperties(),
              runSmokeTest, useGeneratedConfigs, useClusterHostInfo);
        } catch (Exception e) {
          LOG.info("Caught an exception while updating host components, will not try again: {}", e.getMessage(), e);
          // !!! IllegalArgumentException results in a 400 response, RuntimeException results in 500.
          if (e instanceof IllegalArgumentException) {
            throw (IllegalArgumentException) e;
          } else {
            throw new RuntimeException("Update Host request submission failed: " + e, e);
          }
        }

        return stageContainer;
      }
    });
    notifyUpdate(Resource.Type.HostComponent, request, predicate);
    return requestStages;
  }


  /**
   * Determine whether a host component state change is valid.
   * Looks at projected state from the current stages associated with the request.
   *
   *
   * @param stages        request stages
   * @param startState    host component start state
   * @param desiredState  host component desired state
   * @param host          host where state change is occurring
   *
   * @return whether the state transition is valid
   */
  private boolean isValidStateTransition(RequestStageContainer stages, State startState,
                                         State desiredState, ServiceComponentHost host) {
    //todo: After separating install and start, the install stage is no longer included in the passed in request stage container
    //todo: so we need to re-evaluate this getting projected state from topology manager
    return true;
//    if (stages != null) {
//      State projectedState = stages.getProjectedState(host.getHostName(), host.getServiceComponentName());
//      startState = projectedState == null ? startState : projectedState;
//    }
//
//    return State.isValidStateTransition(startState, desiredState);
  }

  /**
   * Checks if assigning new state does not require performing
   * any additional actions
   */
  public boolean isDirectTransition(State oldState, State newState) {
    switch (newState) {
      case INSTALLED:
        if (oldState == State.DISABLED) {
          return true;
        }
        break;
      case DISABLED:
        if (oldState == State.INSTALLED ||
            oldState == State.INSTALL_FAILED ||
            oldState == State.UNKNOWN) {
          return true;
        }
        break;
      default:
        break;
    }
    return false;
  }

  private ServiceComponent getServiceComponent(String clusterName, String serviceName, String componentName)
      throws AmbariException {

    Clusters clusters = getManagementController().getClusters();
    return clusters.getCluster(clusterName).getService(serviceName).getServiceComponent(componentName);
  }

  // Perform direct transitions (without task generation)
  private void doDirectTransitions(Map<ServiceComponentHost, State> directTransitionScHosts) throws AmbariException {
    for (Map.Entry<ServiceComponentHost, State> entry : directTransitionScHosts.entrySet()) {
      ServiceComponentHost componentHost = entry.getKey();
      State newState = entry.getValue();
      long timestamp = System.currentTimeMillis();
      ServiceComponentHostEvent event;
      componentHost.setDesiredState(newState);
      switch (newState) {
        case DISABLED:
          event = new ServiceComponentHostDisableEvent(
              componentHost.getServiceComponentName(),
              componentHost.getHostName(),
              timestamp);
          break;
        case INSTALLED:
          event = new ServiceComponentHostRestoreEvent(
              componentHost.getServiceComponentName(),
              componentHost.getHostName(),
              timestamp);
          break;
        default:
          throw new AmbariException("Direct transition from " + componentHost.getState() + " to " + newState + " not supported");
      }
      try {
        componentHost.handleEvent(event);
      } catch (InvalidStateTransitionException e) {
        //Should not occur, must be covered by previous checks
        throw new AmbariException("Internal error - not supported transition", e);
      }
    }
  }

  /**
   * Logs request info.
   *
   * @param msg      base log msg
   * @param request  the request to log
   */
  private void logRequestInfo(String msg, ServiceComponentHostRequest request) {
    LOG.info("{}, clusterName={}, serviceName={}, componentName={}, hostname={}, request={}",
        msg,
        request.getClusterName(),
        request.getServiceName(),
        request.getComponentName(),
        request.getHostname(),
        request);
  }

  /**
   * Constructs INFO level log message for {@link ServiceComponentHostRequest}
   * @param msg base  message
   * @param request the request to construct the log message for
   * @param oldState current state of the service host component that the request is for.
   * @param newDesiredState new desired state for the service host component
   */
  private String getServiceComponentRequestInfoLogMessage(String msg, ServiceComponentHostRequest request, State oldState, State newDesiredState) {
    StringBuilder sb = new StringBuilder();

    sb.append(msg)
      .append(", clusterName=").append(request.getClusterName())
      .append(", serviceName=").append(request.getServiceName())
      .append(", componentName=").append(request.getComponentName())
      .append(", hostname=").append(request.getHostname())
      .append(", currentState=").append(oldState == null ? "null" : oldState)
      .append(", newDesiredState=").append(newDesiredState == null ? "null" : newDesiredState);

    return sb.toString();
  }

  /**
   * Get the "operation level" from the request.
   *
   * @param requestProperties  request properties
   * @return  the "operation level"
   */
  private Resource.Type determineOperationLevel(Map<String, String> requestProperties) {
    // Determine operation level
    Resource.Type reqOpLvl;
    if (requestProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      reqOpLvl = new RequestOperationLevel(requestProperties).getLevel();
    } else {
      String message = "Can not determine request operation level. " +
          "Operation level property should " +
          "be specified for this request.";
      LOG.warn(message);
      reqOpLvl = Resource.Type.Cluster;
    }
    return reqOpLvl;
  }

  /**
   * Validate a host component request.
   *
   * @param request  request to validate
   * @throws IllegalArgumentException if the request is invalid
   */
  private void validateServiceComponentHostRequest(ServiceComponentHostRequest request) {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()
        || request.getHostname() == null
        || request.getHostname().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments"
          + ", cluster name, component name and host name should be"
          + " provided");
    }

    if (request.getAdminState() != null) {
      throw new IllegalArgumentException("Property adminState cannot be modified through update. Use service " +
          "specific DECOMMISSION action to decommision/recommission components.");
    }
  }


  // ----- inner classes ---------------------------------------------------

  /**
   * Predicate that identifies client components.
   */
  private  class ClientComponentPredicate implements Predicate {
    @Override
    public boolean evaluate(Resource resource) {
      boolean isClient = false;

      String componentName = (String) resource.getPropertyValue(COMPONENT_NAME);
      try {
        if (componentName != null && !componentName.isEmpty()) {
          AmbariManagementController managementController = getManagementController();
          String clusterName = (String) resource.getPropertyValue(CLUSTER_NAME);
          String serviceName = (String) resource.getPropertyValue(SERVICE_NAME);
          if (StringUtils.isEmpty(serviceName)) {
            Cluster cluster = managementController.getClusters().getCluster(clusterName);
            serviceName = managementController.findServiceName(cluster, componentName);
          }

          ServiceComponent sc = getServiceComponent((String) resource.getPropertyValue(
                  CLUSTER_NAME), serviceName, componentName);
          isClient = sc.isClientComponent();
        }
      } catch (AmbariException e) {
        // this is really a system exception since cluster/service should have been already verified
        throw new RuntimeException(
            "An unexpected exception occurred while trying to determine if a component is a client", e);
      }
      return isClient;
    }
  }
}
