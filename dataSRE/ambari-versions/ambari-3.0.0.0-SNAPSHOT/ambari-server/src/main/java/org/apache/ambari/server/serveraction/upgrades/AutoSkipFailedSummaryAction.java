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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.ServiceComponentHostEventWrapper;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * The {@link AutoSkipFailedSummaryAction} is used to check if any
 * {@link HostRoleCommand}s were skipped automatically after they failed during
 * an upgrade. This will be automatically marked as
 * {@link HostRoleStatus#COMPLETED} if there are no skipped failures. Otherwise
 * it will be placed into {@link HostRoleStatus#HOLDING}.
 */
public class AutoSkipFailedSummaryAction extends AbstractUpgradeServerAction {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AutoSkipFailedSummaryAction.class);

  /**
   * The standard output template message.
   */
  private static final String FAILURE_STD_OUT_TEMPLATE = "There were {0} skipped failure(s) that must be addressed before you can proceed. Please resolve each failure before continuing with the upgrade.";

  private static final String SKIPPED_SERVICE_CHECK = "service_check";
  private static final String SKIPPED_HOST_COMPONENT = "host_component";
  private static final String SKIPPED = "skipped";
  private static final String FAILURES = "failures";

  /**
   * Used to lookup the tasks that need to be checked for
   * {@link HostRoleStatus#SKIPPED_FAILED}.
   */
  @Inject
  private HostRoleCommandDAO m_hostRoleCommandDAO;

  /**
   * Used for writing structured out.
   */
  @Inject
  private Gson m_gson;

  /**
   * Used to look up service check name -> service name bindings
   */
  @Inject
  private ActionMetadata actionMetadata;


  /**
   * A mapping of host -> Map<key,info> for each failure.
   */
  private Map<String, Object> m_structuredFailures = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    HostRoleCommand hostRoleCommand = getHostRoleCommand();
    long requestId = hostRoleCommand.getRequestId();
    long stageId = hostRoleCommand.getStageId();

    String clusterName = hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);

    // use the host role command to get to the parent upgrade group
    UpgradeItemEntity upgradeItem = m_upgradeDAO.findUpgradeItemByRequestAndStage(requestId,stageId);
    UpgradeGroupEntity upgradeGroup = upgradeItem.getGroupEntity();

    // find all of the stages in this group
    long upgradeGroupId = upgradeGroup.getId();
    UpgradeGroupEntity upgradeGroupEntity = m_upgradeDAO.findUpgradeGroup(upgradeGroupId);
    List<UpgradeItemEntity> groupUpgradeItems = upgradeGroupEntity.getItems();
    TreeSet<Long> stageIds = new TreeSet<>();
    for (UpgradeItemEntity groupUpgradeItem : groupUpgradeItems) {
      stageIds.add(groupUpgradeItem.getStageId());
    }

    // for every stage, find all tasks that have been SKIPPED_FAILED - we use a
    // bit of trickery here since within any given request, the stage ID are
    // always sequential. This allows us to make a simple query instead of some
    // overly complex IN or NESTED SELECT query
    long minStageId = stageIds.first();
    long maxStageId = stageIds.last();

    List<HostRoleCommandEntity> skippedTasks = m_hostRoleCommandDAO.findByStatusBetweenStages(
        hostRoleCommand.getRequestId(),
        HostRoleStatus.SKIPPED_FAILED, minStageId, maxStageId);

    if (skippedTasks.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
          "There were no skipped failures", null);
    }

    StringBuilder buffer = new StringBuilder("The following steps failed but were automatically skipped:\n");
    Set<String> skippedCategories = new HashSet<>();
    Map<String, Object> skippedFailures = new HashMap<>();

    Set<String> skippedServiceChecks = new HashSet<>();
    Map<String, Object> hostComponents= new HashMap<>();

    // Internal representation for failed host components
    // To avoid duplicates
    // Format: <hostname, Set<Role>>
    Map<String, Set<Role>> publishedHostComponents= new HashMap<>();

    for (HostRoleCommandEntity skippedTask : skippedTasks) {
      try {
        String skippedCategory;
        if (skippedTask.getRoleCommand().equals(RoleCommand.SERVICE_CHECK)) {
          skippedCategory = SKIPPED_SERVICE_CHECK;

          String serviceCheckActionName = skippedTask.getRole().toString();
          String service = actionMetadata.getServiceNameByServiceCheckAction(serviceCheckActionName);
          skippedServiceChecks.add(service);

          skippedFailures.put(SKIPPED_SERVICE_CHECK, skippedServiceChecks);
          m_structuredFailures.put(FAILURES, skippedFailures);
        } else {
          skippedCategory = SKIPPED_HOST_COMPONENT;

          String hostName = skippedTask.getHostName();
          if (null != hostName) {
            List<Object> failures = (List<Object>) hostComponents.get(hostName);
            if (null == failures) {
              failures = new ArrayList<>();
              hostComponents.put(hostName, failures);

              publishedHostComponents.put(hostName, new HashSet<>());
            }

            Set<Role> publishedHostComponentsOnHost = publishedHostComponents.get(hostName);
            Role role = skippedTask.getRole();
            if (! publishedHostComponentsOnHost.contains(role)) {
              HashMap<String, String> details = new HashMap<>();

              String service = cluster.getServiceByComponentName(role.toString()).getName();

              details.put("service", service);
              details.put("component", role.toString());
              failures.add(details);
            }
          }

          skippedFailures.put(SKIPPED_HOST_COMPONENT, hostComponents);
          m_structuredFailures.put(FAILURES, skippedFailures);
        }

        skippedCategories.add(skippedCategory);

        ServiceComponentHostEventWrapper eventWrapper = new ServiceComponentHostEventWrapper(
          skippedTask.getEvent());

        ServiceComponentHostEvent event = eventWrapper.getEvent();

        buffer.append(event.getServiceComponentName());
        if (null != event.getHostName()) {
          buffer.append(" on ");
          buffer.append(event.getHostName());
        }

        buffer.append(": ");
        buffer.append(skippedTask.getCommandDetail());
        buffer.append("\n");
      } catch (Exception exception) {
        LOG.warn("Unable to extract failure information for {}", skippedTask);
        buffer.append(": ");
        buffer.append(skippedTask);
      }
    }

    m_structuredFailures.put(SKIPPED, skippedCategories);

    String structuredOutput = m_gson.toJson(m_structuredFailures);
    String standardOutput = MessageFormat.format(FAILURE_STD_OUT_TEMPLATE, skippedTasks.size());
    String standardError = buffer.toString();

    return createCommandReport(0, HostRoleStatus.HOLDING, structuredOutput, standardOutput,
        standardError);
  }
}
