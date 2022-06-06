/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.checks;

import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.state.MaintenanceState.OFF;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
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
import com.google.inject.Singleton;

/**
 * This checks if the source and target version has an entry for each OS type in the cluster.
 */
@Singleton
@UpgradeCheckInfo(
  group = UpgradeCheckGroup.REPOSITORY_VERSION,
  required = { UpgradeType.NON_ROLLING, UpgradeType.ROLLING })
public class MissingOsInRepoVersionCheck extends ClusterCheck {
  public static final String SOURCE_OS = "source_os";
  public static final String TARGET_OS = "target_os";

  static final UpgradeCheckDescription MISSING_OS_IN_REPO_VERSION = new UpgradeCheckDescription("MISSING_OS_IN_REPO_VERSION",
      UpgradeCheckType.CLUSTER,
      "Missing OS in repository version.",
      new ImmutableMap.Builder<String, String>()
        .put(MissingOsInRepoVersionCheck.SOURCE_OS, "The source version must have an entry for each OS type in the cluster")
        .put(MissingOsInRepoVersionCheck.TARGET_OS, "The target version must have an entry for each OS type in the cluster")
        .build());

  public MissingOsInRepoVersionCheck() {
    super(MISSING_OS_IN_REPO_VERSION);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    Set<String> osFamiliesInCluster = osFamiliesInCluster(cluster(request));
    if (!targetOsFamilies(request).containsAll(osFamiliesInCluster)) {
      result.setFailReason(getFailReason(TARGET_OS, result, request));
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailedOn(new LinkedHashSet<>(osFamiliesInCluster));
    } else if (!sourceOsFamilies(request).containsAll(osFamiliesInCluster)) {
      result.setFailReason(getFailReason(SOURCE_OS, result, request));
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailedOn(new LinkedHashSet<>(osFamiliesInCluster));
    }

    return result;
  }

  private Cluster cluster(UpgradeCheckRequest prerequisiteCheck) throws AmbariException {
    return clustersProvider.get().getCluster(prerequisiteCheck.getClusterName());
  }

  /**
   * @return set of each os family in the cluster, excluding hosts which are in maintenance state
   */
  private Set<String> osFamiliesInCluster(Cluster cluster) {
    return cluster.getHosts().stream()
      .filter(host -> host.getMaintenanceState(cluster.getClusterId()) == OFF)
      .map(Host::getOsFamily)
      .collect(toSet());
  }

  /**
   * @return set of each os family in the source stack
   */
  private Set<String> sourceOsFamilies(UpgradeCheckRequest request) throws AmbariException {
    StackId stackId = new StackId(request.getTargetRepositoryVersion().getStackId());
    return ambariMetaInfo.get().getStack(stackId).getRepositoriesByOs().keySet();
  }

  /**
   * @return set of each os family in the target repository
   */
  private Set<String> targetOsFamilies(UpgradeCheckRequest request) {
    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();
    RepositoryVersionEntity entity = repositoryVersionDaoProvider.get().findByPK(
        repositoryVersion.getId());

    return entity
      .getRepoOsEntities()
      .stream()
      .map(RepoOsEntity::getFamily)
      .collect(toSet());
  }
}
