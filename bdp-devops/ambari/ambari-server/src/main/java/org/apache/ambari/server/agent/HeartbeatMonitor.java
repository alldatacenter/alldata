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
package org.apache.ambari.server.agent;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.MessageNotDelivered;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHeartbeatLostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;

/**
 * Monitors the node state and heartbeats.
 */
public class HeartbeatMonitor implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(HeartbeatMonitor.class);
  private Clusters clusters;
  private ActionManager actionManager;
  private final int threadWakeupInterval; //1 minute
  private volatile boolean shouldRun = true;
  private Thread monitorThread = null;
  private final ConfigHelper configHelper;
  private final AmbariMetaInfo ambariMetaInfo;
  private final AmbariManagementController ambariManagementController;
  private final Configuration configuration;
  private final AgentRequests agentRequests;
  private final AmbariEventPublisher ambariEventPublisher;

  public HeartbeatMonitor(Clusters clusters, ActionManager am,
                          int threadWakeupInterval, Injector injector) {
    this.clusters = clusters;
    actionManager = am;
    this.threadWakeupInterval = threadWakeupInterval;
    configHelper = injector.getInstance(ConfigHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariManagementController = injector.getInstance(
            AmbariManagementController.class);
    configuration = injector.getInstance(Configuration.class);
    agentRequests = new AgentRequests();
    ambariEventPublisher = injector.getInstance(AmbariEventPublisher.class);
    ambariEventPublisher.register(this);
  }

  public void shutdown() {
    shouldRun = false;
  }

  public void start() {
    monitorThread = new Thread(this, "ambari-hearbeat-monitor");
    monitorThread.start();
  }

  void join(long millis) throws InterruptedException {
    monitorThread.join(millis);
  }

  public boolean isAlive() {
    return monitorThread.isAlive();
  }

  public AgentRequests getAgentRequests() {
    return agentRequests;
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        doWork();
        LOG.trace("Putting monitor to sleep for {} milliseconds", threadWakeupInterval);
        Thread.sleep(threadWakeupInterval);
      } catch (InterruptedException ex) {
        LOG.warn("Scheduler thread is interrupted going to stop", ex);
        shouldRun = false;
      } catch (Exception ex) {
        LOG.warn("Exception received", ex);
      } catch (Throwable t) {
        LOG.warn("ERROR", t);
      }
    }
  }

  //Go through all the nodes, check for last heartbeat or any waiting state
  //If heartbeat is lost, update node clusters state, purge the action queue
  //notify action manager for node failure.
  private void doWork() throws InvalidStateTransitionException, AmbariException {
    List<Host> allHosts = clusters.getHosts();
    long now = System.currentTimeMillis();
    for (Host hostObj : allHosts) {
      if (hostObj.getState() == HostState.HEARTBEAT_LOST) {
        //do not check if host already known be lost
        continue;
      }
      Long hostId = hostObj.getHostId();
      HostState hostState = hostObj.getState();

      long lastHeartbeat = 0;
      try {
        lastHeartbeat = clusters.getHostById(hostId).getLastHeartbeatTime();
      } catch (AmbariException e) {
        LOG.warn("Exception in getting host object; Is it fatal?", e);
      }
      if (lastHeartbeat + 2 * threadWakeupInterval < now) {
        handleHeartbeatLost(hostId);
      }
      if (hostState == HostState.WAITING_FOR_HOST_STATUS_UPDATES) {
        long timeSpentInState = hostObj.getTimeInState();
        if (timeSpentInState + 5 * threadWakeupInterval < now) {
          //Go back to init, the agent will be asked to register again in the next heartbeat
          LOG.warn("timeSpentInState + 5*threadWakeupInterval < now, Go back to init");
          hostObj.setState(HostState.INIT);
        }
      }
    }
  }

  /**
   * @param hostname
   * @return list of commands to get status of service components on a concrete host
   */
  public List<StatusCommand> generateStatusCommands(String hostname) throws AmbariException {
    List<StatusCommand> cmds = new ArrayList<>();

    for (Cluster cl : clusters.getClustersForHost(hostname)) {
      Map<String, DesiredConfig> desiredConfigs = cl.getDesiredConfigs();
      for (ServiceComponentHost sch : cl.getServiceComponentHosts(hostname)) {
        switch (sch.getState()) {
          case INIT:
          case INSTALLING:
          case STARTING:
          case STOPPING:
            //don't send commands until component is installed at least
            continue;
          default:
            StatusCommand statusCmd = createStatusCommand(hostname, cl, sch, desiredConfigs);
            cmds.add(statusCmd);
        }

      }
    }
    return cmds;
  }

  /**
   * Generates status command and fills all appropriate fields.
   * @throws AmbariException
   */
  private StatusCommand createStatusCommand(String hostname, Cluster cluster,
      ServiceComponentHost sch, Map<String, DesiredConfig> desiredConfigs) throws AmbariException {
    String serviceName = sch.getServiceName();
    String componentName = sch.getServiceComponentName();

    StackId stackId = sch.getDesiredStackId();

    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
            stackId.getStackName(), stackId.getStackVersion(),
            serviceName, componentName);

    Map<String, Map<String, String>> configurations = new TreeMap<>();
    Map<String, Map<String,  Map<String, String>>> configurationAttributes = new TreeMap<>();

    // get the cluster config for type '*-env'
    // apply config group overrides
    //Config clusterConfig = cluster.getDesiredConfigByType(GLOBAL);
    Collection<Config> clusterConfigs = cluster.getAllConfigs();

    // creating list with desired config types to validate if cluster config actual
    Set<String> desiredConfigTypes = desiredConfigs.keySet();

    // Apply global properties for this host from all config groups
    Map<String, Map<String, String>> allConfigTags = configHelper
        .getEffectiveDesiredTags(cluster, hostname);

    for(Config clusterConfig: clusterConfigs) {
      String configType = clusterConfig.getType();
      if(!configType.endsWith("-env") || !desiredConfigTypes.contains(configType)) {
        continue;
      }

      // cluster config for 'global'
      Map<String, String> props = new HashMap<>(clusterConfig.getProperties());

      Map<String, Map<String, String>> configTags = new HashMap<>();

      for (Map.Entry<String, Map<String, String>> entry : allConfigTags.entrySet()) {
        if (entry.getKey().equals(clusterConfig.getType())) {
          configTags.put(clusterConfig.getType(), entry.getValue());
        }
      }

      Map<String, Map<String, String>> properties = configHelper
              .getEffectiveConfigProperties(cluster, configTags);

      if (!properties.isEmpty()) {
        for (Map<String, String> propertyMap : properties.values()) {
          props.putAll(propertyMap);
        }
      }

      configurations.put(clusterConfig.getType(), props);

      Map<String, Map<String, String>> attrs = new TreeMap<>();
      configHelper.cloneAttributesMap(clusterConfig.getPropertiesAttributes(), attrs);

      Map<String, Map<String, Map<String, String>>> attributes = configHelper
          .getEffectiveConfigAttributes(cluster, configTags);
      for (Map<String, Map<String, String>> attributesMap : attributes.values()) {
        configHelper.cloneAttributesMap(attributesMap, attrs);
      }
      configurationAttributes.put(clusterConfig.getType(), attrs);
    }

    StatusCommand statusCmd = new StatusCommand();
    statusCmd.setClusterName(cluster.getClusterName());
    statusCmd.setServiceName(serviceName);
    statusCmd.setComponentName(componentName);
    statusCmd.setConfigurations(configurations);
    statusCmd.setConfigurationAttributes(configurationAttributes);
    statusCmd.setHostname(hostname);

    // If Agent wants the command and the States differ
    statusCmd.setDesiredState(sch.getDesiredState());
    statusCmd.setHasStaleConfigs(configHelper.isStaleConfigs(sch, desiredConfigs));
    if (getAgentRequests().shouldSendExecutionDetails(hostname, componentName)) {
      LOG.info(componentName + " is at " + sch.getState() + " adding more payload per agent ask");
      statusCmd.setPayloadLevel(StatusCommand.StatusCommandPayload.EXECUTION_COMMAND);
    }

    // Fill command params
    Map<String, String> commandParams = statusCmd.getCommandParams();

    String commandTimeout = configuration.getDefaultAgentTaskTimeout(false);
    CommandScriptDefinition script = componentInfo.getCommandScript();
    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Component %s of service %s has not " +
                "command script defined", componentName, serviceName);
        throw new AmbariException(message);
      }
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);
    commandParams.put(SERVICE_PACKAGE_FOLDER,
       serviceInfo.getServicePackageFolder());
    commandParams.put(HOOKS_FOLDER, configuration.getProperty(Configuration.HOOKS_FOLDER));
    // Fill host level params
    Map<String, String> hostLevelParams = statusCmd.getHostLevelParams();
    hostLevelParams.put(JDK_LOCATION, ambariManagementController.getJdkResourceUrl());
    hostLevelParams.put(STACK_NAME, stackId.getStackName());
    hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());

    if (statusCmd.getPayloadLevel() == StatusCommand.StatusCommandPayload.EXECUTION_COMMAND) {
      ExecutionCommand ec = ambariManagementController.getExecutionCommand(cluster, sch, RoleCommand.START);
      statusCmd.setExecutionCommand(ec);
      LOG.debug("{} has more payload for execution command", componentName);
    }

    return statusCmd;
  }

  private void handleHeartbeatLost(Long hostId) throws AmbariException, InvalidStateTransitionException {
    Host hostObj = clusters.getHostById(hostId);
    String host = hostObj.getHostName();
    LOG.warn("Heartbeat lost from host " + host);
    //Heartbeat is expired
    hostObj.handleEvent(new HostHeartbeatLostEvent(host));

    // mark all components that are not clients with unknown status
    for (Cluster cluster : clusters.getClustersForHost(hostObj.getHostName())) {
      for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostObj.getHostName())) {
        Service s = cluster.getService(sch.getServiceName());
        ServiceComponent sc = s.getServiceComponent(sch.getServiceComponentName());
        if (!sc.isClientComponent() &&
            !sch.getState().equals(State.INIT) &&
            !sch.getState().equals(State.INSTALLING) &&
            !sch.getState().equals(State.INSTALL_FAILED) &&
            !sch.getState().equals(State.UNINSTALLED) &&
            !sch.getState().equals(State.DISABLED)) {
          LOG.warn("Setting component state to UNKNOWN for component " + sc.getName() + " on " + host);
          State oldState = sch.getState();
          sch.setState(State.UNKNOWN);
        }
      }
    }

    //Purge action queue
    //notify action manager
    actionManager.handleLostHost(host);
  }

  @Subscribe
  public void onMessageNotDelivered(MessageNotDelivered messageNotDelivered) {
    try {
      Host hostObj = clusters.getHostById(messageNotDelivered.getHostId());
      if (hostObj.getState() == HostState.HEARTBEAT_LOST) {
        //do not check if host already known be lost
        return;
      }
      handleHeartbeatLost(messageNotDelivered.getHostId());
    } catch (Exception e) {
      LOG.error("Error during host to heartbeat lost moving", e);
    }
  }
}
