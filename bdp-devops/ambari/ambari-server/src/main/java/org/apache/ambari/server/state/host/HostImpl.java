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
package org.apache.ambari.server.state.host;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.events.HostStateUpdateEvent;
import org.apache.ambari.server.events.HostStatusUpdateEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.cache.HostConfigMappingImpl;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostEvent;
import org.apache.ambari.server.state.HostEventType;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;

public class HostImpl implements Host {

  private static final Logger LOG = LoggerFactory.getLogger(HostImpl.class);
  private static final String HARDWAREISA = "hardware_isa";
  private static final String HARDWAREMODEL = "hardware_model";
  private static final String INTERFACES = "interfaces";
  private static final String KERNEL = "kernel";
  private static final String KERNELMAJOREVERSON = "kernel_majorversion";
  private static final String KERNELRELEASE = "kernel_release";
  private static final String KERNELVERSION = "kernel_version";
  private static final String MACADDRESS = "mac_address";
  private static final String NETMASK = "netmask";
  private static final String OSFAMILY = "os_family";
  private static final String PHYSICALPROCESSORCOUNT = "physicalprocessors_count";
  private static final String PROCESSORCOUNT = "processors_count";
  private static final String SELINUXENABLED = "selinux_enabled";
  private static final String SWAPSIZE = "swap_size";
  private static final String SWAPFREE = "swap_free";
  private static final String TIMEZONE = "timezone";
  private static final String OS_RELEASE_VERSION = "os_release_version";

  @Inject
  private final Gson gson;

  private static final Type hostAttributesType =
      new TypeToken<Map<String, String>>() {}.getType();

  private static final Type maintMapType =
      new TypeToken<Map<Long, MaintenanceState>>() {}.getType();

  ReadWriteLock rwLock;
  private final Lock writeLock;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private HostStateDAO hostStateDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private ClusterDAO clusterDAO;

  @Inject
  private Clusters clusters;

  @Inject
  private HostConfigMappingDAO hostConfigMappingDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private MaintenanceStateHelper maintenanceStateHelper;

  /**
   * The ID of the host which is to retrieve it from JPA.
   */
  private final long hostId;

  /**
   * The name of the host, stored inside of this business object to prevent JPA
   * lookups since it never changes.
   */
  private final String hostName;

  private long lastHeartbeatTime = 0L;

  /**
   * An agent can re-register several times without restarting.
   * We should save the agent start time to timeout execution commands only on restart, not re-register.
   */
  private long lastAgentStartTime = 0L;
  private AgentEnv lastAgentEnv = null;
  private List<DiskInfo> disksInfo = new CopyOnWriteArrayList<>();
  private RecoveryReport recoveryReport = new RecoveryReport();
  private Integer currentPingPort = null;

  private final StateMachine<HostState, HostEventType, HostEvent> stateMachine;
  private final ConcurrentMap<Long, MaintenanceState> maintMap;

  // In-memory status, based on host components states
  private String status = HealthStatus.UNKNOWN.name();

  // In-memory prefix of log file paths that is retrieved when the agent registers with the server
  private String prefix;

  /**
   * Used to publish events relating to host CRUD operations.
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  @Inject
  private TopologyManager topologyManager;

  private static final StateMachineFactory
    <HostImpl, HostState, HostEventType, HostEvent>
      stateMachineFactory
        = new StateMachineFactory<HostImpl, HostState, HostEventType, HostEvent>
        (HostState.INIT)

   // define the state machine of a Host

   // Transition from INIT state
   // when the initial registration request is received
   .addTransition(HostState.INIT, HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())
   // when a heartbeat is lost right after registration
   .addTransition(HostState.INIT, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST, new HostHeartbeatLostTransition())

   // Transition from WAITING_FOR_STATUS_UPDATES state
   // when the host has responded to its status update requests
   // TODO this will create problems if the host is not healthy
   // TODO Based on discussion with Jitendra, ignoring this for now
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES, HostState.HEALTHY,
       HostEventType.HOST_STATUS_UPDATES_RECEIVED,
       new HostStatusUpdatesReceivedTransition())
   // when a normal heartbeat is received
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_HEARTBEAT_HEALTHY)   // TODO: Heartbeat is ignored here
   // when a heartbeart denoting host as unhealthy is received
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES, // Still waiting for component status
       HostEventType.HOST_HEARTBEAT_UNHEALTHY,
       new HostBecameUnhealthyTransition()) // TODO: Not sure
  // when a heartbeat is lost and status update is not received
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST,
       new HostHeartbeatLostTransition())

   // Transitions from HEALTHY state
   // when a normal heartbeat is received
   .addTransition(HostState.HEALTHY, HostState.HEALTHY,
       HostEventType.HOST_HEARTBEAT_HEALTHY,
       new HostHeartbeatReceivedTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(HostState.HEALTHY, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST,
       new HostHeartbeatLostTransition())
   // when a heartbeart denoting host as unhealthy is received
   .addTransition(HostState.HEALTHY, HostState.UNHEALTHY,
       HostEventType.HOST_HEARTBEAT_UNHEALTHY,
       new HostBecameUnhealthyTransition())
   // if a new registration request is received
   .addTransition(HostState.HEALTHY,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())

   // Transitions from UNHEALTHY state
   // when a normal heartbeat is received
   .addTransition(HostState.UNHEALTHY, HostState.HEALTHY,
       HostEventType.HOST_HEARTBEAT_HEALTHY,
       new HostBecameHealthyTransition())
   // when a heartbeart denoting host as unhealthy is received
   .addTransition(HostState.UNHEALTHY, HostState.UNHEALTHY,
       HostEventType.HOST_HEARTBEAT_UNHEALTHY,
       new HostHeartbeatReceivedTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(HostState.UNHEALTHY, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST,
       new HostHeartbeatLostTransition())
   // if a new registration request is received
   .addTransition(HostState.UNHEALTHY,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())

   // Transitions from HEARTBEAT_LOST state
   // when a heartbeat is not received within the configured timeout period
   .addTransition(HostState.HEARTBEAT_LOST, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST)
   // if a new registration request is received
   .addTransition(HostState.HEARTBEAT_LOST,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())

   .installTopology();

  @Inject
  public HostImpl(@Assisted HostEntity hostEntity, Gson gson, HostDAO hostDAO, HostStateDAO hostStateDAO) {
    this.gson = gson;
    this.hostDAO = hostDAO;
    this.hostStateDAO = hostStateDAO;

    stateMachine = stateMachineFactory.make(this);
    rwLock = new ReentrantReadWriteLock();
    writeLock = rwLock.writeLock();

    HostStateEntity hostStateEntity = hostEntity.getHostStateEntity();
    if (hostStateEntity == null) {
      hostStateEntity = new HostStateEntity();
      hostStateEntity.setHostEntity(hostEntity);
      hostEntity.setHostStateEntity(hostStateEntity);
      hostStateEntity.setHealthStatus(gson.toJson(new HostHealthStatus(HealthStatus.UNKNOWN, "")));
    } else {
      stateMachine.setCurrentState(hostStateEntity.getCurrentState());
    }

    // persist the host
    if (null == hostEntity.getHostId()) {
      persistEntities(hostEntity);

      for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
        try {
          clusters.getClusterById(clusterEntity.getClusterId()).refresh();
        } catch (AmbariException e) {
          LOG.error("Error while looking up the cluster", e);
          throw new RuntimeException("Cluster '" + clusterEntity.getClusterId() + "' was removed",
              e);
        }
      }
    }

    // set the host ID which will be used to retrieve it from JPA
    hostId = hostEntity.getHostId();
    hostName = hostEntity.getHostName();

    // populate the maintenance map
    maintMap = ensureMaintMap(hostEntity.getHostStateEntity());
  }

  @Override
  public int compareTo(Object o) {
    if ((o != null ) && (o instanceof Host)) {
      return getHostName().compareTo(((Host) o).getHostName());
    } else {
      return -1;
    }
  }

  static class HostRegistrationReceived
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostRegistrationRequestEvent e = (HostRegistrationRequestEvent) event;
      host.updateHost(e);

      String agentVersion = null;
      if (e.agentVersion != null) {
        agentVersion = e.agentVersion.getVersion();
      }
      LOG.info("Received host registration, host="
        + e.hostInfo
        + ", registrationTime=" + e.registrationTime
        + ", agentVersion=" + agentVersion);

      host.clusters.updateHostMappings(host);

      //todo: proper host joined notification
      boolean associatedWithCluster = false;
      try {
        associatedWithCluster = host.clusters.getClustersForHost(host.getPublicHostName()).size() > 0;
      } catch (HostNotFoundException e1) {
        associatedWithCluster = false;
      } catch (AmbariException e1) {
        // only HostNotFoundException is thrown
        LOG.error("Unable to determine the clusters for host", e1);
      }

      host.topologyManager.onHostRegistered(host, associatedWithCluster);

      host.setHealthStatus(new HostHealthStatus(HealthStatus.HEALTHY,
          host.getHealthStatus().getHealthReport()));
      // initialize agent times in the last time to prevent setting registering/heartbeat times for failed registration.
      host.updateHostTimestamps(e);
    }
  }

  static class HostStatusUpdatesReceivedTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostStatusUpdatesReceivedEvent e = (HostStatusUpdatesReceivedEvent)event;
      // TODO Audit logs
      LOG.debug("Host transition to host status updates received state, host={}, heartbeatTime={}", e.getHostName(), e.getTimestamp());
      host.setHealthStatus(new HostHealthStatus(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthReport()));
    }
  }

  static class HostHeartbeatReceivedTransition
    implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      long heartbeatTime = 0;
      switch (event.getType()) {
        case HOST_HEARTBEAT_HEALTHY:
          HostHealthyHeartbeatEvent hhevent = (HostHealthyHeartbeatEvent) event;
          heartbeatTime = hhevent.getHeartbeatTime();
          if (null != hhevent.getAgentEnv()) {
            host.setLastAgentEnv(hhevent.getAgentEnv());
          }
          if (null != hhevent.getMounts() && !hhevent.getMounts().isEmpty()) {
            host.setDisksInfo(hhevent.getMounts());
          }
          break;
        case HOST_HEARTBEAT_UNHEALTHY:
          heartbeatTime =
            ((HostUnhealthyHeartbeatEvent)event).getHeartbeatTime();
          break;
        default:
          break;
      }
      if (0 == heartbeatTime) {
        LOG.error("heartbeatTime = 0 !!!");
        // TODO handle error
      }
      // host.setLastHeartbeatState(new Object());
      host.setLastHeartbeatTime(heartbeatTime);
    }
  }

  static class HostBecameHealthyTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostHealthyHeartbeatEvent e = (HostHealthyHeartbeatEvent) event;
      host.setLastHeartbeatTime(e.getHeartbeatTime());
      // TODO Audit logs
      LOG.debug("Host transitioned to a healthy state, host={}, heartbeatTime={}", e.getHostName(), e.getHeartbeatTime());
      host.setHealthStatus(new HostHealthStatus(HealthStatus.HEALTHY, host.getHealthStatus().getHealthReport()));
    }
  }

  static class HostBecameUnhealthyTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostUnhealthyHeartbeatEvent e = (HostUnhealthyHeartbeatEvent) event;
      host.setLastHeartbeatTime(e.getHeartbeatTime());
      // TODO Audit logs
      LOG.debug("Host transitioned to an unhealthy state, host={}, heartbeatTime={}, healthStatus={}",
        e.getHostName(), e.getHeartbeatTime(), e.getHealthStatus());
      host.setHealthStatus(e.getHealthStatus());
    }
  }

  static class HostHeartbeatLostTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostHeartbeatLostEvent e = (HostHeartbeatLostEvent) event;
      // TODO Audit logs
      LOG.debug("Host transitioned to heartbeat lost state, host={}, lastHeartbeatTime={}", e.getHostName(), host.getLastHeartbeatTime());
      host.setHealthStatus(new HostHealthStatus(HealthStatus.UNKNOWN, host.getHealthStatus().getHealthReport()));
      host.setLastAgentStartTime(0);
      host.topologyManager.onHostHeartBeatLost(host);
    }
  }

  /**
   * @param hostInfo  the host information
   */
  @Override
  @Transactional
  public void importHostInfo(HostInfo hostInfo) {
    if (hostInfo.getIPAddress() != null && !hostInfo.getIPAddress().isEmpty()) {
      setIPv4(hostInfo.getIPAddress());
      setIPv6(hostInfo.getIPAddress());
    }

    setCpuCount(hostInfo.getProcessorCount());
    setPhCpuCount(hostInfo.getPhysicalProcessorCount());
    setTotalMemBytes(hostInfo.getMemoryTotal());
    setAvailableMemBytes(hostInfo.getFreeMemory());

    if (hostInfo.getArchitecture() != null && !hostInfo.getArchitecture().isEmpty()) {
      setOsArch(hostInfo.getArchitecture());
    }

    if (hostInfo.getOS() != null && !hostInfo.getOS().isEmpty()) {
      String osType = hostInfo.getOS();
      if (hostInfo.getOSRelease() != null) {
        String[] release = hostInfo.getOSRelease().split("\\.");
        if (release.length > 0) {
          osType += release[0];
        }
      }
      setOsType(osType.toLowerCase());
    }

    if (hostInfo.getMounts() != null && !hostInfo.getMounts().isEmpty()) {
      setDisksInfo(hostInfo.getMounts());
    }

    // FIXME add all other information into host attributes
    setAgentVersion(new AgentVersion(hostInfo.getAgentUserId()));

    Map<String, String> attrs = new HashMap<>();
    if (hostInfo.getHardwareIsa() != null) {
      attrs.put(HARDWAREISA, hostInfo.getHardwareIsa());
    }
    if (hostInfo.getHardwareModel() != null) {
      attrs.put(HARDWAREMODEL, hostInfo.getHardwareModel());
    }
    if (hostInfo.getInterfaces() != null) {
      attrs.put(INTERFACES, hostInfo.getInterfaces());
    }
    if (hostInfo.getKernel() != null) {
      attrs.put(KERNEL, hostInfo.getKernel());
    }
    if (hostInfo.getKernelMajVersion() != null) {
      attrs.put(KERNELMAJOREVERSON, hostInfo.getKernelMajVersion());
    }
    if (hostInfo.getKernelRelease() != null) {
      attrs.put(KERNELRELEASE, hostInfo.getKernelRelease());
    }
    if (hostInfo.getKernelVersion() != null) {
      attrs.put(KERNELVERSION, hostInfo.getKernelVersion());
    }
    if (hostInfo.getMacAddress() != null) {
      attrs.put(MACADDRESS, hostInfo.getMacAddress());
    }
    if (hostInfo.getNetMask() != null) {
      attrs.put(NETMASK, hostInfo.getNetMask());
    }
    if (hostInfo.getOSFamily() != null) {
      attrs.put(OSFAMILY, hostInfo.getOSFamily());
    }
    if (hostInfo.getPhysicalProcessorCount() != 0) {
      attrs.put(PHYSICALPROCESSORCOUNT, Long.toString(hostInfo.getPhysicalProcessorCount()));
    }
    if (hostInfo.getProcessorCount() != 0) {
      attrs.put(PROCESSORCOUNT, Long.toString(hostInfo.getProcessorCount()));
    }
    if (Boolean.toString(hostInfo.getSeLinux()) != null) {
      attrs.put(SELINUXENABLED, Boolean.toString(hostInfo.getSeLinux()));
    }
    if (hostInfo.getSwapSize() != null) {
      attrs.put(SWAPSIZE, hostInfo.getSwapSize());
    }
    if (hostInfo.getSwapFree() != null) {
      attrs.put(SWAPFREE, hostInfo.getSwapFree());
    }
    if (hostInfo.getTimeZone() != null) {
      attrs.put(TIMEZONE, hostInfo.getTimeZone());
    }
    if (hostInfo.getOSRelease() != null) {
      attrs.put(OS_RELEASE_VERSION, hostInfo.getOSRelease());
    }

    setHostAttributes(attrs);
  }

  @Override
  public void setLastAgentEnv(AgentEnv env) {
    lastAgentEnv = env;
  }

  @Override
  public AgentEnv getLastAgentEnv() {
    return lastAgentEnv;
  }

  @Override
  public HostState getState() {
    return stateMachine.getCurrentState();
  }

  @Override
  public void setState(HostState state) {
    stateMachine.setCurrentState(state);
    HostStateEntity hostStateEntity = getHostStateEntity();
    ambariEventPublisher.publish(new HostStateUpdateEvent(getHostName(), state));

    if (hostStateEntity != null) {
      hostStateEntity.setCurrentState(state);
      hostStateEntity.setTimeInState(System.currentTimeMillis());
      hostStateDAO.merge(hostStateEntity);
    }
  }

  @Override
  public void setStateMachineState(HostState state) {
    stateMachine.setCurrentState(state);
    ambariEventPublisher.publish(new HostStateUpdateEvent(getHostName(), state));
  }

  @Override
  public void handleEvent(HostEvent event)
      throws InvalidStateTransitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling Host event, eventType={}, event={}", event.getType().name(), event);
    }
    HostState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
      } catch (InvalidStateTransitionException e) {
        LOG.error("Can't handle Host event at current state"
            + ", host=" + getHostName()
            + ", currentState=" + oldState
            + ", eventType=" + event.getType()
            + ", event=" + event);
        throw e;
      }
    }
    finally {
      writeLock.unlock();
    }
    if (oldState != getState()) {
      ambariEventPublisher.publish(new HostStateUpdateEvent(getHostName(), getState()));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Host transitioned to a new state, host={}, oldState={}, currentState={}, eventType={}, event={}",
          getHostName(), oldState, getState(), event.getType().name(), event);
      }
    }
  }

  @Override
  public String getHostName() {
    return hostName;
  }

  @Override
  public Long getHostId() {
    return hostId;
  }

  @Override
  public Integer getCurrentPingPort() {
    return currentPingPort;
  }

  @Override
  public void setCurrentPingPort(Integer currentPingPort) {
    this.currentPingPort = currentPingPort;
  }

  @Override
  public void setPublicHostName(String hostName) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setPublicHostName(hostName);
    hostDAO.merge(hostEntity);
  }

  @Override
  public String getPublicHostName() {
    return getHostEntity().getPublicHostName();
  }

  @Override
  public String getIPv4() {
    return getHostEntity().getIpv4();
  }

  @Override
  public void setIPv4(String ip) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setIpv4(ip);
    hostDAO.merge(hostEntity);
  }

  @Override
  public String getIPv6() {
    return getHostEntity().getIpv6();
  }

  @Override
  public void setIPv6(String ip) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setIpv6(ip);
    hostDAO.merge(hostEntity);
  }

  @Override
  public int getCpuCount() {
    return getHostEntity().getCpuCount();
  }

  @Override
  public void setCpuCount(int cpuCount) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setCpuCount(cpuCount);
    hostDAO.merge(hostEntity);
  }

  @Override
  public int getPhCpuCount() {
    return getHostEntity().getPhCpuCount();
  }

  @Override
  public void setPhCpuCount(int phCpuCount) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setPhCpuCount(phCpuCount);
    hostDAO.merge(hostEntity);
  }


  @Override
  public long getTotalMemBytes() {
    return getHostEntity().getTotalMem();
  }

  @Override
  public void setTotalMemBytes(long totalMemBytes) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setTotalMem(totalMemBytes);
    hostDAO.merge(hostEntity);
  }

  @Override
  public long getAvailableMemBytes() {
    HostStateEntity hostStateEntity = getHostStateEntity();
    return hostStateEntity != null ? hostStateEntity.getAvailableMem() : 0;
  }

  @Override
  public void setAvailableMemBytes(long availableMemBytes) {
    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      hostStateEntity.setAvailableMem(availableMemBytes);
      hostStateDAO.merge(hostStateEntity);
    }
  }

  @Override
  public String getOsArch() {
    return getHostEntity().getOsArch();
  }

  @Override
  public void setOsArch(String osArch) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setOsArch(osArch);
    hostDAO.merge(hostEntity);
  }

  @Override
  public String getOsInfo() {
    return getHostEntity().getOsInfo();
  }

  @Override
  public void setOsInfo(String osInfo) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setOsInfo(osInfo);
    hostDAO.merge(hostEntity);
  }

  @Override
  public String getOsType() {
    return getHostEntity().getOsType();
  }

  @Override
  public void setOsType(String osType) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setOsType(osType);
    hostDAO.merge(hostEntity);
  }

  @Override
  public String getOsFamily() {
    Map<String, String> hostAttributes = getHostAttributes();
	  return getOSFamilyFromHostAttributes(hostAttributes);
  }

  @Override
  public String getOsFamily(Map<String, String> hostAttributes) {
	  return getOSFamilyFromHostAttributes(hostAttributes);
  }

  @Override
  public String getOSFamilyFromHostAttributes(Map<String, String> hostAttributes) {
    try {
      String majorVersion = hostAttributes.get(OS_RELEASE_VERSION).split("\\.")[0];
      return hostAttributes.get(OSFAMILY) + majorVersion;
    } catch(Exception e) {
      LOG.error("Error while getting os family from host attributes:", e);
    }
    return null;
  }

  @Override
  public List<DiskInfo> getDisksInfo() {
    return disksInfo;
  }

  @Override
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    this.disksInfo = disksInfo;
  }

  @Override
  public RecoveryReport getRecoveryReport() {
    return recoveryReport;
  }

  @Override
  public void setRecoveryReport(RecoveryReport recoveryReport) {
    this.recoveryReport = recoveryReport;
  }

  @Override
  public HostHealthStatus getHealthStatus() {
    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      return gson.fromJson(hostStateEntity.getHealthStatus(), HostHealthStatus.class);
    }

    return null;
  }

  @Override
  public HostHealthStatus getHealthStatus(HostStateEntity hostStateEntity) {
    if (hostStateEntity != null) {
      return gson.fromJson(hostStateEntity.getHealthStatus(), HostHealthStatus.class);
    }

    return null;
  }

  @Override
  public void setHealthStatus(HostHealthStatus healthStatus) {
    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      hostStateEntity.setHealthStatus(gson.toJson(healthStatus));

      if (healthStatus.getHealthStatus().equals(HealthStatus.UNKNOWN)) {
        setStatus(HealthStatus.UNKNOWN.name());
      }

      hostStateDAO.merge(hostStateEntity);
    }
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  @Override
  public void setPrefix(String prefix) {
    if (StringUtils.isNotBlank(prefix) && !StringUtils.equals(this.prefix, prefix)) {
      this.prefix = prefix;
    }
  }

  @Override
  public Map<String, String> getHostAttributes() {
    return gson.fromJson(getHostEntity().getHostAttributes(), hostAttributesType);
  }

  @Override
  public Map<String, String> getHostAttributes(HostEntity hostEntity) {
    return gson.fromJson(hostEntity.getHostAttributes(), hostAttributesType);
  }

  @Override
  public void setHostAttributes(Map<String, String> hostAttributes) {
    HostEntity hostEntity = getHostEntity();
    Map<String, String> hostAttrs = gson.fromJson(hostEntity.getHostAttributes(), hostAttributesType);

    if (hostAttrs == null) {
      hostAttrs = new ConcurrentHashMap<>();
    }

    hostAttrs.putAll(hostAttributes);
    hostEntity.setHostAttributes(gson.toJson(hostAttrs,hostAttributesType));
    hostDAO.merge(hostEntity);
  }

  @Override
  public String getRackInfo() {
    return getHostEntity().getRackInfo();
  }

  @Override
  public void setRackInfo(String rackInfo) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setRackInfo(rackInfo);
    hostDAO.merge(hostEntity);
  }

  @Override
  public long getLastRegistrationTime() {
    return getHostEntity().getLastRegistrationTime();
  }

  @Override
  public void setLastRegistrationTime(long lastRegistrationTime) {
    HostEntity hostEntity = getHostEntity();
    hostEntity.setLastRegistrationTime(lastRegistrationTime);
    hostDAO.merge(hostEntity);
  }

  @Override
  public long getLastHeartbeatTime() {
    return lastHeartbeatTime;
  }

  @Override
  public void setLastHeartbeatTime(long lastHeartbeatTime) {
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  @Override
  public long getLastAgentStartTime() {
    return lastAgentStartTime;
  }

  @Override
  public void setLastAgentStartTime(long lastAgentStartTime) {
    this.lastAgentStartTime = lastAgentStartTime;
  }

  @Override
  public AgentVersion getAgentVersion() {
    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      return gson.fromJson(hostStateEntity.getAgentVersion(), AgentVersion.class);
    }

    return null;
  }

  @Override
  public AgentVersion getAgentVersion(HostStateEntity hostStateEntity) {
    if (hostStateEntity != null) {
      return gson.fromJson(hostStateEntity.getAgentVersion(), AgentVersion.class);
    }

    return null;
  }

  @Override
  public void setAgentVersion(AgentVersion agentVersion) {
    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      hostStateEntity.setAgentVersion(gson.toJson(agentVersion));
      hostStateDAO.merge(hostStateEntity);
    }
  }

  @Override
  public long getTimeInState() {
    HostStateEntity hostStateEntity = getHostStateEntity();
    Long timeInState = hostStateEntity != null ? hostStateEntity.getTimeInState() :  null;
    return timeInState != null ? timeInState : 0L;
  }

  @Override
  public void setTimeInState(long timeInState) {
    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      hostStateEntity.setTimeInState(timeInState);
      hostStateDAO.merge(hostStateEntity);
    }
  }


  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public void setStatus(String status) {
    if (!Objects.equals(this.status, status)) {
      ambariEventPublisher.publish(new HostStatusUpdateEvent(getHostName(), status));
    }
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Host that = (Host) o;

    return getHostName().equals(that.getHostName());
  }

  @Override
  public int hashCode() {
    return (null == getHostName() ? 0 : getHostName().hashCode());
  }

  @Override
  public HostResponse convertToResponse() {
    HostResponse r = new HostResponse(getHostName());

    HostEntity hostEntity = getHostEntity();
    HostStateEntity hostStateEntity = getHostStateEntity();

    Map<String, String> hostAttributes = getHostAttributes(hostEntity);
    r.setHostAttributes(hostAttributes);
    r.setOsFamily(getOsFamily(hostAttributes));

    r.setAgentVersion(getAgentVersion(hostStateEntity));
    r.setHealthStatus(getHealthStatus(hostStateEntity));

    r.setPhCpuCount(hostEntity.getPhCpuCount());
    r.setCpuCount(hostEntity.getCpuCount());
    r.setIpv4(hostEntity.getIpv4());
    r.setOsArch(hostEntity.getOsArch());
    r.setOsType(hostEntity.getOsType());
    r.setTotalMemBytes(hostEntity.getTotalMem());
    r.setLastRegistrationTime(hostEntity.getLastRegistrationTime());
    r.setPublicHostName(hostEntity.getPublicHostName());
    r.setRackInfo(hostEntity.getRackInfo());

    r.setDisksInfo(getDisksInfo());
    r.setStatus(getStatus());
    r.setLastHeartbeatTime(getLastHeartbeatTime());
    r.setLastAgentEnv(lastAgentEnv);
    r.setRecoveryReport(getRecoveryReport());
    r.setRecoverySummary(getRecoveryReport().getSummary());
    r.setHostState(getState());
    return r;
  }

  @Transactional
  void persistEntities(HostEntity hostEntity) {
    hostDAO.create(hostEntity);
    if (!hostEntity.getClusterEntities().isEmpty()) {
      for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
        clusterEntity.getHostEntities().add(hostEntity);
        clusterDAO.merge(clusterEntity);
      }
    }
  }

  @Override
  @Transactional
  public boolean addDesiredConfig(long clusterId, boolean selected, String user, Config config) {
    if (null == user) {
      throw new NullPointerException("User must be specified.");
    }

    HostConfigMapping exist = getDesiredConfigEntity(clusterId, config.getType());
    if (null != exist && exist.getVersion().equals(config.getTag())) {
      if (!selected) {
        exist.setSelected(0);
        hostConfigMappingDAO.merge(exist);
      }
      return false;
    }

    writeLock.lock();

    HostEntity hostEntity = getHostEntity();

    try {
      // set all old mappings for this type to empty
      for (HostConfigMapping e : hostConfigMappingDAO.findByType(clusterId,
          hostEntity.getHostId(), config.getType())) {
        e.setSelected(0);
        hostConfigMappingDAO.merge(e);
      }

      HostConfigMapping hostConfigMapping = new HostConfigMappingImpl();
      hostConfigMapping.setClusterId(clusterId);
      hostConfigMapping.setCreateTimestamp(System.currentTimeMillis());
      hostConfigMapping.setHostId(hostEntity.getHostId());
      hostConfigMapping.setSelected(1);
      hostConfigMapping.setUser(user);
      hostConfigMapping.setType(config.getType());
      hostConfigMapping.setVersion(config.getTag());

      hostConfigMappingDAO.create(hostConfigMapping);
    }
    finally {
      writeLock.unlock();
    }

    hostDAO.merge(hostEntity);

    return true;
  }

  @Override
  public Map<String, DesiredConfig> getDesiredConfigs(long clusterId) {
    Map<String, DesiredConfig> map = new HashMap<>();

    for (HostConfigMapping e : hostConfigMappingDAO.findSelected(
        clusterId, getHostId())) {

      DesiredConfig dc = new DesiredConfig();
      dc.setTag(e.getVersion());
      dc.setServiceName(e.getServiceName());
      map.put(e.getType(), dc);

    }
    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, HostConfig> getDesiredHostConfigs(Cluster cluster,
      Map<String, DesiredConfig> clusterDesiredConfigs) throws AmbariException {
    Map<String, HostConfig> hostConfigMap = new HashMap<>();

    if( null == cluster ){
      clusterDesiredConfigs = new HashMap<>();
    }

    // per method contract, fetch if not supplied
    if (null == clusterDesiredConfigs) {
      clusterDesiredConfigs = cluster.getDesiredConfigs();
    }

    if (clusterDesiredConfigs != null) {
      for (Map.Entry<String, DesiredConfig> desiredConfigEntry
          : clusterDesiredConfigs.entrySet()) {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setDefaultVersionTag(desiredConfigEntry.getValue().getTag());
        hostConfigMap.put(desiredConfigEntry.getKey(), hostConfig);
      }
    }

    Map<Long, ConfigGroup> configGroups = (cluster == null) ? new HashMap<>() : cluster.getConfigGroupsByHostname(getHostName());

    if (configGroups != null && !configGroups.isEmpty()) {
      for (ConfigGroup configGroup : configGroups.values()) {
        for (Map.Entry<String, Config> configEntry : configGroup
            .getConfigurations().entrySet()) {

          String configType = configEntry.getKey();
          // HostConfig config holds configType -> versionTag, per config group
          HostConfig hostConfig = hostConfigMap.get(configType);
          if (hostConfig == null) {
            hostConfig = new HostConfig();
            hostConfigMap.put(configType, hostConfig);
            if (cluster != null) {
              Config conf = cluster.getDesiredConfigByType(configType);
              if(conf == null) {
                LOG.error("Config inconsistency exists:"+
                    " unknown configType="+configType);
              } else {
                hostConfig.setDefaultVersionTag(conf.getTag());
              }
            }
          }
          Config config = configEntry.getValue();
          hostConfig.getConfigGroupOverrides().put(configGroup.getId(),
              config.getTag());
        }
      }
    }
    return hostConfigMap;
  }

  private HostConfigMapping getDesiredConfigEntity(long clusterId, String type) {
    return hostConfigMappingDAO.findSelectedByType(clusterId, getHostId(), type);
  }

  private ConcurrentMap<Long, MaintenanceState> ensureMaintMap(HostStateEntity hostStateEntity) {
    if (null == hostStateEntity || null == hostStateEntity.getMaintenanceState()) {
      return new ConcurrentHashMap<>();
    }

    String entity = hostStateEntity.getMaintenanceState();
    final ConcurrentMap<Long, MaintenanceState> map;

    try {
      Map<Long, MaintenanceState> gsonMap = gson.fromJson(entity, maintMapType);
      map = new ConcurrentHashMap<>(gsonMap);
    } catch (Exception e) {
      return new ConcurrentHashMap<>();
    }

    return map;
  }

  @Override
  public void setMaintenanceState(long clusterId, MaintenanceState state) {
    maintMap.put(clusterId, state);
    String json = gson.toJson(maintMap, maintMapType);

    HostStateEntity hostStateEntity = getHostStateEntity();
    if (hostStateEntity != null) {
      hostStateEntity.setMaintenanceState(json);
      hostStateDAO.merge(hostStateEntity);

      // broadcast the maintenance mode change
      MaintenanceModeEvent event = new MaintenanceModeEvent(state, clusterId, this);
      eventPublisher.publish(event);
    }
  }

  @Override
  public MaintenanceState getMaintenanceState(long clusterId) {
    if (!maintMap.containsKey(clusterId)) {
      maintMap.put(clusterId, MaintenanceState.OFF);
    }

    return maintMap.get(clusterId);
  }

  /**
   * Get all of the HostVersionEntity objects for the host.
   *
   * @return all of the HostVersionEntity objects for the host
   */
  @Override
  public List<HostVersionEntity> getAllHostVersions() {
    return hostVersionDAO.findByHost(getHostName());
  }

  // Get the cached host entity or load it fresh through the DAO.
  @Override
  public HostEntity getHostEntity() {
    return hostDAO.findById(hostId);
  }

  // Get the cached host state entity or load it fresh through the DAO.
  public HostStateEntity getHostStateEntity() {
    return hostStateDAO.findByHostId(hostId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasComponentsAdvertisingVersions(StackId stackId) throws AmbariException {
    HostEntity hostEntity = getHostEntity();

    for (HostComponentStateEntity componentState : hostEntity.getHostComponentStateEntities()) {
      ComponentInfo component = ambariMetaInfo.getComponent(stackId.getStackName(),
          stackId.getStackVersion(), componentState.getServiceName(),
          componentState.getComponentName());

      if (component.isVersionAdvertised()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void calculateHostStatus(Long clusterId) throws AmbariException {
    //Use actual component status to compute the host status
    int masterCount = 0;
    int mastersRunning = 0;
    int slaveCount = 0;
    int slavesRunning = 0;

    StackId stackId;
    Cluster cluster = clusters.getCluster(clusterId);
    stackId = cluster.getDesiredStackVersion();


    List<ServiceComponentHost> scHosts = cluster.getServiceComponentHosts(hostName);
    for (ServiceComponentHost scHost : scHosts) {
      ComponentInfo componentInfo =
          ambariMetaInfo.getComponent(stackId.getStackName(),
              stackId.getStackVersion(), scHost.getServiceName(),
              scHost.getServiceComponentName());

      String status = scHost.getState().name();

      String category = componentInfo.getCategory();
      if (category == null) {
        LOG.warn("In stack {}-{} service {} component {} category is null!",
                stackId.getStackName(), stackId.getStackVersion(), scHost.getServiceName(), scHost.getServiceComponentName());
        continue;
      }

      if (MaintenanceState.OFF == maintenanceStateHelper.getEffectiveState(scHost, this)) {
        if (Objects.equals("MASTER", category)) {
          ++masterCount;
          if (Objects.equals("STARTED", status)) {
            ++mastersRunning;
          }
        } else if (Objects.equals("SLAVE", category)) {
          ++slaveCount;
          if (Objects.equals("STARTED", status)) {
            ++slavesRunning;
          }
        }
      }
    }

    HostHealthStatus.HealthStatus healthStatus;
    if (masterCount == mastersRunning && slaveCount == slavesRunning) {
      healthStatus = HostHealthStatus.HealthStatus.HEALTHY;
    } else if (masterCount > 0 && mastersRunning < masterCount) {
      healthStatus = HostHealthStatus.HealthStatus.UNHEALTHY;
    } else {
      healthStatus = HostHealthStatus.HealthStatus.ALERT;
    }

    setStatus(healthStatus.name());
  }

  @Transactional
  public void updateHost(HostRegistrationRequestEvent e) {
    importHostInfo(e.hostInfo);
    setLastAgentEnv(e.agentEnv);
    setAgentVersion(e.agentVersion);
    setPublicHostName(e.publicHostName);
    setState(HostState.INIT);
  }

  @Transactional
  public void updateHostTimestamps(HostRegistrationRequestEvent e) {
    setLastHeartbeatTime(e.registrationTime);
    setLastRegistrationTime(e.registrationTime);
    setLastAgentStartTime(e.agentStartTime);
    setTimeInState(e.registrationTime);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRepositoryVersionCorrect(RepositoryVersionEntity repositoryVersion)
      throws AmbariException {
    HostEntity hostEntity = getHostEntity();
    Collection<HostComponentStateEntity> hostComponentStates = hostEntity.getHostComponentStateEntities();

    // for every host component, if it matches the desired repo and has reported
    // the correct version then we're good
    for (HostComponentStateEntity hostComponentState : hostComponentStates) {
      ServiceComponentDesiredStateEntity desiredComponmentState = hostComponentState.getServiceComponentDesiredStateEntity();
      RepositoryVersionEntity desiredRepositoryVersion = desiredComponmentState.getDesiredRepositoryVersion();

      ComponentInfo componentInfo = ambariMetaInfo.getComponent(
          desiredRepositoryVersion.getStackName(), desiredRepositoryVersion.getStackVersion(),
          hostComponentState.getServiceName(), hostComponentState.getComponentName());

      // skip components which don't advertise a version
      if (!componentInfo.isVersionAdvertised()) {
        continue;
      }

      // we only care about checking the specified repo version for this host
      if (!repositoryVersion.equals(desiredRepositoryVersion)) {
        continue;
      }

      String versionAdvertised = hostComponentState.getVersion();
      if (hostComponentState.getUpgradeState() == UpgradeState.IN_PROGRESS
          || !StringUtils.equals(versionAdvertised, repositoryVersion.getVersion())) {
        return false;
      }
    }

    return true;
  }
}


