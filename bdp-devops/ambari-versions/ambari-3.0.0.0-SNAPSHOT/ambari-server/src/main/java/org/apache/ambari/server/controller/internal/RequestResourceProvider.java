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

import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.CLUSTER_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.COMPONENT_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.HOST_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.SERVICE_NAME;
import static org.apache.ambari.server.security.authorization.RoleAuthorization.AMBARI_VIEW_STATUS_INFO;
import static org.apache.ambari.server.security.authorization.RoleAuthorization.CLUSTER_VIEW_STATUS_INFO;
import static org.apache.ambari.server.security.authorization.RoleAuthorization.HOST_VIEW_STATUS_INFO;
import static org.apache.ambari.server.security.authorization.RoleAuthorization.SERVICE_VIEW_STATUS_INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.PredicateCompiler;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Resource provider for request resources.
 */
@StaticallyInject
public class RequestResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(RequestResourceProvider.class);

  @Inject
  private static RequestDAO s_requestDAO = null;

  @Inject
  private static HostRoleCommandDAO s_hostRoleCommandDAO = null;

  @Inject
  private static TopologyManager topologyManager;

  // ----- Property ID constants ---------------------------------------------
  // Requests
  public static final String REQUESTS = "Requests";
  public static final String REQUEST_INFO = "RequestInfo";
  public static final String REQUEST_CLUSTER_NAME_PROPERTY_ID = REQUESTS + "/cluster_name";
  public static final String REQUEST_CLUSTER_ID_PROPERTY_ID = REQUESTS + "/cluster_id";
  public static final String REQUEST_ID_PROPERTY_ID = REQUESTS + "/id";
  public static final String REQUEST_STATUS_PROPERTY_ID = REQUESTS + "/request_status";
  public static final String REQUEST_ABORT_REASON_PROPERTY_ID = REQUESTS + "/abort_reason";
  public static final String REQUEST_CONTEXT_ID = REQUESTS + "/request_context";
  public static final String REQUEST_SOURCE_SCHEDULE = REQUESTS + "/request_schedule";
  public static final String REQUEST_SOURCE_SCHEDULE_ID = REQUESTS + "/request_schedule/schedule_id";
  public static final String REQUEST_SOURCE_SCHEDULE_HREF = REQUESTS + "/request_schedule/href";
  public static final String REQUEST_TYPE_ID = REQUESTS + "/type";
  public static final String REQUEST_INPUTS_ID = REQUESTS + "/inputs";
  public static final String REQUEST_CLUSTER_HOST_INFO_ID = REQUESTS + "/cluster_host_info";
  public static final String REQUEST_RESOURCE_FILTER_ID = REQUESTS + "/resource_filters";
  public static final String REQUEST_OPERATION_LEVEL_ID = REQUESTS + "/operation_level";
  public static final String REQUEST_CREATE_TIME_ID = REQUESTS + "/create_time";
  public static final String REQUEST_START_TIME_ID = REQUESTS + "/start_time";
  public static final String REQUEST_END_TIME_ID = REQUESTS + "/end_time";
  public static final String REQUEST_EXCLUSIVE_ID = REQUESTS + "/exclusive";
  public static final String REQUEST_TASK_CNT_ID = REQUESTS + "/task_count";
  public static final String REQUEST_FAILED_TASK_CNT_ID = REQUESTS + "/failed_task_count";
  public static final String REQUEST_ABORTED_TASK_CNT_ID = REQUESTS + "/aborted_task_count";
  public static final String REQUEST_TIMED_OUT_TASK_CNT_ID = REQUESTS + "/timed_out_task_count";
  public static final String REQUEST_COMPLETED_TASK_CNT_ID = REQUESTS + "/completed_task_count";
  public static final String REQUEST_QUEUED_TASK_CNT_ID = REQUESTS + "/queued_task_count";
  public static final String REQUEST_PROGRESS_PERCENT_ID = REQUESTS + "/progress_percent";
  public static final String REQUEST_REMOVE_PENDING_HOST_REQUESTS_ID = REQUESTS + "/remove_pending_host_requests";
  public static final String REQUEST_PENDING_HOST_REQUEST_COUNT_ID = REQUESTS + "/pending_host_request_count";
  public static final String REQUEST_USER_NAME = REQUESTS + "/user_name";
  public static final String COMMAND_ID = "command";
  public static final String SERVICE_ID = "service_name";
  public static final String COMPONENT_ID = "component_name";
  public static final String HOSTS_ID = "hosts"; // This is actually a list of hosts
  public static final String HOSTS_PREDICATE = "hosts_predicate";
  public static final String ACTION_ID = "action";
  public static final String INPUTS_ID = "parameters";
  public static final String EXCLUSIVE_ID = "exclusive";
  public static final String HAS_RESOURCE_FILTERS = "HAS_RESOURCE_FILTERS";
  public static final String CONTEXT = "context";

  private static final Set<String> PK_PROPERTY_IDS = ImmutableSet.of(REQUEST_ID_PROPERTY_ID);

  private PredicateCompiler predicateCompiler = new PredicateCompiler();

  /**
   * The key property ids for a Request resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Request, REQUEST_ID_PROPERTY_ID)
      .put(Resource.Type.Cluster, REQUEST_CLUSTER_NAME_PROPERTY_ID)
      .build();

  static Set<String> PROPERTY_IDS = Sets.newHashSet(
    REQUEST_ID_PROPERTY_ID,
    REQUEST_CLUSTER_NAME_PROPERTY_ID,
    REQUEST_CLUSTER_ID_PROPERTY_ID,
    REQUEST_STATUS_PROPERTY_ID,
    REQUEST_ABORT_REASON_PROPERTY_ID,
    REQUEST_CONTEXT_ID,
    REQUEST_SOURCE_SCHEDULE,
    REQUEST_SOURCE_SCHEDULE_ID,
    REQUEST_SOURCE_SCHEDULE_HREF,
    REQUEST_TYPE_ID,
    REQUEST_INPUTS_ID,
    REQUEST_RESOURCE_FILTER_ID,
    REQUEST_OPERATION_LEVEL_ID,
    REQUEST_CREATE_TIME_ID,
    REQUEST_START_TIME_ID,
    REQUEST_END_TIME_ID,
    REQUEST_EXCLUSIVE_ID,
    REQUEST_TASK_CNT_ID,
    REQUEST_FAILED_TASK_CNT_ID,
    REQUEST_ABORTED_TASK_CNT_ID,
    REQUEST_TIMED_OUT_TASK_CNT_ID,
    REQUEST_COMPLETED_TASK_CNT_ID,
    REQUEST_QUEUED_TASK_CNT_ID,
    REQUEST_PROGRESS_PERCENT_ID,
    REQUEST_REMOVE_PENDING_HOST_REQUESTS_ID,
    REQUEST_PENDING_HOST_REQUEST_COUNT_ID,
    REQUEST_CLUSTER_HOST_INFO_ID,
    REQUEST_USER_NAME
  );

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  RequestResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Request, PROPERTY_IDS, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException, NoSuchParentResourceException, ResourceAlreadyExistsException {
    if (request.getProperties().size() > 1) {
      throw new UnsupportedOperationException("Multiple actions/commands cannot be executed at the same time.");
    }
    final ExecuteActionRequest actionRequest = getActionRequest(request);
    final Map<String, String> requestInfoProperties = request.getRequestInfoProperties();

    return getRequestStatus(createResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException, AuthorizationException {

        String clusterName = actionRequest.getClusterName();

        ResourceType resourceType;
        Long resourceId;

        if (StringUtils.isEmpty(clusterName)) {
          resourceType = ResourceType.AMBARI;
          resourceId = null;
        } else {
          resourceType = ResourceType.CLUSTER;
          resourceId = getClusterResourceId(clusterName);
        }

        if (actionRequest.isCommand()) {
          String commandName = actionRequest.getCommandName();

          if (StringUtils.isEmpty(commandName)) {
            commandName = "_unknown_command_";
          }

          if (commandName.endsWith("_SERVICE_CHECK")) {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_RUN_SERVICE_CHECK)) {
              throw new AuthorizationException("The authenticated user is not authorized to execute service checks.");
            }
          } else if (commandName.equals("DECOMMISSION")) {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION)) {
              throw new AuthorizationException("The authenticated user is not authorized to decommission services.");
            }
          } else {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND)) {
              throw new AuthorizationException(String.format("The authenticated user is not authorized to execute the command, %s.",
                  commandName));
            }
          }
        } else {
          String actionName = actionRequest.getActionName();

          if (StringUtils.isEmpty(actionName)) {
            actionName = "_unknown_action_";
          }

          if (actionName.contains("SERVICE_CHECK")) {
            if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, RoleAuthorization.SERVICE_RUN_SERVICE_CHECK)) {
              throw new AuthorizationException("The authenticated user is not authorized to execute service checks.");
            }
          } else {
            // A custom action has been requested
            ActionDefinition actionDefinition = getManagementController().getAmbariMetaInfo().getActionDefinition(actionName);

            Set<RoleAuthorization> permissions = (actionDefinition == null)
                ? null
                : actionDefinition.getPermissions();

            // here goes ResourceType handling for some specific custom actions
            ResourceType customActionResourceType = resourceType;
            if (actionName.contains("check_host")) { // check_host custom action
              customActionResourceType = ResourceType.CLUSTER;
            }

            if (!AuthorizationHelper.isAuthorized(customActionResourceType, resourceId, permissions)) {
              throw new AuthorizationException(String.format("The authenticated user is not authorized to execute the action %s.", actionName));
            }
          }
        }

        return getManagementController().createAction(actionRequest, requestInfoProperties);
      }
    }));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    String maxResultsRaw = request.getRequestInfoProperties().get(BaseRequest.PAGE_SIZE_PROPERTY_KEY);
    String ascOrderRaw = request.getRequestInfoProperties().get(BaseRequest.ASC_ORDER_PROPERTY_KEY);

    Integer maxResults = (maxResultsRaw == null ? null : Integer.parseInt(maxResultsRaw));
    Boolean ascOrder = (ascOrderRaw == null ? null : Boolean.parseBoolean(ascOrderRaw));

    if (null == predicate) {
      authorizeGetResources(null);
      // the no-arg call to /requests is here
      resources.addAll(getRequestResources(null, null, null, maxResults, ascOrder, requestedIds));
    } else {
      // process /requests with a predicate
      // process /clusters/[cluster]/requests
      // process /clusters/[cluster]/requests with a predicate

      for (Map<String, Object> properties : getPropertyMaps(predicate)) {
        String clusterName = (String) properties.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);

        Long requestId = null;
        if (properties.get(REQUEST_ID_PROPERTY_ID) != null) {
          requestId = Long.valueOf((String) properties.get(REQUEST_ID_PROPERTY_ID));
        }

        String requestStatus = null;
        if (properties.get(REQUEST_STATUS_PROPERTY_ID) != null) {
          requestStatus = (String) properties.get(REQUEST_STATUS_PROPERTY_ID);
        }

        authorizeGetResources(clusterName);
        resources.addAll(getRequestResources(clusterName, requestId, requestStatus, maxResults,
            ascOrder, requestedIds));
      }
    }

    return resources;
  }

  private void authorizeGetResources(String clusterName) throws NoSuchParentResourceException, AuthorizationException {
    final boolean ambariLevelRequest = StringUtils.isBlank(clusterName);
    final ResourceType resourceType = ambariLevelRequest ? ResourceType.AMBARI : ResourceType.CLUSTER;
    Long resourceId;
    try {
      resourceId = ambariLevelRequest ? null : getClusterResourceId(clusterName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException("Error while fetching cluster resource ID", e);
    }
    final Set<RoleAuthorization> requiredAuthorizations = ambariLevelRequest ? Sets.newHashSet(AMBARI_VIEW_STATUS_INFO)
        : Sets.newHashSet(CLUSTER_VIEW_STATUS_INFO, HOST_VIEW_STATUS_INFO, SERVICE_VIEW_STATUS_INFO);

    if (!AuthorizationHelper.isAuthorized(resourceType, resourceId, requiredAuthorizations)) {
      throw new AuthorizationException(String.format("The authenticated user is not authorized to fetch request related information."));
    }
  }

  @Override
  public RequestStatus updateResources(Request requestInfo, Predicate predicate)
          throws SystemException, UnsupportedPropertyException,
          NoSuchResourceException, NoSuchParentResourceException {

    AmbariManagementController amc = getManagementController();
    final Set<RequestRequest> requests = new HashSet<>();

    Iterator<Map<String,Object>> iterator = requestInfo.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }
    // Validate
    List<org.apache.ambari.server.actionmanager.Request> targets =
      new ArrayList<>();
    for (RequestRequest updateRequest : requests) {
      ActionManager actionManager = amc.getActionManager();
      List<org.apache.ambari.server.actionmanager.Request> internalRequests =
              actionManager.getRequests(Collections.singletonList(updateRequest.getRequestId()));
      if (internalRequests.size() == 0) {
        throw new IllegalArgumentException(
                String.format("Request %s does not exist", updateRequest.getRequestId()));
      }
      // There should be only one request with this id (or no request at all)
      org.apache.ambari.server.actionmanager.Request internalRequest = internalRequests.get(0);

      if (updateRequest.isRemovePendingHostRequests()) {
        if (internalRequest instanceof LogicalRequest) {
          targets.add(internalRequest);
        } else {
          throw new IllegalArgumentException("Request with id: " + internalRequest.getRequestId() + "is not a Logical Request.");
        }
      } else {
        // Validate update request (check constraints on state value and presence of abort reason)
        if (updateRequest.getAbortReason() == null || updateRequest.getAbortReason().isEmpty()) {
          throw new IllegalArgumentException("Abort reason can not be empty.");
        }

        if (updateRequest.getStatus() != HostRoleStatus.ABORTED) {
          throw new IllegalArgumentException(
                  String.format("%s is wrong value. The only allowed value " +
                                  "for updating request status is ABORTED",
                          updateRequest.getStatus()));
        }

        HostRoleStatus internalRequestStatus =
                CalculatedStatus.statusFromStages(internalRequest.getStages()).getStatus();

        if (internalRequestStatus.isCompletedState()) {
          // Ignore updates to completed requests to avoid throwing exception on race condition
        } else {
          // Validation passed
          targets.add(internalRequest);
        }
      }

    }

    // Perform update
    Iterator<RequestRequest> reqIterator = requests.iterator();
    for (org.apache.ambari.server.actionmanager.Request target : targets) {
      if (target instanceof LogicalRequest) {
        topologyManager.removePendingHostRequests(target.getClusterName(), target.getRequestId());
      } else {
        String reason = reqIterator.next().getAbortReason();
        amc.getActionManager().cancelRequest(target.getRequestId(), reason);
      }
    }
    return getRequestStatus(null);
  }

  private RequestRequest getRequest(Map<String, Object> propertyMap) {
    // Cluster name may be empty for custom actions
    String clusterNameStr = (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);
    String requestIdStr = (String) propertyMap.get(REQUEST_ID_PROPERTY_ID);
    long requestId = Integer.parseInt(requestIdStr);
    String requestStatusStr = (String) propertyMap.get(REQUEST_STATUS_PROPERTY_ID);
    HostRoleStatus requestStatus = null;
    if (requestStatusStr != null) {
      // This conversion may throw IllegalArgumentException, it is OK
      // in this case it will be mapped to HTTP 400 Bad Request
      requestStatus = HostRoleStatus.valueOf(requestStatusStr);
    }
    String abortReason = (String) propertyMap.get(REQUEST_ABORT_REASON_PROPERTY_ID);
    String removePendingHostRequests = (String) propertyMap.get(REQUEST_REMOVE_PENDING_HOST_REQUESTS_ID);

    RequestRequest requestRequest = new RequestRequest(clusterNameStr, requestId);
    requestRequest.setStatus(requestStatus);
    requestRequest.setAbortReason(abortReason);
    if (removePendingHostRequests != null) {
      requestRequest.setRemovePendingHostRequests(Boolean.valueOf(removePendingHostRequests));
    }

    return requestRequest;

  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  // ----- AbstractResourceProvider -----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }


  // ----- utility methods --------------------------------------------------

  // Get request to execute an action/command
  @SuppressWarnings("unchecked")
  private ExecuteActionRequest getActionRequest(Request request)
      throws UnsupportedOperationException, SystemException {
    Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    Map<String, Object> propertyMap = request.getProperties().iterator().next();

    Boolean isCommand = requestInfoProperties.containsKey(COMMAND_ID);
    String commandName = null;
    String actionName = null;
    if (isCommand) {
      if (requestInfoProperties.containsKey(ACTION_ID)) {
        throw new UnsupportedOperationException("Both command and action cannot be specified.");
      }
      commandName = requestInfoProperties.get(COMMAND_ID);
    } else {
      if (!requestInfoProperties.containsKey(ACTION_ID)) {
        throw new UnsupportedOperationException("Either command or action must be specified.");
      }
      actionName = requestInfoProperties.get(ACTION_ID);
    }

    List<RequestResourceFilter> resourceFilterList = null;
    Set<Map<String, Object>> resourceFilters;

    Map<String, String> params = new HashMap<>();
    Object resourceFilterObj = propertyMap.get(REQUEST_RESOURCE_FILTER_ID);
    if (resourceFilterObj != null && resourceFilterObj instanceof HashSet) {
      resourceFilters = (HashSet<Map<String, Object>>) resourceFilterObj;
      resourceFilterList = new ArrayList<>();

      for (Map<String, Object> resourceMap : resourceFilters) {
        params.put(HAS_RESOURCE_FILTERS, "true");
        resourceFilterList.addAll(parseRequestResourceFilter(resourceMap,
          (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID)));
      }
      LOG.debug("RequestResourceFilters : {}", resourceFilters);
    }
    // Extract operation level property
    RequestOperationLevel operationLevel = null;
    if (requestInfoProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      operationLevel = new RequestOperationLevel(requestInfoProperties);
    }

    String keyPrefix = INPUTS_ID + "/";
    for (String key : requestInfoProperties.keySet()) {
      if (key.startsWith(keyPrefix)) {
        params.put(key.substring(keyPrefix.length()), requestInfoProperties.get(key));
      }
    }

    boolean exclusive = false;
    if (requestInfoProperties.containsKey(EXCLUSIVE_ID)) {
      exclusive = Boolean.valueOf(requestInfoProperties.get(EXCLUSIVE_ID).trim());
    }

    return new ExecuteActionRequest(
      (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID),
      commandName,
      actionName,
      resourceFilterList,
      operationLevel,
      params, exclusive);
  }

  /**
   *  Allow host component resource predicate to decide hosts to operate on.
   * @param resourceMap Properties
   * @param clusterName clusterName
   * @return Populated resource filter
   * @throws SystemException
   */
  private List<RequestResourceFilter> parseRequestResourceFilter(Map<String, Object> resourceMap,
                                                           String clusterName) throws SystemException {

    List<RequestResourceFilter> resourceFilterList = new ArrayList<>();

    String serviceName = (String) resourceMap.get(SERVICE_ID);
    String componentName = (String) resourceMap.get(COMPONENT_ID);
    String hostsPredicate = (String) resourceMap.get(HOSTS_PREDICATE);
    Object hostListStr = resourceMap.get(HOSTS_ID);
    List<String> hostList = Collections.emptyList();
    if (hostListStr != null) {
      hostList = new ArrayList<>();
      for (String hostName : ((String) hostListStr).split(",")) {
        hostList.add(hostName.trim());
      }
      resourceFilterList.add(new RequestResourceFilter(serviceName, componentName, hostList));
    } else if (hostsPredicate != null) {
        // Parse the predicate as key=value and apply to the ResourceProvider predicate
      Predicate filterPredicate;
      try {
        filterPredicate = predicateCompiler.compile(hostsPredicate);
      } catch (InvalidQueryException e) {
        String msg = "Invalid predicate expression provided: " + hostsPredicate;
        LOG.warn(msg, e);
        throw new SystemException(msg, e);
      }

      Set<String> propertyIds = new HashSet<>();
      propertyIds.add(CLUSTER_NAME);
      propertyIds.add(SERVICE_NAME);
      propertyIds.add(COMPONENT_NAME);

      Request request = PropertyHelper.getReadRequest(propertyIds);
      
      try {
        ClusterController clusterController = ClusterControllerHelper.getClusterController();
        QueryResponse queryResponse = clusterController.getResources(
          Resource.Type.HostComponent, request, filterPredicate);
        Iterable<Resource> resourceIterable = clusterController.getIterable(
          Resource.Type.HostComponent, queryResponse, request,
          filterPredicate, null, null);
        
        // Allow request to span services / components using just the predicate
        Map<ServiceComponentTuple, List<String>> tupleListMap = new HashMap<>();
        for (Resource resource : resourceIterable) {
          String hostnameStr = (String) resource.getPropertyValue(HOST_NAME);
          if (hostnameStr != null) {
            String computedServiceName = (String) resource.getPropertyValue(SERVICE_NAME);
            String computedComponentName = (String) resource.getPropertyValue(COMPONENT_NAME);
            ServiceComponentTuple tuple = new ServiceComponentTuple(computedServiceName, computedComponentName);

            if (!tupleListMap.containsKey(tuple)) {
              hostList = new ArrayList<>();
              hostList.add(hostnameStr);
              tupleListMap.put(tuple, hostList);
            } else {
              tupleListMap.get(tuple).add(hostnameStr);
            }
          }
        }
        if (!tupleListMap.isEmpty()) {
          for (Map.Entry<ServiceComponentTuple, List<String>> entry : tupleListMap.entrySet()) {
            resourceFilterList.add(new RequestResourceFilter(
              entry.getKey().getServiceName(),
              entry.getKey().getComponentName(),
              entry.getValue()
            ));
          }
        }
      } catch (Exception e) {
        LOG.warn("Exception finding requested resources with serviceName = " + serviceName +
          ", componentName = " + componentName +
          ", hostPredicate" + " = " + hostsPredicate, e);
      }
    } else {
      resourceFilterList.add(new RequestResourceFilter(serviceName, componentName, hostList));
    }

    return resourceFilterList;
  }

  /**
   * Represent a map key as a ServiceComponent
   */
  class ServiceComponentTuple {
    final String serviceName;
    final String componentName;

    ServiceComponentTuple(String serviceName, String componentName) {
      this.serviceName = serviceName;
      this.componentName = componentName;
    }

    public String getServiceName() {
      return serviceName;
    }

    public String getComponentName() {
      return componentName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ServiceComponentTuple that = (ServiceComponentTuple) o;

      if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) {
        return false;
      }
      return !(componentName != null ? !componentName.equals(that.componentName) : that.componentName != null);

    }

    @Override
    public int hashCode() {
      int result = serviceName != null ? serviceName.hashCode() : 0;
      result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
      return result;
    }
  }

  /**
   * Gets all of the request resources for the given properties.
   */
  private Set<Resource> getRequestResources(String clusterName,
                                            Long requestId,
                                            String requestStatus,
                                            Integer maxResults,
                                            Boolean ascOrder,
                                            Set<String> requestedPropertyIds)
      throws NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> response = new HashSet<>();
    ActionManager actionManager = getManagementController().getActionManager();

    Long clusterId = null;

    if (clusterName != null) {
      Clusters clusters = getManagementController().getClusters();
      //validate that cluster exists, throws exception if it doesn't.
      try {
        Cluster cluster = clusters.getCluster(clusterName);
        clusterId = cluster.getClusterId();
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(e.getMessage(), e);
      }
    }

    List<Long> requestIds = Collections.emptyList();

    if (requestId == null) {
      org.apache.ambari.server.actionmanager.RequestStatus status = null;
      if (requestStatus != null) {
        status = org.apache.ambari.server.actionmanager.RequestStatus.valueOf(requestStatus);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a Get Request Status request, requestId=null, requestStatus={}", status);
      }

      maxResults = (maxResults != null) ? maxResults : BaseRequest.DEFAULT_PAGE_SIZE;
      ascOrder = (ascOrder != null) ? ascOrder : false;

      if (null == status) {
        if (null != clusterId) {
          // !!! don't mix results of cluster vs non-cluster
          requestIds = s_requestDAO.findAllRequestIds(maxResults, ascOrder, clusterId);
        } else {
          // !!! not a cluster, so get all requests NOT affiliated with a cluster
          requestIds = s_requestDAO.findAllRequestIds(maxResults, ascOrder, null);
        }
      } else {
        // !!! this call will result in mixed results of cluster and non-cluster.  this
        // will get fixed in a future iteration, as the host_role_command table does not
        // currently have direct cluster affinity, and changing that is a tad destructive.
        requestIds = actionManager.getRequestsByStatus(status, maxResults, ascOrder);
      }

      LOG.debug("List<Long> requestIds = actionManager.getRequestsByStatus = {}", requestIds.size());

      response.addAll(getRequestResources(clusterId, clusterName, requestIds,
          requestedPropertyIds));
    } else {
      Collection<Resource> responses = getRequestResources(
          clusterId, clusterName, Collections.singletonList(requestId), requestedPropertyIds);

      if (responses.isEmpty()) {
        throw new NoSuchResourceException("Request resource doesn't exist.");
      }
      response.addAll(responses);
    }
    return response;
  }

  // Get all of the request resources for the given set of request ids
  private Collection<Resource> getRequestResources(Long clusterId, String clusterName,
      List<Long> requestIds, Set<String> requestedPropertyIds) {

    Map<Long, Resource> resourceMap = new HashMap<>();

    List<RequestEntity> requests = s_requestDAO.findByPks(requestIds, true);


    //todo: this was (and still is) in ActionManager but this class was changed to not use ActionManager recently
    List<RequestEntity> topologyRequestEntities = new ArrayList<>();
    Collection<? extends org.apache.ambari.server.actionmanager.Request> topologyRequests =
        topologyManager.getRequests(requestIds);
    for (org.apache.ambari.server.actionmanager.Request request : topologyRequests) {
      topologyRequestEntities.add(request.constructNewPersistenceEntity());
    }

    // if requests is empty, map is Collections.emptyMap() which can't be added to so create a new map
    if (requests.isEmpty()) {
      requests = new ArrayList<>();
    }

    requests.addAll(topologyRequestEntities);

    for (RequestEntity re : requests) {
      if ((null == clusterId && (null == re.getClusterId() || -1L == re.getClusterId())) ||          // if cluster IS NOT requested AND the db request is not for a cluster
          (null != clusterId && null != re.getRequestId() && re.getClusterId().equals(clusterId))) { // if cluster IS requested and the request has a cluster id equal to the one requested
        Resource r = getRequestResource(re, clusterName, requestedPropertyIds);
        resourceMap.put(re.getRequestId(), r);
      }
    }

    return resourceMap.values();
  }

  private Resource getRequestResource(RequestEntity entity, String clusterName,
      Set<String> requestedPropertyIds) {
    Resource resource = new ResourceImpl(Resource.Type.Request);

    if (null != clusterName) {
      setResourceProperty(resource, REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedPropertyIds);
    } else if (null != entity.getClusterId() && -1L != entity.getClusterId()) {
      setResourceProperty(resource, REQUEST_CLUSTER_ID_PROPERTY_ID, entity.getClusterId(), requestedPropertyIds);
    }

    setResourceProperty(resource, REQUEST_ID_PROPERTY_ID, entity.getRequestId(), requestedPropertyIds);
    String requestContext = entity.getRequestContext();
    setResourceProperty(resource, REQUEST_CONTEXT_ID, requestContext, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TYPE_ID, entity.getRequestType(), requestedPropertyIds);

    // Mask any sensitive data fields in the inputs data structure
    if (isPropertyRequested(REQUEST_INPUTS_ID, requestedPropertyIds)) {
      String value = entity.getInputs();
      if (!StringUtils.isBlank(value)) {
        value = SecretReference.maskPasswordInPropertyMap(value);
      }
      resource.setProperty(REQUEST_INPUTS_ID, value);
    }

    if (isPropertyRequested(REQUEST_CLUSTER_HOST_INFO_ID, requestedPropertyIds)) {
      resource.setProperty(REQUEST_CLUSTER_HOST_INFO_ID, entity.getClusterHostInfo());
    }

    setResourceProperty(resource, REQUEST_RESOURCE_FILTER_ID,
        org.apache.ambari.server.actionmanager.Request.filtersFromEntity(entity),
        requestedPropertyIds);

    RequestOperationLevel operationLevel = org.apache.ambari.server.actionmanager.Request.operationLevelFromEntity(entity);
    String opLevelStr = null;
    if (operationLevel != null) {
      opLevelStr = RequestOperationLevel.getExternalLevelName(
              operationLevel.getLevel().toString());
    }
    setResourceProperty(resource, REQUEST_OPERATION_LEVEL_ID, opLevelStr, requestedPropertyIds);

    setResourceProperty(resource, REQUEST_CREATE_TIME_ID, entity.getCreateTime(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_START_TIME_ID, entity.getStartTime(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_END_TIME_ID, entity.getEndTime(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_EXCLUSIVE_ID, entity.isExclusive(), requestedPropertyIds);

    if (entity.getRequestScheduleId() != null) {
      setResourceProperty(resource, REQUEST_SOURCE_SCHEDULE_ID, entity.getRequestScheduleId(), requestedPropertyIds);
    } else {
      setResourceProperty(resource, REQUEST_SOURCE_SCHEDULE, null, requestedPropertyIds);
    }

    Map<Long, HostRoleCommandStatusSummaryDTO> summary = s_hostRoleCommandDAO.findAggregateCounts(entity.getRequestId());

    // get summaries from TopologyManager for logical requests
    summary.putAll(topologyManager.getStageSummaries(entity.getRequestId()));

    // summary might be empty due to delete host have cleared all
    // HostRoleCommands or due to hosts haven't registered yet with the cluster
    // when the cluster is provisioned with a Blueprint
    final CalculatedStatus status;
    LogicalRequest logicalRequest = topologyManager.getRequest(entity.getRequestId());
    if (summary.isEmpty() && null != logicalRequest) {
      status = logicalRequest.calculateStatus();
      if (status == CalculatedStatus.ABORTED) {
        Optional<String> failureReason = logicalRequest.getFailureReason();
        if (failureReason.isPresent()) {
          requestContext += "\nFAILED: " + failureReason.get();
          setResourceProperty(resource, REQUEST_CONTEXT_ID, requestContext, requestedPropertyIds);
        }
      }
    } else {
      // there are either tasks or this is not a logical request, so do normal
      // status calculations
      status = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());
    }

    if (null != logicalRequest) {
      setResourceProperty(resource, REQUEST_PENDING_HOST_REQUEST_COUNT_ID, logicalRequest.getPendingHostRequestCount(), requestedPropertyIds);
    }

    setResourceProperty(resource, REQUEST_STATUS_PROPERTY_ID, status.getStatus().toString(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_PROGRESS_PERCENT_ID, status.getPercent(), requestedPropertyIds);

    int taskCount = 0;
    for (HostRoleCommandStatusSummaryDTO dto : summary.values()) {
      taskCount += dto.getTaskTotal();
    }

    Map<HostRoleStatus, Integer> hostRoleStatusCounters = CalculatedStatus.calculateTaskStatusCounts(
        summary, summary.keySet());

    setResourceProperty(resource, REQUEST_TASK_CNT_ID, taskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_FAILED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.FAILED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_ABORTED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.ABORTED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TIMED_OUT_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.TIMEDOUT), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_QUEUED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.QUEUED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_COMPLETED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.COMPLETED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_USER_NAME, entity.getUserName(), requestedPropertyIds);

    return resource;
  }


}
