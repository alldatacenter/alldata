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

import java.text.MessageFormat;
import java.util.LinkedHashSet;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.state.Cluster;
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
 * The {@link PreviousUpgradeCompleted} class is used to determine if there is a
 * prior upgrade or downgrade which has not completed. If the most recent
 * upgrade/downgrade is in a non-completed state, then this check will fail.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.DEFAULT,
    order = 4.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class PreviousUpgradeCompleted extends ClusterCheck {

  static final UpgradeCheckDescription PREVIOUS_UPGRADE_COMPLETED = new UpgradeCheckDescription("PREVIOUS_UPGRADE_COMPLETED",
      UpgradeCheckType.CLUSTER,
      "A previous upgrade did not complete.",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The last upgrade attempt did not complete. {{fails}}").build());

  /**
   * The message displayed as part of this pre-upgrade check.
   */
  public static final String ERROR_MESSAGE = "There is an existing {0} {1} {2} which has not completed. This {3} must be completed before a new upgrade or downgrade can begin.";

  /**
   * Constructor.
   */
  public PreviousUpgradeCompleted() {
    super(PREVIOUS_UPGRADE_COMPLETED);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);

    String errorMessage = null;
    UpgradeEntity upgradeInProgress = cluster.getUpgradeInProgress();
    if (null != upgradeInProgress) {
      Direction direction = upgradeInProgress.getDirection();
      String directionText = direction.getText(false);
      String prepositionText = direction.getPreposition();

      errorMessage = MessageFormat.format(ERROR_MESSAGE, directionText, prepositionText,
          upgradeInProgress.getRepositoryVersion().getVersion(), directionText);
    }

    if (null != errorMessage) {
      LinkedHashSet<String> failedOn = new LinkedHashSet<>();
      failedOn.add(cluster.getClusterName());
      result.setFailedOn(failedOn);
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(errorMessage);
    }

    return result;
  }
}
