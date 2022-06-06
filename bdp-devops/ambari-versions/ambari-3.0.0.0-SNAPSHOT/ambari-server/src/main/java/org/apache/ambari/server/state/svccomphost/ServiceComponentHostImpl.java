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

package org.apache.ambari.server.state.svccomphost;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AlertDefinitionCommand;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.AlertHashInvalidationEvent;
import org.apache.ambari.server.events.HostComponentUpdate;
import org.apache.ambari.server.events.HostComponentsUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.StaleConfigsUpdateEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.BlueprintProvisioningState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceComponentHostImpl implements ServiceComponentHost {

  private static final Logger LOG =
      LoggerFactory.getLogger(ServiceComponentHostImpl.class);

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock writeLock = readWriteLock.writeLock();

  private final ServiceComponent serviceComponent;

  private final Host host;

  private final HostComponentStateDAO hostComponentStateDAO;

  private final HostComponentDesiredStateDAO hostComponentDesiredStateDAO;

  private final HostDAO hostDAO;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  private final ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;

  private final Clusters clusters;

  @Inject
  private ConfigHelper helper;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private RepositoryVersionHelper repositoryVersionHelper;

  @Inject
  STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private Provider<TopologyHolder> m_topologyHolder;

  @Inject
  private Provider<HostLevelParamsHolder> m_hostLevelParamsHolder;

  /**
   * Used for creating commands to send to the agents when alert definitions are
   * added as the result of a service install.
   */
  @Inject
  private AlertDefinitionHash alertDefinitionHash;

  /**
   * Used for topology event creation updates
   */
  @Inject
  private Provider<AmbariManagementController> controller;

  /**
   * Used to publish events relating to service CRUD operations.
   */
  private final AmbariEventPublisher eventPublisher;

  /**
   * Data access object for stack.
   */
  private final StackDAO stackDAO;

  /**
   * The desired component state entity id.
   */
  private final Long desiredStateEntityId;
  /**
   * Cache the generated id for host component state for fast lookups.
   */
  private final Long hostComponentStateId;

  private long lastOpStartTime;
  private long lastOpEndTime;
  private long lastOpLastUpdateTime;
  private AtomicReference<MaintenanceState> maintenanceState = new AtomicReference<>();

  private ConcurrentMap<String, HostConfig> actualConfigs = new ConcurrentHashMap<>();
  private ImmutableList<Map<String, String>> processes = ImmutableList.of();

  /**
   * Used for preventing multiple components on the same host from trying to
   * recalculate versions concurrently.
   */
  private static final Striped<Lock> HOST_VERSION_LOCK = Striped.lazyWeakLock(20);

  /**
   * The name of the host (which should never, ever change)
   */
  private final String hostName;

  private static final StateMachineFactory
  <ServiceComponentHostImpl, State,
  ServiceComponentHostEventType, ServiceComponentHostEvent>
    daemonStateMachineFactory
      = new StateMachineFactory<ServiceComponentHostImpl,
          State, ServiceComponentHostEventType,
          ServiceComponentHostEvent>
          (State.INIT)

  // define the state machine of a HostServiceComponent for runnable
  // components

  .addTransition(State.INIT,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new AlertDefinitionCommandTransition())

  .addTransition(State.INSTALLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new ServiceComponentHostOpCompletedTransition())

  // Allow transition on abort
  .addTransition(State.INSTALLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLING,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.INSTALLING,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLING,
      State.INSTALL_FAILED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALL_FAILED,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALL_FAILED,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
      new ServiceComponentHostOpStartedTransition())

  // Allow transition on abort
  .addTransition(State.INSTALL_FAILED,
      State.INSTALL_FAILED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLED,
      State.STARTING,
      ServiceComponentHostEventType.HOST_SVCCOMP_START,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.INSTALLED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STARTED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOPPED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTING,
      State.STARTING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.STARTING,
      State.STARTING,
      ServiceComponentHostEventType.HOST_SVCCOMP_START,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.STARTING,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STARTED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLED,
      State.STARTING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.STARTED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STARTED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTED,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.STARTED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  // Allow transition on abort
  .addTransition(State.STARTED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOPPED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STOPPING,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.STOPPING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOPPED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STOPPING,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTED,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UNINSTALLING,
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.UNINSTALLING,
      State.UNINSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UNINSTALLING,
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.UPGRADING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.UNINSTALLING,
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UNINSTALLING,
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UNINSTALLED,
      State.INSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UNINSTALLED,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.WIPING_OUT,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.WIPING_OUT,
      State.INIT,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.WIPING_OUT,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.WIPING_OUT,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.WIPING_OUT,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.DISABLED,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UNKNOWN,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UNKNOWN,
      State.UNKNOWN,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALL_FAILED,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.DISABLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_RESTORE,
      new ServiceComponentHostOpCompletedTransition())

     .installTopology();

  private static final StateMachineFactory
  <ServiceComponentHostImpl, State,
  ServiceComponentHostEventType, ServiceComponentHostEvent>
    clientStateMachineFactory
      = new StateMachineFactory<ServiceComponentHostImpl,
          State, ServiceComponentHostEventType,
          ServiceComponentHostEvent>
          (State.INIT)

  // define the state machine of a HostServiceComponent for client only
  // components

     .addTransition(State.INIT,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())

     .addTransition(State.INSTALLED,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())

     // Allow transition on abort
     .addTransition(State.INSTALLED,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALL_FAILED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALL_FAILED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
      // Allow transition on abort
     .addTransition(State.INSTALL_FAILED, State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALL_FAILED,
         State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())

    .addTransition(State.INSTALLED,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.INSTALLED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
    .addTransition(State.INSTALLED,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.UPGRADING,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
         new ServiceComponentHostOpInProgressTransition())

     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.WIPING_OUT,
         State.INIT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .installTopology();

  private final StateMachine<State,
      ServiceComponentHostEventType, ServiceComponentHostEvent> stateMachine;

  static class ServiceComponentHostOpCompletedTransition
     implements SingleArcTransition<ServiceComponentHostImpl,
         ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());

    }
  }

  /**
   * The {@link AlertDefinitionCommandTransition} is used to capture the
   * transition from {@link State#INSTALLING} to {@link State#INSTALLED} so that
   * the host affected will have new {@link AlertDefinitionCommand}s pushed to
   * it.
   */
  static class AlertDefinitionCommandTransition implements
      SingleArcTransition<ServiceComponentHostImpl, ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      if (event.getType() != ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED) {
        return;
      }

      // invalidate the host
      String hostName = impl.getHostName();
      impl.alertDefinitionHash.invalidate(impl.getClusterName(), hostName);

      // publish the event
      AlertHashInvalidationEvent hashInvalidationEvent = new AlertHashInvalidationEvent(
          impl.getClusterId(), Collections.singletonList(hostName));

      impl.eventPublisher.publish(hashInvalidationEvent);
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());

    }
  }

  static class ServiceComponentHostOpStartedTransition
    implements SingleArcTransition<ServiceComponentHostImpl,
        ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      // TODO Audit logs
      // FIXME handle restartOp event
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
      if (event.getType() ==
          ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL) {
        ServiceComponentHostInstallEvent e =
            (ServiceComponentHostInstallEvent) event;
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating live stack version during INSTALL event, new stack version={}", e.getStackId());
        }
      }
    }
  }

  static class ServiceComponentHostOpInProgressTransition
    implements SingleArcTransition<ServiceComponentHostImpl,
        ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
    }
  }

  private void resetLastOpInfo() {
    setLastOpStartTime(-1);
    setLastOpLastUpdateTime(-1);
    setLastOpEndTime(-1);
  }

  private void updateLastOpInfo(ServiceComponentHostEventType eventType,
      long time) {
    try {
      writeLock.lock();
      switch (eventType) {
        case HOST_SVCCOMP_INSTALL:
        case HOST_SVCCOMP_START:
        case HOST_SVCCOMP_STOP:
        case HOST_SVCCOMP_UNINSTALL:
        case HOST_SVCCOMP_WIPEOUT:
        case HOST_SVCCOMP_OP_RESTART:
          resetLastOpInfo();
          setLastOpStartTime(time);
          break;
        case HOST_SVCCOMP_OP_FAILED:
        case HOST_SVCCOMP_OP_SUCCEEDED:
        case HOST_SVCCOMP_STOPPED:
        case HOST_SVCCOMP_STARTED:
          setLastOpLastUpdateTime(time);
          setLastOpEndTime(time);
          break;
        case HOST_SVCCOMP_OP_IN_PROGRESS:
          setLastOpLastUpdateTime(time);
          break;
      }
    } finally {
      writeLock.unlock();
    }
  }

  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
      @Assisted String hostName, @Assisted ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity,
      Clusters clusters, StackDAO stackDAO, HostDAO hostDAO,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      HostComponentStateDAO hostComponentStateDAO,
      HostComponentDesiredStateDAO hostComponentDesiredStateDAO,
      AmbariEventPublisher eventPublisher) {

    this.serviceComponent = serviceComponent;
    this.hostName = hostName;
    this.clusters = clusters;
    this.stackDAO = stackDAO;
    this.hostDAO = hostDAO;
    this.serviceComponentDesiredStateDAO = serviceComponentDesiredStateDAO;
    this.hostComponentStateDAO = hostComponentStateDAO;
    this.hostComponentDesiredStateDAO = hostComponentDesiredStateDAO;
    this.eventPublisher = eventPublisher;

    if (serviceComponent.isClientComponent()) {
      stateMachine = clientStateMachineFactory.make(this);
    } else {
      stateMachine = daemonStateMachineFactory.make(this);
    }

    HostEntity hostEntity = null;
    try {
      host = clusters.getHost(hostName);
      hostEntity = hostDAO.findByName(hostName);
      if (hostEntity == null) {
        throw new AmbariException("Could not find host " + hostName);
      }
    } catch (AmbariException e) {
      LOG.error("Host '{}' was not found" + hostName);
      throw new RuntimeException(e);
    }

    StackId stackId = serviceComponent.getDesiredStackId();
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    HostComponentStateEntity stateEntity = new HostComponentStateEntity();
    stateEntity.setClusterId(serviceComponent.getClusterId());
    stateEntity.setComponentName(serviceComponent.getName());
    stateEntity.setServiceName(serviceComponent.getServiceName());
    stateEntity.setVersion(State.UNKNOWN.toString());
    stateEntity.setHostEntity(hostEntity);
    stateEntity.setCurrentState(stateMachine.getCurrentState());
    stateEntity.setUpgradeState(UpgradeState.NONE);

    HostComponentDesiredStateEntity desiredStateEntity = new HostComponentDesiredStateEntity();
    desiredStateEntity.setClusterId(serviceComponent.getClusterId());
    desiredStateEntity.setComponentName(serviceComponent.getName());
    desiredStateEntity.setServiceName(serviceComponent.getServiceName());
    desiredStateEntity.setHostEntity(hostEntity);
    desiredStateEntity.setDesiredState(State.INIT);

    if(!serviceComponent.isMasterComponent() && !serviceComponent.isClientComponent()) {
      desiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    } else {
      desiredStateEntity.setAdminState(null);
    }

    persistEntities(hostEntity, stateEntity, desiredStateEntity, serviceComponentDesiredStateEntity);

    // publish the service component installed event
    ServiceComponentInstalledEvent event = new ServiceComponentInstalledEvent(getClusterId(),
        stackId.getStackName(), stackId.getStackVersion(), getServiceName(),
        getServiceComponentName(), getHostName(), isRecoveryEnabled(), serviceComponent.isMasterComponent());

    eventPublisher.publish(event);

    desiredStateEntityId = desiredStateEntity.getId();
    hostComponentStateId = stateEntity.getId();

    resetLastOpInfo();
  }
  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
      @Assisted String hostName,
      Clusters clusters, StackDAO stackDAO, HostDAO hostDAO,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      HostComponentStateDAO hostComponentStateDAO,
      HostComponentDesiredStateDAO hostComponentDesiredStateDAO,
      AmbariEventPublisher eventPublisher) {
      this(serviceComponent, hostName, null, clusters, stackDAO, hostDAO,
          serviceComponentDesiredStateDAO, hostComponentStateDAO, hostComponentDesiredStateDAO, eventPublisher);
  }

  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
      @Assisted HostComponentStateEntity stateEntity,
      @Assisted HostComponentDesiredStateEntity desiredStateEntity, Clusters clusters,
      StackDAO stackDAO, HostDAO hostDAO,
      ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO,
      HostComponentStateDAO hostComponentStateDAO,
      HostComponentDesiredStateDAO hostComponentDesiredStateDAO,
      AmbariEventPublisher eventPublisher) {

    hostName = stateEntity.getHostName();

    this.serviceComponent = serviceComponent;
    this.clusters = clusters;
    this.stackDAO = stackDAO;
    this.hostDAO = hostDAO;
    this.serviceComponentDesiredStateDAO = serviceComponentDesiredStateDAO;
    this.hostComponentStateDAO = hostComponentStateDAO;
    this.hostComponentDesiredStateDAO = hostComponentDesiredStateDAO;
    this.eventPublisher = eventPublisher;

    desiredStateEntityId = desiredStateEntity.getId();
    hostComponentStateId = stateEntity.getId();

    //TODO implement State Machine init as now type choosing is hardcoded in above code
    if (serviceComponent.isClientComponent()) {
      stateMachine = clientStateMachineFactory.make(this);
    } else {
      stateMachine = daemonStateMachineFactory.make(this);
    }
    stateMachine.setCurrentState(stateEntity.getCurrentState());

    try {
      host = clusters.getHost(stateEntity.getHostName());
    } catch (AmbariException e) {
      //TODO exception? impossible due to database restrictions
      LOG.error("Host '{}' was not found " + stateEntity.getHostName());
      throw new RuntimeException(e);
    }
  }

  @Override
  public State getState() {
    // there's no reason to lock around the state machine for this SCH since
    // the state machine is synchronized
    return stateMachine.getCurrentState();
  }

  @Override
  @Transactional
  public void setState(State state) {
    State oldState = getState();
    stateMachine.setCurrentState(state);
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      stateEntity.setCurrentState(state);
      stateEntity = hostComponentStateDAO.merge(stateEntity);
      if (!oldState.equals(state)) {
        STOMPUpdatePublisher.publish(new HostComponentsUpdateEvent(Collections.singletonList(
            HostComponentUpdate.createHostComponentStatusUpdate(stateEntity, oldState))));
      }
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + ", " + "hostName = " + getHostName());
    }
  }

  @Override
  public String getVersion() {
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      return stateEntity.getVersion();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may "
          + "have been previously deleted, serviceName = " + getServiceName() + ", "
          + "componentName = " + getServiceComponentName() + ", " + "hostName = " + getHostName());
    }

    return null;
  }

  @Override
  @Transactional
  public void setVersion(String version) throws AmbariException {
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      stateEntity.setVersion(version);
      stateEntity = hostComponentStateDAO.merge(stateEntity);

      ServiceComponentHostRequest serviceComponentHostRequest = new ServiceComponentHostRequest(
          serviceComponent.getClusterName(), serviceComponent.getServiceName(),
          serviceComponent.getName(), hostName, getDesiredState().name());

      TopologyUpdateEvent updateEvent = controller.get().getAddedComponentsTopologyEvent(
          Collections.singleton(serviceComponentHostRequest));

      m_topologyHolder.get().updateData(updateEvent);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + ", " + "hostName = " + getHostName());
    }
  }

  /**
   * To be called during the upgrade of a specific Component in a host.
   * The potential upgrade states are NONE (default), PENDING, IN_PROGRESS, FAILED.
   * If the upgrade completes successfully, the upgradeState should be set back to NONE.
   * If the upgrade fails, then the user can retry to set it back into PENDING or IN_PROGRESS.
   * If the upgrade is aborted, then the upgradeState should be set back to NONE.
   *
   * @param upgradeState  the upgrade state
   */
  @Override
  @Transactional
  public void setUpgradeState(UpgradeState upgradeState) {
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      stateEntity.setUpgradeState(upgradeState);
      stateEntity = hostComponentStateDAO.merge(stateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + ", " + "hostName = " + getHostName());
    }
  }

  @Override
  public UpgradeState getUpgradeState() {
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      return stateEntity.getUpgradeState();
    } else {
      LOG.warn("Trying to fetch a state entity from an object that may "
          + "have been previously deleted, serviceName = " + getServiceName() + ", "
          + "componentName = " + getServiceComponentName() + ", " + "hostName = " + getHostName());
    }

    return UpgradeState.NONE;
  }


  @Override
  @Transactional
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling ServiceComponentHostEvent event, eventType={}, event={}", event.getType().name(), event);
    }
    State oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
        HostComponentStateEntity stateEntity = getStateEntity();
        boolean statusUpdated = !stateEntity.getCurrentState().equals(stateMachine.getCurrentState());
        stateEntity.setCurrentState(stateMachine.getCurrentState());
        stateEntity = hostComponentStateDAO.merge(stateEntity);
        if (statusUpdated) {
          STOMPUpdatePublisher.publish(new HostComponentsUpdateEvent(Collections.singletonList(
              HostComponentUpdate.createHostComponentStatusUpdate(stateEntity, oldState))));
        }
        if (event.getType().equals(ServiceComponentHostEventType.HOST_SVCCOMP_START)) {
          HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
          if (desiredStateEntity.getBlueprintProvisioningState() == BlueprintProvisioningState.IN_PROGRESS) {
            desiredStateEntity.setBlueprintProvisioningState(BlueprintProvisioningState.FINISHED);
            hostComponentDesiredStateDAO.merge(desiredStateEntity);
            m_hostLevelParamsHolder.get().updateData(m_hostLevelParamsHolder.get().getCurrentData(getHost().getHostId()));
          }
        }
        // TODO Audit logs
      } catch (InvalidStateTransitionException e) {
        LOG.error("Can't handle ServiceComponentHostEvent event at"
            + " current state"
            + ", serviceComponentName=" + getServiceComponentName()
            + ", hostName=" + getHostName()
          + ", currentState=" + oldState
            + ", eventType=" + event.getType()
            + ", event=" + event);
        throw e;
      } catch (AmbariException e) {
        LOG.error("Can't update topology on hosts on ServiceComponentHostEvent event: "
            + "serviceComponentName=" + getServiceComponentName()
            + ", hostName=" + getHostName()
            + ", currentState=" + oldState
            + ", eventType=" + event.getType()
            + ", event=" + event);
      }
    } finally {
      writeLock.unlock();
    }

    if (!oldState.equals(getState())) {
      LOG.info("Host role transitioned to a new state"
               + ", serviceComponentName=" + getServiceComponentName()
               + ", hostName=" + getHostName()
               + ", oldState=" + oldState
               + ", currentState=" + getState());
      if (LOG.isDebugEnabled()) {
        LOG.debug("ServiceComponentHost transitioned to a new state, serviceComponentName={}, hostName={}, oldState={}, currentState={}, eventType={}, event={}",
          getServiceComponentName(), getHostName(), oldState, getState(), event.getType().name(), event);
      }
    }
  }

  @Override
  public String getServiceComponentName() {
    return serviceComponent.getName();
  }

  @Override
  public String getHostName() {
    return host.getHostName();
  }

  @Override
  public String getPublicHostName() {
    return host.getPublicHostName();
  }

  @Override
  public Host getHost() {
    return host;
  }

  /**
   * @return the lastOpStartTime
   */
  public long getLastOpStartTime() {
    return lastOpStartTime;
  }

  /**
   * @param lastOpStartTime the lastOpStartTime to set
   */
  public void setLastOpStartTime(long lastOpStartTime) {
    this.lastOpStartTime = lastOpStartTime;
  }

  /**
   * @return the lastOpEndTime
   */
  public long getLastOpEndTime() {
    return lastOpEndTime;
  }

  /**
   * @param lastOpEndTime the lastOpEndTime to set
   */
  public void setLastOpEndTime(long lastOpEndTime) {
    this.lastOpEndTime = lastOpEndTime;
  }

  /**
   * @return the lastOpLastUpdateTime
   */
  public long getLastOpLastUpdateTime() {
    return lastOpLastUpdateTime;
  }

  /**
   * @param lastOpLastUpdateTime the lastOpLastUpdateTime to set
   */
  public void setLastOpLastUpdateTime(long lastOpLastUpdateTime) {
    this.lastOpLastUpdateTime = lastOpLastUpdateTime;
  }

  @Override
  public long getClusterId() {
    return serviceComponent.getClusterId();
  }

  @Override
  public String getServiceName() {
    return serviceComponent.getServiceName();
  }

  @Override
  public boolean isClientComponent() {
    return serviceComponent.isClientComponent();
  }

  @Override
  public State getDesiredState() {
    HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    if (desiredStateEntity != null) {
      return desiredStateEntity.getDesiredState();
    } else {
      LOG.warn("Trying to fetch a member from an entity object that may "
          + "have been previously deleted, serviceName = " + getServiceName() + ", "
          + "componentName = " + getServiceComponentName() + ", " + "hostName = " + getHostName());
    }

    return null;
  }

  @Override
  public void setDesiredState(State state) {
    LOG.debug("Set DesiredState on serviceName = {} componentName = {} hostName = {} to {} ",
        getServiceName(), getServiceComponentName(), getHostName(), state);

    HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    if (desiredStateEntity != null) {
      desiredStateEntity.setDesiredState(state);
      hostComponentDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + "hostName = " + getHostName());
    }
  }

  @Override
  public HostComponentAdminState getComponentAdminState() {
    HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    return getComponentAdminStateFromDesiredStateEntity(desiredStateEntity);
  }

  private HostComponentAdminState getComponentAdminStateFromDesiredStateEntity(HostComponentDesiredStateEntity desiredStateEntity) {
    if (desiredStateEntity != null) {
      HostComponentAdminState adminState = desiredStateEntity.getAdminState();
      if (adminState == null && !serviceComponent.isClientComponent()
              && !serviceComponent.isMasterComponent()) {
        adminState = HostComponentAdminState.INSERVICE;
      }
      return adminState;
    }

    return null;
  }

  @Override
  public void setComponentAdminState(HostComponentAdminState attribute) {
    LOG.debug("Set ComponentAdminState on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), attribute);

    HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    if (desiredStateEntity != null) {
      desiredStateEntity.setAdminState(attribute);
      hostComponentDesiredStateDAO.merge(desiredStateEntity);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + "hostName = " + getHostName());
    }
  }

  @Override
  public ServiceComponentHostResponse convertToResponse(Map<String, DesiredConfig> desiredConfigs) {
    HostComponentStateEntity hostComponentStateEntity = getStateEntity();
    HostEntity hostEntity = hostComponentStateEntity.getHostEntity();

    HostComponentDesiredStateEntity hostComponentDesiredStateEntity = getDesiredStateEntity();

    String clusterName = serviceComponent.getClusterName();
    String serviceName = serviceComponent.getServiceName();
    String serviceComponentName = serviceComponent.getName();
    String hostName = getHostName();
    String publicHostName = hostEntity.getPublicHostName();
    String state = getState().toString();
    String desiredState = (hostComponentDesiredStateEntity == null) ? null : hostComponentDesiredStateEntity.getDesiredState().toString();
    String desiredStackId = serviceComponent.getDesiredStackId().getStackId();
    HostComponentAdminState componentAdminState = getComponentAdminStateFromDesiredStateEntity(hostComponentDesiredStateEntity);
    UpgradeState upgradeState = hostComponentStateEntity.getUpgradeState();

    String displayName = null;
    try {
      StackId stackVersion = serviceComponent.getDesiredStackId();
      ComponentInfo compInfo = ambariMetaInfo.getComponent(stackVersion.getStackName(),
              stackVersion.getStackVersion(), serviceName, serviceComponentName);
      displayName = compInfo.getDisplayName();
    } catch (AmbariException e) {
      displayName = serviceComponentName;
    }

    String desiredRepositoryVersion = null;
    RepositoryVersionEntity repositoryVersion = serviceComponent.getDesiredRepositoryVersion();
    if (null != repositoryVersion) {
      desiredRepositoryVersion = repositoryVersion.getVersion();
    }

    ServiceComponentHostResponse r = new ServiceComponentHostResponse(clusterName, serviceName,
        serviceComponentName, displayName, hostName, publicHostName, state, getVersion(),
        desiredState, desiredStackId, desiredRepositoryVersion, componentAdminState);

    r.setActualConfigs(actualConfigs);
    r.setUpgradeState(upgradeState);

    try {
      r.setStaleConfig(helper.isStaleConfigs(this, desiredConfigs, hostComponentDesiredStateEntity));
    } catch (Exception e) {
      LOG.error("Could not determine stale config", e);
    }

    try {
      Cluster cluster = clusters.getCluster(clusterName);
      ServiceComponent serviceComponent = cluster.getService(serviceName).getServiceComponent(serviceComponentName);
      ServiceComponentHost sch = serviceComponent.getServiceComponentHost(hostName);
      String refreshConfigsCommand = helper.getRefreshConfigsCommand(cluster,sch);
      r.setReloadConfig(refreshConfigsCommand != null);
    } catch (Exception e) {
      LOG.error("Could not determine reload config flag", e);
    }

    return r;
  }

  @Override
  public ServiceComponentHostResponse convertToResponseStatusOnly(Map<String, DesiredConfig> desiredConfigs,
                                                                  boolean collectStaleConfigsStatus) {
    String clusterName = serviceComponent.getClusterName();
    String serviceName = serviceComponent.getServiceName();
    String serviceComponentName = serviceComponent.getName();
    String state = getState().toString();

    ServiceComponentHostResponse r = new ServiceComponentHostResponse(clusterName, serviceName,
        serviceComponentName, null, hostName, null, state, null,
        null, null, null, null);

    if (collectStaleConfigsStatus) {

      try {
        HostComponentDesiredStateEntity hostComponentDesiredStateEntity = getDesiredStateEntity();
        r.setStaleConfig(helper.isStaleConfigs(this, desiredConfigs, hostComponentDesiredStateEntity));
      } catch (Exception e) {
        LOG.error("Could not determine stale config", e);
      }
    } else {
      r.setStaleConfig(false);
    }

    return r;
  }

  @Override
  public String getClusterName() {
    return serviceComponent.getClusterName();
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("ServiceComponentHost={ hostname=").append(getHostName())
    .append(", serviceComponentName=")
    .append(serviceComponent.getName())
    .append(", clusterName=")
    .append(serviceComponent.getClusterName())
    .append(", serviceName=")
    .append(serviceComponent.getServiceName())
    .append(", desiredStackVersion=")
    .append(serviceComponent.getDesiredStackId())
    .append(", desiredState=")
    .append(getDesiredState())
    .append(", version=")
    .append(getVersion())
    .append(", state=")
    .append(getState())
    .append(" }");
  }

  @Transactional
  void persistEntities(HostEntity hostEntity, HostComponentStateEntity stateEntity,
                       HostComponentDesiredStateEntity desiredStateEntity,
                       ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {

    if (serviceComponentDesiredStateEntity == null) {
      serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
          serviceComponent.getClusterId(), serviceComponent.getServiceName(), serviceComponent.getName());
    }

    desiredStateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
    desiredStateEntity.setHostEntity(hostEntity);
    desiredStateEntity.setHostId(hostEntity.getHostId());

    stateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
    stateEntity.setHostEntity(hostEntity);

    hostComponentStateDAO.create(stateEntity);
    hostComponentDesiredStateDAO.create(desiredStateEntity);

    serviceComponentDesiredStateEntity.getHostComponentDesiredStateEntities().add(
        desiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    hostEntity.addHostComponentStateEntity(stateEntity);
    hostEntity.addHostComponentDesiredStateEntity(desiredStateEntity);
    hostEntity = hostDAO.merge(hostEntity);
  }

  @Override
  public boolean canBeRemoved() {
    return getState().isRemovableState();
  }

  @Override
  public void delete(DeleteHostComponentStatusMetaData deleteMetaData) {
    boolean fireRemovalEvent = false;

    writeLock.lock();
    String version = getVersion();
    try {
      removeEntities();
      fireRemovalEvent = true;
      clusters.getCluster(getClusterName()).removeServiceComponentHost(this);
    } catch (AmbariException ex) {
      LOG.error("Unable to remove a service component from a host", ex);
    } finally {
      writeLock.unlock();
    }

    // publish event for the removal of the SCH after the removal is
    // completed, but only if it was persisted
    if (fireRemovalEvent) {
      long clusterId = getClusterId();
      StackId stackId = serviceComponent.getDesiredStackId();
      String stackVersion = stackId.getStackVersion();
      String stackName = stackId.getStackName();
      String serviceName = getServiceName();
      String componentName = getServiceComponentName();
      String hostName = getHostName();
      State lastComponentState = getState();
      boolean recoveryEnabled = isRecoveryEnabled();
      boolean masterComponent = serviceComponent.isMasterComponent();

      ServiceComponentUninstalledEvent event = new ServiceComponentUninstalledEvent(
          clusterId, stackName, stackVersion, serviceName, componentName,
          hostName, recoveryEnabled, masterComponent, host.getHostId());

      eventPublisher.publish(event);
      deleteMetaData.addDeletedHostComponent(componentName,
          serviceName,
          hostName,
          getHost().getHostId(),
          Long.toString(clusterId),
          version,
          lastComponentState);
    }
  }

  @Transactional
  protected void removeEntities() {
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      HostEntity hostEntity = stateEntity.getHostEntity();
      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();

      // Make sure that the state entity is removed from its host entity
      hostEntity.removeHostComponentStateEntity(stateEntity);
      hostEntity.removeHostComponentDesiredStateEntity(desiredStateEntity);

      hostDAO.merge(hostEntity);

      hostComponentDesiredStateDAO.remove(desiredStateEntity);
      hostComponentStateDAO.remove(stateEntity);
    }
  }

  @Override
  public Map<String, HostConfig> getActualConfigs() {
    return actualConfigs;
  }

  @Override
  public HostState getHostState() {
    return host.getState();
  }

  @Override
  public boolean isRecoveryEnabled() {
    return serviceComponent.isRecoveryEnabled();
  }

  @Override
  public void setMaintenanceState(MaintenanceState state) {
    LOG.debug("Set MaintenanceState on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), state);

    HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    if (desiredStateEntity != null) {
      desiredStateEntity.setMaintenanceState(state);
      maintenanceState.set(hostComponentDesiredStateDAO.merge(desiredStateEntity).getMaintenanceState());

      // broadcast the maintenance mode change
      MaintenanceModeEvent event = new MaintenanceModeEvent(state, this);
      eventPublisher.publish(event);
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + ", hostName = " + getHostName());
    }
  }

  @Override
  public MaintenanceState getMaintenanceState() {
    if (maintenanceState.get() == null) {
      maintenanceState.set(getDesiredStateEntity().getMaintenanceState());
    }
    return maintenanceState.get();
  }

  @Override
  public void setProcesses(List<Map<String, String>> procs) {
    processes = ImmutableList.copyOf(procs);
  }

  @Override
  public List<Map<String, String>> getProcesses() {
    return processes;
  }

  @Override
  public boolean isRestartRequired() {
    return getDesiredStateEntity().isRestartRequired();
  }

  @Override
  public boolean isRestartRequired(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    return hostComponentDesiredStateEntity.isRestartRequired();
  }

  @Override
  @Transactional
  public void setRestartRequired(boolean restartRequired) {
    if (setRestartRequiredWithoutEventPublishing(restartRequired)) {
      eventPublisher.publish(new StaleConfigsUpdateEvent(this, restartRequired));
    }
  }

  @Override
  @Transactional
  public boolean setRestartRequiredWithoutEventPublishing(boolean restartRequired) {
    LOG.debug("Set RestartRequired on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), restartRequired);

    HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
    if (desiredStateEntity != null) {
      desiredStateEntity.setRestartRequired(restartRequired);
      hostComponentDesiredStateDAO.merge(desiredStateEntity);
      return true;
    } else {
      LOG.warn("Setting a member on an entity object that may have been "
          + "previously deleted, serviceName = " + getServiceName() + ", " + "componentName = "
          + getServiceComponentName() + ", hostName = " + getHostName());
    }
    return false;
  }

  @Transactional
  RepositoryVersionEntity createRepositoryVersion(String version, final StackId stackId, final StackInfo stackInfo) throws AmbariException {
    // During an Ambari Upgrade from 1.7.0 -> 2.0.0, the Repo Version will not exist, so bootstrap it.
    LOG.info("Creating new repository version " + stackId.getStackName() + "-" + version);

    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
      stackId.getStackVersion());

    // Ensure that the version provided is part of the Stack.
    // E.g., version 2.3.0.0 is part of HDP 2.3, so is 2.3.0.0-1234
    if (null == version) {
      throw new AmbariException(MessageFormat.format("Cannot create Repository Version for Stack {0}-{1} if the version is empty",
          stackId.getStackName(), stackId.getStackVersion()));
    }

    return repositoryVersionDAO.create(
        stackEntity,
        version,
        stackId.getStackName() + "-" + version,
        repositoryVersionHelper.createRepoOsEntities(stackInfo.getRepositories()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public HostVersionEntity recalculateHostVersionState() throws AmbariException {
    HostEntity hostEntity = host.getHostEntity();
    RepositoryVersionEntity repositoryVersion = serviceComponent.getDesiredRepositoryVersion();
    HostVersionEntity hostVersionEntity = hostVersionDAO.findHostVersionByHostAndRepository(
        hostEntity, repositoryVersion);

    Lock lock = HOST_VERSION_LOCK.get(host.getHostName());
    lock.lock();
    try {
      // Create one if it doesn't already exist. It will be possible to make
      // further transitions below.
      if (hostVersionEntity == null) {
        hostVersionEntity = new HostVersionEntity(hostEntity, repositoryVersion,
            RepositoryVersionState.INSTALLING);

        LOG.info("Creating host version for {}, state={}, repo={} (repo_id={})",
            hostVersionEntity.getHostName(), hostVersionEntity.getState(),
            hostVersionEntity.getRepositoryVersion().getVersion(),
            hostVersionEntity.getRepositoryVersion().getId());

        hostVersionDAO.create(hostVersionEntity);
      }

      if (hostVersionEntity.getState() != RepositoryVersionState.CURRENT) {
        if (host.isRepositoryVersionCorrect(repositoryVersion)) {
          hostVersionEntity.setState(RepositoryVersionState.CURRENT);
          hostVersionEntity = hostVersionDAO.merge(hostVersionEntity);
        }
      }
    } finally {
      lock.unlock();
    }
    return hostVersionEntity;
  }

  /**
   * Gets the desired state entity for this {@link ServiceComponentHost}.
   *
   * @return
   */
  @Override
  public HostComponentDesiredStateEntity getDesiredStateEntity() {
    return hostComponentDesiredStateDAO.findById(desiredStateEntityId);
  }

  /**
   * Gets the state entity for this {@link ServiceComponentHost}.
   *
   * @return the {@link HostComponentStateEntity} for this
   *         {@link ServiceComponentHost}, or {@code null} if there is none.
   */
  private HostComponentStateEntity getStateEntity() {
    return hostComponentStateDAO.findById(hostComponentStateId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceComponent getServiceComponent() {
    return serviceComponent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StackId getDesiredStackId() {
    return serviceComponent.getDesiredStackId();
  }

}
