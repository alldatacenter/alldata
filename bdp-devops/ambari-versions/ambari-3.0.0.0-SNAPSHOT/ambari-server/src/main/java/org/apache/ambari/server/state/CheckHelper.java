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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.checks.OrchestrationQualification;
import org.apache.ambari.server.checks.UpgradeTypeQualification;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.CheckQualification;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class CheckHelper {
  /**
   * Log.
   */
  private static final Logger LOG = LoggerFactory.getLogger(CheckHelper.class);

  /**
   * Used for retrieving {@link RepositoryVersionEntity} instances.
   */
  @Inject
  protected Provider<RepositoryVersionDAO> repositoryVersionDaoProvider;

  /**
   * Used for retrieving {@link Cluster} instances.
   */
  @Inject
  protected Provider<Clusters> clustersProvider;

  @Inject
  protected Provider<AmbariMetaInfo> metaInfoProvider;


  /**
   * Gets any {@link UpgradeCheck}s which have passed all of their {@link CheckQualification}s
   * for the given upgrade request.
   *
   * @param request
   * @param upgradeChecks
   * @return
   */
  public List<UpgradeCheck> getApplicableChecks(UpgradeCheckRequest request,
      List<UpgradeCheck> upgradeChecks) {

    List<UpgradeCheck> applicableChecks = new LinkedList<>();
    for (UpgradeCheck check : upgradeChecks) {
      // build some required qualifications
      List<CheckQualification> qualifications = Lists.newArrayList(
          new ServiceQualification(check),
          new OrchestrationQualification(check.getClass()),
          new UpgradeTypeQualification(check.getClass()));

      // add any extras from the check
      List<CheckQualification> checkQualifications = check.getQualifications();
      if (CollectionUtils.isNotEmpty(checkQualifications)) {
        qualifications.addAll(checkQualifications);
      }

      // run the applicability check
      try {
        boolean checkIsApplicable = true;
        for (CheckQualification qualification : qualifications) {
          if (!qualification.isApplicable(request)) {
            checkIsApplicable = false;
            break;
          }
        }

        if (checkIsApplicable) {
          applicableChecks.add(check);
        }
      } catch (Exception ex) {
        LOG.error(
            "Unable to determine whether the pre-upgrade check {} is applicable to this upgrade",
            check.getCheckDescription().name(), ex);
      }
    }

    return applicableChecks;
  }

  /**
   * Executes all registered pre-requisite checks.
   *
   * @param request
   *          pre-requisite check request
   * @return list of pre-requisite check results
   */
  public List<UpgradeCheckResult> performChecks(UpgradeCheckRequest request,
                                               List<UpgradeCheck> upgradeChecks, Configuration config) {

    final String clusterName = request.getClusterName();
    final List<UpgradeCheckResult> results = new ArrayList<>();
    final boolean canBypassPreChecks = config.isUpgradePrecheckBypass();

    List<UpgradeCheck> applicablePreChecks = getApplicableChecks(request, upgradeChecks);

    for (UpgradeCheck check : applicablePreChecks) {
      UpgradeCheckResult result = new UpgradeCheckResult(check);
      try {
        result = check.perform(request);
      } catch (ClusterNotFoundException ex) {
        result.setFailReason("Cluster with name " + clusterName + " doesn't exists");
        result.setStatus(UpgradeCheckStatus.FAIL);
      } catch (Exception ex) {
        LOG.error("Check " + check.getCheckDescription().name() + " failed", ex);
        result.setFailReason("Unexpected server error happened");
        result.setStatus(UpgradeCheckStatus.FAIL);
      }

      if (result.getStatus() == UpgradeCheckStatus.FAIL && canBypassPreChecks) {
        LOG.error("Check {} failed but stack upgrade is allowed to bypass failures. Error to bypass: {}. Failed on: {}",
          check.getCheckDescription().name(),
          result.getFailReason(),
          StringUtils.join(result.getFailedOn(), ", "));
        result.setStatus(UpgradeCheckStatus.BYPASS);
      }

      results.add(result);
      request.addResult(check.getCheckDescription(), result.getStatus());
    }

    return results;
  }

  /**
   * Gets a de-serialized {@link VersionDefinitionXml} from the repository for
   * this upgrade.
   *
   * @param request
   *          the upgrade check request.
   * @return the VDF XML
   * @throws AmbariException
   */
  public final VersionDefinitionXml getVersionDefinitionXml(UpgradeCheckRequest request) throws AmbariException {
    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();
    RepositoryVersionEntity entity = repositoryVersionDaoProvider.get().findByPK(
        repositoryVersion.getId());

    try {
      VersionDefinitionXml vdf = entity.getRepositoryXml();
      return vdf;
    } catch (Exception exception) {
      throw new AmbariException("Unable to run upgrade checks because of an invalid VDF",
          exception);
    }
  }

  /**
   * Gets the services participating in the upgrade from the VDF.
   *
   * @param request
   *          the upgrade check request.
   * @return the services participating in the upgrade, which can either be all
   *         of the cluster's services or a subset based on repository type.
   */
  public final Set<String> getServicesInUpgrade(UpgradeCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    // the check is scoped to some services, so determine if any of those
    // services are included in this upgrade
    try {
      VersionDefinitionXml vdf = getVersionDefinitionXml(request);
      ClusterVersionSummary clusterVersionSummary = vdf.getClusterSummary(cluster,
          metaInfoProvider.get());
      return clusterVersionSummary.getAvailableServiceNames();
    } catch (Exception exception) {
      throw new AmbariException("Unable to run upgrade checks because of an invalid VDF",
          exception);
    }
  }

  /**
   * The {@link ServiceQualification} class is used to determine if the
   * service(s) associated with an upgraade check are both installed in the
   * cluster and included in thr upgrade.
   * <p/>
   * If a service is installed but not included in the upgrade (for example of
   * the upgrade is a patch upgrade), then the check should not qualify to run.
   */
  final class ServiceQualification implements CheckQualification {

    /**
     *
     */
    private final UpgradeCheck m_upgradeCheck;

    /**
     * Constructor.
     *
     * @param upgradeCheck
     */
    public ServiceQualification(UpgradeCheck upgradeCheck) {
      m_upgradeCheck = upgradeCheck;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(UpgradeCheckRequest request) throws AmbariException {

      Set<String> applicableServices = m_upgradeCheck.getApplicableServices();

      // if the check is not scoped to any particular service, then it passes
      // this qualification
      if (applicableServices.isEmpty()) {
        return true;
      }

      Set<String> servicesForUpgrade = getServicesInUpgrade(request);

      for (String serviceInUpgrade : servicesForUpgrade) {
        if (applicableServices.contains(serviceInUpgrade)) {
          return true;
        }
      }

      return false;
    }
  }
}
