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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.agent.stomp.dto.HostLevelParamsCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.agent.stomp.dto.TopologyHost;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
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
import org.apache.ambari.server.events.HostLevelParamsUpdateEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;


/**
 * Resource provider for host resources.
 */
public class HostResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HostResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  // Hosts
  public static final String RESPONSE_KEY = "Hosts";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";

  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String CPU_COUNT_PROPERTY_ID = "cpu_count";
  public static final String DESIRED_CONFIGS_PROPERTY_ID = "desired_configs";
  public static final String DISK_INFO_PROPERTY_ID = "disk_info";
  public static final String HOST_HEALTH_REPORT_PROPERTY_ID = "host_health_report";
  public static final String HOST_NAME_PROPERTY_ID = "host_name";
  public static final String HOST_STATUS_PROPERTY_ID = "host_status";
  public static final String IP_PROPERTY_ID = "ip";
  public static final String LAST_AGENT_ENV_PROPERTY_ID = "last_agent_env";
  public static final String LAST_HEARTBEAT_TIME_PROPERTY_ID = "last_heartbeat_time";
  public static final String LAST_REGISTRATION_TIME_PROPERTY_ID = "last_registration_time";
  public static final String MAINTENANCE_STATE_PROPERTY_ID = "maintenance_state";
  public static final String OS_ARCH_PROPERTY_ID = "os_arch";
  public static final String OS_FAMILY_PROPERTY_ID = "os_family";
  public static final String OS_TYPE_PROPERTY_ID = "os_type";
  public static final String PHYSICAL_CPU_COUNT_PROPERTY_ID = "ph_cpu_count";
  public static final String PUBLIC_NAME_PROPERTY_ID = "public_host_name";
  public static final String RACK_INFO_PROPERTY_ID = "rack_info";
  public static final String RECOVERY_REPORT_PROPERTY_ID = "recovery_report";
  public static final String RECOVERY_SUMMARY_PROPERTY_ID = "recovery_summary";
  public static final String STATE_PROPERTY_ID = "host_state";
  public static final String TOTAL_MEM_PROPERTY_ID = "total_mem";
  public static final String ATTRIBUTES_PROPERTY_ID = "attributes";

  public static final String HOST_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME_PROPERTY_ID;
  public static final String HOST_CPU_COUNT_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CPU_COUNT_PROPERTY_ID;
  public static final String HOST_DESIRED_CONFIGS_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + DESIRED_CONFIGS_PROPERTY_ID;
  public static final String HOST_DISK_INFO_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + DISK_INFO_PROPERTY_ID;
  public static final String HOST_HOST_HEALTH_REPORT_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + HOST_HEALTH_REPORT_PROPERTY_ID;
  public static final String HOST_HOST_STATUS_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + HOST_STATUS_PROPERTY_ID;
  public static final String HOST_IP_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + IP_PROPERTY_ID;
  public static final String HOST_LAST_AGENT_ENV_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + LAST_AGENT_ENV_PROPERTY_ID;
  public static final String HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + LAST_HEARTBEAT_TIME_PROPERTY_ID;
  public static final String HOST_LAST_REGISTRATION_TIME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + LAST_REGISTRATION_TIME_PROPERTY_ID;
  public static final String HOST_MAINTENANCE_STATE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + MAINTENANCE_STATE_PROPERTY_ID;
  public static final String HOST_HOST_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + HOST_NAME_PROPERTY_ID;
  public static final String HOST_OS_ARCH_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + OS_ARCH_PROPERTY_ID;
  public static final String HOST_OS_FAMILY_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + OS_FAMILY_PROPERTY_ID;
  public static final String HOST_OS_TYPE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + OS_TYPE_PROPERTY_ID;
  public static final String HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + PHYSICAL_CPU_COUNT_PROPERTY_ID;
  public static final String HOST_PUBLIC_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + PUBLIC_NAME_PROPERTY_ID;
  public static final String HOST_RACK_INFO_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + RACK_INFO_PROPERTY_ID;
  public static final String HOST_RECOVERY_REPORT_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + RECOVERY_REPORT_PROPERTY_ID;
  public static final String HOST_RECOVERY_SUMMARY_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + RECOVERY_SUMMARY_PROPERTY_ID;
  public static final String HOST_STATE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + STATE_PROPERTY_ID;
  public static final String HOST_TOTAL_MEM_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + TOTAL_MEM_PROPERTY_ID;
  public static final String HOST_ATTRIBUTES_PROPERTY_ID = PropertyHelper.getPropertyId(RESPONSE_KEY,ATTRIBUTES_PROPERTY_ID);

  public static final String BLUEPRINT_PROPERTY_ID = "blueprint";
  public static final String HOST_GROUP_PROPERTY_ID = "host_group";
  public static final String HOST_COUNT_PROPERTY_ID = "host_count";
  public static final String HOST_PREDICATE_PROPERTY_ID = "host_predicate";

  //todo use the same json structure for cluster host addition (cluster template and upscale)

  /**
   * The key property ids for a Host resource.
   */
  public static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Host, HOST_HOST_NAME_PROPERTY_ID)
      .put(Resource.Type.Cluster, HOST_CLUSTER_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Host resource.
   */
  public static final Set<String> propertyIds = ImmutableSet.of(
      HOST_CLUSTER_NAME_PROPERTY_ID,
      HOST_CPU_COUNT_PROPERTY_ID,
      HOST_DESIRED_CONFIGS_PROPERTY_ID,
      HOST_DISK_INFO_PROPERTY_ID,
      HOST_HOST_HEALTH_REPORT_PROPERTY_ID,
      HOST_HOST_STATUS_PROPERTY_ID,
      HOST_IP_PROPERTY_ID,
      HOST_LAST_AGENT_ENV_PROPERTY_ID,
      HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID,
      HOST_LAST_REGISTRATION_TIME_PROPERTY_ID,
      HOST_MAINTENANCE_STATE_PROPERTY_ID,
      HOST_HOST_NAME_PROPERTY_ID,
      HOST_OS_ARCH_PROPERTY_ID,
      HOST_OS_FAMILY_PROPERTY_ID,
      HOST_OS_TYPE_PROPERTY_ID,
      HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID,
      HOST_PUBLIC_NAME_PROPERTY_ID,
      HOST_RACK_INFO_PROPERTY_ID,
      HOST_RECOVERY_REPORT_PROPERTY_ID,
      HOST_RECOVERY_SUMMARY_PROPERTY_ID,
      HOST_STATE_PROPERTY_ID,
      HOST_TOTAL_MEM_PROPERTY_ID,
      HOST_ATTRIBUTES_PROPERTY_ID);

  @Inject
  private OsFamily osFamily;

  @Inject
  private static TopologyManager topologyManager;

  @Inject
  private TopologyHolder topologyHolder;

  @Inject
  private HostLevelParamsHolder hostLevelParamsHolder;

  @Inject
  private RecoveryConfigHelper recoveryConfigHelper;


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController  the management controller
   */
  @AssistedInject
  HostResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.Host, propertyIds, keyPropertyIds, managementController);

    Set<RoleAuthorization> authorizationsAddDelete = EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS);

    setRequiredCreateAuthorizations(authorizationsAddDelete);
    setRequiredDeleteAuthorizations(authorizationsAddDelete);
    setRequiredGetAuthorizations(RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER);
    setRequiredUpdateAuthorizations(RoleAuthorization.AUTHORIZATIONS_UPDATE_CLUSTER);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
          UnsupportedPropertyException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    RequestStatusResponse createResponse = null;
    if (isHostGroupRequest(request)) {
      createResponse = submitHostRequests(request);
    } else {
      createResources((Command<Void>) () -> {
        createHosts(request);
        return null;
      });
    }
    notifyCreate(Resource.Type.Host, request);

    return getRequestStatus(createResponse);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(null));
    }
    else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<HostResponse> responses = getResources(() -> getHosts(requests));

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    for (HostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Host);

      // TODO : properly handle more than one cluster
      if (response.getClusterName() != null
          && !response.getClusterName().isEmpty()) {
        setResourceProperty(resource, HOST_CLUSTER_NAME_PROPERTY_ID,
            response.getClusterName(), requestedIds);
      }
      setResourceProperty(resource, HOST_HOST_NAME_PROPERTY_ID,
          response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_PUBLIC_NAME_PROPERTY_ID,
          response.getPublicHostName(), requestedIds);
      setResourceProperty(resource, HOST_IP_PROPERTY_ID,
          response.getIpv4(), requestedIds);
      setResourceProperty(resource, HOST_TOTAL_MEM_PROPERTY_ID,
          response.getTotalMemBytes(), requestedIds);
      setResourceProperty(resource, HOST_CPU_COUNT_PROPERTY_ID,
          response.getCpuCount(), requestedIds);
      setResourceProperty(resource, HOST_PHYSICAL_CPU_COUNT_PROPERTY_ID,
          response.getPhCpuCount(), requestedIds);
      setResourceProperty(resource, HOST_OS_ARCH_PROPERTY_ID,
          response.getOsArch(), requestedIds);
      setResourceProperty(resource, HOST_OS_TYPE_PROPERTY_ID,
          response.getOsType(), requestedIds);
      setResourceProperty(resource, HOST_OS_FAMILY_PROPERTY_ID,
          response.getOsFamily(), requestedIds);
      setResourceProperty(resource, HOST_RACK_INFO_PROPERTY_ID,
          response.getRackInfo(), requestedIds);
      setResourceProperty(resource, HOST_LAST_HEARTBEAT_TIME_PROPERTY_ID,
          response.getLastHeartbeatTime(), requestedIds);
      setResourceProperty(resource, HOST_LAST_AGENT_ENV_PROPERTY_ID,
          response.getLastAgentEnv(), requestedIds);
      setResourceProperty(resource, HOST_LAST_REGISTRATION_TIME_PROPERTY_ID,
          response.getLastRegistrationTime(), requestedIds);
      setResourceProperty(resource, HOST_HOST_STATUS_PROPERTY_ID,
          response.getStatus(),requestedIds);
      setResourceProperty(resource, HOST_HOST_HEALTH_REPORT_PROPERTY_ID,
          response.getHealthReport(), requestedIds);
      setResourceProperty(resource, HOST_RECOVERY_REPORT_PROPERTY_ID,
          response.getRecoveryReport(), requestedIds);
      setResourceProperty(resource, HOST_RECOVERY_SUMMARY_PROPERTY_ID,
          response.getRecoverySummary(), requestedIds);
      setResourceProperty(resource, HOST_DISK_INFO_PROPERTY_ID,
          response.getDisksInfo(), requestedIds);
      setResourceProperty(resource, HOST_STATE_PROPERTY_ID,
          response.getHostState(), requestedIds);
      setResourceProperty(resource, HOST_DESIRED_CONFIGS_PROPERTY_ID,
          response.getDesiredHostConfigs(), requestedIds);

      // only when a cluster request
      if (null != response.getMaintenanceState()) {
        setResourceProperty(resource, HOST_MAINTENANCE_STATE_PROPERTY_ID,
            response.getMaintenanceState(), requestedIds);
      }

      resources.add(resource);
    }
    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources((Command<Void>) () -> {
      updateHosts(requests);
      return null;
    });

    notifyUpdate(Resource.Type.Host, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<HostRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    DeleteStatusMetaData deleteStatusMetaData = modifyResources(() -> deleteHosts(requests, request.isDryRunRequest()));

    if (!request.isDryRunRequest()) {
      notifyDelete(Resource.Type.Host, predicate);
    }

    return getRequestStatus(null, null, deleteStatusMetaData);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> baseUnsupported = super.checkPropertyIds(propertyIds);

    baseUnsupported.remove(BLUEPRINT_PROPERTY_ID);
    baseUnsupported.remove(HOST_GROUP_PROPERTY_ID);
    baseUnsupported.remove(HOST_NAME_PROPERTY_ID);
    baseUnsupported.remove(HOST_COUNT_PROPERTY_ID);
    baseUnsupported.remove(HOST_PREDICATE_PROPERTY_ID);
    baseUnsupported.remove(RACK_INFO_PROPERTY_ID);

    return checkConfigPropertyIds(baseUnsupported, "Hosts");
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Determine if a request is a high level "add hosts" call or a simple lower level request
   * to add a host resources.
   *
   * @param request  current request
   * @return true if this is a high level "add hosts" request;
   *         false if it is a simple create host resources request
   */
  private boolean isHostGroupRequest(Request request) {
    boolean isHostGroupRequest = false;
    Set<Map<String, Object>> properties = request.getProperties();
    if (properties != null && ! properties.isEmpty()) {
      //todo: for now, either all or none of the hosts need to specify a hg.  Unable to mix.
      String hgName = (String) properties.iterator().next().get(HOST_GROUP_PROPERTY_ID);
      isHostGroupRequest = hgName != null && ! hgName.isEmpty();
    }
    return isHostGroupRequest;
  }

  /**
   * Get a host request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private HostRequest getRequest(Map<String, Object> properties) {

    if (properties == null) {
      return new HostRequest(null, null);
    }

    HostRequest hostRequest = new HostRequest(
        getHostNameFromProperties(properties),
        (String) properties.get(HOST_CLUSTER_NAME_PROPERTY_ID)
    );
    hostRequest.setPublicHostName((String) properties.get(HOST_PUBLIC_NAME_PROPERTY_ID));

    String rackInfo = (String) ((null != properties.get(HOST_RACK_INFO_PROPERTY_ID))? properties.get(HOST_RACK_INFO_PROPERTY_ID):
            properties.get(RACK_INFO_PROPERTY_ID));

    hostRequest.setRackInfo(rackInfo);
    hostRequest.setBlueprintName((String) properties.get(BLUEPRINT_PROPERTY_ID));
    hostRequest.setHostGroupName((String) properties.get(HOST_GROUP_PROPERTY_ID));

    Object o = properties.get(HOST_MAINTENANCE_STATE_PROPERTY_ID);
    if (null != o) {
      hostRequest.setMaintenanceState(o.toString());
    }

    List<ConfigurationRequest> cr = getConfigurationRequests("Hosts", properties);

    hostRequest.setDesiredConfigs(cr);

    return hostRequest;
  }


  /**
   * Accepts a request with registered hosts and if the request contains a cluster name then will map all of the
   * hosts onto that cluster.
   * @param request Request that must contain registered hosts, and optionally a cluster.
   */
  public synchronized void createHosts(Request request)
      throws AmbariException, AuthorizationException {

    Set<Map<String, Object>> propertySet = request.getProperties();
    if (propertySet == null || propertySet.isEmpty()) {
      LOG.warn("Received a create host request with no associated property sets");
      return;
    }

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    Set<String> duplicates = new HashSet<>();
    Set<String> unknowns = new HashSet<>();
    Set<String> allHosts = new HashSet<>();


    Set<HostRequest> hostRequests = new HashSet<>();
    for (Map<String, Object> propertyMap : propertySet) {
      HostRequest hostRequest = getRequest(propertyMap);
      hostRequests.add(hostRequest);
      if (! propertyMap.containsKey(HOST_GROUP_PROPERTY_ID)) {
        createHostResource(clusters, duplicates, unknowns, allHosts, hostRequest);
      }
    }

    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("Invalid request contains duplicate hostnames"
              + ", hostnames=" + String.join(",", duplicates));
    }

    if (!unknowns.isEmpty()) {
      throw new IllegalArgumentException("Attempted to add unknown hosts to a cluster.  " +
              "These hosts have not been registered with the server: " +
              String.join(",", unknowns));
    }

    Map<String, Set<String>> hostClustersMap = new HashMap<>();
    Map<String, Map<String, String>> hostAttributes = new HashMap<>();
    Set<String> allClusterSet = new HashSet<>();

    TreeMap<String, TopologyCluster> addedTopologies = new TreeMap<>();
    List<HostLevelParamsUpdateEvent> hostLevelParamsUpdateEvents = new ArrayList<>();
    for (HostRequest hostRequest : hostRequests) {
      if (hostRequest.getHostname() != null &&
          !hostRequest.getHostname().isEmpty() &&
          hostRequest.getClusterName() != null &&
          !hostRequest.getClusterName().isEmpty()){

        Set<String> clusterSet = new HashSet<>();
        clusterSet.add(hostRequest.getClusterName());
        allClusterSet.add(hostRequest.getClusterName());
        hostClustersMap.put(hostRequest.getHostname(), clusterSet);
        Cluster cl = clusters.getCluster(hostRequest.getClusterName());
        String clusterId = Long.toString(cl.getClusterId());
        if (!addedTopologies.containsKey(clusterId)) {
          addedTopologies.put(clusterId, new TopologyCluster());
        }
        Host addedHost = clusters.getHost(hostRequest.getHostname());
        addedTopologies.get(clusterId).addTopologyHost(new TopologyHost(addedHost.getHostId(), addedHost.getHostName(),
            addedHost.getRackInfo(), addedHost.getIPv4()));
        HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent = new HostLevelParamsUpdateEvent(addedHost.getHostId(),
            clusterId,
            new HostLevelParamsCluster( recoveryConfigHelper.getRecoveryConfig(cl.getClusterName(), addedHost.getHostName()),
            getManagementController().getBlueprintProvisioningStates(cl.getClusterId(), addedHost.getHostId())
        ));
        hostLevelParamsUpdateEvents.add(hostLevelParamsUpdateEvent);
      }
    }
    clusters.updateHostWithClusterAndAttributes(hostClustersMap, hostAttributes);

    // TODO add rack change to topology update
    updateHostRackInfoIfChanged(clusters, hostRequests);

    for (HostLevelParamsUpdateEvent hostLevelParamsUpdateEvent : hostLevelParamsUpdateEvents) {
      hostLevelParamsHolder.updateData(hostLevelParamsUpdateEvent);
    }
    TopologyUpdateEvent topologyUpdateEvent =
        new TopologyUpdateEvent(addedTopologies, UpdateEventType.UPDATE);
    topologyHolder.updateData(topologyUpdateEvent);
  }

  /**
   * Iterates through the provided host request and checks if there is rack info provided.
   * If the rack info differs from the rack info of the host than updates it with the value from
   * the host request.
   * @param clusters
   * @param hostRequests
   * @throws AmbariException
   * @throws AuthorizationException
   */
  private void updateHostRackInfoIfChanged(Clusters clusters, Set<HostRequest> hostRequests)
    throws AmbariException, AuthorizationException {

    HashSet<String> rackChangeAffectedClusters = new HashSet<>();

    for (HostRequest hostRequest : hostRequests) {
      String clusterName = hostRequest.getClusterName();

      if (StringUtils.isNotBlank(clusterName)) {
        Cluster cluster = clusters.getCluster(clusterName);
        Host host = clusters.getHost(hostRequest.getHostname());

        if (updateHostRackInfoIfChanged(cluster, host, hostRequest))
          rackChangeAffectedClusters.add(clusterName);
      }
    }
    // TODO rack change topology update
    for (String clusterName: rackChangeAffectedClusters) {
      getManagementController().registerRackChange(clusterName);
    }
  }



  /**
   * If the rack info provided in the request differs from the rack info of the host
   * update the rack info of the host with the value from the host request
   *
   * @param cluster The cluster to check user privileges against. User is required
   *                to have {@link RoleAuthorization#HOST_ADD_DELETE_HOSTS} rights on the cluster.
   * @param host The host of which rack information is to be updated
   * @param hostRequest
   * @return true is host was updated otherwise false
   * @throws AmbariException
   * @throws AuthorizationException
   */
  private boolean updateHostRackInfoIfChanged(Cluster cluster, Host host, HostRequest hostRequest)
    throws AmbariException, AuthorizationException {

    Long resourceId = cluster.getResourceId();

    String hostRackInfo = host.getRackInfo();
    String requestRackInfo = hostRequest.getRackInfo();

    boolean rackChange = requestRackInfo != null && !requestRackInfo.equals(hostRackInfo);

    if (rackChange) {
      if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_ADD_DELETE_HOSTS)) {
        throw new AuthorizationException("The authenticated user is not authorized to update host rack information");
      }
      //TODO topology update

      host.setRackInfo(requestRackInfo);
    }

    return rackChange;
  }

  private void createHostResource(Clusters clusters, Set<String> duplicates,
                                  Set<String> unknowns, Set<String> allHosts,
                                  HostRequest request)
      throws AmbariException {


    if (request.getHostname() == null
        || request.getHostname().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, hostname"
          + " cannot be null");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createHost request, hostname={}, request={}", request.getHostname(), request);
    }

    if (allHosts.contains(request.getHostname())) {
      // throw dup error later
      duplicates.add(request.getHostname());
      return;
    }
    allHosts.add(request.getHostname());

    try {
      // ensure host is registered
      clusters.getHost(request.getHostname());
    }
    catch (HostNotFoundException e) {
      unknowns.add(request.getHostname());
      return;
    }

    if (request.getClusterName() != null) {
      try {
        // validate that cluster_name is valid
        clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a host to a cluster which doesn't exist: "
            + " clusterName=" + request.getClusterName());
      }
    }
  }

  public RequestStatusResponse install(final String cluster, final String hostname,
                                       Collection<String> skipInstallForComponents,
                                       Collection<String> dontSkipInstallForComponents, final boolean skipFailure,
                                       boolean useClusterHostInfo)
      throws SystemException,
      NoSuchParentResourceException,
      UnsupportedPropertyException {


    return ((HostComponentResourceProvider) getResourceProvider(Resource.Type.HostComponent)).
        install(cluster, hostname, skipInstallForComponents, dontSkipInstallForComponents, skipFailure, useClusterHostInfo);
  }

  public RequestStatusResponse start(final String cluster, final String hostname)
      throws SystemException,
      NoSuchParentResourceException,
      UnsupportedPropertyException {

    return ((HostComponentResourceProvider) getResourceProvider(Resource.Type.HostComponent)).
        start(cluster, hostname);
  }

  protected Set<HostResponse> getHosts(Set<HostRequest> requests) throws AmbariException {
    Set<HostResponse> response = new HashSet<>();

    AmbariManagementController controller = getManagementController();

    for (HostRequest request : requests) {
      try {
        response.addAll(getHosts(controller, request, osFamily));
      } catch (HostNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  /**
   * @param osFamily provides OS to OS family lookup; may be null if OS family is ignored anyway (eg. for liveness check)
   */
  protected static Set<HostResponse> getHosts(AmbariManagementController controller, HostRequest request, OsFamily osFamily)
      throws AmbariException {

    //TODO/FIXME host can only belong to a single cluster so get host directly from Cluster
    //TODO/FIXME what is the requirement for filtering on host attributes?

    List<Host> hosts;
    Set<HostResponse> response = new HashSet<>();
    Cluster           cluster  = null;

    Clusters                   clusters   = controller.getClusters();

    String clusterName = request.getClusterName();
    String hostName    = request.getHostname();

    if (clusterName != null) {
      //validate that cluster exists, throws exception if it doesn't.
      try {
        cluster = clusters.getCluster(clusterName);
      } catch (ObjectNotFoundException e) {
        throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
      }
    }

    if (hostName == null) {
      hosts = clusters.getHosts();
    } else {
      hosts = new ArrayList<>();
      try {
        hosts.add(clusters.getHost(request.getHostname()));
      } catch (HostNotFoundException e) {
        // add cluster name
        throw new HostNotFoundException(clusterName, hostName);
      }
    }

    // retrieve the cluster desired configs once instead of per host
    Map<String, DesiredConfig> desiredConfigs = null;
    if (null != cluster) {
      desiredConfigs = cluster.getDesiredConfigs();
    }

    for (Host h : hosts) {
      if (clusterName != null) {
        if (clusters.getClustersForHost(h.getHostName()).contains(cluster)) {
          HostResponse r = h.convertToResponse();

          r.setClusterName(clusterName);
          r.setDesiredHostConfigs(h.getDesiredHostConfigs(cluster, desiredConfigs));
          r.setMaintenanceState(h.getMaintenanceState(cluster.getClusterId()));
          if (osFamily != null) {
            String hostOsFamily = osFamily.find(r.getOsType());
            if (hostOsFamily == null) {
              LOG.error("Can not find host OS family. For OS type = '{}' and host name = '{}'", r.getOsType(), r.getHostname());
            }
            r.setOsFamily(hostOsFamily);
          }

          response.add(r);
        } else if (hostName != null) {
          throw new HostNotFoundException(clusterName, hostName);
        }
      } else {
        HostResponse r = h.convertToResponse();

        Set<Cluster> clustersForHost = clusters.getClustersForHost(h.getHostName());
        //todo: host can only belong to a single cluster
        if (clustersForHost != null && clustersForHost.size() != 0) {
          Cluster clusterForHost = clustersForHost.iterator().next();
          r.setClusterName(clusterForHost.getClusterName());
          r.setDesiredHostConfigs(h.getDesiredHostConfigs(clusterForHost, null));
          r.setMaintenanceState(h.getMaintenanceState(clusterForHost.getClusterId()));
        }

        response.add(r);
      }
    }
    return response;
  }

  protected synchronized void updateHosts(Set<HostRequest> requests) throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();

    for (HostRequest request : requests) {
      if (request.getHostname() == null || request.getHostname().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, hostname should be provided");
      }
    }

    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();
    for (HostRequest request : requests) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received an updateHost request, hostname={}, request={}", request.getHostname(), request);
      }

      Host host = clusters.getHost(request.getHostname());

      String clusterName = request.getClusterName();
      Cluster cluster = clusters.getCluster(clusterName);
      Long clusterId = cluster.getClusterId();
      Long resourceId = cluster.getResourceId();
      TopologyHost topologyHost = new TopologyHost(host.getHostId(), host.getHostName());

      try {
        // The below method call throws an exception when trying to create a duplicate mapping in the clusterhostmapping
        // table. This is done to detect duplicates during host create. In order to be robust, handle these gracefully.
        clusters.mapAndPublishHostsToCluster(new HashSet<>(Arrays.asList(request.getHostname())), clusterName);
      } catch (DuplicateResourceException e) {
        // do nothing
      }

      boolean rackChange = updateHostRackInfoIfChanged(cluster, host, request);

      if (rackChange) {
        topologyHost.setRackName(host.getRackInfo());
      }

      if (null != request.getPublicHostName()) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_ADD_DELETE_HOSTS)) {
          throw new AuthorizationException("The authenticated user is not authorized to update host attributes");
        }
        host.setPublicHostName(request.getPublicHostName());
        topologyHost.setHostName(request.getPublicHostName());
      }

      if (null != clusterName && null != request.getMaintenanceState()) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, resourceId, RoleAuthorization.HOST_TOGGLE_MAINTENANCE)) {
          throw new AuthorizationException("The authenticated user is not authorized to update host maintenance state");
        }
        MaintenanceState newState = MaintenanceState.valueOf(request.getMaintenanceState());
        MaintenanceState oldState = host.getMaintenanceState(clusterId);
        if (!newState.equals(oldState)) {
          if (newState.equals(MaintenanceState.IMPLIED_FROM_HOST)
              || newState.equals(MaintenanceState.IMPLIED_FROM_SERVICE)) {
            throw new IllegalArgumentException("Invalid arguments, can only set " +
              "maintenance state to one of " + EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            host.setMaintenanceState(clusterId, newState);
          }
        }
      }

      // Create configurations
      if (null != clusterName && null != request.getDesiredConfigs()) {
        if (clusters.getHostsForCluster(clusterName).containsKey(host.getHostName())) {

          for (ConfigurationRequest cr : request.getDesiredConfigs()) {
            if (null != cr.getProperties() && cr.getProperties().size() > 0) {
              LOG.info(MessageFormat.format("Applying configuration with tag ''{0}'' to host ''{1}'' in cluster ''{2}''",
                  cr.getVersionTag(),
                  request.getHostname(),
                  clusterName));

              cr.setClusterName(cluster.getClusterName());
              controller.createConfiguration(cr);
            }

            Config baseConfig = cluster.getConfig(cr.getType(), cr.getVersionTag());
            if (null != baseConfig) {
              String authName = controller.getAuthName();
              DesiredConfig oldConfig = host.getDesiredConfigs(clusterId).get(cr.getType());

              if (host.addDesiredConfig(clusterId, cr.isSelected(), authName,  baseConfig)) {
                Logger logger = LoggerFactory.getLogger("configchange");
                logger.info("(configchange) cluster '" + cluster.getClusterName() + "', "
                    + "host '" + host.getHostName() + "' "
                    + "changed by: '" + authName + "'; "
                    + "type='" + baseConfig.getType() + "' "
                    + "version='" + baseConfig.getVersion() + "'"
                    + "tag='" + baseConfig.getTag() + "'"
                    + (null == oldConfig ? "" : ", from='" + oldConfig.getTag() + "'"));
              }
            }
          }
        }
      }

      if (StringUtils.isNotBlank(clusterName) && rackChange) {
        // Authorization check for this update was performed before we got to this point.
        controller.registerRackChange(clusterName);
      }

      if (!topologyUpdates.containsKey(clusterId.toString())) {
        topologyUpdates.put(clusterId.toString(), new TopologyCluster());
      }
      topologyUpdates.get(clusterId.toString()).addTopologyHost(topologyHost);
      TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(topologyUpdates,
          UpdateEventType.UPDATE);
      topologyHolder.updateData(topologyUpdateEvent);
      //todo: if attempt was made to update a property other than those
      //todo: that are allowed above, should throw exception
    }
  }

  @Transactional
  protected DeleteStatusMetaData deleteHosts(Set<HostRequest> requests, boolean dryRun)
      throws AmbariException {

    AmbariManagementController controller = getManagementController();
    Clusters                   clusters   = controller.getClusters();
    DeleteStatusMetaData deleteStatusMetaData = new DeleteStatusMetaData();
    List<HostRequest> okToRemove = new ArrayList<>();

    for (HostRequest hostRequest : requests) {
      String hostName = hostRequest.getHostname();
      if (null == hostName) {
        continue;
      }

      try {
        validateHostInDeleteFriendlyState(hostRequest, clusters);
        okToRemove.add(hostRequest);
      } catch (Exception ex) {
        deleteStatusMetaData.addException(hostName, ex);
      }
    }

    //If dry run, don't delete. just assume it can be successfully deleted.
    if (dryRun) {
      for (HostRequest request : okToRemove) {
        deleteStatusMetaData.addDeletedKey(request.getHostname());
      }
    } else {
      processDeleteHostRequests(okToRemove, clusters, deleteStatusMetaData);
    }

    //Do not break behavior for existing clients where delete request contains only 1 host.
    //Response for these requests will have empty body with appropriate error code.
    //dryRun is a new feature so its ok to unify the behavior
    if (!dryRun) {
      if (deleteStatusMetaData.getDeletedKeys().size() + deleteStatusMetaData.getExceptionForKeys().size() == 1) {
        if (deleteStatusMetaData.getDeletedKeys().size() == 1) {
          return null;
        }
        for (Map.Entry<String, Exception> entry : deleteStatusMetaData.getExceptionForKeys().entrySet()) {
          Exception ex = entry.getValue();
          if (ex instanceof AmbariException) {
            throw (AmbariException) ex;
          } else {
            throw new AmbariException(ex.getMessage(), ex);
          }
        }
      }
    }

    return deleteStatusMetaData;
  }

  private void processDeleteHostRequests(List<HostRequest> requests,  Clusters clusters, DeleteStatusMetaData deleteStatusMetaData) throws AmbariException {
    Set<String> hostsClusters = new HashSet<>();
    Set<String> hostNames = new HashSet<>();
    Set<Long> hostIds = new HashSet<>();
    TreeMap<String, TopologyCluster> topologyUpdates = new TreeMap<>();
    for (HostRequest hostRequest : requests) {
      // Assume the user also wants to delete it entirely, including all clusters.
      String hostname = hostRequest.getHostname();
      Long hostId = clusters.getHost(hostname).getHostId();
      hostNames.add(hostname);
      hostIds.add(hostId);

      if (hostRequest.getClusterName() != null) {
        hostsClusters.add(hostRequest.getClusterName());
      }

      LOG.info("Received Delete request for host {} from cluster {}.", hostname, hostRequest.getClusterName());

      // delete all host components
      Set<ServiceComponentHostRequest> schrs = new HashSet<>();
      for (Cluster cluster : clusters.getClustersForHost(hostname)) {
        List<ServiceComponentHost> list = cluster.getServiceComponentHosts(hostname);
        for (ServiceComponentHost sch : list) {
          ServiceComponentHostRequest schr = new ServiceComponentHostRequest(cluster.getClusterName(),
                                                                             sch.getServiceName(),
                                                                             sch.getServiceComponentName(),
                                                                             sch.getHostName(),
                                                                             null);
          schrs.add(schr);
        }
      }
      DeleteStatusMetaData componentDeleteStatus = null;
      if (schrs.size() > 0) {
        try {
          componentDeleteStatus = getManagementController().deleteHostComponents(schrs);
        } catch (Exception ex) {
          deleteStatusMetaData.addException(hostname, ex);
        }
      }

      if (componentDeleteStatus != null) {
        for (String key : componentDeleteStatus.getDeletedKeys()) {
          deleteStatusMetaData.addDeletedKey(key);
        }
        for (String key : componentDeleteStatus.getExceptionForKeys().keySet()) {
          deleteStatusMetaData.addException(key, componentDeleteStatus.getExceptionForKeys().get(key));
        }
      }

      if (hostRequest.getClusterName() != null) {
        hostsClusters.add(hostRequest.getClusterName());
      }
      try {
        Set<Cluster> hostClusters = new HashSet<>(clusters.getClustersForHost(hostname));
        clusters.deleteHost(hostname);
        for (Cluster cluster : hostClusters) {
          String clusterId = Long.toString(cluster.getClusterId());
          if (!topologyUpdates.containsKey(clusterId)) {
            topologyUpdates.put(clusterId, new TopologyCluster());
          }
          topologyUpdates.get(clusterId).getTopologyHosts().add(new TopologyHost(hostId, hostname));
        }
        deleteStatusMetaData.addDeletedKey(hostname);
      } catch (Exception ex) {
        deleteStatusMetaData.addException(hostname, ex);
      }
      removeHostFromClusterTopology(clusters, hostRequest);
      for (LogicalRequest logicalRequest: topologyManager.getRequests(Collections.emptyList())) {
        logicalRequest.removeHostRequestByHostName(hostname);
      }
    }
    clusters.publishHostsDeletion(hostIds, hostNames);
    TopologyUpdateEvent topologyUpdateEvent = new TopologyUpdateEvent(topologyUpdates,
        UpdateEventType.DELETE);
    topologyHolder.updateData(topologyUpdateEvent);
  }

  private void validateHostInDeleteFriendlyState(HostRequest hostRequest, Clusters clusters) throws AmbariException {
    Set<String> clusterNamesForHost = new HashSet<>();
    String hostName = hostRequest.getHostname();
    if (null != hostRequest.getClusterName()) {
      clusterNamesForHost.add(hostRequest.getClusterName());
    } else {
      Set<Cluster> clustersForHost = clusters.getClustersForHost(hostRequest.getHostname());
      if (null != clustersForHost) {
        for (Cluster c : clustersForHost) {
          clusterNamesForHost.add(c.getClusterName());
        }
      }
    }

    for (String clusterName : clusterNamesForHost) {
      Cluster cluster = clusters.getCluster(clusterName);

      List<ServiceComponentHost> list = cluster.getServiceComponentHosts(hostName);

      if (!list.isEmpty()) {
        List<String> componentsStarted = new ArrayList<>();
        for (ServiceComponentHost sch : list) {
          if (!sch.canBeRemoved()) {
            componentsStarted.add(sch.getServiceComponentName());
          }
        }

        // error if components are running
        if (!componentsStarted.isEmpty()) {
          StringBuilder reason = new StringBuilder("Cannot remove host ")
              .append(hostName)
              .append(" from ")
              .append(hostRequest.getClusterName())
              .append(
                  ".  The following roles exist, and these components are not in the removable state: ");

          reason.append(StringUtils.join(componentsStarted, ", "));

          throw new AmbariException(reason.toString());
        }
      }
    }
  }

  /**
   * Removes hostname from the stateful cluster topology
   */
  private void removeHostFromClusterTopology(Clusters clusters, HostRequest hostRequest) throws AmbariException{
    if (hostRequest.getClusterName() == null) {
      for (Cluster c : clusters.getClusters().values()) {
        removeHostFromClusterTopology(c.getClusterId(), hostRequest.getHostname());
      }
    } else {
      long clusterId = clusters.getCluster(hostRequest.getClusterName()).getClusterId();
      removeHostFromClusterTopology(clusterId, hostRequest.getHostname());
    }
  }

  private void removeHostFromClusterTopology(long clusterId, String hostname) {
    ClusterTopology clusterTopology = topologyManager.getClusterTopology(clusterId);
    if(clusterTopology != null) {
      clusterTopology.removeHost(hostname);
    }
  }

  /**
   * Obtain the hostname from the request properties.  The hostname property name may differ
   * depending on the request type.  For the low level host resource creation calls, it is always
   * "Hosts/host_name".  For multi host "add host from hostgroup", the hostname property is a top level
   * property "host_name".
   *
   * @param properties  request properties
   *
   * @return the host name for the host request
   */
  public static String getHostNameFromProperties(Map<String, Object> properties) {
    String hostname = (String) properties.get(HOST_HOST_NAME_PROPERTY_ID);

    return hostname != null ? hostname :
        (String) properties.get(HOST_NAME_PROPERTY_ID);
  }

  //todo: for api/v1/hosts we also end up here so we need to ensure proper 400 response
  //todo: since a user shouldn't be posing to that endpoint
  private RequestStatusResponse submitHostRequests(Request request) throws SystemException {
    ScaleClusterRequest requestRequest;
    try {
      requestRequest = new ScaleClusterRequest(request.getProperties());
    } catch (InvalidTopologyTemplateException e) {
      throw new IllegalArgumentException("Invalid Add Hosts Template: " + e, e);
    }

    try {
      return topologyManager.scaleHosts(requestRequest);
    } catch (InvalidTopologyException e) {
      throw new IllegalArgumentException("Topology validation failed: " + e, e);
    } catch (AmbariException e) {
      //todo: handle non-system exceptions
      e.printStackTrace();
      //todo: for now just throw SystemException
      throw new SystemException("Unable to add hosts", e);
    }
  }

  //todo: proper static injection of topology manager
  public static void setTopologyManager(TopologyManager topologyManager) {
    HostResourceProvider.topologyManager = topologyManager;
  }
}
