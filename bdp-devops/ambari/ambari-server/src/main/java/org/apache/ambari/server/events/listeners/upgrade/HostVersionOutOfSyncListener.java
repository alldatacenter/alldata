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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.HostsAddedEvent;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.logging.LockFactory;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link org.apache.ambari.server.events.listeners.upgrade.HostVersionOutOfSyncListener} class
 * handles {@link org.apache.ambari.server.events.ServiceInstalledEvent} and
 * {@link org.apache.ambari.server.events.ServiceComponentInstalledEvent}
 * to update {@link org.apache.ambari.server.state.RepositoryVersionState}
 */
@Singleton
@EagerSingleton
public class HostVersionOutOfSyncListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(HostVersionOutOfSyncListener.class);

  @Inject
  private Provider<HostVersionDAO> hostVersionDAO;

  @Inject
  private Provider<HostDAO> hostDAO;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  private Provider<AmbariMetaInfo> ami;

  @Inject
  private Provider<RepositoryVersionDAO> repositoryVersionDAO;

  /**
   * The publisher may be an asynchronous, multi-threaded one, so to avoid the (rare, but possible) case
   * of both an Install and Uninstall event occurring at the same time, we use a Lock.
   */
  private final Lock m_lock;

  @Inject
  public HostVersionOutOfSyncListener(AmbariEventPublisher ambariEventPublisher, LockFactory lockFactory) {
    ambariEventPublisher.register(this);

    m_lock = lockFactory.newLock("hostVersionOutOfSyncListenerLock");
  }

  @Subscribe
  @Transactional
  public void onServiceComponentEvent(ServiceComponentInstalledEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    m_lock.lock();

    try {
      Cluster cluster = clusters.get().getClusterById(event.getClusterId());
      List<HostVersionEntity> hostVersionEntities =
          hostVersionDAO.get().findByClusterAndHost(cluster.getClusterName(), event.getHostName());

      Service service = cluster.getService(event.getServiceName());
      ServiceComponent serviceComponent = service.getServiceComponent(event.getComponentName());
      RepositoryVersionEntity componentRepo = serviceComponent.getDesiredRepositoryVersion();

      for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
        StackEntity hostStackEntity = hostVersionEntity.getRepositoryVersion().getStack();
        StackId hostStackId = new StackId(hostStackEntity);

        // If added components do not advertise version, it makes no sense to mark version OUT_OF_SYNC
        // We perform check per-stack version, because component may be not versionAdvertised in current
        // stack, but become versionAdvertised in some future (installed, but not yet upgraded to) stack
        String serviceName = event.getServiceName();
        String componentName = event.getComponentName();

        // Skip lookup if stack does not contain the component
        if (!ami.get().isValidServiceComponent(hostStackId.getStackName(),
            hostStackId.getStackVersion(), serviceName, componentName)) {
          LOG.debug("Component not found is host stack, stack={}, version={}, service={}, component={}",
              hostStackId.getStackName(), hostStackId.getStackVersion(), serviceName, componentName);
          continue;
        }
        ComponentInfo component = ami.get().getComponent(hostStackId.getStackName(),
                hostStackId.getStackVersion(), serviceName, componentName);

        if (!component.isVersionAdvertised()) {
          RepositoryVersionState state = checkAllHostComponents(hostStackId, hostVersionEntity.getHostEntity());
          if (null != state) {
            hostVersionEntity.setState(state);
            hostVersionDAO.get().merge(hostVersionEntity);
          }
          continue;
        }

        // !!! we shouldn't be changing other versions to OUT_OF_SYNC if the event
        // component repository doesn't match
        if (!hostVersionEntity.getRepositoryVersion().equals(componentRepo)) {
          continue;
        }

        switch (hostVersionEntity.getState()) {
          case INSTALLED:
          case NOT_REQUIRED:
            hostVersionEntity.setState(RepositoryVersionState.OUT_OF_SYNC);
            hostVersionDAO.get().merge(hostVersionEntity);
            break;
          default:
            break;
        }
      }
    } catch (AmbariException e) {
      LOG.error("Can not update hosts about out of sync", e);
    } finally {
      m_lock.unlock();
    }
  }

  @Subscribe
  @Transactional
  public void onServiceComponentHostEvent(ServiceComponentUninstalledEvent event) {

    m_lock.lock();

    try {
      Cluster cluster = clusters.get().getClusterById(event.getClusterId());
      List<HostVersionEntity> hostVersionEntities =
          hostVersionDAO.get().findByClusterAndHost(cluster.getClusterName(), event.getHostName());

      for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
        HostEntity hostEntity = hostVersionEntity.getHostEntity();
        RepositoryVersionEntity repoVersionEntity = hostVersionEntity.getRepositoryVersion();
        StackId stackId = repoVersionEntity.getStackId();

        if (null == stackId) {
          LOG.info("Stack id could not be loaded for host version {}, repo {}", hostVersionEntity.getHostName(),
              repoVersionEntity.getVersion());
          continue;
        }

        RepositoryVersionState repoState = checkAllHostComponents(stackId, hostEntity);
        if (null != repoState) {
          hostVersionEntity.setState(repoState);
          hostVersionDAO.get().merge(hostVersionEntity);
        }
      }

    } catch (AmbariException e) {
      LOG.error("Cannot update states after a component was uninstalled: {}", event, e);
    } finally {
      m_lock.unlock();
    }
  }

  /**
   * Checks if all the components advertise version.  If additional states need to be
   * computed, add on to the logic of this method; make sure the usages are checked for
   * correctness.
   *
   * @param stackId the stack id
   * @param host    the host entity to find components
   * @return {@code null} if there should be no state change.  non-{@code null} to change it
   */
  private RepositoryVersionState checkAllHostComponents(StackId stackId,
      HostEntity host) throws AmbariException {

    Collection<HostComponentDesiredStateEntity> hostComponents = host.getHostComponentDesiredStateEntities();

    for (HostComponentDesiredStateEntity hostComponent : hostComponents) {
      // Skip lookup if stack does not contain the component
      if (!ami.get().isValidServiceComponent(stackId.getStackName(),
          stackId.getStackVersion(), hostComponent.getServiceName(), hostComponent.getComponentName())) {
        LOG.debug("Component not found is host stack, stack={}, version={}, service={}, component={}",
            stackId.getStackName(), stackId.getStackVersion(),
            hostComponent.getServiceName(), hostComponent.getComponentName());
        continue;
      }
      ComponentInfo ci = ami.get().getComponent(stackId.getStackName(), stackId.getStackVersion(),
          hostComponent.getServiceName(), hostComponent.getComponentName());

      if (ci.isVersionAdvertised()) {
        return null;
      }
    }

    return RepositoryVersionState.NOT_REQUIRED;
  }

  @Subscribe
  @Transactional
  public void onServiceEvent(ServiceInstalledEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    try {
      Cluster cluster = clusters.get().getClusterById(event.getClusterId());

      Map<String, ServiceComponent> serviceComponents = cluster.getService(event.getServiceName()).getServiceComponents();
      // Determine hosts that become OUT_OF_SYNC when adding components for new service
      Map<String, List<ServiceComponent>> affectedHosts =
        new HashMap<>();
      for (ServiceComponent component : serviceComponents.values()) {
        for (String hostname : component.getServiceComponentHosts().keySet()) {
          if (! affectedHosts.containsKey(hostname)) {
            affectedHosts.put(hostname, new ArrayList<>());
          }
          affectedHosts.get(hostname).add(component);
        }
      }
      for (String hostName : affectedHosts.keySet()) {
        List<HostVersionEntity> hostVersionEntities =
            hostVersionDAO.get().findByClusterAndHost(cluster.getClusterName(), hostName);
        for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
          RepositoryVersionEntity repositoryVersion = hostVersionEntity.getRepositoryVersion();
          // If added components do not advertise version, it makes no sense to mark version OUT_OF_SYNC
          // We perform check per-stack version, because component may be not versionAdvertised in current
          // stack, but become versionAdvertised in some future (installed, but not yet upgraded to) stack
          boolean hasChangedComponentsWithVersions = false;
          String serviceName = event.getServiceName();
          for (ServiceComponent comp : affectedHosts.get(hostName)) {
            String componentName = comp.getName();

            // Skip lookup if stack does not contain the component
            if (!ami.get().isValidServiceComponent(repositoryVersion.getStackName(),
                repositoryVersion.getStackVersion(), serviceName, componentName)) {
              LOG.debug("Component not found is host stack, stack={}, version={}, service={}, component={}",
                  repositoryVersion.getStackName(), repositoryVersion.getStackVersion(), serviceName, componentName);
              continue;
            }
            ComponentInfo component = ami.get().getComponent(repositoryVersion.getStackName(),
                    repositoryVersion.getStackVersion(), serviceName, componentName);
            if (component.isVersionAdvertised()) {
              hasChangedComponentsWithVersions = true;
            }
          }
          if (! hasChangedComponentsWithVersions) {
            continue;
          }

          if (hostVersionEntity.getState().equals(RepositoryVersionState.INSTALLED)) {
            hostVersionEntity.setState(RepositoryVersionState.OUT_OF_SYNC);
            hostVersionDAO.get().merge(hostVersionEntity);
          }
        }
      }

    } catch (AmbariException e) {
      LOG.error("Can not update hosts about out of sync", e);
    }
  }

  /**
   * When hosts are added, add a host_version record for every repo_version in the database.
   *
   * @param event the add event
   */
  @Subscribe
  @Transactional
  public void onHostEvent(HostsAddedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    // create host version entries for every repository
    @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES, comment="Eventually take into account deleted repositories")
    List<RepositoryVersionEntity> repos = repositoryVersionDAO.get().findAll();

    for (String hostName : event.getHostNames()) {
      HostEntity hostEntity = hostDAO.get().findByName(hostName);

      for (RepositoryVersionEntity repositoryVersion : repos) {

        // we don't have the knowledge yet to know if we need the record
        HostVersionEntity missingHostVersion = new HostVersionEntity(hostEntity,
            repositoryVersion, RepositoryVersionState.NOT_REQUIRED);

        LOG.info("Creating host version for {}, state={}, repo={} (repo_id={})",
          missingHostVersion.getHostName(), missingHostVersion.getState(),
          missingHostVersion.getRepositoryVersion().getVersion(), missingHostVersion.getRepositoryVersion().getId());

        hostVersionDAO.get().create(missingHostVersion);
        hostDAO.get().merge(hostEntity);

        hostEntity.getHostVersionEntities().add(missingHostVersion);
        hostEntity = hostDAO.get().merge(hostEntity);
      }
    }
  }

  /**
   * Host repo_version entities are removed via cascade.
   *
   * @param event
   *          the removal event.
   */
  @Subscribe
  @Transactional
  public void onHostEvent(HostsRemovedEvent event) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }
  }

}
