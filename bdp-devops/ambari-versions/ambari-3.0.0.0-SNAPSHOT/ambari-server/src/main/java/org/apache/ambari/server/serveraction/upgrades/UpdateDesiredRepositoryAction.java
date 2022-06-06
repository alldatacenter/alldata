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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.RepositoryType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * Action that represents updating the Desired Stack Id during the middle of a stack upgrade (typically NonRolling).
 * In a {@link org.apache.ambari.spi.upgrade.UpgradeType#NON_ROLLING}, the effective Stack Id is
 * actually changed half-way through calculating the Actions, and this serves to update the database to make it
 * evident to the user at which point it changed.
 */
public class UpdateDesiredRepositoryAction extends AbstractUpgradeServerAction {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpdateDesiredRepositoryAction.class);

  /**
   * The Ambari configuration.
   */
  @Inject
  private Configuration m_configuration;

  /**
   * Used for restting host version states on downgrade.
   */
  @Inject
  private HostVersionDAO m_hostVersionDAO;

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);
    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    Map<String, String> roleParams = getExecutionCommand().getRoleParams();

    // Make a best attempt at setting the username
    String userName;
    if (roleParams != null && roleParams.containsKey(ServerAction.ACTION_USER_NAME)) {
      userName = roleParams.get(ServerAction.ACTION_USER_NAME);
    } else {
      userName = m_configuration.getAnonymousAuditName();
      LOG.warn(String.format("Did not receive role parameter %s, will save configs using anonymous username %s", ServerAction.ACTION_USER_NAME, userName));
    }

    CommandReport commandReport = updateDesiredRepositoryVersion(cluster, upgradeContext, userName);
    m_upgradeHelper.publishDesiredRepositoriesUpdates(upgradeContext);
    return commandReport;
  }

  /**
   * Sets the desired repository version for services participating in the
   * upgrade.
   *
   * @param cluster
   *          the cluster
   * @param upgradeContext
   *          the upgrade context
   * @param userName
   *          username performing the action
   * @return the command report to return
   */
  @Transactional
  CommandReport updateDesiredRepositoryVersion(
      Cluster cluster, UpgradeContext upgradeContext, String userName)
      throws AmbariException, InterruptedException {

    StringBuilder out = new StringBuilder();
    StringBuilder err = new StringBuilder();

    try {
      // the desired repository message to put in the command report - this will
      // change based on the type of upgrade and the services participating
      if (upgradeContext.getDirection() == Direction.UPGRADE) {
        final String message;
        RepositoryVersionEntity targetRepositoryVersion = upgradeContext.getRepositoryVersion();

        if (upgradeContext.getOrchestrationType() == RepositoryType.STANDARD) {
          message = MessageFormat.format(
              "Updating the desired repository version to {0} for all cluster services.",
              targetRepositoryVersion.getVersion());
        } else {
          Set<String> servicesInUpgrade = upgradeContext.getSupportedServices();
          message = MessageFormat.format(
              "Updating the desired repository version to {0} for the following services: {1}",
              targetRepositoryVersion.getVersion(), StringUtils.join(servicesInUpgrade, ','));
        }

        out.append(message).append(System.lineSeparator());

        // move the cluster's desired stack as well
        StackId targetStackId = targetRepositoryVersion.getStackId();
        cluster.setDesiredStackVersion(targetStackId);
      }

      if( upgradeContext.getDirection() == Direction.DOWNGRADE ){
        String message = "Updating the desired repository back their original values for the following services:";
        out.append(message).append(System.lineSeparator());

        Map<String, RepositoryVersionEntity> targetVersionsByService = upgradeContext.getTargetVersions();
        for (String serviceName : targetVersionsByService.keySet()) {
          RepositoryVersionEntity repositoryVersion = targetVersionsByService.get(serviceName);

          message = String.format("  %s to %s", serviceName, repositoryVersion.getVersion());
          out.append(message).append(System.lineSeparator());
        }
      }

      // move repositories to the right version and create/revert configs
      m_upgradeHelper.updateDesiredRepositoriesAndConfigs(upgradeContext);

      // a downgrade must force host versions back to INSTALLED for the
      // repository which failed to be upgraded.
      if (upgradeContext.getDirection() == Direction.DOWNGRADE) {
        RepositoryVersionEntity downgradeFromRepositoryVersion = upgradeContext.getRepositoryVersion();
        out.append(String.format("Setting host versions back to %s for repository version %s",
            RepositoryVersionState.INSTALLED, downgradeFromRepositoryVersion.getVersion()));

        List<HostVersionEntity> hostVersionsToReset = m_hostVersionDAO.findHostVersionByClusterAndRepository(
            cluster.getClusterId(), downgradeFromRepositoryVersion);

        for (HostVersionEntity hostVersion : hostVersionsToReset) {
          if( hostVersion.getState() != RepositoryVersionState.NOT_REQUIRED ){
            hostVersion.setState(RepositoryVersionState.INSTALLED);
          }
        }

        // move the cluster's desired stack back to it's current stack on downgrade
        StackId targetStackId = cluster.getCurrentStackVersion();
        cluster.setDesiredStackVersion(targetStackId);
      }

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", out.toString(), err.toString());
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      err.append(sw);

      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", out.toString(), err.toString());
    }
  }
}
