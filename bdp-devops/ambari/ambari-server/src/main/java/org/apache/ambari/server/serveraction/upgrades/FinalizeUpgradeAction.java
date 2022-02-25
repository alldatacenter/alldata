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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.StackUpgradeFinishEvent;
import org.apache.ambari.server.events.publishers.VersionEventPublisher;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.repository.AvailableService;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.RepositoryType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.text.StrBuilder;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

/**
 * Action that represents finalizing the Upgrade by completing any database changes.
 */
public class FinalizeUpgradeAction extends AbstractUpgradeServerAction {

  public static final String PREVIOUS_UPGRADE_NOT_COMPLETED_MSG = "It is possible that a previous upgrade was not finalized. " +
      "For this reason, Ambari will not remove any configs. Please ensure that all database records are correct.";

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private VersionEventPublisher versionEventPublisher;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);

    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    if (upgradeContext.getDirection() == Direction.UPGRADE) {
      return finalizeUpgrade(upgradeContext);
    } else {
      return finalizeDowngrade(upgradeContext);
    }
  }

  /**
   * Execution path for upgrade.
   * @return the command report
   */
  private CommandReport finalizeUpgrade(UpgradeContext upgradeContext)
    throws AmbariException, InterruptedException {

    Direction direction = upgradeContext.getDirection();
    RepositoryType repositoryType = upgradeContext.getOrchestrationType();

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    try {
      Cluster cluster = upgradeContext.getCluster();
      RepositoryVersionEntity repositoryVersion = upgradeContext.getRepositoryVersion();
      String version = repositoryVersion.getVersion();

      String message;
      if (upgradeContext.getOrchestrationType() == RepositoryType.STANDARD) {
        message = MessageFormat.format("Finalizing the upgrade to {0} for all cluster services.", version);
      } else {
        Set<String> servicesInUpgrade = upgradeContext.getSupportedServices();

        message = MessageFormat.format(
            "Finalizing the upgrade to {0} for the following services: {1}",
            version, StringUtils.join(servicesInUpgrade, ','));
      }

      outSB.append(message).append(System.lineSeparator());

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      Set<InfoTuple> errors = validateComponentVersions(upgradeContext);
      if (!errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(String.format(
            "The following %d host component(s) "
                + "have not been upgraded to version %s. Please install and upgrade "
                + "the Stack Version on those hosts and try again.\nHost components:",
            errors.size(), version)).append(System.lineSeparator());

        for (InfoTuple error : errors) {
          messageBuff.append(String.format("%s on host %s\n", error.componentName, error.hostName));
        }

        throw new AmbariException(messageBuff.toString());
      }

      // find every host version for this upgrade and ensure it has transitioned
      // to CURRENT if required
      List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
          cluster.getClusterId(), repositoryVersion);

      Set<HostVersionEntity> hostVersionsAllowed = new HashSet<>();
      Set<String> hostsWithoutCorrectVersionState = new HashSet<>();

      // for every host version for this repository, determine if any didn't
      // transition correctly
      for (HostVersionEntity hostVersion : hostVersions) {
        RepositoryVersionState hostVersionState = hostVersion.getState();
        switch (hostVersionState) {
          case CURRENT:
          case NOT_REQUIRED: {
            hostVersionsAllowed.add(hostVersion);
            break;
          }
          default: {
            hostsWithoutCorrectVersionState.add(hostVersion.getHostName());
            break;
          }
        }
      }

      // throw an exception if there are hosts which did not transition the
      // repository to CURRENT
      if (hostsWithoutCorrectVersionState.size() > 0) {
        message = String.format(
            "The following %d host(s) have not been upgraded to version %s. "
                + "Please install and upgrade the Stack Version on those hosts and try again.\nHosts: %s",
            hostsWithoutCorrectVersionState.size(), version,
            StringUtils.join(hostsWithoutCorrectVersionState, ", "));
        outSB.append(message);
        outSB.append(System.lineSeparator());
        throw new AmbariException(message);
      }

      outSB.append(
          String.format("Finalizing the upgrade state and repository version for %d host(s).",
              hostVersionsAllowed.size())).append(System.lineSeparator());

      // at this point, all host versions are correct - do some cleanup like
      // resetting the upgrade state
      for (HostVersionEntity hostVersion : hostVersionsAllowed) {
        Collection<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostVersion.getHostName());
        for (HostComponentStateEntity hostComponentStateEntity: hostComponentStates) {
          hostComponentStateEntity.setUpgradeState(UpgradeState.NONE);
          hostComponentStateDAO.merge(hostComponentStateEntity);
        }
      }

      // move host versions from CURRENT to INSTALLED if their repos are no
      // longer used
      finalizeHostVersionsNotDesired(cluster, upgradeContext);

      if (upgradeContext.getOrchestrationType() == RepositoryType.STANDARD) {
        outSB.append(String.format("Finalizing the version for cluster %s.\n", cluster.getClusterName()));
        cluster.setCurrentStackVersion(cluster.getDesiredStackVersion());
      }

      // mark revertable
      if (repositoryType.isRevertable() && direction == Direction.UPGRADE) {
        UpgradeEntity upgradeEntity = cluster.getUpgradeInProgress();
        upgradeEntity.setRevertAllowed(true);
        upgradeEntity = m_upgradeDAO.merge(upgradeEntity);
      }

      // Reset upgrade state
      cluster.setUpgradeEntity(null);

      // the upgrade is done!
      versionEventPublisher.publish(new StackUpgradeFinishEvent(cluster));

      message = String.format("The upgrade to %s has completed.", version);
      outSB.append(message).append(System.lineSeparator());
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      errSB.append(e.getMessage());
      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }

  /**
   * Execution path for downgrade.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return the command report
   */
  private CommandReport finalizeDowngrade(UpgradeContext upgradeContext)
      throws AmbariException, InterruptedException {

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    try {
      Cluster cluster = upgradeContext.getCluster();
      RepositoryVersionEntity downgradeFromRepositoryVersion = upgradeContext.getRepositoryVersion();
      String downgradeFromVersion = downgradeFromRepositoryVersion.getVersion();
      Set<String> servicesInUpgrade = upgradeContext.getSupportedServices();

      String message;

      if (upgradeContext.getOrchestrationType() == RepositoryType.STANDARD) {
        message = MessageFormat.format(
            "Finalizing the downgrade from {0} for all cluster services.",
            downgradeFromVersion);
      } else {
        message = MessageFormat.format(
            "Finalizing the downgrade from {0} for the following services: {1}",
            downgradeFromVersion, StringUtils.join(servicesInUpgrade, ','));
      }

      outSB.append(message).append(System.lineSeparator());

      // iterate through all host components and make sure that they are on the
      // correct version; if they are not, then this will throw an exception
      Set<InfoTuple> errors = validateComponentVersions(upgradeContext);
      if (!errors.isEmpty()) {
        StrBuilder messageBuff = new StrBuilder(String.format(
            "The following %d host component(s) have not been downgraded to their desired versions:",
            errors.size())).append(System.lineSeparator());

        for (InfoTuple error : errors) {
          messageBuff.append(String.format("%s: %s (current = %s, desired = %s)", error.hostName,
              error.componentName, error.currentVersion, error.targetVersion));

          messageBuff.append(System.lineSeparator());
        }

        throw new AmbariException(messageBuff.toString());
      }

      finalizeHostVersionsNotDesired(cluster, upgradeContext);

      // for every repository being downgraded to, ensure the host versions are correct
      Map<String, RepositoryVersionEntity> targetVersionsByService = upgradeContext.getTargetVersions();
      Set<RepositoryVersionEntity> targetRepositoryVersions = new HashSet<>();
      for (String service : targetVersionsByService.keySet()) {
        targetRepositoryVersions.add(targetVersionsByService.get(service));
      }

      // move host versions in the downgrade back to CURRENT if they are not
      for (RepositoryVersionEntity targetRepositoryVersion : targetRepositoryVersions) {
        // find host versions
        List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
            cluster.getClusterId(), targetRepositoryVersion);

        outSB.append(String.format("Finalizing %d host(s) back to %s", hostVersions.size(),
            targetRepositoryVersion.getVersion())).append(System.lineSeparator());

        for (HostVersionEntity hostVersion : hostVersions) {
          if (hostVersion.getState() != RepositoryVersionState.CURRENT) {
            hostVersion.setState(RepositoryVersionState.CURRENT);
            hostVersionDAO.merge(hostVersion);
          }

          List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(
              hostVersion.getHostName());

          for (HostComponentStateEntity hostComponentState : hostComponentStates) {
            hostComponentState.setUpgradeState(UpgradeState.NONE);
            hostComponentStateDAO.merge(hostComponentState);
          }
        }
      }

      // remove any configurations for services which crossed a stack boundary
      for (String serviceName : servicesInUpgrade) {
        RepositoryVersionEntity sourceRepositoryVersion = upgradeContext.getSourceRepositoryVersion(serviceName);
        RepositoryVersionEntity targetRepositoryVersion = upgradeContext.getTargetRepositoryVersion(serviceName);
        StackId sourceStackId = sourceRepositoryVersion.getStackId();
        StackId targetStackId = targetRepositoryVersion.getStackId();
        // only work with configurations when crossing stacks
        if (!sourceStackId.equals(targetStackId)) {
          outSB.append(
              String.format("Removing %s configurations for %s", sourceStackId,
                  serviceName)).append(System.lineSeparator());

          cluster.removeConfigurations(sourceStackId, serviceName);
        }
      }

      // ensure that when downgrading, we set the desired back to the
      // original value
      versionEventPublisher.publish(new StackUpgradeFinishEvent(cluster));

      // Reset upgrade state
      cluster.setUpgradeEntity(null);

      message = String.format("The downgrade from %s has completed.", downgradeFromVersion);
      outSB.append(message).append(System.lineSeparator());

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      errSB.append(sw);

      return createCommandReport(-1, HostRoleStatus.FAILED, "{}", outSB.toString(), errSB.toString());
    }
  }

  /**
   * Gets any host components which have not been propertly upgraded or
   * downgraded.
   *
   * @param upgradeContext
   *          the upgrade context (not {@code null}).
   * @return a list of {@link InfoTuple} representing components which should
   *         have been upgraded but did not.
   */
  protected Set<InfoTuple> validateComponentVersions(UpgradeContext upgradeContext)
      throws AmbariException {

    Set<InfoTuple> errors = new TreeSet<>();

    Cluster cluster = upgradeContext.getCluster();
    Set<String> servicesParticipating = upgradeContext.getSupportedServices();
    for (String serviceName : servicesParticipating) {
      Service service = cluster.getService(serviceName);
      RepositoryVersionEntity repositoryVersionEntity = upgradeContext.getTargetRepositoryVersion(serviceName);
      StackId targetStackId = repositoryVersionEntity.getStackId();
      String targetVersion = repositoryVersionEntity.getVersion();

      for (ServiceComponent serviceComponent : service.getServiceComponents().values()) {
        ComponentInfo componentInfo = ambariMetaInfo.getComponent(targetStackId.getStackName(),
            targetStackId.getStackVersion(), service.getName(), serviceComponent.getName());

        if (!componentInfo.isVersionAdvertised()) {
          continue;
        }

        for (ServiceComponentHost serviceComponentHost : serviceComponent.getServiceComponentHosts().values()) {
          if (!StringUtils.equals(targetVersion, serviceComponentHost.getVersion())) {
            errors.add(new InfoTuple(service.getName(), serviceComponent.getName(),
                serviceComponentHost.getHostName(), serviceComponentHost.getVersion(),
                targetVersion));
          }
        }
      }
    }


    return errors;
  }

  /**
   * Moves any {@link HostVersionEntity}s which are
   * {@link RepositoryVersionState#CURRENT} to either
   * {@link RepositoryVersionState#INSTALLED} or
   * {@link RepositoryVersionState#NOT_REQUIRED} if their assocaited
   * repositories are no longer in use.
   * <p/>
   * If this is a patch reversion, then this method will also attempt to
   * determine if the downgrade-from repository needs to be set to
   * {@link RepositoryVersionState#OUT_OF_SYNC}. If a patch upgrade was
   * completed and then a service added after that happens to be specified in
   * the VDF, then we must mark the repository as OUT_OF_SYNC since those
   * service's packages were never distributed.
   *
   * @param cluster
   * @throws AmbariException
   */
  private void finalizeHostVersionsNotDesired(Cluster cluster, UpgradeContext upgradeContext)
      throws AmbariException {
    // create a set of all of the repos that the services are on
    Set<RepositoryVersionEntity> desiredRepoVersions = new HashSet<>();
    Set<String> serviceNames = cluster.getServices().keySet();
    for (String serviceName : serviceNames) {
      Service service = cluster.getService(serviceName);
      desiredRepoVersions.add(service.getDesiredRepositoryVersion());
    }

    // if any CURRENT host version is for a repo which is no longer desired by
    // ANY service, move it to INSTALLED
    List<HostVersionEntity> currentHostVersions = hostVersionDAO.findByClusterAndState(
        cluster.getClusterName(), RepositoryVersionState.CURRENT);

    for (HostVersionEntity hostVersionEntity : currentHostVersions) {
      RepositoryVersionEntity hostRepoVersion = hostVersionEntity.getRepositoryVersion();
      if (!desiredRepoVersions.contains(hostRepoVersion)) {
        hostVersionEntity.setState(RepositoryVersionState.INSTALLED);
        hostVersionEntity = hostVersionDAO.merge(hostVersionEntity);
      }
    }

    // reverting a patch requires us to check the repository state and
    // possibly set it to OUT_OF_SYNC if services were added after this patch
    // was originally distributed
    if (upgradeContext.isPatchRevert()) {
      RepositoryVersionEntity repositoryVersionEntity = upgradeContext.getRepositoryVersion();

      final VersionDefinitionXml vdfXml;
      try {
        vdfXml = repositoryVersionEntity.getRepositoryXml();
      } catch (Exception exception) {
        throw new AmbariException("The VDF's XML could not be deserialized", exception);
      }

      StackInfo stack = ambariMetaInfo.getStack(repositoryVersionEntity.getStackId());

      // grab the services in the VDF, the services which were part of the
      // revert, and the services in the cluster to determine if a service was
      // added to the cluster after the patch was originally applied
      Collection<AvailableService> availableServices = vdfXml.getAvailableServices(stack);
      Set<String> participatingServices = upgradeContext.getSupportedServices();
      Set<String> clusterServices = cluster.getServices().keySet();

      boolean resetRepoStateToOutOfSync = false;
      for (AvailableService availableService : availableServices) {
        if (clusterServices.contains(availableService.getName())
            && !participatingServices.contains(availableService.getName())) {
          resetRepoStateToOutOfSync = true;
          break;
        }
      }

      if (resetRepoStateToOutOfSync) {
        List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
            cluster.getClusterId(), repositoryVersionEntity);
        for (HostVersionEntity hostVersion : hostVersions) {
          hostVersion.setState(RepositoryVersionState.OUT_OF_SYNC);
          hostVersion = hostVersionDAO.merge(hostVersion);
        }
      }
    }
  }

  protected static class InfoTuple implements Comparable<InfoTuple> {
    protected final String serviceName;
    protected final String componentName;
    protected final String hostName;
    protected final String currentVersion;
    protected final String targetVersion;

    protected InfoTuple(String service, String component, String host, String version,
        String desiredVersion) {
      serviceName = service;
      componentName = component;
      hostName = host;
      currentVersion = version;
      targetVersion = desiredVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(InfoTuple that) {
      int compare = hostName.compareTo(that.hostName);
      if (compare != 0) {
        return compare;
      }

      compare = serviceName.compareTo(that.serviceName);
      if (compare != 0) {
        return compare;
      }

      compare = componentName.compareTo(that.componentName);
      if (compare != 0) {
        return compare;
      }

      return compare;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(hostName, serviceName, componentName, currentVersion, targetVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }

      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      InfoTuple that = (InfoTuple) object;

      EqualsBuilder equalsBuilder = new EqualsBuilder();
      equalsBuilder.append(hostName, that.hostName);
      equalsBuilder.append(serviceName, that.serviceName);
      equalsBuilder.append(componentName, that.componentName);
      equalsBuilder.append(currentVersion, that.currentVersion);
      equalsBuilder.append(targetVersion, that.targetVersion);
      ;
      return equalsBuilder.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("host", hostName)
          .add("component", componentName)
          .add("current", currentVersion)
          .add("target", targetVersion).toString();
    }
  }
}
