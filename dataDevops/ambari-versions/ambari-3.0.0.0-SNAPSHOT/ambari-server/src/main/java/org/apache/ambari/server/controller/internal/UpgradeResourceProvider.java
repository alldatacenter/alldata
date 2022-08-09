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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.cleanup.ClasspathScannerUtils;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteCommandJson;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl.SupportedCustomOperation;
import org.apache.ambari.server.controller.UpdateConfigurationPolicy;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.events.UpgradeUpdateEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.serveraction.upgrades.PluginUpgradeServerAction;
import org.apache.ambari.server.stack.upgrade.AddComponentTask;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradePack;
import org.apache.ambari.server.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.stack.upgrade.CreateAndConfigureTask;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.ManualTask;
import org.apache.ambari.server.stack.upgrade.ServerSideActionTask;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.stack.upgrade.UpdateStackGrouping;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradeScope;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeGroupHolder;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.spi.upgrade.UpgradeAction;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;

/**
 * Manages the ability to start and get status of upgrades.
 */
@StaticallyInject
public class UpgradeResourceProvider extends AbstractControllerResourceProvider {

  public static final String UPGRADE_CLUSTER_NAME = "Upgrade/cluster_name";
  public static final String UPGRADE_REPO_VERSION_ID = "Upgrade/repository_version_id";
  public static final String UPGRADE_TYPE = "Upgrade/upgrade_type";
  public static final String UPGRADE_PACK = "Upgrade/pack";
  public static final String UPGRADE_ID = "Upgrade/upgrade_id";
  public static final String UPGRADE_REQUEST_ID = "Upgrade/request_id";
  public static final String UPGRADE_ASSOCIATED_VERSION = "Upgrade/associated_version";
  public static final String UPGRADE_VERSIONS = "Upgrade/versions";
  public static final String UPGRADE_DIRECTION = "Upgrade/direction";
  public static final String UPGRADE_DOWNGRADE_ALLOWED = "Upgrade/downgrade_allowed";
  public static final String UPGRADE_REQUEST_STATUS = "Upgrade/request_status";
  public static final String UPGRADE_SUSPENDED = "Upgrade/suspended";
  public static final String UPGRADE_ABORT_REASON = "Upgrade/abort_reason";
  public static final String UPGRADE_SKIP_PREREQUISITE_CHECKS = "Upgrade/skip_prerequisite_checks";
  public static final String UPGRADE_FAIL_ON_CHECK_WARNINGS = "Upgrade/fail_on_check_warnings";

  /**
   * Skip slave/client component failures if the tasks are skippable.
   */
  public static final String UPGRADE_SKIP_FAILURES = "Upgrade/skip_failures";

  /**
   * Skip service check failures if the tasks are skippable.
   */
  public static final String UPGRADE_SKIP_SC_FAILURES = "Upgrade/skip_service_check_failures";

  /**
   * Skip manual verification tasks for hands-free upgrade/downgrade experience.
   */
  public static final String UPGRADE_SKIP_MANUAL_VERIFICATION = "Upgrade/skip_manual_verification";

  /**
   * When creating an upgrade of type {@link UpgradeType#HOST_ORDERED}, this
   * specifies the order in which the hosts are upgraded.
   * </p>
   *
   * <pre>
   * "host_order": [
   *   { "hosts":
   *       [ "c6401.ambari.apache.org, "c6402.ambari.apache.org", "c6403.ambari.apache.org" ],
   *     "service_checks": ["ZOOKEEPER"]
   *   },
   *   {
   *     "hosts": [ "c6404.ambari.apache.org, "c6405.ambari.apache.org"],
   *     "service_checks": ["ZOOKEEPER", "KAFKA"]
   *   }
   * ]
   * </pre>
   *
   */
  public static final String UPGRADE_HOST_ORDERED_HOSTS = "Upgrade/host_order";

  /**
   * Allows reversion of a successful upgrade of a patch.
   */
  public static final String UPGRADE_REVERT_UPGRADE_ID = "Upgrade/revert_upgrade_id";

  /**
   * The role that will be used when creating HRC's for the type
   * {@link StageWrapper.Type#UPGRADE_TASKS}.
   */
  protected static final String EXECUTE_TASK_ROLE = "ru_execute_tasks";

  /*
   * Lifted from RequestResourceProvider
   */
  private static final String REQUEST_CONTEXT_ID = "Upgrade/request_context";
  private static final String REQUEST_TYPE_ID = "Upgrade/type";
  private static final String REQUEST_CREATE_TIME_ID = "Upgrade/create_time";
  private static final String REQUEST_START_TIME_ID = "Upgrade/start_time";
  private static final String REQUEST_END_TIME_ID = "Upgrade/end_time";
  private static final String REQUEST_EXCLUSIVE_ID = "Upgrade/exclusive";

  protected static final String REQUEST_PROGRESS_PERCENT_ID = "Upgrade/progress_percent";
  private static final String REQUEST_STATUS_PROPERTY_ID = "Upgrade/request_status";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
      Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_CLUSTER_NAME));

  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  private static final String DEFAULT_REASON_TEMPLATE = "Aborting upgrade %s";

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  @Inject
  static UpgradeDAO s_upgradeDAO;

  @Inject
  private static Provider<AmbariMetaInfo> s_metaProvider = null;

  @Inject
  private static Provider<RequestFactory> s_requestFactory;

  @Inject
  private static Provider<StageFactory> s_stageFactory;

  @Inject
  private static Provider<Clusters> clusters = null;

  @Inject
  private static Provider<AmbariActionExecutionHelper> s_actionExecutionHelper;

  @Inject
  private static Provider<AmbariCustomCommandExecutionHelper> s_commandExecutionHelper;

  @Inject
  private static RequestDAO s_requestDAO = null;

  @Inject
  private static HostRoleCommandDAO s_hostRoleCommandDAO = null;

  /**
   * Used to generated the correct tasks and stages during an upgrade.
   */
  @Inject
  private static UpgradeHelper s_upgradeHelper;

  @Inject
  private static Configuration s_configuration;

  /**
   * Used to create instances of {@link UpgradeContext} with injected
   * dependencies.
   */
  @Inject
  private static UpgradeContextFactory s_upgradeContextFactory;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private RequestDAO requestDAO;

  /**
   * Used for creating keytab regeneration stage inside of an upgrade request.
   */
  @Inject
  private static Provider<KerberosHelper> s_kerberosHelper;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_CLUSTER_NAME);
    PROPERTY_IDS.add(UPGRADE_REPO_VERSION_ID);
    PROPERTY_IDS.add(UPGRADE_TYPE);
    PROPERTY_IDS.add(UPGRADE_PACK);
    PROPERTY_IDS.add(UPGRADE_ID);
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_ASSOCIATED_VERSION);
    PROPERTY_IDS.add(UPGRADE_VERSIONS);
    PROPERTY_IDS.add(UPGRADE_DIRECTION);
    PROPERTY_IDS.add(UPGRADE_DOWNGRADE_ALLOWED);
    PROPERTY_IDS.add(UPGRADE_SUSPENDED);
    PROPERTY_IDS.add(UPGRADE_SKIP_FAILURES);
    PROPERTY_IDS.add(UPGRADE_SKIP_SC_FAILURES);
    PROPERTY_IDS.add(UPGRADE_SKIP_MANUAL_VERIFICATION);
    PROPERTY_IDS.add(UPGRADE_SKIP_PREREQUISITE_CHECKS);
    PROPERTY_IDS.add(UPGRADE_FAIL_ON_CHECK_WARNINGS);
    PROPERTY_IDS.add(UPGRADE_HOST_ORDERED_HOSTS);
    PROPERTY_IDS.add(UPGRADE_REVERT_UPGRADE_ID);

    PROPERTY_IDS.add(REQUEST_CONTEXT_ID);
    PROPERTY_IDS.add(REQUEST_CREATE_TIME_ID);
    PROPERTY_IDS.add(REQUEST_END_TIME_ID);
    PROPERTY_IDS.add(REQUEST_EXCLUSIVE_ID);
    PROPERTY_IDS.add(REQUEST_PROGRESS_PERCENT_ID);
    PROPERTY_IDS.add(REQUEST_START_TIME_ID);
    PROPERTY_IDS.add(REQUEST_STATUS_PROPERTY_ID);
    PROPERTY_IDS.add(REQUEST_TYPE_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_CLUSTER_NAME);
  }

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeResourceProvider.class);

  /**
   * Constructor.
   *
   * @param controller
   *          the controller
   */
  @Inject
  public UpgradeResourceProvider(@Assisted AmbariManagementController controller) {
    super(Resource.Type.Upgrade, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    Set<Map<String, Object>> requestMaps = request.getProperties();

    if (requestMaps.size() > 1) {
      throw new SystemException("Can only initiate one upgrade per request.");
    }

    // !!! above check ensures only one
    final Map<String, Object> requestMap = requestMaps.iterator().next();
    final String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);
    final Cluster cluster;

    try {
      cluster = clusters.get().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(
          String.format("Cluster %s could not be loaded", clusterName));
    }

    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK))) {
      throw new AuthorizationException("The authenticated user does not have authorization to " +
          "manage upgrade and downgrade");
    }

    UpgradeEntity entity = createResources(new Command<UpgradeEntity>() {
      @Override
      public UpgradeEntity invoke() throws AmbariException, AuthorizationException {

        // create the context, validating the properties in the process
        final UpgradeContext upgradeContext = s_upgradeContextFactory.create(cluster, requestMap);

        try {
          return createUpgrade(upgradeContext);
        } catch (Exception e) {
          LOG.error("Error appears during upgrade task submitting", e);

          // Any error caused in the createUpgrade will initiate transaction
          // rollback
          // As we operate inside with cluster data, any cache which belongs to
          // cluster need to be flushed
          clusters.get().invalidate(cluster);
          throw e;
        }
      }
    });

    if (null == entity) {
      throw new SystemException("Could not load upgrade");
    }

    notifyCreate(Resource.Type.Upgrade, request);

    Resource res = new ResourceImpl(Resource.Type.Upgrade);
    res.setProperty(UPGRADE_REQUEST_ID, entity.getRequestId());
    return new RequestStatusImpl(null, Collections.singleton(res));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException(
            "The cluster name is required when querying for upgrades");
      }

      Cluster cluster;
      try {
        cluster = clusters.get().getCluster(clusterName);
      } catch (AmbariException e) {
        throw new NoSuchResourceException(
            String.format("Cluster %s could not be loaded", clusterName));
      }

      List<UpgradeEntity> upgrades = new ArrayList<>();

      String upgradeIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);
      if (null != upgradeIdStr) {
        UpgradeEntity upgrade = s_upgradeDAO.findUpgradeByRequestId(Long.valueOf(upgradeIdStr));

        if (null != upgrade) {
          upgrades.add(upgrade);
        }
      } else {
        upgrades = s_upgradeDAO.findUpgrades(cluster.getClusterId());
      }

      for (UpgradeEntity entity : upgrades) {
        Resource r = toResource(entity, clusterName, requestPropertyIds);
        results.add(r);

        RequestEntity rentity = s_requestDAO.findByPK(entity.getRequestId());

        setResourceProperty(r, REQUEST_CONTEXT_ID, rentity.getRequestContext(), requestPropertyIds);
        setResourceProperty(r, REQUEST_TYPE_ID, rentity.getRequestType(), requestPropertyIds);
        setResourceProperty(r, REQUEST_CREATE_TIME_ID, rentity.getCreateTime(), requestPropertyIds);
        setResourceProperty(r, REQUEST_START_TIME_ID, rentity.getStartTime(), requestPropertyIds);
        setResourceProperty(r, REQUEST_END_TIME_ID, rentity.getEndTime(), requestPropertyIds);
        setResourceProperty(r, REQUEST_EXCLUSIVE_ID, rentity.isExclusive(), requestPropertyIds);

        Map<Long, HostRoleCommandStatusSummaryDTO> summary = s_hostRoleCommandDAO.findAggregateCounts(
            entity.getRequestId());

        CalculatedStatus calc = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());

        if (calc.getStatus() == HostRoleStatus.ABORTED && entity.isSuspended()) {
          double percent = calculateAbortedProgress(summary);
          setResourceProperty(r, REQUEST_PROGRESS_PERCENT_ID, percent*100, requestPropertyIds);
        } else {
          setResourceProperty(r, REQUEST_PROGRESS_PERCENT_ID, calc.getPercent(), requestPropertyIds);
        }

        setResourceProperty(r, REQUEST_STATUS_PROPERTY_ID, calc.getStatus(), requestPropertyIds);
      }
    }

    return results;
  }

  /**
   * Unlike in CalculatedStatus, we can't use ABORTED here as a COMPLETED state.
   * Therefore, the values will be slightly off since in CalulatedStatus, ABORTED
   * contributes all of its progress to the overall progress, but here it
   * contributes none of it.
   *
   * Since this is specifically for ABORTED upgrades that are
   * also suspended, the percentages should come out pretty close after ABORTED move back
   * to PENDING.
   *
   * @return the percent complete, counting ABORTED as zero percent.
   */
  public static double calculateAbortedProgress(Map<Long, HostRoleCommandStatusSummaryDTO> summary) {
    // !!! use the raw states to determine percent completes
    Map<HostRoleStatus, Integer> countTotals = new HashMap<>();
    int totalTasks = 0;


    for (HostRoleCommandStatusSummaryDTO statusSummary : summary.values()) {
      totalTasks += statusSummary.getTaskTotal();
      for (Map.Entry<HostRoleStatus, Integer> entry : statusSummary.getCounts().entrySet()) {
        if (!countTotals.containsKey(entry.getKey())) {
          countTotals.put(entry.getKey(), Integer.valueOf(0));
        }
        countTotals.put(entry.getKey(), countTotals.get(entry.getKey()) + entry.getValue());
      }
    }

    double percent = 0d;

    for (HostRoleStatus status : HostRoleStatus.values()) {
      if (!countTotals.containsKey(status)) {
        countTotals.put(status, Integer.valueOf(0));
      }
      double countValue = countTotals.get(status);

      // !!! calculation lifted from CalculatedStatus
      switch (status) {
        case ABORTED:
          // !!! see javadoc
          break;
        case HOLDING:
        case HOLDING_FAILED:
        case HOLDING_TIMEDOUT:
        case IN_PROGRESS:
        case PENDING:  // shouldn't be any, we're supposed to be ABORTED
          percent += countValue * 0.35d;
          break;
        case QUEUED:
          percent += countValue * 0.09d;
          break;
        default:
          if (status.isCompletedState()) {
            percent += countValue / totalTasks;
          }
          break;
      }
    }

    return percent;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> requestMaps = request.getProperties();

    if (requestMaps.size() > 1) {
      throw new SystemException("Can only update one upgrade per request.");
    }

    // !!! above check ensures only one
    final Map<String, Object> propertyMap = requestMaps.iterator().next();

    final String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);
    final Cluster cluster;

    try {
      cluster = clusters.get().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(
          String.format("Cluster %s could not be loaded", clusterName));
    }

    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK))) {
      throw new AuthorizationException("The authenticated user does not have authorization to " +
          "manage upgrade and downgrade");
    }

    String requestIdProperty = (String) propertyMap.get(UPGRADE_REQUEST_ID);
    if (null == requestIdProperty) {
      throw new IllegalArgumentException(String.format("%s is required", UPGRADE_REQUEST_ID));
    }

    long requestId = Long.parseLong(requestIdProperty);
    UpgradeEntity upgradeEntity = s_upgradeDAO.findUpgradeByRequestId(requestId);
    if( null == upgradeEntity){
      String exceptionMessage = MessageFormat.format("The upgrade with request ID {0} was not found", requestIdProperty);
      throw new NoSuchParentResourceException(exceptionMessage);
    }

    // the properties which are allowed to be updated; the request must include
    // at least 1
    List<String> updatableProperties = Lists.newArrayList(UPGRADE_REQUEST_STATUS,
        UPGRADE_SKIP_FAILURES, UPGRADE_SKIP_SC_FAILURES);

    boolean isRequiredPropertyInRequest = CollectionUtils.containsAny(updatableProperties,
        propertyMap.keySet());

    if (!isRequiredPropertyInRequest) {
      String exceptionMessage = MessageFormat.format(
          "At least one of the following properties is required in the request: {0}",
          StringUtils.join(updatableProperties, ", "));
      throw new IllegalArgumentException(exceptionMessage);
    }

    String requestStatus = (String) propertyMap.get(UPGRADE_REQUEST_STATUS);
    String skipFailuresRequestProperty = (String) propertyMap.get(UPGRADE_SKIP_FAILURES);
    String skipServiceCheckFailuresRequestProperty = (String) propertyMap.get(UPGRADE_SKIP_SC_FAILURES);

    if (null != requestStatus) {
      HostRoleStatus status = HostRoleStatus.valueOf(requestStatus);

      // When aborting an upgrade, the suspend flag must be present to indicate
      // if the upgrade is merely being paused (suspended=true) or aborted to initiate a downgrade (suspended=false).
      boolean suspended = false;
      if (status == HostRoleStatus.ABORTED && !propertyMap.containsKey(UPGRADE_SUSPENDED)){
        throw new IllegalArgumentException(String.format(
            "When changing the state of an upgrade to %s, the %s property is required to be either true or false.",
            status, UPGRADE_SUSPENDED ));
      } else if (status == HostRoleStatus.ABORTED) {
        suspended = Boolean.valueOf((String) propertyMap.get(UPGRADE_SUSPENDED));
      }

      try {
        setUpgradeRequestStatus(cluster, requestId, status, suspended, propertyMap);
      } catch (AmbariException ambariException) {
        throw new SystemException(ambariException.getMessage(), ambariException);
      }
    }

    // if either of the skip failure settings are in the request, then we need
    // to iterate over the entire series of tasks anyway, so do them both at the
    // same time
    if (StringUtils.isNotEmpty(skipFailuresRequestProperty)
        || StringUtils.isNotEmpty(skipServiceCheckFailuresRequestProperty)) {
      // grab the current settings for both
      boolean skipFailures = upgradeEntity.isComponentFailureAutoSkipped();
      boolean skipServiceCheckFailures = upgradeEntity.isServiceCheckFailureAutoSkipped();

      if (null != skipFailuresRequestProperty) {
        skipFailures = Boolean.parseBoolean(skipFailuresRequestProperty);
      }

      if (null != skipServiceCheckFailuresRequestProperty) {
        skipServiceCheckFailures = Boolean.parseBoolean(skipServiceCheckFailuresRequestProperty);
      }

      s_hostRoleCommandDAO.updateAutomaticSkipOnFailure(requestId, skipFailures, skipServiceCheckFailures);

      upgradeEntity.setAutoSkipComponentFailures(skipFailures);
      upgradeEntity.setAutoSkipServiceCheckFailures(skipServiceCheckFailures);
      s_upgradeDAO.merge(upgradeEntity);

    }

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete Upgrades");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeEntity entity, String clusterName, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.Upgrade);

    setResourceProperty(resource, UPGRADE_ID, entity.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, UPGRADE_TYPE, entity.getUpgradeType(), requestedIds);
    setResourceProperty(resource, UPGRADE_PACK, entity.getUpgradePackage(), requestedIds);
    setResourceProperty(resource, UPGRADE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_DIRECTION, entity.getDirection(), requestedIds);
    setResourceProperty(resource, UPGRADE_SUSPENDED, entity.isSuspended(), requestedIds);
    setResourceProperty(resource, UPGRADE_DOWNGRADE_ALLOWED, entity.isDowngradeAllowed(), requestedIds);
    setResourceProperty(resource, UPGRADE_SKIP_FAILURES, entity.isComponentFailureAutoSkipped(), requestedIds);
    setResourceProperty(resource, UPGRADE_SKIP_SC_FAILURES, entity.isServiceCheckFailureAutoSkipped(), requestedIds);

    // set the assocaited to/from version (to/from is dictated by direction)
    RepositoryVersionEntity repositoryVersion = entity.getRepositoryVersion();
    setResourceProperty(resource, UPGRADE_ASSOCIATED_VERSION, repositoryVersion.getVersion(), requestedIds);

    // now set the target verison for all services in the upgrade
    Map<String, RepositoryVersions> repositoryVersions = new HashMap<>();
    for( UpgradeHistoryEntity history : entity.getHistory() ){
      RepositoryVersions serviceVersions = repositoryVersions.get(history.getServiceName());
      if (null != serviceVersions) {
        continue;
      }

      serviceVersions = new RepositoryVersions(history.getFromReposistoryVersion(),
          history.getTargetRepositoryVersion());

      repositoryVersions.put(history.getServiceName(), serviceVersions);
    }

    setResourceProperty(resource, UPGRADE_VERSIONS, repositoryVersions, requestedIds);

    return resource;
  }

  /**
   * Inject variables into the
   * {@link org.apache.ambari.server.orm.entities.UpgradeItemEntity}, whose
   * tasks may use strings like {{configType/propertyName}} that need to be
   * retrieved from the properties.
   *
   * @param configHelper
   *          Configuration Helper
   * @param cluster
   *          Cluster
   * @param upgradeItem
   *          the item whose tasks will be injected.
   */
  private void injectVariables(ConfigHelper configHelper, Cluster cluster,
      UpgradeItemEntity upgradeItem) {
    final String regexp = "(\\{\\{.*?\\}\\})";

    String task = upgradeItem.getTasks();
    if (task != null && !task.isEmpty()) {
      Matcher m = Pattern.compile(regexp).matcher(task);
      while (m.find()) {
        String origVar = m.group(1);
        String configValue = configHelper.getPlaceholderValueFromDesiredConfigurations(cluster,
            origVar);

        if (null != configValue) {
          task = task.replace(origVar, configValue);
        } else {
          LOG.error("Unable to retrieve value for {}", origVar);
        }

      }
      upgradeItem.setTasks(task);
    }
  }

  /**
   * Creates the upgrade. All Request/Stage/Task and Upgrade entities will exist
   * in the database when this method completes.
   * <p/>
   * This method itself must not be wrapped in a transaction since it can
   * potentially make 1000's of database calls while building the entities
   * before persisting them. This would create a long-lived transaction which
   * could lead to database deadlock issues. Instead, only the creation of the
   * actual entities is wrapped in a {@link Transactional} block.
   *
   * @param upgradeContext
   * @return
   * @throws AmbariException
   * @throws AuthorizationException
   */
  protected UpgradeEntity createUpgrade(UpgradeContext upgradeContext)
      throws AmbariException, AuthorizationException {

    UpgradePack pack = upgradeContext.getUpgradePack();
    Cluster cluster = upgradeContext.getCluster();
    Direction direction = upgradeContext.getDirection();

    ConfigHelper configHelper = getManagementController().getConfigHelper();

    List<UpgradeGroupHolder> groups = s_upgradeHelper.createSequence(pack, upgradeContext);

    if (groups.isEmpty()) {
      throw new AmbariException("There are no groupings available");
    }

    // Non Rolling Upgrades require a group with name "UPDATE_DESIRED_REPOSITORY_ID".
    // This is needed as a marker to indicate which version to use when an upgrade is paused.
    if (pack.getType() == UpgradeType.NON_ROLLING) {
      boolean foundUpdateDesiredRepositoryIdGrouping = false;
      for (UpgradeGroupHolder group : groups) {
        if (group.groupClass == UpdateStackGrouping.class) {
          foundUpdateDesiredRepositoryIdGrouping = true;
          break;
        }
      }

      if (!foundUpdateDesiredRepositoryIdGrouping) {
        throw new AmbariException(String.format(
            "Express upgrade packs are required to have a group of type %s. The upgrade pack %s is missing this grouping.",
            "update-stack", pack.getName()));
      }
    }

    List<UpgradeGroupEntity> groupEntities = new ArrayList<>();
    RequestStageContainer req = createRequest(upgradeContext);

    UpgradeEntity upgrade = new UpgradeEntity();
    upgrade.setRepositoryVersion(upgradeContext.getRepositoryVersion());
    upgrade.setClusterId(cluster.getClusterId());
    upgrade.setDirection(direction);
    upgrade.setUpgradePackage(pack.getName());
    upgrade.setUpgradePackStackId(pack.getOwnerStackId());
    upgrade.setUpgradeType(pack.getType());
    upgrade.setAutoSkipComponentFailures(upgradeContext.isComponentFailureAutoSkipped());
    upgrade.setAutoSkipServiceCheckFailures(upgradeContext.isServiceCheckFailureAutoSkipped());
    upgrade.setDowngradeAllowed(upgradeContext.isDowngradeAllowed());
    upgrade.setOrchestration(upgradeContext.getOrchestrationType());

    // create to/from history for this upgrade - this should be done before any
    // possible changes to the desired version for components
    addComponentHistoryToUpgrade(cluster, upgrade, upgradeContext);

    /*
    During a Rolling Upgrade, change the desired Stack Id if jumping across
    major stack versions (e.g., HDP 2.2 -> 2.3), and then set config changes
    so they are applied on the newer stack.

    During a {@link UpgradeType.NON_ROLLING} upgrade, the stack is applied during the middle of the upgrade (after
    stopping all services), and the configs are applied immediately before starting the services.
    The Upgrade Pack is responsible for calling {@link org.apache.ambari.server.serveraction.upgrades.UpdateDesiredRepositoryAction}
    at the appropriate moment during the orchestration.
    */
    if (pack.getType() == UpgradeType.ROLLING || pack.getType() == UpgradeType.HOST_ORDERED) {
      if (direction == Direction.UPGRADE) {
        StackEntity targetStack = upgradeContext.getRepositoryVersion().getStack();
        cluster.setDesiredStackVersion(
            new StackId(targetStack.getStackName(), targetStack.getStackVersion()));
      }
      s_upgradeHelper.updateDesiredRepositoriesAndConfigs(upgradeContext);
      s_upgradeHelper.publishDesiredRepositoriesUpdates(upgradeContext);
      if (direction == Direction.DOWNGRADE) {
        StackId targetStack = upgradeContext.getCluster().getCurrentStackVersion();
        cluster.setDesiredStackVersion(targetStack);
      }
    }

    // resolve or build a proper config upgrade pack - always start out with the config pack
    // for the current stack and merge into that
    //
    // HDP 2.2 to 2.3 should start with the config-upgrade.xml from HDP 2.2
    // HDP 2.2 to 2.4 should start with HDP 2.2 and merge in HDP 2.3's config-upgrade.xml
    ConfigUpgradePack configUpgradePack = ConfigurationPackBuilder.build(upgradeContext);

    // !!! effectiveStack in an EU must start as the source stack, since we're generating
    // commands for the "old" version
    StackId effectiveStack = upgradeContext.getTargetStack();
    if (upgradeContext.getType() == UpgradeType.NON_ROLLING) {
      effectiveStack = upgradeContext.getSourceStack();
    }

    // create the upgrade and request
    for (UpgradeGroupHolder group : groups) {

      if (upgradeContext.getType() == UpgradeType.NON_ROLLING
          && UpdateStackGrouping.class.equals(group.groupClass)) {
        effectiveStack = upgradeContext.getTargetStack();
      }

      List<UpgradeItemEntity> itemEntities = new ArrayList<>();

      for (StageWrapper wrapper : group.items) {

        switch(wrapper.getType()) {
          case SERVER_SIDE_ACTION:{
            // !!! each stage is guaranteed to be of one type. but because there
            // is a bug that prevents one stage with multiple tasks assigned for
            // the same host, break them out into individual stages.
            for (TaskWrapper taskWrapper : wrapper.getTasks()) {
              for (Task task : taskWrapper.getTasks()) {
                if (upgradeContext.isManualVerificationAutoSkipped()
                    && task.getType() == Task.Type.MANUAL) {
                  continue;
                }

                UpgradeItemEntity itemEntity = new UpgradeItemEntity();

                itemEntity.setText(wrapper.getText());
                itemEntity.setTasks(wrapper.getTasksJson());
                itemEntity.setHosts(wrapper.getHostsJson());

                injectVariables(configHelper, cluster, itemEntity);
                if (makeServerSideStage(group, upgradeContext, effectiveStack, req,
                    itemEntity, (ServerSideActionTask) task, configUpgradePack)) {
                  itemEntities.add(itemEntity);
                }
              }
            }
            break;
          }
          case REGENERATE_KEYTABS: {
            try {
              // remmeber how many stages we had before adding keytab stuff
              int stageCount = req.getStages().size();

              // build a map of request properties which say to
              //   - only regenerate missing tabs
              //   - allow all tasks which fail to be retried (so the upgrade doesn't abort)
              Map<String, String> requestProperties = new HashMap<>();
              requestProperties.put(SupportedCustomOperation.REGENERATE_KEYTABS.name().toLowerCase(), "missing");
              requestProperties.put(KerberosHelper.ALLOW_RETRY, Boolean.TRUE.toString().toLowerCase());
              requestProperties.put(KerberosHelper.DIRECTIVE_CONFIG_UPDATE_POLICY, UpdateConfigurationPolicy.NEW_AND_IDENTITIES.name());

              // add stages to the upgrade which will regenerate missing keytabs only
              req = s_kerberosHelper.get().executeCustomOperations(cluster, requestProperties, req, null);

              // for every stage which was added for kerberos stuff create an
              // associated upgrade item for it
              List<Stage> stages = req.getStages();
              int newStageCount = stages.size();
              for (int i = stageCount; i < newStageCount; i++) {
                Stage stage = stages.get(i);
                stage.setSkippable(group.skippable);
                stage.setAutoSkipFailureSupported(group.supportsAutoSkipOnFailure);

                UpgradeItemEntity itemEntity = new UpgradeItemEntity();
                itemEntity.setStageId(stage.getStageId());
                itemEntity.setText(stage.getRequestContext());
                itemEntity.setTasks(wrapper.getTasksJson());
                itemEntity.setHosts(wrapper.getHostsJson());
                itemEntities.add(itemEntity);
                injectVariables(configHelper, cluster, itemEntity);
              }
            } catch (KerberosOperationException kerberosOperationException) {
              throw new AmbariException("Unable to build keytab regeneration stage",
                  kerberosOperationException);
            }

            break;
          }
          default: {
            UpgradeItemEntity itemEntity = new UpgradeItemEntity();
            itemEntity.setText(wrapper.getText());
            itemEntity.setTasks(wrapper.getTasksJson());
            itemEntity.setHosts(wrapper.getHostsJson());
            itemEntities.add(itemEntity);

            injectVariables(configHelper, cluster, itemEntity);

            // upgrade items match a stage
            createStage(group, upgradeContext, effectiveStack, req, itemEntity, wrapper);

            break;
          }
        }
      }

      if(!itemEntities.isEmpty()) {
        UpgradeGroupEntity groupEntity = new UpgradeGroupEntity();
        groupEntity.setName(group.name);
        groupEntity.setTitle(group.title);
        groupEntity.setItems(itemEntities);
        groupEntities.add(groupEntity);
      }
    }

    // set all of the groups we just created
    upgrade.setUpgradeGroups(groupEntities);

    req.getRequestStatusResponse();
    return createUpgradeInsideTransaction(cluster, req, upgrade, upgradeContext);
  }

  /**
   * Creates the Request/Stage/Task entities and the Upgrade entities inside of
   * a single transaction. We break this out since the work to get us to this
   * point could take a very long time and involve many queries to the database
   * as the commands are being built.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @param request
   *          the request to persist with all stages and tasks created in memory
   *          (not {@code null}).
   * @param upgradeEntity
   *          the upgrade to create and associate with the newly created request
   *          (not {@code null}).
   * @param upgradeContext
   *          the upgrade context associated with the upgrade being created.
   * @return the persisted {@link UpgradeEntity} encapsulating all
   *         {@link UpgradeGroupEntity} and {@link UpgradeItemEntity}.
   * @throws AmbariException
   */
  @Transactional
  UpgradeEntity createUpgradeInsideTransaction(Cluster cluster,
      RequestStageContainer request,
      UpgradeEntity upgradeEntity, UpgradeContext upgradeContext) throws AmbariException {

    // if this is a patch reversion, then we must unset the revertable flag of
    // the upgrade being reverted
    if (upgradeContext.isPatchRevert()) {
      UpgradeEntity upgradeBeingReverted = s_upgradeDAO.findUpgrade(
          upgradeContext.getPatchRevertUpgradeId());

      upgradeBeingReverted.setRevertAllowed(false);
      upgradeBeingReverted = s_upgradeDAO.merge(upgradeBeingReverted);
    }

    request.persist();
    RequestEntity requestEntity = s_requestDAO.findByPK(request.getId());

    upgradeEntity.setRequestEntity(requestEntity);
    s_upgradeDAO.create(upgradeEntity);

    STOMPUpdatePublisher.publish(UpgradeUpdateEvent
        .formFullEvent(s_hostRoleCommandDAO, s_requestDAO, upgradeEntity, UpdateEventType.CREATE));
    cluster.setUpgradeEntity(upgradeEntity);

    return upgradeEntity;
  }

  private RequestStageContainer createRequest(UpgradeContext upgradeContext) throws AmbariException {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
        actionManager.getNextRequestId(), null, s_requestFactory.get(), actionManager);

    Direction direction = upgradeContext.getDirection();
    RepositoryVersionEntity repositoryVersion = upgradeContext.getRepositoryVersion();

    requestStages.setRequestContext(String.format("%s %s %s", direction.getVerb(true),
        direction.getPreposition(), repositoryVersion.getVersion()));

    return requestStages;
  }

  private void createStage(UpgradeGroupHolder group, UpgradeContext context,
      StackId stackId,
      RequestStageContainer request, UpgradeItemEntity entity, StageWrapper wrapper)
          throws AmbariException {

    boolean skippable = group.skippable;
    boolean supportsAutoSkipOnFailure = group.supportsAutoSkipOnFailure;
    boolean allowRetry = group.allowRetry;

    switch (wrapper.getType()) {
      case CONFIGURE:
      case START:
      case STOP:
      case RESTART:
        makeCommandStage(context, request, stackId, entity, wrapper, skippable,
            supportsAutoSkipOnFailure, allowRetry);
        break;
      case UPGRADE_TASKS:
        makeActionStage(context, request, stackId, entity, wrapper, skippable,
            supportsAutoSkipOnFailure, allowRetry);
        break;
      case SERVICE_CHECK:
        makeServiceCheckStage(context, request, stackId, entity, wrapper,
            skippable, supportsAutoSkipOnFailure, allowRetry);
        break;
      default:
        break;
    }
  }

  /**
   * Modify the commandParams by applying additional parameters from the stage.
   * @param wrapper Stage Wrapper that may contain additional parameters.
   * @param commandParams Parameters to modify.
   */
  private void applyAdditionalParameters(StageWrapper wrapper, Map<String, String> commandParams) {
    if (wrapper.getParams() != null) {
      for (Map.Entry<String, String> pair : wrapper.getParams().entrySet()) {
        if (!commandParams.containsKey(pair.getKey())) {
          commandParams.put(pair.getKey(), pair.getValue());
        }
      }
    }
  }

  /**
   * Creates an action stage using the {@link #EXECUTE_TASK_ROLE} custom action
   * to execute some Python command.
   *
   * @param context
   *          the upgrade context.
   * @param request
   *          the request object to add the stage to.
   * @param effectiveRepositoryVersion
   *          the stack/version to use when generating content for the command.
   *          On some upgrade types, this may change during the course of the
   *          upgrade orchestration. An express upgrade changes this after
   *          stopping all services.
   * @param entity
   *          the upgrade entity to set the stage information on
   * @param wrapper
   *          the stage wrapper containing information to generate the stage.
   * @param skippable
   *          {@code true} to mark the stage as being skippable if a failure
   *          occurs.
   * @param supportsAutoSkipOnFailure
   *          {@code true} to automatically skip on a failure.
   * @param allowRetry
   *          {@code true} to be able to retry the failed stage.
   * @throws AmbariException
   */
  private void makeActionStage(UpgradeContext context, RequestStageContainer request,
      StackId stackId, UpgradeItemEntity entity,
      StageWrapper wrapper, boolean skippable, boolean supportsAutoSkipOnFailure,
      boolean allowRetry) throws AmbariException {

    if (0 == wrapper.getHosts().size()) {
      throw new AmbariException(
          String.format("Cannot create action for '%s' with no hosts", wrapper.getText()));
    }

    Cluster cluster = context.getCluster();

    LOG.debug("Analyzing upgrade item {} with tasks: {}.", entity.getText(), entity.getTasks());

    // if the service/component are specified, then make sure to grab them off
    // of the wrapper so they can be stored on the command for use later
    String serviceName = null;
    String componentName = null;
    if (wrapper.getTasks() != null && wrapper.getTasks().size() > 0
        && wrapper.getTasks().get(0).getService() != null) {
      TaskWrapper taskWrapper = wrapper.getTasks().get(0);
      serviceName = taskWrapper.getService();
      componentName = taskWrapper.getComponent();
    }

    Map<String, String> params = getNewParameterMap(request, context);
    params.put(UpgradeContext.COMMAND_PARAM_TASKS, entity.getTasks());

    // !!! when not scoped to a component (generic execution task)
    if (context.isScoped(UpgradeScope.COMPLETE) && null == componentName) {
      if (context.getDirection().isUpgrade()) {
        params.put(KeyNames.VERSION, context.getRepositoryVersion().getVersion());
      } else {
        // !!! in a full downgrade, the target version should be any of the history versions
        UpgradeEntity lastUpgrade = s_upgradeDAO.findLastUpgradeForCluster(
            cluster.getClusterId(), Direction.UPGRADE);

        @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES,
            comment = "Shouldn't be getting the overall downgrade-to version.")
        UpgradeHistoryEntity lastHistory = lastUpgrade.getHistory().iterator().next();
        params.put(KeyNames.VERSION, lastHistory.getFromReposistoryVersion().getVersion());
      }
    }

    // Apply additional parameters to the command that come from the stage.
    applyAdditionalParameters(wrapper, params);


    // add each host to this stage
    RequestResourceFilter filter = new RequestResourceFilter(serviceName, componentName,
        new ArrayList<>(wrapper.getHosts()));

    ActionExecutionContext actionContext = buildActionExecutionContext(cluster, context,
        EXECUTE_TASK_ROLE, stackId, Collections.singletonList(filter), params,
        allowRetry, wrapper.getMaxTimeout(s_configuration));

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, stackId, null);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), entity.getText(), jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);
    stage.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    s_actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null);

    // need to set meaningful text on the command
    for (Map<String, HostRoleCommand> map : stage.getHostRoleCommands().values()) {
      for (HostRoleCommand hrc : map.values()) {
        hrc.setCommandDetail(entity.getText());
      }
    }

    request.addStages(Collections.singletonList(stage));
  }

  /**
   * Used to create a stage for restart, start, or stop.
   * @param context Upgrade Context
   * @param request Container for stage
   * @param entity Upgrade Item
   * @param wrapper Stage
   * @param skippable Whether the item can be skipped
   * @param allowRetry Whether the item is allowed to be retried
   * @throws AmbariException
   */
  private void makeCommandStage(UpgradeContext context, RequestStageContainer request,
      StackId stackId, UpgradeItemEntity entity,
      StageWrapper wrapper, boolean skippable, boolean supportsAutoSkipOnFailure,
      boolean allowRetry) throws AmbariException {

    Cluster cluster = context.getCluster();

    List<RequestResourceFilter> filters = new ArrayList<>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      String serviceName = tw.getService();
      String componentName = tw.getComponent();

      // add each host to this stage
      filters.add(
          new RequestResourceFilter(serviceName, componentName,
          new ArrayList<>(tw.getHosts())));
    }

    String function = null;
    switch (wrapper.getType()) {
      case CONFIGURE:
      case START:
      case STOP:
      case RESTART:
        function = wrapper.getType().name();
        break;
      default:
        function = "UNKNOWN";
        break;
    }

    Map<String, String> commandParams = getNewParameterMap(request, context);

    // Apply additional parameters to the command that come from the stage.
    applyAdditionalParameters(wrapper, commandParams);

    ActionExecutionContext actionContext = buildActionExecutionContext(cluster, context, function,
        stackId, filters, commandParams, allowRetry,
        wrapper.getMaxTimeout(s_configuration));

    // commands created here might be for future components which have not been
    // added to the cluster yet
    actionContext.setIsFutureCommand(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, stackId, null);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), entity.getText(), jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);
    stage.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("command", function);

    // !!! it is unclear the implications of this on rolling or express upgrade.  To turn
    // this off, set "allow-retry" to false in the Upgrade Pack group
    if (allowRetry && context.getType() == UpgradeType.HOST_ORDERED) {
      requestParams.put(KeyNames.COMMAND_RETRY_ENABLED, Boolean.TRUE.toString().toLowerCase());
    }

    s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams, jsons);

    request.addStages(Collections.singletonList(stage));
  }

  private void makeServiceCheckStage(UpgradeContext context, RequestStageContainer request,
      StackId stackId, UpgradeItemEntity entity,
      StageWrapper wrapper, boolean skippable, boolean supportsAutoSkipOnFailure,
      boolean allowRetry) throws AmbariException {

    List<RequestResourceFilter> filters = new ArrayList<>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      List<String> hosts = tw.getHosts().stream().collect(Collectors.toList());
      filters.add(new RequestResourceFilter(tw.getService(), "", hosts));
    }

    Cluster cluster = context.getCluster();

    Map<String, String> commandParams = getNewParameterMap(request, context);

    // Apply additional parameters to the command that come from the stage.
    applyAdditionalParameters(wrapper, commandParams);

    ActionExecutionContext actionContext = buildActionExecutionContext(cluster, context,
        "SERVICE_CHECK", stackId, filters, commandParams, allowRetry,
        wrapper.getMaxTimeout(s_configuration));
    actionContext.setAutoSkipFailures(context.isServiceCheckFailureAutoSkipped());

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, stackId, null);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), entity.getText(), jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);
    stage.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = getNewParameterMap(request, context);
    s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams, jsons);

    request.addStages(Collections.singletonList(stage));
  }

  /**
   * Creates a stage consisting of server side actions. If the server action is
   * a {@link ServerAction} instance, then it will schedule it after verifying
   * that the class can be loaded. If the action is an {@link UpgradeAction},
   * then it will be scheduled as a {@link PluginUpgradeServerAction}.
   *
   * @param group
   *          the upgrade group
   * @param context
   *          the upgrade context containing all of the information about the
   *          upgrade.
   * @param stackId
   *          the ID of the stack that this stage is being created for. In some
   *          upgrades, the stack ID changes part way through, such as during an
   *          express upgrade.
   * @param request
   *          the request being constructed, but not yet persisted.
   * @param entity
   *          the upgrade item entity associated with the stage.
   * @param task
   *          the task from the upgrade pack XML which this command is being
   *          built for.
   * @param configUpgradePack
   *          the configuration changes for the upgrade.
   * @return {@code true} if the stage creation was successful, or {@code false}
   *         if no processing was performed.
   * @throws AmbariException
   */
  private boolean makeServerSideStage(UpgradeGroupHolder group, UpgradeContext context,
      StackId stackId, RequestStageContainer request,
      UpgradeItemEntity entity, ServerSideActionTask task, ConfigUpgradePack configUpgradePack)
      throws AmbariException {

    Cluster cluster = context.getCluster();
    UpgradePack upgradePack = context.getUpgradePack();

    Map<String, String> commandParams = getNewParameterMap(request, context);
    commandParams.put(UpgradeContext.COMMAND_PARAM_UPGRADE_PACK, upgradePack.getName());

    // Notice that this does not apply any params because the input does not specify a stage.
    // All of the other actions do use additional params.

    String itemDetail = entity.getText();
    String stageText = StringUtils.abbreviate(entity.getText(), 255);

    boolean process = true;

    switch (task.getType()) {
      case SERVER_ACTION:
      case MANUAL: {
        ServerSideActionTask serverTask = task;

        if (null != serverTask.summary) {
          stageText = serverTask.summary;
        }

        if (task.getType() == Task.Type.MANUAL) {
          ManualTask mt = (ManualTask) task;

          if (StringUtils.isNotBlank(mt.structuredOut)) {
            commandParams.put(UpgradeContext.COMMAND_PARAM_STRUCT_OUT, mt.structuredOut);
          }
        }

        if (!serverTask.messages.isEmpty()) {
          JsonArray messageArray = new JsonArray();
          for (String message : serverTask.messages) {
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("message", message);
            messageArray.add(messageObj);
          }
          itemDetail = messageArray.toString();

          entity.setText(itemDetail);

          //To be used later on by the Stage...
          itemDetail = StringUtils.join(serverTask.messages, " ");
        }
        break;
      }
      case CONFIGURE: {
        ConfigureTask ct = (ConfigureTask) task;

        // !!! would prefer to do this in the sequence generator, but there's too many
        // places to miss
        if (context.getOrchestrationType().isRevertable() && !ct.supportsPatch) {
          process = false;
        }

        Map<String, String> configurationChanges =
                ct.getConfigurationChanges(cluster, configUpgradePack);

        // add all configuration changes to the command params
        commandParams.putAll(configurationChanges);

        // extract the config type to build the summary
        String configType = configurationChanges.get(ConfigureTask.PARAMETER_CONFIG_TYPE);
        if (null != configType) {
          itemDetail = String.format("Updating configuration %s", configType);
        } else {
          itemDetail = "Skipping Configuration Task "
              + StringUtils.defaultString(ct.id, "(missing id)");
        }

        entity.setText(itemDetail);

        String configureTaskSummary = ct.getSummary(configUpgradePack);
        if (null != configureTaskSummary) {
          stageText = configureTaskSummary;
        } else {
          stageText = itemDetail;
        }

        break;
      }
      case CREATE_AND_CONFIGURE: {
        CreateAndConfigureTask ct = (CreateAndConfigureTask) task;

        // !!! would prefer to do this in the sequence generator, but there's too many
        // places to miss
        if (context.getOrchestrationType().isRevertable() && !ct.supportsPatch) {
          process = false;
        }

        Map<String, String> configurationChanges =
                ct.getConfigurationChanges(cluster, configUpgradePack);

        // add all configuration changes to the command params
        commandParams.putAll(configurationChanges);

        // extract the config type to build the summary
        String configType = configurationChanges.get(CreateAndConfigureTask.PARAMETER_CONFIG_TYPE);
        if (null != configType) {
          itemDetail = String.format("Updating configuration %s", configType);
        } else {
          itemDetail = "Skipping Configuration Task "
              + StringUtils.defaultString(ct.id, "(missing id)");
        }

        entity.setText(itemDetail);

        String configureTaskSummary = ct.getSummary(configUpgradePack);
        if (null != configureTaskSummary) {
          stageText = configureTaskSummary;
        } else {
          stageText = itemDetail;
        }

        break;
      }
      case ADD_COMPONENT: {
        AddComponentTask addComponentTask = (AddComponentTask) task;
        String serializedTask = addComponentTask.toJson();
        commandParams.put(AddComponentTask.PARAMETER_SERIALIZED_ADD_COMPONENT_TASK, serializedTask);
        break;
      }
      default:
        break;
    }

    if (!process) {
      return false;
    }

    ActionExecutionContext actionContext = buildActionExecutionContext(cluster, context,
        Role.AMBARI_SERVER_ACTION.toString(), stackId, Collections.emptyList(),
        commandParams, group.allowRetry, Short.valueOf((short) -1));

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, context.getRepositoryVersion().getStackId(), null);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), stageText, jsons.getCommandParamsForStage(),
      jsons.getHostParamsForStage());

    stage.setSkippable(group.skippable);
    stage.setAutoSkipFailureSupported(group.supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> taskParameters = task.getParameters();
    commandParams.putAll(taskParameters);

    // attempt to get a classloader from the stack (for plugins), and if there
    // is not one, then default to Ambari's classloader
    StackInfo stackInfo = s_metaProvider.get().getStack(stackId);
    ClassLoader classLoader = stackInfo.getLibraryClassLoader();
    if (null == classLoader) {
      classLoader = ClasspathScannerUtils.class.getClassLoader();
    }

    // try to figure out what the class is so it can be scheduled correctly
    final String classToSchedule;
    String taskClass = task.getImplementationClass();
    try {
      Class<?> clazz = classLoader.loadClass(taskClass);
      if (UpgradeAction.class.isAssignableFrom(clazz)) {
        // upgrade actions are scheduled using our own server action, passing
        // the name of the upgrade action in via the command params
        classToSchedule = PluginUpgradeServerAction.class.getName();
        commandParams.put(ServerAction.WRAPPED_CLASS_NAME, taskClass);
      } else if (ServerAction.class.isAssignableFrom(clazz)) {
        classToSchedule = task.getImplementationClass();
      } else {
        throw new AmbariException("The class " + taskClass
            + " was not able to be scheduled during the upgrade because it is not compatible");
      }
    } catch (ClassNotFoundException cnfe) {
      LOG.error("Unable to load {} specified in the upgrade pack", taskClass, cnfe);
      throw new AmbariException("The class " + taskClass
          + " was not able to be scheduled during the upgrade because it was not found");
    }

    stage.addServerActionCommand(classToSchedule,
        getManagementController().getAuthName(), Role.AMBARI_SERVER_ACTION, RoleCommand.EXECUTE,
        cluster.getClusterName(),
        new ServiceComponentHostServerActionEvent(null, System.currentTimeMillis()), commandParams,
        itemDetail, null, s_configuration.getDefaultServerTaskTimeout(), group.allowRetry,
        context.isComponentFailureAutoSkipped());

    request.addStages(Collections.singletonList(stage));

    return true;
  }

  /**
   * Gets a map initialized with parameters required for upgrades to work. The
   * following properties are already set:
   * <ul>
   * <li>{@link UpgradeContext#COMMAND_PARAM_CLUSTER_NAME}
   * <li>{@link UpgradeContext#COMMAND_PARAM_DIRECTION}
   * <li>{@link UpgradeContext#COMMAND_PARAM_UPGRADE_TYPE}
   * <li>{@link KeyNames#REFRESH_CONFIG_TAGS_BEFORE_EXECUTION} - necessary in
   * order to have the commands contain the correct configurations. Otherwise,
   * they will contain the configurations that were available at the time the
   * command was created. For upgrades, this is problematic since the commands
   * are all created ahead of time, but the upgrade may change configs as part
   * of the upgrade pack.</li>
   * <li>{@link UpgradeContext#COMMAND_PARAM_REQUEST_ID}</li> the ID of the request.
   * <ul>
   *
   * @return the initialized parameter map.
   */
  private Map<String, String> getNewParameterMap(RequestStageContainer requestStageContainer,
      UpgradeContext context) {
    Map<String, String> parameters = context.getInitializedCommandParameters();
    parameters.put(UpgradeContext.COMMAND_PARAM_REQUEST_ID,
        String.valueOf(requestStageContainer.getId()));
    return parameters;
  }

  /**
   * Changes the status of the specified request for an upgrade. The valid
   * values are:
   * <ul>
   * <li>{@link HostRoleStatus#ABORTED}</li>
   * <li>{@link HostRoleStatus#PENDING}</li>
   * </ul>
   * This method will also adjust the cluster->upgrade association correctly
   * based on the new status being supplied.
   *
   * @param cluster
   *          the cluster
   * @param requestId
   *          the request to change the status for.
   * @param status
   *          the status to set on the associated request.
   * @param suspended
   *          if the value of the specified status is
   *          {@link HostRoleStatus#ABORTED}, then this boolean will control
   *          whether the upgrade is suspended (still associated with the
   *          cluster) or aborted (no longer associated with the cluster).
   * @param propertyMap
   *          the map of request properties (needed for things like abort reason
   *          if present)
   */
  @Transactional
  void setUpgradeRequestStatus(Cluster cluster, long requestId, HostRoleStatus status,
      boolean suspended, Map<String, Object> propertyMap) throws AmbariException {
    // these are the only two states we allow
    if (status != HostRoleStatus.ABORTED && status != HostRoleStatus.PENDING) {
      throw new IllegalArgumentException(String.format("Cannot set status %s, only %s is allowed",
          status, EnumSet.of(HostRoleStatus.ABORTED, HostRoleStatus.PENDING)));
    }

    String reason = (String) propertyMap.get(UPGRADE_ABORT_REASON);
    if (null == reason) {
      reason = String.format(DEFAULT_REASON_TEMPLATE, requestId);
    }

    // do not try to pull back the entire request here as they can be massive
    // and cause OOM problems; instead, use the count of statuses to determine
    // the state of the upgrade request
    Map<Long, HostRoleCommandStatusSummaryDTO> aggregateCounts = s_hostRoleCommandDAO.findAggregateCounts(requestId);
    CalculatedStatus calculatedStatus = CalculatedStatus.statusFromStageSummary(aggregateCounts,
        aggregateCounts.keySet());

    HostRoleStatus internalStatus = calculatedStatus.getStatus();

    if (HostRoleStatus.PENDING == status && !(internalStatus == HostRoleStatus.ABORTED || internalStatus == HostRoleStatus.IN_PROGRESS)) {
      throw new IllegalArgumentException(
          String.format("Can only set status to %s when the upgrade is %s (currently %s)", status,
              HostRoleStatus.ABORTED, internalStatus));
    }

    ActionManager actionManager = getManagementController().getActionManager();

    if (HostRoleStatus.ABORTED == status && !internalStatus.isCompletedState()) {
      // cancel the request
      actionManager.cancelRequest(requestId, reason);

      // either suspend the upgrade or abort it outright
      UpgradeEntity upgradeEntity = s_upgradeDAO.findUpgradeByRequestId(requestId);
      if (suspended) {
        // set the upgrade to suspended
        upgradeEntity.setSuspended(suspended);
        upgradeEntity = s_upgradeDAO.merge(upgradeEntity);
        STOMPUpdatePublisher.publish(UpgradeUpdateEvent.formUpdateEvent(hostRoleCommandDAO,requestDAO, upgradeEntity));
      } else {
        // otherwise remove the association with the cluster since it's being
        // full aborted
        cluster.setUpgradeEntity(null);
      }

    } else if (status == HostRoleStatus.PENDING) {
      List<Long> taskIds = new ArrayList<>();

      // pull back only ABORTED tasks in order to set them to PENDING - other
      // status (such as TIMEDOUT and FAILED) must remain since they are
      // considered to have been completed
      List<HostRoleCommandEntity> hrcEntities = s_hostRoleCommandDAO.findByRequestIdAndStatuses(
          requestId, Sets.newHashSet(HostRoleStatus.ABORTED));

      for (HostRoleCommandEntity hrcEntity : hrcEntities) {
        taskIds.add(hrcEntity.getTaskId());
      }

      actionManager.resubmitTasks(taskIds);

      UpgradeEntity lastUpgradeItemForCluster = s_upgradeDAO.findLastUpgradeOrDowngradeForCluster(cluster.getClusterId());
      lastUpgradeItemForCluster.setSuspended(false);
      lastUpgradeItemForCluster = s_upgradeDAO.merge(lastUpgradeItemForCluster);
      STOMPUpdatePublisher.publish(UpgradeUpdateEvent.formUpdateEvent(hostRoleCommandDAO, requestDAO, lastUpgradeItemForCluster));
    }
  }

  /**
   * Creates the {@link UpgradeHistoryEntity} instances for this upgrade for
   * every component participating.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @param upgrade
   *          the upgrade to add the entities to (not {@code null}).
   * @param upgradeContext
   *          the upgrade context for this upgrade (not {@code null}).
   */
  private void addComponentHistoryToUpgrade(Cluster cluster, UpgradeEntity upgrade,
      UpgradeContext upgradeContext) throws AmbariException {
    Set<String> services = upgradeContext.getSupportedServices();
    for (String serviceName : services) {
      Service service = cluster.getService(serviceName);
      Map<String, ServiceComponent> componentMap = service.getServiceComponents();
      for (ServiceComponent component : componentMap.values()) {
        UpgradeHistoryEntity history = new UpgradeHistoryEntity();
        history.setUpgrade(upgrade);
        history.setServiceName(serviceName);
        history.setComponentName(component.getName());

        // depending on whether this is an upgrade or a downgrade, the history
        // will be different
        if (upgradeContext.getDirection() == Direction.UPGRADE) {
          history.setFromRepositoryVersion(component.getDesiredRepositoryVersion());
          history.setTargetRepositoryVersion(upgradeContext.getRepositoryVersion());
        } else {
          // the target version on a downgrade is the original version that the
          // service was on in the upgrade
          RepositoryVersionEntity targetRepositoryVersion = upgradeContext.getTargetRepositoryVersion(
              serviceName);

          history.setFromRepositoryVersion(upgradeContext.getRepositoryVersion());
          history.setTargetRepositoryVersion(targetRepositoryVersion);
        }

        // add the history
        upgrade.addHistory(history);
      }
    }
  }

  /**
   * Constructs an {@link ActionExecutionContext}, setting common parameters for
   * all types of commands.
   *
   * @param cluster
   *          the cluster
   * @param context
   *          the upgrade context
   * @param role
   *          the role for the command
   * @param repositoryVersion
   *          the repository version which will be used mostly for the stack ID
   *          when building the command and resolving stack-based properties
   *          (like hooks folders)
   * @param resourceFilters
   *          the filters for where the request will run
   * @param commandParams
   *          the command parameter map
   * @param allowRetry
   *          {@code true} to allow retry of the command
   * @param timeout
   *          the timeout for the command.
   * @return the {@link ActionExecutionContext}.
   */
  private ActionExecutionContext buildActionExecutionContext(Cluster cluster,
      UpgradeContext context, String role, StackId stackId,
      List<RequestResourceFilter> resourceFilters, Map<String, String> commandParams,
      boolean allowRetry, short timeout) {

    ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
        role, resourceFilters, commandParams);

    actionContext.setStackId(stackId);
    actionContext.setTimeout(timeout);
    actionContext.setRetryAllowed(allowRetry);
    actionContext.setAutoSkipFailures(context.isComponentFailureAutoSkipped());

    // hosts in maintenance mode are excluded from the upgrade
    actionContext.setMaintenanceModeHostExcluded(true);

    return actionContext;
  }

  /**
   * Builds the correct {@link ConfigUpgradePack} based on the upgrade and
   * source stack.
   * <ul>
   * <li>HDP 2.2 to HDP 2.2
   * <ul>
   * <li>Uses {@code config-upgrade.xml} from HDP 2.2
   * </ul>
   * <li>HDP 2.2 to HDP 2.3
   * <ul>
   * <li>Uses {@code config-upgrade.xml} from HDP 2.2
   * </ul>
   * <li>HDP 2.2 to HDP 2.4
   * <ul>
   * <li>Uses {@code config-upgrade.xml} from HDP 2.2 merged with the one from
   * HDP 2.3
   * </ul>
   * <ul>
   */
  public static final class ConfigurationPackBuilder {

    /**
     * Builds the configurations to use for the specified upgrade and source
     * stack.
     *
     * @param cx
     *          the upgrade context(not {@code null}).
     * @return the {@link ConfigUpgradePack} which contains all of the necessary
     *         configuration definitions for the upgrade.
     */
    public static ConfigUpgradePack build(UpgradeContext cx) {
      final UpgradePack upgradePack = cx.getUpgradePack();
      final StackId stackId;

      if (cx.getDirection() == Direction.UPGRADE) {
        stackId = cx.getStackIdFromVersions(cx.getSourceVersions());
      } else {
        stackId = cx.getStackIdFromVersions(cx.getTargetVersions());
      }

      List<UpgradePack.IntermediateStack> intermediateStacks = upgradePack.getIntermediateStacks();
      ConfigUpgradePack configUpgradePack = s_metaProvider.get().getConfigUpgradePack(
        stackId.getStackName(), stackId.getStackVersion());

      // merge in any intermediate stacks
      if (null != intermediateStacks) {

        // start out with the source stack's config pack
        ArrayList<ConfigUpgradePack> configPacksToMerge = Lists.newArrayList(configUpgradePack);

        for (UpgradePack.IntermediateStack intermediateStack : intermediateStacks) {
          ConfigUpgradePack intermediateConfigUpgradePack = s_metaProvider.get().getConfigUpgradePack(
            stackId.getStackName(), intermediateStack.version);

          configPacksToMerge.add(intermediateConfigUpgradePack);
        }

        // merge all together
        configUpgradePack = ConfigUpgradePack.merge(configPacksToMerge);
      }

      return configUpgradePack;
    }
  }

  /**
   * The {@link RepositoryVersions} class is used to represent to/from versions
   * of a service during an upgrade or downgrade.
   */
  final static class RepositoryVersions {
    @JsonProperty("from_repository_id")
    final long fromRepositoryId;

    @JsonProperty("from_repository_version")
    final String fromRepositoryVersion;

    @JsonProperty("to_repository_id")
    final long toRepositoryId;

    @JsonProperty("to_repository_version")
    final String toRepositoryVersion;

    /**
     * Constructor.
     */
    public RepositoryVersions(RepositoryVersionEntity from, RepositoryVersionEntity to) {
      fromRepositoryId = from.getId();
      fromRepositoryVersion = from.getVersion();

      toRepositoryId = to.getId();
      toRepositoryVersion = to.getVersion();
    }
  }
}
