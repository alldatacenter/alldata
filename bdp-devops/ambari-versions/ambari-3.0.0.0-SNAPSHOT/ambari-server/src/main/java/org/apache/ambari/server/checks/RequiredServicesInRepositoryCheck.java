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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * The {@link RequiredServicesInRepositoryCheck} is used to ensure that if there
 * are any services which require other services to also be included in the
 * upgrade that they are included in the repository.
 * <p/>
 * This check is to prevent problems which can be caused by trying to patch
 * upgrade services which have known depdenencies on other services because of
 * things like hard coded versions.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.REPOSITORY_VERSION,
    order = 1.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED },
    orchestration = { RepositoryType.PATCH, RepositoryType.MAINT, RepositoryType.SERVICE })
public class RequiredServicesInRepositoryCheck extends ClusterCheck {

  static final UpgradeCheckDescription VALID_SERVICES_INCLUDED_IN_REPOSITORY = new UpgradeCheckDescription("VALID_SERVICES_INCLUDED_IN_REPOSITORY",
      UpgradeCheckType.CLUSTER,
      "The repository is missing services which are required",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following services are included in the upgrade but the repository is missing their dependencies:\n%s").build());

  /**
   * Constructor.
   */
  public RequiredServicesInRepositoryCheck() {
    super(VALID_SERVICES_INCLUDED_IN_REPOSITORY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    String clusterName = request.getClusterName();
    Cluster cluster = clustersProvider.get().getCluster(clusterName);

    VersionDefinitionXml xml = checkHelperProvider.get().getVersionDefinitionXml(request);

    Set<String> missingDependencies = xml.getMissingDependencies(cluster, ambariMetaInfo.get());

    if (!missingDependencies.isEmpty()) {
      String failReasonTemplate = getFailReason(result, request);

      String message = String.format(
          "The following services are also required to be included in this upgrade: %s",
          StringUtils.join(missingDependencies, ", "));

      result.setFailedOn(new LinkedHashSet<>(missingDependencies));
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(String.format(failReasonTemplate, message));

      Set<ServiceDetail> missingServiceDetails = missingDependencies.stream().map(
          missingService -> new ServiceDetail(missingService)).collect(
              Collectors.toCollection(TreeSet::new));

      result.getFailedDetail().addAll(missingServiceDetails);
      return result;
    }

    result.setStatus(UpgradeCheckStatus.PASS);
    return result;
  }
}
