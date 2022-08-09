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
package org.apache.ambari.server.events.listeners.upgrade;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.HostComponentVersionAdvertisedEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link StackVersionListener} class handles the propagation of versions
 * advertised by the {@link org.apache.ambari.server.state.ServiceComponentHost}
 * that bubble up to the
 * {@link org.apache.ambari.server.orm.entities.HostVersionEntity}.
 */
@Singleton
@EagerSingleton
public class StackVersionListener {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackVersionListener.class);
  public static final String UNKNOWN_VERSION = State.UNKNOWN.toString();

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  /**
   * Used for looking up a component's advertising version status given a stack
   * and name.
   */
  private Provider<AmbariMetaInfo> ambariMetaInfoProvider;

  private final Provider<HostLevelParamsHolder> m_hostLevelParamsHolder;

  /**
   * Constructor.
   *
   * @param eventPublisher  the publisher
   */
  @Inject
  public StackVersionListener(VersionEventPublisher eventPublisher,
                              Provider<AmbariMetaInfo> ambariMetaInfoProvider,
                              Provider<HostLevelParamsHolder> m_hostLevelParamsHolder) {
    this.ambariMetaInfoProvider = ambariMetaInfoProvider;
    this.m_hostLevelParamsHolder = m_hostLevelParamsHolder;
    eventPublisher.register(this);
  }

  @Subscribe
  public void onAmbariEvent(HostComponentVersionAdvertisedEvent event) {
    LOG.debug("Received event {}", event);

    Cluster cluster = event.getCluster();

    ServiceComponentHost sch = event.getServiceComponentHost();

    String newVersion = event.getVersion();
    if (StringUtils.isEmpty(newVersion)) {
      return;
    }

    // if the cluster is upgrading, there's no need to update the repo version -
    // it better be right
    if (null != event.getRepositoryVersionId() && null == cluster.getUpgradeInProgress()) {
      // !!! make sure the repo_version record actually has the same version.
      // This is NOT true when installing a cluster using a public repo where the
      // exact version is not known in advance.
      RepositoryVersionEntity rve = repositoryVersionDAO.findByPK(event.getRepositoryVersionId());
      if (null != rve) {
        String currentRepoVersion = rve.getVersion();
        if (!StringUtils.equals(currentRepoVersion, newVersion)) {
          rve.setVersion(newVersion);
          rve.setResolved(false);
        }
        // the reported versions are the same - we should ensure that the repo is resolved
        if (!rve.isResolved()) {
          rve.setResolved(true);
          repositoryVersionDAO.merge(rve);
        }
      }
    }

    // Update host component version value if needed
    try {
      // get the component information for the desired stack; if a component
      // moves from UNKNOWN to providing a version, we must do the version
      // advertised check against the target stack
      StackId desiredStackId = sch.getDesiredStackId();

      AmbariMetaInfo ambariMetaInfo = ambariMetaInfoProvider.get();
      ComponentInfo componentInfo = ambariMetaInfo.getComponent(desiredStackId.getStackName(),
          desiredStackId.getStackVersion(), sch.getServiceName(), sch.getServiceComponentName());

      // not advertising a version, do nothing
      if (!componentInfo.isVersionAdvertised()) {
        // that's odd; a version came back - log it and still do nothing
        if (!StringUtils.equalsIgnoreCase(UNKNOWN_VERSION, newVersion)) {
          LOG.warn(
              "ServiceComponent {} doesn't advertise version, however ServiceHostComponent {} on host {} advertised version as {}. Skipping version update",
              sch.getServiceComponentName(), sch.getServiceComponentName(), sch.getHostName(),
              newVersion);
        }
        return;
      }

      ServiceComponent sc = cluster.getService(sch.getServiceName()).getServiceComponent(
          sch.getServiceComponentName());

      // proces the UNKNOWN version
      if (StringUtils.equalsIgnoreCase(UNKNOWN_VERSION, newVersion)) {
        processUnknownDesiredVersion(cluster, sc, sch, newVersion);
        return;
      }

      processComponentAdvertisedVersion(cluster, sc, sch, newVersion);
    } catch (Exception e) {
      LOG.error(
          "Unable to propagate version for ServiceHostComponent on component: {}, host: {}. Error: {}",
          sch.getServiceComponentName(), sch.getHostName(), e.getMessage());
    }
  }


  /**
   * Updates the version and {@link UpgradeState} for the specified
   * {@link ServiceComponentHost} if necessary. If the version or the upgrade
   * state changes, then this method will call
   * {@link ServiceComponentHost#recalculateHostVersionState()} in order to
   * ensure that the host version state is properly updated.
   * <p/>
   *
   *
   * @param cluster
   * @param sc
   * @param sch
   * @param newVersion
   * @throws AmbariException
   */
  private void processComponentAdvertisedVersion(Cluster cluster, ServiceComponent sc,
      ServiceComponentHost sch, String newVersion) throws AmbariException {
    if (StringUtils.isBlank(newVersion)) {
      return;
    }

    String previousVersion = sch.getVersion();
    String desiredVersion = sc.getDesiredVersion();
    UpgradeState upgradeState = sch.getUpgradeState();

    boolean versionIsCorrect = StringUtils.equals(desiredVersion, newVersion);

    // update the SCH to the new version reported only if it changed
    if (!StringUtils.equals(previousVersion, newVersion)) {
      sch.setVersion(newVersion);
    }

    if (previousVersion == null || StringUtils.equalsIgnoreCase(UNKNOWN_VERSION, previousVersion)) {
      // value may be "UNKNOWN" when upgrading from older Ambari versions
      // or if host component reports it's version for the first time
      sch.setUpgradeState(UpgradeState.NONE);
      sch.recalculateHostVersionState();
    } else {
      if (versionIsCorrect) {
        boolean isUpgradeInProgressForThisComponent = null != cluster.getUpgradeInProgress()
            && upgradeState != UpgradeState.NONE;

        if (isUpgradeInProgressForThisComponent) {
          setUpgradeStateAndRecalculateHostVersions(sch, UpgradeState.COMPLETE);
        } else {
          // no upgrade in progress for this component, then this should always
          // be NONE
          setUpgradeStateAndRecalculateHostVersions(sch, UpgradeState.NONE);
        }
      } else {
        // if the versions don't match for any reason, regardless of upgrade
        // state, then VERSION_MISMATCH it
        setUpgradeStateAndRecalculateHostVersions(sch, UpgradeState.VERSION_MISMATCH);
      }
    }

    sc.updateRepositoryState(newVersion);
  }

  /**
   * Possible situation after upgrade from older Ambari version. Just use
   * reported component version as desired version
   * @param cluster target cluster
   * @param sc target service component
   * @param sch target host component
   * @param newVersion advertised version
   */
  private void processUnknownDesiredVersion(Cluster cluster, ServiceComponent sc,
                                            ServiceComponentHost sch,
                                            String newVersion) throws AmbariException {
    sch.setUpgradeState(UpgradeState.NONE);
    sch.setVersion(newVersion);
    sch.recalculateHostVersionState();
  }

  /**
   * @param sch
   * @param upgradeState
   * @throws AmbariException
   */
  private void setUpgradeStateAndRecalculateHostVersions(ServiceComponentHost sch,
      UpgradeState upgradeState) throws AmbariException {

    if (sch.getUpgradeState() == upgradeState) {
      return;
    }

    // if the upgrade state changes, then also recalculate host versions
    sch.setUpgradeState(upgradeState);
    sch.recalculateHostVersionState();
  }
}
