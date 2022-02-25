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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.upgrade.CreateAndConfigureTask;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * The {@link CreateAndConfigureAction} is used to alter a configuration property during
 * an upgrade. It also creates the config type if it does not exist as a desired config for the cluster.
 * It will only produce a new configuration if an actual change is
 * occuring. For some configure tasks, the value is already at the desired
 * property or the conditions of the task are not met. In these cases, a new
 * configuration will not be created. This task can perform any of the following
 * actions in a single declaration:
 * <ul>
 * <li>Copy a configuration to a new property key, optionally setting a default
 * if the original property did not exist</li>
 * <li>Copy a configuration to a new property key from one configuration type to
 * another, optionally setting a default if the original property did not exist</li>
 * <li>Rename a configuration, optionally setting a default if the original
 * property did not exist</li>
 * <li>Delete a configuration property</li>
 * <li>Set a configuration property</li>
 * <li>Conditionally set a configuration property based on another configuration
 * property value</li>
 * </ul>
 */
public class CreateAndConfigureAction extends ConfigureAction {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAndConfigureAction.class);

  /**
   * Used to lookup the cluster.
   */
  @Inject
  private Clusters m_clusters;

  /**
   * Used to update the configuration properties.
   */
  @Inject
  private AmbariManagementController m_controller;

  /**
   * Used to assist in the creation of a {@link ConfigurationRequest} to update
   * configuration values.
   */
  @Inject
  private ConfigHelper m_configHelper;


  @Override
  public CommandReport execute(
      ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    LOG.info("Create and Configure...");

    Map<String,String> commandParameters = getCommandParameters();
    if( null == commandParameters || commandParameters.isEmpty() ){
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to change configuration values without command parameters");
    }

    String clusterName = commandParameters.get("clusterName");
    Cluster cluster = m_clusters.getCluster(clusterName);
    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    Direction direction = upgradeContext.getDirection();
    if (direction == Direction.DOWNGRADE) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", "", "Skip changing configuration values for downgrade");
    }

    String configType = commandParameters.get(CreateAndConfigureTask.PARAMETER_CONFIG_TYPE);
    String serviceName = cluster.getServiceByConfigType(configType);

    if (StringUtils.isBlank(serviceName)) {
      serviceName = commandParameters.get(CreateAndConfigureTask.PARAMETER_ASSOCIATED_SERVICE);
    }

    RepositoryVersionEntity sourceRepoVersion = upgradeContext.getSourceRepositoryVersion(serviceName);
    RepositoryVersionEntity targetRepoVersion = upgradeContext.getTargetRepositoryVersion(serviceName);
    StackId sourceStackId = sourceRepoVersion.getStackId();
    StackId targetStackId = targetRepoVersion.getStackId();

    if (!sourceStackId.equals(targetStackId)){
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to change configuration values across stacks. Use regular config task type instead.");
    }

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    if (desiredConfig == null) {
      LOG.info(String.format("Could not find desired config type with name %s. Create it with default values.", configType));

      // populate a map with default configurations from the new stack
      Map<String, Map<String, String>> newServiceDefaultConfigsByType = m_configHelper.getDefaultProperties(
          targetStackId, serviceName);

      if (!newServiceDefaultConfigsByType.containsKey(configType)){
        String error = String.format("%s in %s does not contain configuration type %s", serviceName, targetStackId.getStackId(), configType);
        LOG.error(error);
        return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", error);
      }

      Map<String, String> defaultConfigsForType = newServiceDefaultConfigsByType.get(configType);
      // Remove any property for the new config type whose value is NULL
      Iterator<Map.Entry<String, String>> iter = defaultConfigsForType.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, String> entry = iter.next();
        if (entry.getValue() == null) {
          iter.remove();
        }
      }

      String serviceVersionNote = String.format("%s %s %s", direction.getText(true),
          direction.getPreposition(), upgradeContext.getRepositoryVersion().getVersion());

      m_configHelper.createConfigType(cluster, targetStackId,
          m_controller,
          configType, defaultConfigsForType,
          m_controller.getAuthName(), serviceVersionNote);
    }

    return super.execute(requestSharedDataContext);
  }
}