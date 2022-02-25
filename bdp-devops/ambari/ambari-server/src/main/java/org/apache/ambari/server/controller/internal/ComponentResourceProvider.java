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
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.STOMPComponentsDeleteHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

/**
 * Resource provider for component resources.
 */
public class ComponentResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ComponentResourceProvider.class);

  public static final String SERVICE_COMPONENT_INFO = "ServiceComponentInfo";

  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String SERVICE_NAME_PROPERTY_ID = "service_name";
  public static final String COMPONENT_NAME_PROPERTY_ID  = "component_name";
  public static final String DISPLAY_NAME_PROPERTY_ID = "display_name";
  public static final String STATE_PROPERTY_ID = "state";
  public static final String CATEGORY_PROPERTY_ID = "category";
  public static final String TOTAL_COUNT_PROPERTY_ID = "total_count";
  public static final String STARTED_COUNT_PROPERTY_ID = "started_count";
  public static final String INSTALLED_COUNT_PROPERTY_ID = "installed_count";
  public static final String INSTALLED_AND_MAINTENANCE_OFF_COUNT_PROPERTY_ID = "installed_and_maintenance_off_count";
  public static final String INIT_COUNT_PROPERTY_ID = "init_count";
  public static final String UNKNOWN_COUNT_PROPERTY_ID = "unknown_count";
  public static final String INSTALL_FAILED_COUNT_PROPERTY_ID = "install_failed_count";
  public static final String RECOVERY_ENABLED_PROPERTY_ID = "recovery_enabled";
  public static final String DESIRED_STACK_PROPERTY_ID = "desired_stack";
  public static final String DESIRED_VERSION_PROPERTY_ID = "desired_version";
  public static final String REPOSITORY_STATE_PROPERTY_ID = "repository_state";

  // Components
  public static final String CLUSTER_NAME = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME_PROPERTY_ID;
  public static final String SERVICE_NAME = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + SERVICE_NAME_PROPERTY_ID;
  public static final String COMPONENT_NAME = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + COMPONENT_NAME_PROPERTY_ID;
  public static final String DISPLAY_NAME = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + DISPLAY_NAME_PROPERTY_ID;
  public static final String STATE = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + STATE_PROPERTY_ID;
  public static final String CATEGORY = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + CATEGORY_PROPERTY_ID;
  public static final String TOTAL_COUNT = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + TOTAL_COUNT_PROPERTY_ID;
  public static final String STARTED_COUNT = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + STARTED_COUNT_PROPERTY_ID;
  public static final String INSTALLED_COUNT = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + INSTALLED_COUNT_PROPERTY_ID;
  public static final String INSTALLED_AND_MAINTENANCE_OFF_COUNT =
          SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + INSTALLED_AND_MAINTENANCE_OFF_COUNT_PROPERTY_ID;
  public static final String INIT_COUNT = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + INIT_COUNT_PROPERTY_ID;
  public static final String UNKNOWN_COUNT = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + UNKNOWN_COUNT_PROPERTY_ID;
  public static final String INSTALL_FAILED_COUNT = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + INSTALL_FAILED_COUNT_PROPERTY_ID;
  public static final String RECOVERY_ENABLED = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + RECOVERY_ENABLED_PROPERTY_ID;
  public static final String DESIRED_STACK = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + DESIRED_STACK_PROPERTY_ID;
  public static final String DESIRED_VERSION = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + DESIRED_VERSION_PROPERTY_ID;
  public static final String REPOSITORY_STATE = SERVICE_COMPONENT_INFO + PropertyHelper.EXTERNAL_PATH_SEP + REPOSITORY_STATE_PROPERTY_ID;

  
  private static final String TRUE = "true";

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID = "params/run_smoke_test";

  private static final Set<String> pkPropertyIds = Sets.newHashSet(
          CLUSTER_NAME,
          SERVICE_NAME,
          COMPONENT_NAME);

  /**
   * The property ids for an servce resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for an service resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(CLUSTER_NAME);
    PROPERTY_IDS.add(SERVICE_NAME);
    PROPERTY_IDS.add(COMPONENT_NAME);
    PROPERTY_IDS.add(DISPLAY_NAME);
    PROPERTY_IDS.add(STATE);
    PROPERTY_IDS.add(CATEGORY);
    PROPERTY_IDS.add(TOTAL_COUNT);
    PROPERTY_IDS.add(STARTED_COUNT);
    PROPERTY_IDS.add(INSTALLED_COUNT);
    PROPERTY_IDS.add(INSTALLED_AND_MAINTENANCE_OFF_COUNT);
    PROPERTY_IDS.add(INIT_COUNT);
    PROPERTY_IDS.add(UNKNOWN_COUNT);
    PROPERTY_IDS.add(INSTALL_FAILED_COUNT);
    PROPERTY_IDS.add(RECOVERY_ENABLED);
    PROPERTY_IDS.add(DESIRED_STACK);
    PROPERTY_IDS.add(DESIRED_VERSION);
    PROPERTY_IDS.add(REPOSITORY_STATE);
    PROPERTY_IDS.add(QUERY_PARAMETERS_RUN_SMOKE_TEST_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Component, COMPONENT_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.Service, SERVICE_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, CLUSTER_NAME);
  }

  private MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  private STOMPComponentsDeleteHandler STOMPComponentsDeleteHandler;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a new resource provider for the given management controller.
   *
   * @param managementController  the management controller
   */
  @AssistedInject
  ComponentResourceProvider(@Assisted AmbariManagementController managementController,
      MaintenanceStateHelper maintenanceStateHelper) {
    super(Resource.Type.Component, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
    this.maintenanceStateHelper = maintenanceStateHelper;

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES, RoleAuthorization.HOST_ADD_DELETE_COMPONENTS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES, RoleAuthorization.HOST_ADD_DELETE_COMPONENTS));
    setRequiredGetAuthorizations(RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE);
    setRequiredGetAuthorizations(RoleAuthorization.AUTHORIZATIONS_VIEW_SERVICE);
    setRequiredUpdateAuthorizations(RoleAuthorization.AUTHORIZATIONS_UPDATE_SERVICE);
  }


  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        createComponents(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.Component, request);

    return getRequestStatus(null);
  }

  @Override
  @Transactional
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceComponentResponse> responses = getResources(new Command<Set<ServiceComponentResponse>>() {
      @Override
      public Set<ServiceComponentResponse> invoke() throws AmbariException {
        return getComponents(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    for (ServiceComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Component);
      setResourceProperty(resource, CLUSTER_NAME, response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_NAME, response.getServiceName(), requestedIds);
      setResourceProperty(resource, COMPONENT_NAME, response.getComponentName(), requestedIds);
      setResourceProperty(resource, DISPLAY_NAME, response.getDisplayName(), requestedIds);
      setResourceProperty(resource, STATE, response.getDesiredState(), requestedIds);
      setResourceProperty(resource, CATEGORY, response.getCategory(), requestedIds);
      setResourceProperty(resource, TOTAL_COUNT, response.getServiceComponentStateCount().get("totalCount"), requestedIds);
      setResourceProperty(resource, STARTED_COUNT, response.getServiceComponentStateCount().get("startedCount"), requestedIds);
      setResourceProperty(resource, INSTALLED_COUNT, response.getServiceComponentStateCount().get("installedCount"), requestedIds);
      setResourceProperty(resource, INSTALLED_AND_MAINTENANCE_OFF_COUNT, response.getServiceComponentStateCount().get("installedAndMaintenanceOffCount"), requestedIds);
      setResourceProperty(resource, INSTALL_FAILED_COUNT, response.getServiceComponentStateCount().get("installFailedCount"), requestedIds);
      setResourceProperty(resource, INIT_COUNT, response.getServiceComponentStateCount().get("initCount"), requestedIds);
      setResourceProperty(resource, UNKNOWN_COUNT, response.getServiceComponentStateCount().get("unknownCount"), requestedIds);
      setResourceProperty(resource, RECOVERY_ENABLED, String.valueOf(response.isRecoveryEnabled()), requestedIds);
      setResourceProperty(resource, DESIRED_STACK, response.getDesiredStackId(), requestedIds);
      setResourceProperty(resource, DESIRED_VERSION, response.getDesiredVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_STATE, response.getRepositoryState(), requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Object queryParameterValue = getQueryParameterValue(QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate);
    final boolean runSmokeTest = TRUE.equals(queryParameterValue);

    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException, AuthorizationException {
        return updateComponents(requests, request.getRequestInfoProperties(), runSmokeTest);
      }
    });

    notifyUpdate(Resource.Type.Component, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException, AuthorizationException {
        return deleteComponents(requests);
      }
    });

    notifyDelete(Resource.Type.Component, predicate);
    return getRequestStatus(response);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private ServiceComponentRequest getRequest(Map<String, Object> properties) {
    return new ServiceComponentRequest(
        (String) properties.get(CLUSTER_NAME),
        (String) properties.get(SERVICE_NAME),
        (String) properties.get(COMPONENT_NAME),
        (String) properties.get(STATE),
        (String) properties.get(RECOVERY_ENABLED),
        (String) properties.get(CATEGORY));
  }

  // Create the components for the given requests.
  public void createComponents(Set<ServiceComponentRequest> requests)
      throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters clusters = getManagementController().getClusters();
    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
    ServiceComponentFactory serviceComponentFactory = getManagementController().getServiceComponentFactory();

    // do all validation checks
    Map<String, Map<String, Set<String>>> componentNames = new HashMap<>();
    Set<String> duplicates = new HashSet<>();

    for (ServiceComponentRequest request : requests) {
      Validate.notEmpty(request.getComponentName(), "component name should be non-empty");
      Cluster cluster = getClusterForRequest(request, clusters);

      isAuthorized(cluster, getRequiredCreateAuthorizations());

      setServiceNameIfAbsent(request, cluster, ambariMetaInfo);
      debug("Received a createComponent request: {}", request);

      if (!componentNames.containsKey(request.getClusterName())) {
        componentNames.put(request.getClusterName(), new HashMap<>());
      }

      Map<String, Set<String>> serviceComponents = componentNames.get(request.getClusterName());
      if (!serviceComponents.containsKey(request.getServiceName())) {
        serviceComponents.put(request.getServiceName(), new HashSet<>());
      }

      if (serviceComponents.get(request.getServiceName()).contains(request.getComponentName())) {
        // throw error later for dup
        duplicates.add(request.toString());
        continue;
      }
      serviceComponents.get(request.getServiceName()).add(request.getComponentName());

      if (StringUtils.isNotEmpty(request.getDesiredState())) {
        Validate.isTrue(State.INIT == State.valueOf(request.getDesiredState()),
            "Invalid desired state only INIT state allowed during creation, providedDesiredState=" + request.getDesiredState());
      }

      Service s = getServiceFromCluster(request, cluster);

      try {
        ServiceComponent sc = s.getServiceComponent(request.getComponentName());
        if (sc != null) {
          // throw error later for dup
          duplicates.add(request.toString());
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }

      StackId stackId = s.getDesiredStackId();
      if (!ambariMetaInfo.isValidServiceComponent(stackId.getStackName(),
          stackId.getStackVersion(), s.getName(), request.getComponentName())) {
        throw new IllegalArgumentException("Unsupported or invalid component"
            + " in stack stackInfo=" + stackId.getStackId()
            + " request=" + request);
      }
    }

    // ensure only a single cluster update
    Validate.isTrue(componentNames.size() == 1,
        "Invalid arguments, updates allowed on only one cluster at a time");

    // Validate dups
    if (!duplicates.isEmpty()) {
      //Java8 has StringJoiner library but ambari is not on Java8 yet.
      throw new DuplicateResourceException("Attempted to create one or more components which already exist:"
                            + StringUtils.join(duplicates, ","));
    }

    // now doing actual work
    for (ServiceComponentRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = serviceComponentFactory.createNew(s, request.getComponentName());
      sc.setDesiredRepositoryVersion(s.getDesiredRepositoryVersion());

      if (StringUtils.isNotEmpty(request.getDesiredState())) {
        State state = State.valueOf(request.getDesiredState());
        sc.setDesiredState(state);
      } else {
        sc.setDesiredState(s.getDesiredState());
      }

      /*
       * If request does not have recovery_enabled field,
       * then get the default from the stack definition.
       */
      if (StringUtils.isNotEmpty(request.getRecoveryEnabled())) {
        boolean recoveryEnabled = Boolean.parseBoolean(request.getRecoveryEnabled());
        sc.setRecoveryEnabled(recoveryEnabled);
        LOG.info("Component: {}, recovery_enabled from request: {}", request.getComponentName(), recoveryEnabled);
      } else {
        StackId stackId = s.getDesiredStackId();
        ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                stackId.getStackVersion(), s.getName(), request.getComponentName());
        if (componentInfo == null) {
            throw new AmbariException("Could not get component information from stack definition: Stack=" +
              stackId + ", Service=" + s.getName() + ", Component=" + request.getComponentName());
        }
        sc.setRecoveryEnabled(componentInfo.isRecoveryEnabled());
        LOG.info("Component: {}, recovery_enabled from stack definition:{}", componentInfo.getName(),
                componentInfo.isRecoveryEnabled());
      }

      s.addServiceComponent(sc);
    }
  }

  // Get the components for the given requests.
  protected Set<ServiceComponentResponse> getComponents(Set<ServiceComponentRequest> requests) throws AmbariException {
    Set<ServiceComponentResponse> response = new HashSet<>();
    for (ServiceComponentRequest request : requests) {
      try {
        response.addAll(getComponents(request));
      } catch (ObjectNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  // Get the components for the given request.
  private Set<ServiceComponentResponse> getComponents(ServiceComponentRequest request) throws AmbariException {

    final AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
    final Clusters clusters = getManagementController().getClusters();
    final Cluster cluster = getCluster(request, clusters);

    Set<ServiceComponentResponse> response = new HashSet<>();
    String category = null;


    if (request.getComponentName() != null) {
      setServiceNameIfAbsent(request, cluster, ambariMetaInfo);

      final Service s = getServiceFromCluster(request, cluster);
      ServiceComponent sc = s.getServiceComponent(request.getComponentName());
      ServiceComponentResponse serviceComponentResponse = sc.convertToResponse();

      StackId stackId = sc.getDesiredStackId();

      try {
        ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
            stackId.getStackVersion(), s.getName(), request.getComponentName());
        category = componentInfo.getCategory();
        if (category != null) {
          serviceComponentResponse.setCategory(category);
        }
      } catch (ObjectNotFoundException e) {
        // nothing to do, component doesn't exist
      }

      response.add(serviceComponentResponse);
      return response;
    }

    Set<Service> services = new HashSet<>();
    if (StringUtils.isNotEmpty(request.getServiceName())) {
      services.add(getServiceFromCluster(request, cluster));
    } else {
      services.addAll(cluster.getServices().values());
    }

    final State desiredStateToCheck = getValidDesiredState(request);
    for (Service s : services) {
      // filter on request.getDesiredState()
      for (ServiceComponent sc : s.getServiceComponents().values()) {
        if (desiredStateToCheck != null && desiredStateToCheck != sc.getDesiredState()) {
          // skip non matching state
          continue;
        }

        StackId stackId = sc.getDesiredStackId();

        ServiceComponentResponse serviceComponentResponse = sc.convertToResponse();
        try {
          ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
              stackId.getStackVersion(), s.getName(), sc.getName());
          category = componentInfo.getCategory();
          if (category != null) {
            serviceComponentResponse.setCategory(category);
          }
        } catch (ObjectNotFoundException e) {
          // component doesn't exist, nothing to do
        }
        String requestedCategory = request.getComponentCategory();
        if (StringUtils.isNotEmpty(requestedCategory) && !requestedCategory.equalsIgnoreCase(category)) {
          continue;
        }

        response.add(serviceComponentResponse);
      }
    }
    return response;
  }

  // Update the components for the given requests.
  protected RequestStatusResponse updateComponents(Set<ServiceComponentRequest> requests,
                                                                Map<String, String> requestProperties,
                                                                boolean runSmokeTest)
      throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Clusters clusters = getManagementController().getClusters();
    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();

    Map<State, List<ServiceComponent>> changedComps = new HashMap<>();
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts = new HashMap<>();
    Collection<ServiceComponentHost> ignoredScHosts = new ArrayList<>();

    Set<String> clusterNames = new HashSet<>();
    Map<String, Map<String, Set<String>>> componentNames = new HashMap<>();
    Set<State> seenNewStates = new HashSet<>();

    Collection<ServiceComponent> recoveryEnabledComponents = new ArrayList<>();
    Collection<ServiceComponent> recoveryDisabledComponents = new ArrayList<>();

    // Determine operation level
    Resource.Type reqOpLvl;
    if (requestProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      RequestOperationLevel operationLevel = new RequestOperationLevel(requestProperties);
      reqOpLvl = operationLevel.getLevel();
    } else {
      LOG.warn("Can not determine request operation level. Operation level property should be specified for this request.");
      reqOpLvl = Resource.Type.Cluster;
    }

    for (ServiceComponentRequest request : requests) {
      Validate.notEmpty(request.getComponentName(), "component name should be non-empty");
      final Cluster cluster = getClusterForRequest(request, clusters);
      final String clusterName = request.getClusterName();
      final String componentName = request.getComponentName();

      LOG.info("Received a updateComponent request: {}", request);

      setServiceNameIfAbsent(request, cluster, ambariMetaInfo);
      final String serviceName = request.getServiceName();

      debug("Received a updateComponent request: {}", request);

      clusterNames.add(clusterName);

      Validate.isTrue(clusterNames.size() == 1, "Updates to multiple clusters is not supported");

      if (!componentNames.containsKey(clusterName)) {
        componentNames.put(clusterName, new HashMap<>());
      }
      if (!componentNames.get(clusterName).containsKey(serviceName)) {
        componentNames.get(clusterName).put(serviceName, new HashSet<>());
      }
      if (componentNames.get(clusterName).get(serviceName).contains(componentName)){
        // throw error later for dup
        throw new IllegalArgumentException("Invalid request contains duplicate service components");
      }
      componentNames.get(clusterName).get(serviceName).add(componentName);

      Service s = cluster.getService(serviceName);
      ServiceComponent sc = s.getServiceComponent(componentName);
      State newState = getValidDesiredState(request);

      if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, s)) {
        LOG.info("Operations cannot be applied to component " + componentName
                + " because service " + serviceName +
                " is in the maintenance state of " + s.getMaintenanceState());
        continue;
      }

      // Gather the components affected by the change in
      // auto start state
      if (!StringUtils.isEmpty(request.getRecoveryEnabled())) {
        // Verify that the authenticated user has authorization to change auto-start states for services
        AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, getClusterResourceId(clusterName),
            EnumSet.of(RoleAuthorization.CLUSTER_MANAGE_AUTO_START, RoleAuthorization.SERVICE_MANAGE_AUTO_START));

        boolean newRecoveryEnabled = Boolean.parseBoolean(request.getRecoveryEnabled());
        boolean oldRecoveryEnabled = sc.isRecoveryEnabled();
        LOG.info("Component: {}, oldRecoveryEnabled: {}, newRecoveryEnabled {}",
                componentName, oldRecoveryEnabled, newRecoveryEnabled);
        if (newRecoveryEnabled != oldRecoveryEnabled) {
          if (newRecoveryEnabled) {
            recoveryEnabledComponents.add(sc);
          } else {
            recoveryDisabledComponents.add(sc);
          }
        }
      }

      if (newState == null) {
        debug("Nothing to do for new updateServiceComponent request, request ={}, newDesiredState=null" + request);
        continue;
      }

      if (sc.isClientComponent() && !newState.isValidClientComponentState()) {
        throw new AmbariException("Invalid desired state for a client component");
      }

      seenNewStates.add(newState);

      State oldScState = sc.getDesiredState();
      if (newState != oldScState) {
        // The if user is trying to start or stop the component, ensure authorization
        if (((newState == State.INSTALLED) || (newState == State.STARTED))) {
          isAuthorized(cluster, RoleAuthorization.SERVICE_START_STOP);
        }

        if (!State.isValidDesiredStateTransition(oldScState, newState)) {
          // FIXME throw correct error
          throw new AmbariException("Invalid transition for"
              + " servicecomponent"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sc.getServiceName()
              + ", componentName=" + sc.getName()
              + ", recoveryEnabled=" + sc.isRecoveryEnabled()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }

        if (!changedComps.containsKey(newState)) {
          changedComps.put(newState, new ArrayList<>());
        }
        debug("Handling update to ServiceComponent"
              + ", clusterName=" + clusterName
              + ", serviceName=" + serviceName
              + ", componentName=" + sc.getName()
              + ", recoveryEnabled=" + sc.isRecoveryEnabled()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);

        changedComps.get(newState).add(sc);
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        State oldSchState = sch.getState();
        if (oldSchState == State.DISABLED || oldSchState == State.UNKNOWN) {
          debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
                + ", componentName=" + sc.getName()
                + ", recoveryEnabled=" + sc.isRecoveryEnabled()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          continue;
        }

        if (newState == oldSchState) {
          ignoredScHosts.add(sch);
          debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
                + ", componentName=" + sc.getName()
                + ", recoveryEnabled=" + sc.isRecoveryEnabled()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          continue;
        }

        // do not update or alter any HC that is not active
        if (! maintenanceStateHelper.isOperationAllowed(reqOpLvl, sch)) {
          ignoredScHosts.add(sch);
          debug("Ignoring ServiceComponentHost in maintenance state"
                + ", clusterName=" + clusterName
                + ", serviceName=" + serviceName
                + ", componentName=" + sc.getName()
                + ", recoveryEnabled=" + sc.isRecoveryEnabled()
                + ", hostname=" + sch.getHostName());

          continue;
        }

        if (!State.isValidStateTransition(oldSchState, newState)) {
          // FIXME throw correct error
          throw new AmbariException("Invalid transition for"
              + " servicecomponenthost"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sch.getServiceName()
              + ", componentName=" + sch.getServiceComponentName()
              + ", recoveryEnabled=" + sc.isRecoveryEnabled()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
        }
        if (!changedScHosts.containsKey(sc.getName())) {
          changedScHosts.put(sc.getName(), new HashMap<>());
        }
        if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
          changedScHosts.get(sc.getName()).put(newState, new ArrayList<>());
        }

        debug("Handling update to ServiceComponentHost"
              + ", clusterName=" + clusterName
              + ", serviceName=" + serviceName
              + ", componentName=" + sc.getName()
              + ", recoveryEnabled=" + sc.isRecoveryEnabled()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);

        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }

    Validate.isTrue(seenNewStates.size() <= 1,
        "Cannot handle different desired state changes for a set of service components at the same time");

    // TODO additional validation?

    // Validations completed. Update the affected service components now.

    for (ServiceComponent sc : recoveryEnabledComponents) {
      sc.setRecoveryEnabled(true);
    }

    for (ServiceComponent sc : recoveryDisabledComponents) {
      sc.setRecoveryEnabled(false);
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return getManagementController().createAndPersistStages(cluster, requestProperties, null, null, changedComps, changedScHosts,
        ignoredScHosts, runSmokeTest, false);
  }

  protected RequestStatusResponse deleteComponents(Set<ServiceComponentRequest> requests) throws AmbariException, AuthorizationException {
    Clusters clusters = getManagementController().getClusters();
    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
    DeleteHostComponentStatusMetaData deleteMetaData = new DeleteHostComponentStatusMetaData();

    for (ServiceComponentRequest request : requests) {
      Validate.notEmpty(request.getComponentName(), "component name should be non-empty");
      Cluster cluster = getClusterForRequest(request, clusters);

      setServiceNameIfAbsent(request, cluster, ambariMetaInfo);

      Service s = getServiceFromCluster(request, cluster);

      ServiceComponent sc = s.getServiceComponent(request.getComponentName());

      if (sc != null) {
        deleteHostComponentsForServiceComponent(sc, request, deleteMetaData);
        STOMPComponentsDeleteHandler.processDeleteByMetaDataException(deleteMetaData);
        sc.setDesiredState(State.DISABLED);
        s.deleteServiceComponent(request.getComponentName(), deleteMetaData);
        STOMPComponentsDeleteHandler.processDeleteByMetaDataException(deleteMetaData);
      }
    }
    STOMPComponentsDeleteHandler.processDeleteByMetaData(deleteMetaData);
    return null;
  }

  private void deleteHostComponentsForServiceComponent(ServiceComponent sc, ServiceComponentRequest request,
                                                       DeleteHostComponentStatusMetaData deleteMetaData) throws AmbariException {
    for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
      if (!sch.getDesiredState().isRemovableState()) {
        deleteMetaData.setAmbariException(new AmbariException("Found non removable host component when trying to delete service component." +
            " To remove host component, it must be in DISABLED/INIT/INSTALLED/INSTALL_FAILED/UNKNOWN" +
            "/UNINSTALLED/INSTALLING state."
            + ", request=" + request.toString()
            + ", current state=" + sc.getDesiredState() + "."));
        return;
      }
    }

    for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
      sch.delete(deleteMetaData);
    }
  }
  private Cluster getClusterForRequest(final ServiceComponentRequest request, final Clusters clusters) throws AmbariException {
    Validate.notEmpty(request.getClusterName(), "cluster name should be non-empty");
    try {
      return clusters.getCluster(request.getClusterName());
    } catch (ClusterNotFoundException e) {
      throw new ParentObjectNotFoundException("Attempted to add a component to a cluster which doesn't exist:", e);
    }
  }

  private Service getServiceFromCluster(final ServiceComponentRequest request, final Cluster cluster) throws AmbariException {
    try {
      return cluster.getService(request.getServiceName());
    } catch (ServiceNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Service resource doesn't exist.", e);
    }
  }

  private Cluster getCluster(final ServiceComponentRequest request, final Clusters clusters) throws AmbariException {
    Validate.notEmpty(request.getClusterName(), "cluster name should be non-empty");

    try {
      return clusters.getCluster(request.getClusterName());
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

  }

  private void isAuthorized(final Cluster cluster, final RoleAuthorization roleAuthorization) throws AuthorizationException {
    isAuthorized(cluster, EnumSet.of(roleAuthorization));
  }

  private void isAuthorized(final Cluster cluster, final Set<RoleAuthorization> requiredAuthorizations) throws AuthorizationException {
    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), requiredAuthorizations)) {
      throw new AuthorizationException("The user is not authorized to for roles " + requiredAuthorizations);
    }
  }

  private void setServiceNameIfAbsent(final ServiceComponentRequest request,
                                      final Cluster cluster,
                                      final AmbariMetaInfo ambariMetaInfo) throws AmbariException {
    if (StringUtils.isEmpty(request.getServiceName())) {

      String componentName = request.getComponentName();

      String serviceName = getManagementController().findServiceName(cluster, componentName);

      debug("Looking up service name for component, componentName={}, serviceName={}", componentName, serviceName);

      if (StringUtils.isEmpty(serviceName)) {
        throw new AmbariException("Could not find service for component"
                + ", componentName=" + request.getComponentName()
                + ", clusterName=" + cluster.getClusterName());
      }
      request.setServiceName(serviceName);
    }
  }

  private State getValidDesiredState(ServiceComponentRequest request) {

    if (StringUtils.isEmpty(request.getDesiredState())) {
      return null;
    }
    final State desiredStateToCheck = State.valueOf(request.getDesiredState());
    Validate.isTrue(desiredStateToCheck.isValidDesiredState(),
          "Invalid arguments, invalid desired state, desiredState=" + desiredStateToCheck);
    return desiredStateToCheck;
  }

  private void debug(String format, Object... arguments) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(format, arguments);
    }
  }
}
