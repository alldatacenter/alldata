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

package org.apache.ambari.server.controller;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AGENT_STACK_RETRY_COUNT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AGENT_STACK_RETRY_ON_UNAVAILABILITY;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLIENTS_TO_UPDATE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMPONENT_CATEGORY;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CUSTOM_COMMAND;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GPL_LICENSE_ACCEPTED;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOST_SYS_PREPPED;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.MYSQL_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.NOT_MANAGED_HDFS_PATH_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.ORACLE_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_GROUPS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;
import static org.apache.ambari.server.controller.internal.RequestResourceProvider.HAS_RESOURCE_FILTERS;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.RefreshCommandConfiguration;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Helper class containing logic to process custom command execution requests .
 * This class has special support needed for SERVICE_CHECK and DECOMMISSION.
 * These commands are not pass through as Ambari has specific persistence requirements.
 */
@Singleton
public class AmbariCustomCommandExecutionHelper {
  private final static Logger LOG = LoggerFactory.getLogger(
      AmbariCustomCommandExecutionHelper.class);

  // TODO: Remove the hard-coded mapping when stack definition indicates which slave types can be decommissioned
  static final Map<String, String> masterToSlaveMappingForDecom = ImmutableMap.<String, String>builder()
    .put("NAMENODE", "DATANODE")
    .put("RESOURCEMANAGER", "NODEMANAGER")
    .put("HBASE_MASTER", "HBASE_REGIONSERVER")
    .put("JOBTRACKER", "TASKTRACKER")
    .build();

  public final static String DECOM_INCLUDED_HOSTS = "included_hosts";
  public final static String DECOM_EXCLUDED_HOSTS = "excluded_hosts";
  public final static String ALL_DECOMMISSIONED_HOSTS = "all_decommissioned_hosts";
  public final static String DECOM_SLAVE_COMPONENT = "slave_type";
  public final static String HBASE_MARK_DRAINING_ONLY = "mark_draining_only";
  public final static String UPDATE_FILES_ONLY = "update_files_only";
  public final static String IS_ADD_OR_DELETE_SLAVE_REQUEST = "is_add_or_delete_slave_request";

  private final static String ALIGN_MAINTENANCE_STATE = "align_maintenance_state";

  public final static int MIN_STRICT_SERVICE_CHECK_TIMEOUT = 120;

  @Inject
  private ActionMetadata actionMetadata;

  @Inject
  private Clusters clusters;

  @Inject
  private AmbariManagementController managementController;

  @Inject
  private Gson gson;

  @Inject
  private Configuration configs;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  private Map<String, Map<String, Map<String, String>>> configCredentialsForService = new HashMap<>();

  protected static final String SERVICE_CHECK_COMMAND_NAME = "SERVICE_CHECK";
  protected static final String START_COMMAND_NAME = "START";
  protected static final String RESTART_COMMAND_NAME = "RESTART";
  protected static final String INSTALL_COMMAND_NAME = "INSTALL";
  public static final String DECOMMISSION_COMMAND_NAME = "DECOMMISSION";


  private Boolean isServiceCheckCommand(String command, String service) {
    List<String> actions = actionMetadata.getActions(service);

    return !(actions == null || actions.size() == 0) && actions.contains(command);
  }

  private Boolean isValidCustomCommand(String clusterName,
      String serviceName, String componentName, String commandName)
      throws AmbariException {

    if (componentName == null) {
      return false;
    }

    Cluster cluster = clusters.getCluster(clusterName);
    Service service = cluster.getService(serviceName);
    ServiceComponent component = service.getServiceComponent(componentName);
    StackId stackId = component.getDesiredStackId();

    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
        stackId.getStackName(), stackId.getStackVersion(),
        serviceName, componentName);

    return !(!componentInfo.isCustomCommand(commandName) &&
      !actionMetadata.isDefaultHostComponentCommand(commandName));
  }

  private Boolean isValidCustomCommand(ActionExecutionContext
      actionExecutionContext, RequestResourceFilter resourceFilter)
      throws AmbariException {

    if (actionExecutionContext.isFutureCommand()) {
      return true;
    }

    String clusterName = actionExecutionContext.getClusterName();
    String serviceName = resourceFilter.getServiceName();
    String componentName = resourceFilter.getComponentName();
    String commandName = actionExecutionContext.getActionName();

    if (componentName == null) {
      return false;
    }

    return isValidCustomCommand(clusterName, serviceName, componentName, commandName);
  }

  private Boolean isValidCustomCommand(ExecuteActionRequest actionRequest,
      RequestResourceFilter resourceFilter) throws AmbariException {
    String clusterName = actionRequest.getClusterName();
    String serviceName = resourceFilter.getServiceName();
    String componentName = resourceFilter.getComponentName();
    String commandName = actionRequest.getCommandName();

    if (componentName == null) {
      return false;
    }

    return isValidCustomCommand(clusterName, serviceName, componentName, commandName);
  }

  private String getReadableCustomCommandDetail(ActionExecutionContext
        actionRequest, RequestResourceFilter resourceFilter) {
    StringBuilder sb = new StringBuilder();
    sb.append(actionRequest.getActionName());
    if (resourceFilter.getServiceName() != null
        && !resourceFilter.getServiceName().equals("")) {
      sb.append(" ");
      sb.append(resourceFilter.getServiceName());
    }

    if (resourceFilter.getComponentName() != null
        && !resourceFilter.getComponentName().equals("")) {
      sb.append("/");
      sb.append(resourceFilter.getComponentName());
    }

    return sb.toString();
  }

  /**
   * Called during the start/stop/restart of services, plus custom commands during Stack Upgrade.
   * @param actionExecutionContext Execution Context
   * @param resourceFilter Resource Filter
   * @param stage Command stage
   * @param additionalCommandParams Additional command params to add the the stage
   * @param commandDetail String for the command detail
   * @throws AmbariException
   */
  private void addCustomCommandAction(final ActionExecutionContext actionExecutionContext,
      final RequestResourceFilter resourceFilter, Stage stage, Map<String, String> additionalCommandParams,
      String commandDetail, Map<String, String> requestParams) throws AmbariException {
    final String serviceName = resourceFilter.getServiceName();
    final String componentName = resourceFilter.getComponentName();
    final String commandName = actionExecutionContext.getActionName();
    boolean retryAllowed = actionExecutionContext.isRetryAllowed();
    boolean autoSkipFailure = actionExecutionContext.isFailureAutoSkipped();

    String clusterName = stage.getClusterName();
    final Cluster cluster = clusters.getCluster(clusterName);

    // start with all hosts
    Set<String> candidateHosts = new HashSet<>(resourceFilter.getHostNames());

    // Filter hosts that are in MS
    Set<String> ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
      candidateHosts, new MaintenanceStateHelper.HostPredicate() {
        @Override
        public boolean shouldHostBeRemoved(final String hostname)
            throws AmbariException {
          if (actionExecutionContext.isFutureCommand()) {
            return false;
          }

          return !maintenanceStateHelper.isOperationAllowed(
              cluster, actionExecutionContext.getOperationLevel(),
              resourceFilter, serviceName, componentName, hostname);
        }
      }
    );

    // Filter unhealthy hosts
    Set<String> unhealthyHosts = getUnhealthyHosts(candidateHosts, actionExecutionContext, resourceFilter);

    // log excluded hosts
    if (!ignoredHosts.isEmpty()) {
      if( LOG.isDebugEnabled() ){
        LOG.debug(
            "While building the {} custom command for {}/{}, the following hosts were excluded: unhealthy[{}], maintenance[{}]",
            commandName, serviceName, componentName, StringUtils.join(unhealthyHosts, ','),
            StringUtils.join(ignoredHosts, ','));
      }
    } else if (!unhealthyHosts.isEmpty()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "While building the {} custom command for {}/{}, the following hosts were excluded: unhealthy[{}], maintenance[{}]",
            commandName, serviceName, componentName, StringUtils.join(unhealthyHosts, ','),
            StringUtils.join(ignoredHosts, ','));
      }
    } else if (candidateHosts.isEmpty()) {
      String message = MessageFormat.format(
          "While building the {0} custom command for {1}/{2}, there were no healthy eligible hosts",
          commandName, serviceName, componentName);

      throw new AmbariException(message);
    }

    Service service = cluster.getService(serviceName);

    // grab the stack ID from the service first, and use the context's if it's set
    StackId stackId = service.getDesiredStackId();
    if (null != actionExecutionContext.getStackId()) {
      stackId = actionExecutionContext.getStackId();
    }

    AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(service);

    CustomCommandDefinition customCommandDefinition = null;
    ComponentInfo ci = serviceInfo.getComponentByName(componentName);
    if(ci != null){
      customCommandDefinition = ci.getCustomCommandByName(commandName);
    }

    long nowTimestamp = System.currentTimeMillis();

    for (String hostName : candidateHosts) {
      stage.addHostRoleExecutionCommand(hostName, Role.valueOf(componentName),
          RoleCommand.CUSTOM_COMMAND,
          new ServiceComponentHostOpInProgressEvent(componentName, hostName, nowTimestamp),
          cluster.getClusterName(), serviceName, retryAllowed, autoSkipFailure);

      ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
          componentName).getExecutionCommand();

      // if the command should fetch brand new configuration tags before
      // execution, then we don't need to fetch them now
      if(actionExecutionContext.getParameters() != null && actionExecutionContext.getParameters().containsKey(KeyNames.OVERRIDE_CONFIGS)){
        execCmd.setOverrideConfigs(true);
      }

      HostRoleCommand cmd = stage.getHostRoleCommand(hostName, componentName);
      if (cmd != null) {
        cmd.setCommandDetail(commandDetail);
        cmd.setCustomCommandName(commandName);
        if (customCommandDefinition != null){
          cmd.setOpsDisplayName(customCommandDefinition.getOpsDisplayName());
        }
      }

      //set type background
      if(customCommandDefinition != null && customCommandDefinition.isBackground()){
        cmd.setBackgroundCommand(true);
        execCmd.setCommandType(AgentCommandType.BACKGROUND_EXECUTION_COMMAND);
      }

      execCmd.setComponentVersions(cluster);

      execCmd.setConfigurations(new TreeMap<>());

      // Get the value of credential store enabled from the DB
      Service clusterService = cluster.getService(serviceName);
      execCmd.setCredentialStoreEnabled(String.valueOf(clusterService.isCredentialStoreEnabled()));

      // Get the map of service config type to password properties for the service
      Map<String, Map<String, String>> configCredentials;
      configCredentials = configCredentialsForService.get(clusterService.getName());
      if (configCredentials == null) {
        configCredentials = configHelper.getCredentialStoreEnabledProperties(stackId, clusterService);
        configCredentialsForService.put(clusterService.getName(), configCredentials);
      }

      execCmd.setConfigurationCredentials(configCredentials);

      Map<String, String> hostLevelParams = new TreeMap<>();
      hostLevelParams.put(STACK_NAME, stackId.getStackName());
      hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());

      Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();

      Set<String> userSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.USER, cluster, desiredConfigs);
      String userList = gson.toJson(userSet);
      hostLevelParams.put(USER_LIST, userList);

      //Create a user_group mapping and send it as part of the hostLevelParams
      Map<String, Set<String>> userGroupsMap = configHelper.createUserGroupsMap(
        stackId, cluster, desiredConfigs);
      String userGroups = gson.toJson(userGroupsMap);
      hostLevelParams.put(USER_GROUPS, userGroups);

      Set<String> groupSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.GROUP, cluster, desiredConfigs);
      String groupList = gson.toJson(groupSet);
      hostLevelParams.put(GROUP_LIST, groupList);

      Map<PropertyInfo, String> notManagedHdfsPathMap = configHelper.getPropertiesWithPropertyType(stackId, PropertyType.NOT_MANAGED_HDFS_PATH, cluster, desiredConfigs);
      Set<String> notManagedHdfsPathSet = configHelper.filterInvalidPropertyValues(notManagedHdfsPathMap, NOT_MANAGED_HDFS_PATH_LIST);
      String notManagedHdfsPathList = gson.toJson(notManagedHdfsPathSet);
      hostLevelParams.put(NOT_MANAGED_HDFS_PATH_LIST, notManagedHdfsPathList);

      execCmd.setHostLevelParams(hostLevelParams);

      Map<String, String> commandParams = new TreeMap<>();
      if (additionalCommandParams != null) {
        for (String key : additionalCommandParams.keySet()) {
          commandParams.put(key, additionalCommandParams.get(key));
        }
      }
      commandParams.put(CUSTOM_COMMAND, commandName);

      boolean isInstallCommand = commandName.equals(RoleCommand.INSTALL.toString());
      int commandTimeout = Short.valueOf(configs.getDefaultAgentTaskTimeout(isInstallCommand)).intValue();

      ComponentInfo componentInfo = ambariMetaInfo.getComponent(
          stackId.getStackName(), stackId.getStackVersion(),
          serviceName, componentName);

      if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
        // Service check command is not custom command
        CommandScriptDefinition script = componentInfo.getCommandScript();

        if (script != null) {
          commandParams.put(SCRIPT, script.getScript());
          commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
          if (script.getTimeout() > 0) {
            commandTimeout = script.getTimeout();
          }
        } else {
          String message = String.format("Component %s has not command script " +
              "defined. It is not possible to send command for " +
              "this service", componentName);
          throw new AmbariException(message);
        }
        // We don't need package/repo information to perform service check
      }

      // !!! the action execution context timeout is the final say, but make sure it's at least 60 seconds
      if (null != actionExecutionContext.getTimeout()) {
        commandTimeout = actionExecutionContext.getTimeout().intValue();
        commandTimeout = Math.max(60, commandTimeout);
      }

      if (requestParams != null && requestParams.containsKey(RequestResourceProvider.CONTEXT)) {
        String requestContext = requestParams.get(RequestResourceProvider.CONTEXT);
        if (StringUtils.isNotEmpty(requestContext) && requestContext.toLowerCase().contains("rolling-restart")) {
          Config clusterEnvConfig = cluster.getDesiredConfigByType("cluster-env");
          if (clusterEnvConfig != null) {
            String componentRollingRestartTimeout = clusterEnvConfig.getProperties().get("namenode_rolling_restart_timeout");
            if (StringUtils.isNotEmpty(componentRollingRestartTimeout)) {
              commandTimeout = Integer.parseInt(componentRollingRestartTimeout);
            }
          }
        }
      }

      commandParams.put(COMMAND_TIMEOUT, "" + commandTimeout);

      Map<String, String> roleParams = execCmd.getRoleParams();
      if (roleParams == null) {
        roleParams = new TreeMap<>();
      }

      // if there is a stack upgrade which is currently suspended then pass that
      // information down with the command as some components may need to know
      boolean isUpgradeSuspended = cluster.isUpgradeSuspended();
      if (isUpgradeSuspended) {
        cluster.addSuspendedUpgradeParameters(commandParams, roleParams);
      }
      StageUtils.useAmbariJdkInCommandParams(commandParams, configs);
      roleParams.put(COMPONENT_CATEGORY, componentInfo.getCategory());

      // set reconfigureAction in case of a RECONFIGURE command if there are any
      if (commandName.equals("RECONFIGURE")) {
        String refreshConfigsCommand = configHelper.getRefreshConfigsCommand(cluster, hostName, serviceName, componentName);
        if (refreshConfigsCommand != null && !refreshConfigsCommand.equals(RefreshCommandConfiguration.REFRESH_CONFIGS)) {
              LOG.info("Refreshing configs for {}/{} with command: ", componentName, hostName, refreshConfigsCommand);
          commandParams.put("reconfigureAction", refreshConfigsCommand);
          //execCmd.setForceRefreshConfigTagsBeforeExecution(true);
        }
      }

      execCmd.setCommandParams(commandParams);
      execCmd.setRoleParams(roleParams);

      // skip anything else
      if (actionExecutionContext.isFutureCommand()) {
        continue;
      }

      // perform any server side command related logic - eg - set desired states on restart
      applyCustomCommandBackendLogic(cluster, serviceName, componentName, commandName, hostName);
    }
  }

  private void applyCustomCommandBackendLogic(Cluster cluster, String serviceName, String componentName, String commandName, String hostname) throws AmbariException {
    switch (commandName) {
      case "RESTART":
        ServiceComponent serviceComponent = cluster.getService(serviceName).getServiceComponent(componentName);
        ServiceComponentHost serviceComponentHost = serviceComponent.getServiceComponentHost(hostname);
        State currentDesiredState = serviceComponentHost.getDesiredState();

        if( !serviceComponent.isClientComponent()) {
          if (currentDesiredState != State.STARTED) {
            LOG.info("Updating desired state to {} on RESTART for {}/{} because it was {}",
                State.STARTED, serviceName, componentName, currentDesiredState);

            serviceComponentHost.setDesiredState(State.STARTED);
          }
        } else {
          LOG.debug("Desired state for client components should not be updated on RESTART. Service/Component {}/{}",
              serviceName, componentName);
        }

        break;
      default:
        LOG.debug("No backend operations needed for the custom command: {}", commandName);
        break;
    }
  }

  private void findHostAndAddServiceCheckAction(final ActionExecutionContext actionExecutionContext,
      final RequestResourceFilter resourceFilter, Stage stage) throws AmbariException {

    String clusterName = actionExecutionContext.getClusterName();
    final Cluster cluster = clusters.getCluster(clusterName);
    final String componentName = actionMetadata.getClient(resourceFilter.getServiceName());
    final String serviceName = resourceFilter.getServiceName();
    String smokeTestRole = actionMetadata.getServiceCheckAction(serviceName);
    if (null == smokeTestRole) {
      smokeTestRole = actionExecutionContext.getActionName();
    }

    Set<String> candidateHosts;
    final Map<String, ServiceComponentHost> serviceHostComponents;

    if (componentName != null) {
      serviceHostComponents = cluster.getService(serviceName).getServiceComponent(componentName).getServiceComponentHosts();

      if (serviceHostComponents.isEmpty()) {
        throw new AmbariException(MessageFormat.format("No hosts found for service: {0}, component: {1} in cluster: {2}",
            serviceName, componentName, clusterName));
      }

      // If specified a specific host, run on it as long as it contains the component.
      // Otherwise, use candidates that contain the component.
      List<String> candidateHostsList = resourceFilter.getHostNames();
      if (candidateHostsList != null && !candidateHostsList.isEmpty()) {
        candidateHosts = new HashSet<>(candidateHostsList);

        // Get the intersection.
        candidateHosts.retainAll(serviceHostComponents.keySet());

        if (candidateHosts.isEmpty()) {
          throw new AmbariException(MessageFormat.format("The resource filter for hosts does not contain components for " +
                  "service: {0}, component: {1} in cluster: {2}", serviceName, componentName, clusterName));
        }
      } else {
        candidateHosts = serviceHostComponents.keySet();
      }
    } else {
      // TODO: This code branch looks unreliable (taking random component, should prefer the clients)
      Map<String, ServiceComponent> serviceComponents = cluster.getService(serviceName).getServiceComponents();

      // Filter components without any HOST
      Iterator<String> serviceComponentNameIterator = serviceComponents.keySet().iterator();
      while (serviceComponentNameIterator.hasNext()){
        String componentToCheck = serviceComponentNameIterator.next();
         if (serviceComponents.get(componentToCheck).getServiceComponentHosts().isEmpty()){
           serviceComponentNameIterator.remove();
         }
      }

      if (serviceComponents.isEmpty()) {
        throw new AmbariException(MessageFormat.format("Did not find any hosts with components for service: {0} in cluster: {1}",
            serviceName, clusterName));
      }

      // Pick a random service (should prefer clients).
      ServiceComponent serviceComponent = serviceComponents.values().iterator().next();
      serviceHostComponents = serviceComponent.getServiceComponentHosts();
      candidateHosts = serviceHostComponents.keySet();
    }

    // check if all hostnames are valid.
    for(String candidateHostName: candidateHosts) {
      ServiceComponentHost serviceComponentHost = serviceHostComponents.get(candidateHostName);

      if (serviceComponentHost == null) {
        throw new AmbariException("Provided hostname = "
            + candidateHostName + " is either not a valid cluster host or does not satisfy the filter condition.");
      }
    }

    // Filter out hosts that are in maintenance mode - they should never be included in service checks
    Set<String> hostsInMaintenanceMode = new HashSet<>();
    if (actionExecutionContext.isMaintenanceModeHostExcluded()) {
      Iterator<String> iterator = candidateHosts.iterator();
      while (iterator.hasNext()) {
        String candidateHostName = iterator.next();
        ServiceComponentHost serviceComponentHost = serviceHostComponents.get(candidateHostName);
        Host host = serviceComponentHost.getHost();
        if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.ON) {
          hostsInMaintenanceMode.add(candidateHostName);
          iterator.remove();
        }
      }
    }

    // Filter out hosts that are not healthy, i.e., all hosts should be heartbeating.
    // Pick one randomly. If there are none, throw an exception.
    List<String> healthyHostNames = managementController.selectHealthyHosts(candidateHosts);
    if (healthyHostNames.isEmpty()) {
      String message = MessageFormat.format(
          "While building a service check command for {0}, there were no healthy eligible hosts: unhealthy[{1}], maintenance[{2}]",
          serviceName, StringUtils.join(candidateHosts, ','),
          StringUtils.join(hostsInMaintenanceMode, ','));

      throw new AmbariException(message);
    }

    String preferredHostName = selectRandomHostNameWithPreferenceOnAvailability(healthyHostNames);

    long nowTimestamp = System.currentTimeMillis();
    Map<String, String> actionParameters = actionExecutionContext.getParameters();
    addServiceCheckAction(stage, preferredHostName, smokeTestRole, nowTimestamp, serviceName, componentName,
        actionParameters, actionExecutionContext.isRetryAllowed(),
        actionExecutionContext.isFailureAutoSkipped(),false);
  }

  /**
   * Assuming all hosts are healthy and not in maintenance mode. Rank the hosts based on availability.
   * Let S = all hosts with 0 PENDING/RUNNING/QUEUED/IN-PROGRESS tasks
   * Let S' be all such other hosts.
   *
   * If S is non-empty, pick a random host from it. If S is empty and S' is non-empty, pick a random host from S'.
   * @param candidateHostNames All possible host names
   * @return Random host with a preference for those that are available to process commands immediately.
   */
  private String selectRandomHostNameWithPreferenceOnAvailability(List<String> candidateHostNames) throws AmbariException {
    if (null == candidateHostNames || candidateHostNames.isEmpty()) {
      return null;
    }
    if (candidateHostNames.size() == 1) {
      return candidateHostNames.get(0);
    }

    List<String> hostsWithZeroCommands = new ArrayList<>();
    List<String> hostsWithInProgressCommands = new ArrayList<>();

    Map<Long, Integer> hostIdToCount = hostRoleCommandDAO.getHostIdToCountOfCommandsWithStatus(HostRoleStatus.IN_PROGRESS_STATUSES);
    for (String hostName : candidateHostNames) {
      Host host = clusters.getHost(hostName);

      if (hostIdToCount.containsKey(host.getHostId()) && hostIdToCount.get(host.getHostId()) > 0) {
        hostsWithInProgressCommands.add(hostName);
      } else {
        hostsWithZeroCommands.add(hostName);
      }
    }

    List<String> preferredList = !hostsWithZeroCommands.isEmpty() ? hostsWithZeroCommands : hostsWithInProgressCommands;
    if (!preferredList.isEmpty()) {
      int randomIndex = new Random().nextInt(preferredList.size());
      return preferredList.get(randomIndex);
    }

    return null;
  }

  /**
   * Creates and populates service check EXECUTION_COMMAND for host. Not all
   * EXECUTION_COMMAND parameters are populated here because they are not needed
   * by service check.
   */
  public void addServiceCheckAction(Stage stage, String hostname, String smokeTestRole,
      long nowTimestamp, String serviceName, String componentName,
      Map<String, String> actionParameters, boolean retryAllowed, boolean autoSkipFailure, boolean useLatestConfigs)
          throws AmbariException {

    String clusterName = stage.getClusterName();
    Cluster cluster = clusters.getCluster(clusterName);
    Service service = cluster.getService(serviceName);
    ServiceComponent component = null;
    if (null != componentName) {
      component = service.getServiceComponent(componentName);
    }
    StackId stackId = (null != component) ? component.getDesiredStackId() : service.getDesiredStackId();

    AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);

    stage.addHostRoleExecutionCommand(hostname, Role.valueOf(smokeTestRole),
        RoleCommand.SERVICE_CHECK,
        new ServiceComponentHostOpInProgressEvent(componentName, hostname, nowTimestamp),
        cluster.getClusterName(), serviceName, retryAllowed, autoSkipFailure);

    HostRoleCommand hrc = stage.getHostRoleCommand(hostname, smokeTestRole);
    if (hrc != null) {
      hrc.setCommandDetail(String.format("%s %s", RoleCommand.SERVICE_CHECK.toString(), serviceName));
    }
    // [ type -> [ key, value ] ]
    Map<String, Map<String, String>> configurations =
        new TreeMap<>();

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostname,
        smokeTestRole).getExecutionCommand();

    // if the command should fetch brand new configuration tags before
    // execution, then we don't need to fetch them now
    if(actionParameters != null && actionParameters.containsKey(KeyNames.OVERRIDE_CONFIGS)){
      execCmd.setOverrideConfigs(true);
    }

    execCmd.setConfigurations(configurations);

    // Generate localComponents
    for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostname)) {
      execCmd.getLocalComponents().add(sch.getServiceComponentName());
    }

    Map<String, String> commandParams = new TreeMap<>();
    String commandTimeout = getStatusCommandTimeout(serviceInfo);

    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      // Service check command is not custom command
      CommandScriptDefinition script = serviceInfo.getCommandScript();
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
      } else {
        String message = String.format("Service %s has no command script " +
            "defined. It is not possible to run service check" +
            " for this service", serviceName);
        throw new AmbariException(message);
      }
      // We don't need package/repo information to perform service check
    }

    commandParams.put(COMMAND_TIMEOUT, commandTimeout);
    String checkType = configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.CLUSTER_ENV, ConfigHelper.SERVICE_CHECK_TYPE);
    if (ConfigHelper.SERVICE_CHECK_MINIMAL.equals(checkType)) {
      int actualTimeout = Integer.parseInt(commandParams.get(COMMAND_TIMEOUT)) / 2;
      actualTimeout = actualTimeout < MIN_STRICT_SERVICE_CHECK_TIMEOUT ? MIN_STRICT_SERVICE_CHECK_TIMEOUT : actualTimeout;
      commandParams.put(COMMAND_TIMEOUT, Integer.toString(actualTimeout));
    }

    StageUtils.useAmbariJdkInCommandParams(commandParams, configs);

    execCmd.setCommandParams(commandParams);

    if (actionParameters != null) { // If defined
      execCmd.setRoleParams(actionParameters);
    }
    if (useLatestConfigs) {
      execCmd.setUseLatestConfigs(useLatestConfigs);
    }
  }

  private Set<String> getHostList(Map<String, String> cmdParameters, String key) {
    Set<String> hosts = new HashSet<>();
    if (cmdParameters.containsKey(key)) {
      String allHosts = cmdParameters.get(key);
      if (allHosts != null) {
        for (String hostName : allHosts.trim().split(",")) {
          hosts.add(hostName.trim());
        }
      }
    }
    return hosts;
  }

  /**
   * Processes decommission command. Modifies the host components as needed and then
   * calls into the implementation of a custom command
   */
  private void addDecommissionAction(final ActionExecutionContext actionExecutionContext,
      final RequestResourceFilter resourceFilter, Stage stage, ExecuteCommandJson executeCommandJson)
    throws AmbariException {

    String clusterName = actionExecutionContext.getClusterName();
    final Cluster cluster = clusters.getCluster(clusterName);
    final String serviceName = resourceFilter.getServiceName();
    String masterCompType = resourceFilter.getComponentName();
    List<String> hosts = resourceFilter.getHostNames();

    if (hosts != null && !hosts.isEmpty()) {
      throw new AmbariException("Decommission command cannot be issued with " +
        "target host(s) specified.");
    }

    //Get all hosts to be added and removed
    Set<String> excludedHosts = getHostList(actionExecutionContext.getParameters(),
                                            DECOM_EXCLUDED_HOSTS);
    Set<String> includedHosts = getHostList(actionExecutionContext.getParameters(),
                                            DECOM_INCLUDED_HOSTS);

    if (actionExecutionContext.getParameters().get(IS_ADD_OR_DELETE_SLAVE_REQUEST) != null &&
            actionExecutionContext.getParameters().get(IS_ADD_OR_DELETE_SLAVE_REQUEST).equalsIgnoreCase("true")) {
      includedHosts = getHostList(actionExecutionContext.getParameters(), masterCompType + "_" + DECOM_INCLUDED_HOSTS);
    }

    Set<String> cloneSet = new HashSet<>(excludedHosts);
    cloneSet.retainAll(includedHosts);
    if (cloneSet.size() > 0) {
      throw new AmbariException("Same host cannot be specified for inclusion " +
        "as well as exclusion. Hosts: " + cloneSet);
    }

    Service service = cluster.getService(serviceName);
    if (service == null) {
      throw new AmbariException("Specified service " + serviceName +
        " is not a valid/deployed service.");
    }

    Map<String, ServiceComponent> svcComponents = service.getServiceComponents();
    if (!svcComponents.containsKey(masterCompType)) {
      throw new AmbariException("Specified component " + masterCompType +
        " does not belong to service " + serviceName + ".");
    }

    ServiceComponent masterComponent = svcComponents.get(masterCompType);
    if (!masterComponent.isMasterComponent()) {
      throw new AmbariException("Specified component " + masterCompType +
        " is not a MASTER for service " + serviceName + ".");
    }

    if (!masterToSlaveMappingForDecom.containsKey(masterCompType)) {
      throw new AmbariException("Decommissioning is not supported for " + masterCompType);
    }

    // Find the slave component
    String slaveCompStr = actionExecutionContext.getParameters().get(DECOM_SLAVE_COMPONENT);
    final String slaveCompType;
    if (slaveCompStr == null || slaveCompStr.equals("")) {
      slaveCompType = masterToSlaveMappingForDecom.get(masterCompType);
    } else {
      slaveCompType = slaveCompStr;
      if (!masterToSlaveMappingForDecom.get(masterCompType).equals(slaveCompType)) {
        throw new AmbariException("Component " + slaveCompType + " is not supported for decommissioning.");
      }
    }

    String isDrainOnlyRequest = actionExecutionContext.getParameters().get(HBASE_MARK_DRAINING_ONLY);
    if (isDrainOnlyRequest != null && !slaveCompType.equals(Role.HBASE_REGIONSERVER.name())) {
      throw new AmbariException(HBASE_MARK_DRAINING_ONLY + " is not a valid parameter for " + masterCompType);
    }

    // Filtering hosts based on Maintenance State
    MaintenanceStateHelper.HostPredicate hostPredicate
            = new MaintenanceStateHelper.HostPredicate() {
              @Override
              public boolean shouldHostBeRemoved(final String hostname)
              throws AmbariException {
                //Get UPDATE_FILES_ONLY parameter as string
                String upd_excl_file_only_str = actionExecutionContext.getParameters()
                .get(UPDATE_FILES_ONLY);

                String decom_incl_hosts_str = actionExecutionContext.getParameters()
                .get(DECOM_INCLUDED_HOSTS);
                if ((upd_excl_file_only_str != null &&
                        !upd_excl_file_only_str.trim().equals(""))){
                  upd_excl_file_only_str = upd_excl_file_only_str.trim();
                }

                boolean upd_excl_file_only = false;
                //Parse of possible forms of value
                if (upd_excl_file_only_str != null &&
                        !upd_excl_file_only_str.equals("") &&
                        (upd_excl_file_only_str.equals("\"true\"")
                        || upd_excl_file_only_str.equals("'true'")
                        || upd_excl_file_only_str.equals("true"))){
                  upd_excl_file_only = true;
                }

                // If we just clear *.exclude and component have been already removed we will skip check
                if (upd_excl_file_only && decom_incl_hosts_str != null
                        && !decom_incl_hosts_str.trim().equals("")) {
                  return upd_excl_file_only;
                } else {
                  return !maintenanceStateHelper.isOperationAllowed(
                          cluster, actionExecutionContext.getOperationLevel(),
                          resourceFilter, serviceName, slaveCompType, hostname);
                }
              }
            };
    // Filter excluded hosts
    Set<String> filteredExcludedHosts = new HashSet<>(excludedHosts);
    Set<String> ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
            filteredExcludedHosts, hostPredicate);
    if (! ignoredHosts.isEmpty()) {
      String message = String.format("Some hosts (%s) from host exclude list " +
                      "have been ignored " +
                      "because components on them are in Maintenance state.",
              ignoredHosts);
      LOG.debug(message);
    }

    // Filter included hosts
    Set<String> filteredIncludedHosts = new HashSet<>(includedHosts);
    ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
            filteredIncludedHosts, hostPredicate);
    if (! ignoredHosts.isEmpty()) {
      String message = String.format("Some hosts (%s) from host include list " +
                      "have been ignored " +
                      "because components on them are in Maintenance state.",
              ignoredHosts);
      LOG.debug(message);
    }

    // Decommission only if the sch is in state STARTED or INSTALLED
    for (ServiceComponentHost sch : svcComponents.get(slaveCompType).getServiceComponentHosts().values()) {
      if (filteredExcludedHosts.contains(sch.getHostName())
          && !"true".equals(isDrainOnlyRequest)
          && sch.getState() != State.STARTED) {
        throw new AmbariException("Component " + slaveCompType + " on host " + sch.getHostName() + " cannot be " +
            "decommissioned as its not in STARTED state. Aborting the whole request.");
      }
    }

    String alignMtnStateStr = actionExecutionContext.getParameters().get(ALIGN_MAINTENANCE_STATE);
    boolean alignMtnState = "true".equals(alignMtnStateStr);
    // Set/reset decommissioned flag on all components
    List<String> listOfExcludedHosts = new ArrayList<>();
    for (ServiceComponentHost sch : svcComponents.get(slaveCompType).getServiceComponentHosts().values()) {
      if (filteredExcludedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
        listOfExcludedHosts.add(sch.getHostName());
        if (alignMtnState) {
          sch.setMaintenanceState(MaintenanceState.ON);
          LOG.info("marking Maintenance=ON on " + sch.getHostName());
        }
        LOG.info("Decommissioning " + slaveCompType + " on " + sch.getHostName());
      }
      if (filteredIncludedHosts.contains(sch.getHostName())) {
        sch.setComponentAdminState(HostComponentAdminState.INSERVICE);
        if (alignMtnState) {
          sch.setMaintenanceState(MaintenanceState.OFF);
          LOG.info("marking Maintenance=OFF on " + sch.getHostName());
        }
        LOG.info("Recommissioning " + slaveCompType + " on " + sch.getHostName());
      }
    }

    // In the event there are more than one master host the following logic is applied
    // -- HDFS/DN, MR1/TT, YARN/NM call refresh node on both
    // -- HBASE/RS call only on one host

    // Ensure host is active
    Map<String, ServiceComponentHost> masterSchs = masterComponent.getServiceComponentHosts();
    String primaryCandidate = null;
    for (String hostName : masterSchs.keySet()) {
      if (primaryCandidate == null) {
        primaryCandidate = hostName;
      } else {
        ServiceComponentHost sch = masterSchs.get(hostName);
        if (sch.getState() == State.STARTED) {
          primaryCandidate = hostName;
        }
      }
    }

    StringBuilder commandDetail = getReadableDecommissionCommandDetail
      (actionExecutionContext, filteredIncludedHosts, listOfExcludedHosts);

    for (String hostName : masterSchs.keySet()) {
      RequestResourceFilter commandFilter = new RequestResourceFilter(serviceName,
        masterComponent.getName(), Collections.singletonList(hostName));
      List<RequestResourceFilter> resourceFilters = new ArrayList<>();
      resourceFilters.add(commandFilter);

      ActionExecutionContext commandContext = new ActionExecutionContext(
        clusterName, actionExecutionContext.getActionName(), resourceFilters
      );

      String clusterHostInfoJson = StageUtils.getGson().toJson(
          StageUtils.getClusterHostInfo(cluster));

      // Reset cluster host info as it has changed
      if (executeCommandJson != null) {
        executeCommandJson.setClusterHostInfo(clusterHostInfoJson);
      }

      Map<String, String> commandParams = new HashMap<>();

      commandParams.put(ALL_DECOMMISSIONED_HOSTS,
          StringUtils.join(calculateDecommissionedNodes(service, slaveCompType), ','));

      if (serviceName.equals(Service.Type.HBASE.name())) {
        commandParams.put(DECOM_EXCLUDED_HOSTS, StringUtils.join(listOfExcludedHosts, ','));
        if ((isDrainOnlyRequest != null) && isDrainOnlyRequest.equals("true")) {
          commandParams.put(HBASE_MARK_DRAINING_ONLY, isDrainOnlyRequest);
        } else {
          commandParams.put(HBASE_MARK_DRAINING_ONLY, "false");
        }
      }

      if (!serviceName.equals(Service.Type.HBASE.name()) || hostName.equals(primaryCandidate)) {
        commandParams.put(UPDATE_FILES_ONLY, "false");
        addCustomCommandAction(commandContext, commandFilter, stage, commandParams, commandDetail.toString(), null);
      }
    }
  }

  private Set<String> calculateDecommissionedNodes(Service service, String slaveCompType) throws AmbariException {
    Set<String> decommissionedHostsSet = new HashSet<>();
    ServiceComponent serviceComponent = service.getServiceComponent(slaveCompType);
    for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
      if (serviceComponentHost.getComponentAdminState() == HostComponentAdminState.DECOMMISSIONED) {
        decommissionedHostsSet.add(serviceComponentHost.getHostName());
      }
    }
    return decommissionedHostsSet;
  }


  private StringBuilder getReadableDecommissionCommandDetail(
      ActionExecutionContext actionExecutionContext, Set<String> includedHosts,
      List<String> listOfExcludedHosts) {
    StringBuilder commandDetail = new StringBuilder();
    commandDetail.append(actionExecutionContext.getActionName());
    if (actionExecutionContext.getParameters().containsKey(IS_ADD_OR_DELETE_SLAVE_REQUEST) &&
      actionExecutionContext.getParameters().get(IS_ADD_OR_DELETE_SLAVE_REQUEST).equalsIgnoreCase("true")) {
      commandDetail.append(", Update Include/Exclude Files");
      return commandDetail;
    }
    if (listOfExcludedHosts.size() > 0) {
      commandDetail.append(", Excluded: ").append(StringUtils.join(listOfExcludedHosts, ','));
    }
    if (includedHosts.size() > 0) {
      commandDetail.append(", Included: ").append(StringUtils.join(includedHosts, ','));
    }
    return commandDetail;
  }

  /**
   * Validate custom command and throw exception is invalid request.
   *
   * @param actionRequest  the action request
   *
   * @throws AmbariException if the action can not be validated
   */
  public void validateAction(ExecuteActionRequest actionRequest) throws AmbariException {

    List<RequestResourceFilter> resourceFilters = actionRequest.getResourceFilters();

    if (resourceFilters != null && resourceFilters.isEmpty() &&
            actionRequest.getParameters().containsKey(HAS_RESOURCE_FILTERS) &&
            actionRequest.getParameters().get(HAS_RESOURCE_FILTERS).equalsIgnoreCase("true")) {
      LOG.warn("Couldn't find any resource that satisfies given resource filters");
      return;
    }

    if (resourceFilters == null || resourceFilters.isEmpty()) {
      throw new AmbariException("Command execution cannot proceed without a " +
        "resource filter.");
    }

    for (RequestResourceFilter resourceFilter : resourceFilters) {
      if (resourceFilter.getServiceName() == null
        || resourceFilter.getServiceName().isEmpty()
        || actionRequest.getCommandName() == null
        || actionRequest.getCommandName().isEmpty()) {
        throw new AmbariException("Invalid resource filter : " + "cluster = "
          + actionRequest.getClusterName() + ", service = "
          + resourceFilter.getServiceName() + ", command = "
          + actionRequest.getCommandName());
      }

      if (!isServiceCheckCommand(actionRequest.getCommandName(), resourceFilter.getServiceName())
        && !isValidCustomCommand(actionRequest, resourceFilter)) {
        throw new AmbariException(
          "Unsupported action " + actionRequest.getCommandName() +
            " for Service: " + resourceFilter.getServiceName()
            + " and Component: " + resourceFilter.getComponentName());
      }
    }
  }

  /**
   * Other than Service_Check and Decommission all other commands are pass-through
   *
   * @param actionExecutionContext  received request to execute a command
   * @param stage                   the initial stage for task creation
   * @param requestParams           the request params
   * @param executeCommandJson      set of json arguments passed to the request
   *
   * @throws AmbariException if the commands can not be added
   */
  public void addExecutionCommandsToStage(ActionExecutionContext actionExecutionContext,
      Stage stage, Map<String, String> requestParams, ExecuteCommandJson executeCommandJson)
    throws AmbariException {

    List<RequestResourceFilter> resourceFilters = actionExecutionContext.getResourceFilters();

    for (RequestResourceFilter resourceFilter : resourceFilters) {
      LOG.debug("Received a command execution request, clusterName={}, serviceName={}, request={}",
        actionExecutionContext.getClusterName(), resourceFilter.getServiceName(), actionExecutionContext);

      String actionName = actionExecutionContext.getActionName();
      if (actionName.contains(SERVICE_CHECK_COMMAND_NAME)) {
        findHostAndAddServiceCheckAction(actionExecutionContext, resourceFilter, stage);
      } else if (actionName.equals(DECOMMISSION_COMMAND_NAME)) {
        addDecommissionAction(actionExecutionContext, resourceFilter, stage, executeCommandJson);
      } else if (isValidCustomCommand(actionExecutionContext, resourceFilter)) {

        String commandDetail = getReadableCustomCommandDetail(actionExecutionContext, resourceFilter);
        Map<String, String> extraParams = new HashMap<>();
        String componentName = (null == resourceFilter.getComponentName()) ? null :
            resourceFilter.getComponentName().toLowerCase();

        if (null != componentName && requestParams.containsKey(componentName)) {
          extraParams.put(componentName, requestParams.get(componentName));
        }

        // If command should be retried upon failure then add the option and also the default duration for retry
        if (requestParams.containsKey(KeyNames.COMMAND_RETRY_ENABLED)) {
          extraParams.put(KeyNames.COMMAND_RETRY_ENABLED, requestParams.get(KeyNames.COMMAND_RETRY_ENABLED));
          String commandRetryDuration = ConfigHelper.COMMAND_RETRY_MAX_TIME_IN_SEC_DEFAULT;
          if (requestParams.containsKey(KeyNames.MAX_DURATION_OF_RETRIES)) {
            String commandRetryDurationStr = requestParams.get(KeyNames.MAX_DURATION_OF_RETRIES);
            Integer commandRetryDurationInt = NumberUtils.toInt(commandRetryDurationStr, 0);
            if (commandRetryDurationInt > 0) {
              commandRetryDuration = Integer.toString(commandRetryDurationInt);
            }
          }
          extraParams.put(KeyNames.MAX_DURATION_OF_RETRIES, commandRetryDuration);
        }

        // If command needs to explicitly disable STDOUT/STDERR logging
        if (requestParams.containsKey(KeyNames.LOG_OUTPUT)) {
          extraParams.put(KeyNames.LOG_OUTPUT, requestParams.get(KeyNames.LOG_OUTPUT));
        }

        if(requestParams.containsKey(KeyNames.OVERRIDE_CONFIGS)){
          actionExecutionContext.getParameters().put(KeyNames.OVERRIDE_CONFIGS, requestParams.get(KeyNames.OVERRIDE_CONFIGS));
        }

        RequestOperationLevel operationLevel = actionExecutionContext.getOperationLevel();
        if (operationLevel != null) {
          String clusterName = operationLevel.getClusterName();
          String serviceName = operationLevel.getServiceName();

          if (isTopologyRefreshRequired(actionName, clusterName, serviceName)) {
            extraParams.put(KeyNames.REFRESH_TOPOLOGY, "True");
          }
        }

        addCustomCommandAction(actionExecutionContext, resourceFilter, stage, extraParams, commandDetail, requestParams);
      } else {
        throw new AmbariException("Unsupported action " + actionName);
      }
    }
  }





  /**
   * Helper method to fill execution command information.
   *
   * @param actionExecContext  the context
   * @param cluster            the cluster for the command
   * @param stackId            the stack id used to load service metainfo.
   *
   * @return a wrapper of the important JSON structures to add to a stage
   */
  public ExecuteCommandJson getCommandJson(ActionExecutionContext actionExecContext,
      Cluster cluster, StackId stackId, String requestContext) throws AmbariException {

    Map<String, String> commandParamsStage = StageUtils.getCommandParamsStage(actionExecContext, requestContext);
    Map<String, String> hostParamsStage = new HashMap<>();
    Map<String, Set<String>> clusterHostInfo;
    String clusterHostInfoJson = "{}";

    if (null != cluster) {
      clusterHostInfo = StageUtils.getClusterHostInfo(cluster);

      // Important, because this runs during Stack Uprade, it needs to use the effective Stack Id.
      hostParamsStage = createDefaultHostParams(cluster, stackId);

      String componentName = null;
      String serviceName = null;
      if (actionExecContext.getOperationLevel() != null) {
        componentName = actionExecContext.getOperationLevel().getHostComponentName();
        serviceName = actionExecContext.getOperationLevel().getServiceName();
      }

      if (serviceName != null && componentName != null) {
        Service service = cluster.getService(serviceName);
        ServiceComponent component = service.getServiceComponent(componentName);
        stackId = component.getDesiredStackId();

        ComponentInfo componentInfo = ambariMetaInfo.getComponent(
                stackId.getStackName(), stackId.getStackVersion(),
                serviceName, componentName);
        List<String> clientsToUpdateConfigsList = componentInfo.getClientsToUpdateConfigs();
        if (clientsToUpdateConfigsList == null) {
          clientsToUpdateConfigsList = new ArrayList<>();
          clientsToUpdateConfigsList.add("*");
        }
        String clientsToUpdateConfigs = gson.toJson(clientsToUpdateConfigsList);
        hostParamsStage.put(CLIENTS_TO_UPDATE_CONFIGS, clientsToUpdateConfigs);
      }

      clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
    }

    String hostParamsStageJson = StageUtils.getGson().toJson(hostParamsStage);
    String commandParamsStageJson = StageUtils.getGson().toJson(commandParamsStage);

    return new ExecuteCommandJson(clusterHostInfoJson, commandParamsStageJson,
        hostParamsStageJson);
  }

  Map<String, String> createDefaultHostParams(Cluster cluster, StackId stackId) throws AmbariException {
    if (null == stackId) {
      stackId = cluster.getDesiredStackVersion();
    }

    TreeMap<String, String> hostLevelParams = new TreeMap<>();
    StageUtils.useStackJdkIfExists(hostLevelParams, configs);
    hostLevelParams.put(JDK_LOCATION, managementController.getJdkResourceUrl());
    hostLevelParams.put(STACK_NAME, stackId.getStackName());
    hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());
    hostLevelParams.put(DB_NAME, managementController.getServerDB());
    hostLevelParams.put(MYSQL_JDBC_URL, managementController.getMysqljdbcUrl());
    hostLevelParams.put(ORACLE_JDBC_URL, managementController.getOjdbcUrl());
    hostLevelParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    hostLevelParams.putAll(managementController.getRcaParameters());
    hostLevelParams.put(HOST_SYS_PREPPED, configs.areHostsSysPrepped());
    hostLevelParams.put(AGENT_STACK_RETRY_ON_UNAVAILABILITY, configs.isAgentStackRetryOnInstallEnabled());
    hostLevelParams.put(AGENT_STACK_RETRY_COUNT, configs.getAgentStackRetryOnInstallCount());
    hostLevelParams.put(GPL_LICENSE_ACCEPTED, configs.getGplLicenseAccepted().toString());

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    Map<PropertyInfo, String> notManagedHdfsPathMap = configHelper.getPropertiesWithPropertyType(stackId, PropertyType.NOT_MANAGED_HDFS_PATH, cluster, desiredConfigs);
    Set<String> notManagedHdfsPathSet = configHelper.filterInvalidPropertyValues(notManagedHdfsPathMap, NOT_MANAGED_HDFS_PATH_LIST);
    String notManagedHdfsPathList = gson.toJson(notManagedHdfsPathSet);
    hostLevelParams.put(NOT_MANAGED_HDFS_PATH_LIST, notManagedHdfsPathList);

    for (Map.Entry<String, String> dbConnectorName : configs.getDatabaseConnectorNames().entrySet()) {
      hostLevelParams.put(dbConnectorName.getKey(), dbConnectorName.getValue());
    }

    for (Map.Entry<String, String> previousDBConnectorName : configs.getPreviousDatabaseConnectorNames().entrySet()) {
      hostLevelParams.put(previousDBConnectorName.getKey(), previousDBConnectorName.getValue());
    }


    return hostLevelParams;
  }

  /**
   * Determine whether or not the action should trigger a topology refresh.
   *
   * @param actionName   the action name (i.e. START, RESTART)
   * @param clusterName  the cluster name
   * @param serviceName  the service name
   *
   * @return true if a topology refresh is required for the action
   */
  public boolean isTopologyRefreshRequired(String actionName, String clusterName, String serviceName)
      throws AmbariException {

    if (actionName.equals(START_COMMAND_NAME) || actionName.equals(RESTART_COMMAND_NAME)) {
      Cluster cluster = clusters.getCluster(clusterName);
      StackId stackId = null;
      if (serviceName != null) {
        try {
          Service service = cluster.getService(serviceName);
          stackId = service.getDesiredStackId();
        } catch (AmbariException e) {
          LOG.debug("Could not load service {}, skipping topology check", serviceName);
        }
      }

      if (stackId == null) {
        stackId = cluster.getDesiredStackVersion();
      }

      AmbariMetaInfo ambariMetaInfo = managementController.getAmbariMetaInfo();

      StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      if (stack != null) {
        ServiceInfo serviceInfo = stack.getService(serviceName);

        if (serviceInfo != null) {
          // if there is a chance that this action was triggered by a change in rack info then we want to
          // force a topology refresh
          // TODO : we may be able to be smarter about this and only refresh when the rack info has definitely changed
          Boolean restartRequiredAfterRackChange = serviceInfo.isRestartRequiredAfterRackChange();
          if (restartRequiredAfterRackChange != null && restartRequiredAfterRackChange) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private ServiceComponent getServiceComponent ( ActionExecutionContext actionExecutionContext,
                                                RequestResourceFilter resourceFilter){
    try {
      Cluster cluster = clusters.getCluster(actionExecutionContext.getClusterName());
      Service service = cluster.getService(resourceFilter.getServiceName());

      return service.getServiceComponent(resourceFilter.getComponentName());
    } catch (Exception e) {
      LOG.debug("Unknown error appears during getting service component: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Filter host according to status of host/host components
   * @param hostname Host name to check
   * @param actionExecutionContext Received request to execute a command
   * @param resourceFilter Resource filter
   * @return True if host need to be filtered, False if Not
   * @throws AmbariException
   */
  private boolean filterUnhealthHostItem(String hostname,
                                         ActionExecutionContext actionExecutionContext,
                                         RequestResourceFilter resourceFilter) throws AmbariException {

    RequestOperationLevel operationLevel = actionExecutionContext.getOperationLevel();
    ServiceComponent serviceComponent = getServiceComponent(actionExecutionContext, resourceFilter);
    if (serviceComponent != null && operationLevel != null
                                && operationLevel.getLevel() == Resource.Type.Service // compare operation is allowed only for Service operation level
                                && actionExecutionContext.getResourceFilters().size() > 1  // Check if operation was started in a chain
                                && !serviceComponent.isMasterComponent()
       ){

      return !(clusters.getHost(hostname).getState() == HostState.HEALTHY);
    } else if (serviceComponent != null && operationLevel != null
                                        && operationLevel.getLevel() == Resource.Type.Host  // compare operation is allowed only for host component operation level
                                        && actionExecutionContext.getResourceFilters().size() > 1  // Check if operation was started in a chain
                                        && serviceComponent.getServiceComponentHosts().containsKey(hostname)  // Check if host is assigned to host component
                                        && !serviceComponent.isMasterComponent()
       ){

      State hostState = serviceComponent.getServiceComponentHosts().get(hostname).getState();

      return hostState == State.UNKNOWN;
    }
    return false;
  }


  /**
   * Filter hosts according to status of host/host components
   * @param hosts Host name set to filter
   * @param actionExecutionContext Received request to execute a command
   * @param resourceFilter Resource filter
   * @return Set of excluded hosts
   * @throws AmbariException
   */
  private Set<String> getUnhealthyHosts(Set<String> hosts,
                                          ActionExecutionContext actionExecutionContext,
                                          RequestResourceFilter resourceFilter) throws AmbariException {
    Set<String> removedHosts = new HashSet<>();
    for (String hostname : hosts) {
      if (filterUnhealthHostItem(hostname, actionExecutionContext, resourceFilter)){
        removedHosts.add(hostname);
      }
    }
    hosts.removeAll(removedHosts);
    return removedHosts;
  }

  public String getStatusCommandTimeout(ServiceInfo serviceInfo) throws AmbariException {
    String commandTimeout = configs.getDefaultAgentTaskTimeout(false);

    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      // Service check command is not custom command
      CommandScriptDefinition script = serviceInfo.getCommandScript();
      if (script != null) {
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Service %s has no command script " +
            "defined. It is not possible to run service check" +
            " for this service", serviceInfo.getName());
        throw new AmbariException(message);
      }
    }

    // Try to apply overridden service check timeout value if available
    Long overriddenTimeout = configs.getAgentServiceCheckTaskTimeout();
    if (!overriddenTimeout.equals(Configuration.AGENT_SERVICE_CHECK_TASK_TIMEOUT.getDefaultValue())) {
      commandTimeout = String.valueOf(overriddenTimeout);
    }
    return commandTimeout;
  }
}
