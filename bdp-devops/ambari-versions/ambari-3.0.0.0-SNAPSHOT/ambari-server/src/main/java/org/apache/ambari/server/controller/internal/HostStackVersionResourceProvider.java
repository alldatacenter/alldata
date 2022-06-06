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

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.OVERRIDE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.OVERRIDE_STACK_NAME;

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
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
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
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Resource provider for host stack versions resources.
 */
@StaticallyInject
public class HostStackVersionResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HostStackVersionResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  protected static final String HOST_STACK_VERSION_ID_PROPERTY_ID              = PropertyHelper.getPropertyId("HostStackVersions", "id");
  protected static final String HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("HostStackVersions", "cluster_name");
  protected static final String HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("HostStackVersions", "host_name");
  protected static final String HOST_STACK_VERSION_STACK_PROPERTY_ID           = PropertyHelper.getPropertyId("HostStackVersions", "stack");
  protected static final String HOST_STACK_VERSION_VERSION_PROPERTY_ID         = PropertyHelper.getPropertyId("HostStackVersions", "version");
  protected static final String HOST_STACK_VERSION_STATE_PROPERTY_ID           = PropertyHelper.getPropertyId("HostStackVersions", "state");
  protected static final String HOST_STACK_VERSION_REPOSITORIES_PROPERTY_ID    = PropertyHelper.getPropertyId("HostStackVersions", "repositories");
  protected static final String HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID    = PropertyHelper.getPropertyId("HostStackVersions", "repository_version");

  /**
   * Whether to force creating of install command on a host which is not member of any cluster yet.
   * This will also run hdp-select for specified stack version.
   */
  protected static final String HOST_STACK_VERSION_FORCE_INSTALL_ON_NON_MEMBER_HOST_PROPERTY_ID = PropertyHelper
    .getPropertyId("HostStackVersions", "force_non_member_install");

  /**
   * In case of force_non_member_install = true a list of component names must be provided in the request.
   */
  protected static final String HOST_STACK_VERSION_COMPONENT_NAMES_PROPERTY_ID = PropertyHelper.getPropertyId("HostStackVersions", "components");
  protected static final String COMPONENT_NAME_PROPERTY_ID = "name";

  protected static final String INSTALL_PACKAGES_ACTION = "install_packages";
  protected static final String STACK_SELECT_ACTION = "stack_select_set_all";
  protected static final String INSTALL_PACKAGES_FULL_NAME = "Install Version";


  private static final Set<String> pkPropertyIds = Sets.newHashSet(
          HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_ID_PROPERTY_ID,
          HOST_STACK_VERSION_STACK_PROPERTY_ID,
          HOST_STACK_VERSION_VERSION_PROPERTY_ID,
          HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);

  private static final Set<String> propertyIds = Sets.newHashSet(
          HOST_STACK_VERSION_ID_PROPERTY_ID,
          HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID,
          HOST_STACK_VERSION_STACK_PROPERTY_ID,
          HOST_STACK_VERSION_VERSION_PROPERTY_ID,
          HOST_STACK_VERSION_STATE_PROPERTY_ID,
          HOST_STACK_VERSION_REPOSITORIES_PROPERTY_ID,
          HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID,
          HOST_STACK_VERSION_FORCE_INSTALL_ON_NON_MEMBER_HOST_PROPERTY_ID,
          HOST_STACK_VERSION_COMPONENT_NAMES_PROPERTY_ID);

  private static final Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.Cluster, HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      put(Type.Host, HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID);
      put(Type.HostStackVersion, HOST_STACK_VERSION_ID_PROPERTY_ID);
      put(Type.Stack, HOST_STACK_VERSION_STACK_PROPERTY_ID);
      put(Type.StackVersion, HOST_STACK_VERSION_VERSION_PROPERTY_ID);
      put(Type.RepositoryVersion, HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);
    }
  };

  @Inject
  private static HostVersionDAO hostVersionDAO;

  @Inject
  private static RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private static StageFactory stageFactory;

  @Inject
  private static RequestFactory requestFactory;

  @Inject
  private static Provider<AmbariActionExecutionHelper> actionExecutionHelper;

  @Inject
  private static Configuration configuration;

  @Inject
  private static RepositoryVersionHelper repoVersionHelper;

  /**
   * Constructor.
   */
  public HostStackVersionResourceProvider(
          AmbariManagementController managementController) {
    super(Type.HostStackVersion, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (Map<String, Object> propertyMap: propertyMaps) {
      final String hostName = propertyMap.get(HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID).toString();
      String clusterName = null;
      if (propertyMap.containsKey(HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID)) {
        clusterName = propertyMap.get(HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID).toString();
      }
      final Long id;
      List<HostVersionEntity> requestedEntities;
      if (propertyMap.get(HOST_STACK_VERSION_ID_PROPERTY_ID) == null) {
        if (clusterName == null) {
          requestedEntities = hostVersionDAO.findByHost(hostName);
        } else {
          requestedEntities = hostVersionDAO.findByClusterAndHost(clusterName, hostName);
        }
      } else {
        try {
          id = Long.parseLong(propertyMap.get(HOST_STACK_VERSION_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("Stack version should have numerical id");
        }
        final HostVersionEntity entity = hostVersionDAO.findByPK(id);
        if (entity == null) {
          throw new NoSuchResourceException("There is no stack version with id " + id);
        } else {
          requestedEntities = Collections.singletonList(entity);
        }
      }
      if (requestedEntities != null) {
        addRequestedEntities(resources, requestedEntities, requestedIds, clusterName);
      }
    }

    return resources;
  }


  /**
   * Adds requested entities to resources
   * @param resources a list of resources to add to
   * @param requestedEntities requested entities
   * @param requestedIds
   * @param clusterName name of cluster or null if no any
   */
  public void addRequestedEntities(Set<Resource> resources,
                                   List<HostVersionEntity> requestedEntities,
                                   Set<String> requestedIds,
                                   String clusterName) {
    for (HostVersionEntity entity: requestedEntities) {
      StackId stackId = new StackId(entity.getRepositoryVersion().getStack());

      RepositoryVersionEntity repoVerEntity = repositoryVersionDAO.findByStackAndVersion(
          stackId, entity.getRepositoryVersion().getVersion());

      final Resource resource = new ResourceImpl(Resource.Type.HostStackVersion);

      setResourceProperty(resource, HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, entity.getHostName(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_STACK_PROPERTY_ID, stackId.getStackName(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_VERSION_PROPERTY_ID, stackId.getStackVersion(), requestedIds);
      setResourceProperty(resource, HOST_STACK_VERSION_STATE_PROPERTY_ID, entity.getState().name(), requestedIds);

      if (clusterName != null) {
        setResourceProperty(resource, HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
      }

      if (repoVerEntity != null) {
        Long repoVersionId = repoVerEntity.getId();
        setResourceProperty(resource, HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, repoVersionId, requestedIds);
      }

      resources.add(resource);
    }
  }


  @Override
  public RequestStatus createResources(Request request) throws SystemException,
          UnsupportedPropertyException, ResourceAlreadyExistsException,
          NoSuchParentResourceException {
    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    String hostName;
    final String desiredRepoVersion;
    String stackName;
    String stackVersion;
    if (request.getProperties().size() > 1) {
      throw new UnsupportedOperationException("Multiple requests cannot be executed at the same time.");
    }

    Map<String, Object> propertyMap  = iterator.next();

    Set<String> requiredProperties = Sets.newHashSet(
            HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID,
            HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID,
            HOST_STACK_VERSION_STACK_PROPERTY_ID,
            HOST_STACK_VERSION_VERSION_PROPERTY_ID);

    for (String requiredProperty : requiredProperties) {
      Validate.isTrue(propertyMap.containsKey(requiredProperty),
              String.format("The required property %s is not defined", requiredProperty));
    }

    String clName = (String) propertyMap.get (HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
    hostName = (String) propertyMap.get(HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID);
    desiredRepoVersion = (String) propertyMap.get(HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID);
    stackName = (String) propertyMap.get(HOST_STACK_VERSION_STACK_PROPERTY_ID);
    stackVersion = (String) propertyMap.get(HOST_STACK_VERSION_VERSION_PROPERTY_ID);

    boolean forceInstallOnNonMemberHost = false;
    Set<Map<String, String>> componentNames = null;
    String forceInstallOnNonMemberHostString = (String) propertyMap.get
      (HOST_STACK_VERSION_FORCE_INSTALL_ON_NON_MEMBER_HOST_PROPERTY_ID);

    if (BooleanUtils.toBoolean(forceInstallOnNonMemberHostString)) {
      forceInstallOnNonMemberHost = true;
      componentNames = (Set<Map<String, String>>) propertyMap.get(HOST_STACK_VERSION_COMPONENT_NAMES_PROPERTY_ID);
      if (componentNames == null) {
        throw new IllegalArgumentException("In case " + HOST_STACK_VERSION_FORCE_INSTALL_ON_NON_MEMBER_HOST_PROPERTY_ID + " is set to true, the list of " +
          "components should be specified in request.");
      }
    }

    RequestStageContainer req = createInstallPackagesRequest(hostName, desiredRepoVersion, stackName, stackVersion,
      clName, forceInstallOnNonMemberHost, componentNames);
    return getRequestStatus(req.getRequestStatusResponse());
  }

  private RequestStageContainer createInstallPackagesRequest(String hostName, final String desiredRepoVersion,
                                                             String stackName, String stackVersion, String clName,
                                                             final boolean forceInstallOnNonMemberHost,
                                                             Set<Map<String, String>> componentNames)
    throws NoSuchParentResourceException, SystemException {

    Host host;
    try {
      host = getManagementController().getClusters().getHost(hostName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(
              String.format("Can not find host %s", hostName), e);
    }
    AmbariManagementController managementController = getManagementController();
    AmbariMetaInfo ami = managementController.getAmbariMetaInfo();

    final StackId stackId = new StackId(stackName, stackVersion);
    if (!ami.isSupportedStack(stackName, stackVersion)) {
      throw new NoSuchParentResourceException(String.format("Stack %s is not supported",
              stackId));
    }

    Set<Cluster> clusterSet;
    if (clName == null) {
      try {
        clusterSet = getManagementController().getClusters().getClustersForHost(hostName);
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format((
                "Host %s does not belong to any cluster"
        ), hostName), e);
      }
    } else {
      Cluster cluster;
      try {
        cluster = getManagementController().getClusters().getCluster(clName);
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format((
                "Cluster %s does not exist"
        ), clName), e);
      }
      clusterSet = Collections.singleton(cluster);
    }

    Cluster cluster;
    if (clusterSet.isEmpty()) {
      throw new UnsupportedOperationException(String.format("Host %s belongs " +
        "to 0 clusters with stack id %s. Performing %s action failed.",
        hostName, stackId, INSTALL_PACKAGES_FULL_NAME));
    } else if (clusterSet.size() > 1) {
      throw new UnsupportedOperationException(String.format("Host %s belongs " +
        "to %d clusters with stack id %s. Performing %s action on multiple " +
        "clusters is not supported", hostName, clusterSet.size(), stackId,
        INSTALL_PACKAGES_FULL_NAME));
    } else {
      cluster = clusterSet.iterator().next();
    }

    RepositoryVersionEntity repoVersionEnt = repositoryVersionDAO.findByStackAndVersion(stackId, desiredRepoVersion);
    if (repoVersionEnt==null) {
      throw new IllegalArgumentException(String.format(
              "Repo version %s is not available for stack %s",
              desiredRepoVersion, stackId));
    }

    HostVersionEntity hostVersEntity = hostVersionDAO.findByClusterStackVersionAndHost(clName, stackId,
            desiredRepoVersion, hostName);
    if (!forceInstallOnNonMemberHost) {
      if (hostVersEntity == null) {
        throw new IllegalArgumentException(String.format(
          "Repo version %s for stack %s is not available for host %s",
          desiredRepoVersion, stackId, hostName));
      }
      if (hostVersEntity.getState() != RepositoryVersionState.INSTALLED &&
        hostVersEntity.getState() != RepositoryVersionState.INSTALL_FAILED &&
        hostVersEntity.getState() != RepositoryVersionState.OUT_OF_SYNC) {
        throw new UnsupportedOperationException(String.format("Repo version %s for stack %s " +
            "for host %s is in %s state. Can not transition to INSTALLING state",
          desiredRepoVersion, stackId, hostName, hostVersEntity.getState().toString()));
      }
    }

    // Determine repositories for host
    String osFamily = host.getOsFamily();
    RepoOsEntity osEntity = null;
    for (RepoOsEntity operatingSystem : repoVersionEnt.getRepoOsEntities()) {
      if (osFamily.equals(operatingSystem.getFamily())) {
        osEntity = operatingSystem;
        break;
      }
    }

    if (null == osEntity) {
      throw new SystemException(String.format("Operating System matching %s could not be found",
          osFamily));
    }

    if (CollectionUtils.isEmpty(osEntity.getRepoDefinitionEntities())) {
      throw new SystemException(String.format("Repositories for os type %s are " +
                      "not defined. Repo version=%s, stackId=%s",
        osFamily, desiredRepoVersion, stackId));
    }

    Set<String> servicesOnHost = new HashSet<>();

    if (forceInstallOnNonMemberHost) {
      for (Map<String, String> componentProperties : componentNames) {

        String componentName = componentProperties.get(COMPONENT_NAME_PROPERTY_ID);
        if (StringUtils.isEmpty(componentName)) {
          throw new IllegalArgumentException("Components list contains a component with no 'name' property");
        }

        String serviceName = null;
        try {
          serviceName = ami.getComponentToService(stackName, stackVersion, componentName.trim().toUpperCase());
          if (serviceName == null) {
            throw new IllegalArgumentException("Service not found for component : " + componentName);
          }
          servicesOnHost.add(serviceName);
        } catch (AmbariException e) {
          LOG.error("Service not found for component {}!", componentName, e);
          throw new IllegalArgumentException("Service not found for component : " + componentName);
        }
      }

    } else {
      List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
      for (ServiceComponentHost component : components) {
        servicesOnHost.add(component.getServiceName());
      }
    }

    Map<String, String> roleParams = repoVersionHelper.buildRoleParams(managementController, repoVersionEnt,
        osFamily, servicesOnHost);

    // Create custom action
    RequestResourceFilter filter = new RequestResourceFilter(null, null,
            Collections.singletonList(hostName));

    roleParams.put(OVERRIDE_CONFIGS, null);
    roleParams.put(OVERRIDE_STACK_NAME, null);
    ActionExecutionContext actionContext = new ActionExecutionContext(
            null, INSTALL_PACKAGES_ACTION,
            Collections.singletonList(filter),
            roleParams);
    actionContext.setTimeout(Short.valueOf(configuration.getDefaultAgentTaskTimeout(true)));
    actionContext.setStackId(repoVersionEnt.getStackId());

    repoVersionHelper.addCommandRepositoryToContext(actionContext, repoVersionEnt, osEntity);

    String caption = String.format(INSTALL_PACKAGES_FULL_NAME + " on host %s", hostName);
    RequestStageContainer req = createRequest(caption);

    Map<String, String> hostLevelParams = new HashMap<>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());

    // Generate cluster host info
    String clusterHostInfoJson;
    try {
      clusterHostInfoJson = StageUtils.getGson().toJson(
        StageUtils.getClusterHostInfo(cluster));
    } catch (AmbariException e) {
      throw new SystemException("Could not build cluster topology", e);
    }

    Stage stage = stageFactory.createNew(req.getId(),
            "/tmp/ambari",
            cluster.getClusterName(),
            cluster.getClusterId(),
            caption,
            "{}",
            StageUtils.getGson().toJson(hostLevelParams));

    long stageId = req.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    req.addStages(Collections.singletonList(stage));

    try {
      actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null, !forceInstallOnNonMemberHost);
    } catch (AmbariException e) {
      throw new SystemException("Can not modify stage", e);
    }

    if (forceInstallOnNonMemberHost) {
      addSelectStackStage(desiredRepoVersion, forceInstallOnNonMemberHost, cluster, filter, caption, req,
        hostLevelParams, clusterHostInfoJson);
    }

    try {
      if (!forceInstallOnNonMemberHost) {
        hostVersEntity.setState(RepositoryVersionState.INSTALLING);
        hostVersionDAO.merge(hostVersEntity);
      }
      req.persist();
    } catch (AmbariException e) {
      throw new SystemException("Can not persist request", e);
    }
    return req;
  }

  private void addSelectStackStage(String desiredRepoVersion, boolean forceInstallOnNonMemberHost, Cluster cluster,
                                 RequestResourceFilter filter, String caption, RequestStageContainer req, Map<String, String> hostLevelParams, String clusterHostInfoJson) throws SystemException {
    Stage stage;
    long stageId;
    ActionExecutionContext actionContext;
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("version", desiredRepoVersion);

    stage = stageFactory.createNew(req.getId(),
      "/tmp/ambari",
      cluster.getClusterName(),
      cluster.getClusterId(),
      caption,
      StageUtils.getGson().toJson(commandParams),
      StageUtils.getGson().toJson(hostLevelParams));

    stageId = req.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    req.addStages(Collections.singletonList(stage));

    actionContext = new ActionExecutionContext(
      null, STACK_SELECT_ACTION,
      Collections.singletonList(filter),
      Collections.emptyMap());
    actionContext.setTimeout(Short.valueOf(configuration.getDefaultAgentTaskTimeout(true)));

    try {
      actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null, !forceInstallOnNonMemberHost);
    } catch (AmbariException e) {
      throw new SystemException("Can not modify stage", e);
    }
  }


  private RequestStageContainer createRequest(String caption) {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
            actionManager.getNextRequestId(), null, requestFactory, actionManager);
    requestStages.setRequestContext(caption);

    return requestStages;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Method not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Method not supported");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }
}
