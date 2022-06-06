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
package org.apache.ambari.server.checks;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.serveraction.upgrades.DeleteUnsupportedServicesAndComponents;
import org.apache.ambari.server.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.ServerActionTask;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentSupport;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link ComponentsExistInRepoCheck} is used to determine if any of the
 * components scheduled for upgrade do not exist in the target repository or
 * stack.
 */
@Singleton
@UpgradeCheckInfo(
  group = UpgradeCheckGroup.INFORMATIONAL_WARNING,
  required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING })
public class ComponentsExistInRepoCheck extends ClusterCheck {
  public static final String AUTO_REMOVE = "auto_remove";
  public static final String MANUAL_REMOVE = "manual_remove";
  @Inject
  ServiceComponentSupport serviceComponentSupport;
  @Inject
  UpgradeHelper upgradeHelper;

  static final UpgradeCheckDescription COMPONENTS_EXIST_IN_TARGET_REPO = new UpgradeCheckDescription("COMPONENTS_EXIST_IN_TARGET_REPO",
      UpgradeCheckType.CLUSTER,
      "Check installed services which are not supported in the installed stack",
      new ImmutableMap.Builder<String, String>()
        .put(ComponentsExistInRepoCheck.AUTO_REMOVE, "The following services and/or components do not exist in the target stack and will be automatically removed during the upgrade.")
        .put(ComponentsExistInRepoCheck.MANUAL_REMOVE, "The following components do not exist in the target repository's stack. They must be removed from the cluster before upgrading.")
        .build()
      );

  public ComponentsExistInRepoCheck() {
    super(COMPONENTS_EXIST_IN_TARGET_REPO);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);
    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();
    StackId stackId = new StackId(repositoryVersion.getStackId());

    Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    String stackName = stackId.getStackName();
    String stackVersion = stackId.getStackVersion();
    Collection<String> allUnsupported = serviceComponentSupport.allUnsupported(cluster, stackName, stackVersion);

    report(result, request, allUnsupported);

    return result;
  }

  private void report(UpgradeCheckResult result, UpgradeCheckRequest request, Collection<String> allUnsupported) throws AmbariException {
    if (allUnsupported.isEmpty()) {
      result.setStatus(UpgradeCheckStatus.PASS);
      return;
    }
    result.setFailedOn(new LinkedHashSet<>(allUnsupported));
    if (hasDeleteUnsupportedServicesAction(upgradePack(request))) {
      result.setStatus(UpgradeCheckStatus.WARNING);
      result.setFailReason(getFailReason(AUTO_REMOVE, result, request));
    } else {
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(getFailReason(MANUAL_REMOVE, result, request));
    }
  }

  private UpgradePack upgradePack(UpgradeCheckRequest request) throws AmbariException {
    Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    return upgradeHelper.suggestUpgradePack(
      request.getClusterName(),
      cluster.getCurrentStackVersion(),
      new StackId(request.getTargetRepositoryVersion().getStackId()),
      Direction.UPGRADE,
      request.getUpgradeType(),
      null);
  }

  private boolean hasDeleteUnsupportedServicesAction(UpgradePack upgradePack) {
    return upgradePack.getAllGroups().stream()
      .filter(ClusterGrouping.class::isInstance)
      .flatMap(group -> ((ClusterGrouping) group).executionStages.stream())
      .map(executeStage -> executeStage.task)
      .anyMatch(this::isDeleteUnsupportedTask);
  }

  private boolean isDeleteUnsupportedTask(Task task) {
    return task instanceof ServerActionTask
      && DeleteUnsupportedServicesAndComponents.class.getName().equals(((ServerActionTask)task).getImplementationClass());
  }
}
