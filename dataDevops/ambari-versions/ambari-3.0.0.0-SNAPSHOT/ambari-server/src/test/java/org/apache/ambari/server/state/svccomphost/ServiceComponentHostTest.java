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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class ServiceComponentHostTest {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceComponentHostTest.class);
  @Inject
  private Injector injector;
  @Inject
  private Clusters clusters;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;
  @Inject
  private ConfigFactory configFactory;
  @Inject
  private ConfigGroupFactory configGroupFactory;
  @Inject
  private OrmTestHelper helper;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  private String clusterName = "c1";
  private String hostName1 = "h1";
  private Map<String, String> hostAttributes = new HashMap<>();
  private RepositoryVersionEntity repositoryVersion;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    StackId stackId = new StackId("HDP-2.0.6");
    createCluster(stackId, clusterName);

    repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    Set<String> hostNames = new HashSet<>();
    hostNames.add(hostName1);

    addHostsToCluster(clusterName, hostAttributes, hostNames);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private ClusterEntity createCluster(StackId stackId, String clusterName) throws AmbariException {
    helper.createStack(stackId);
    clusters.addCluster(clusterName, stackId);
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
    Assert.assertNotNull(clusterEntity);
    return clusterEntity;
  }

  private void addHostsToCluster(String clusterName, Map<String, String> hostAttributes, Set<String> hostNames) throws AmbariException {
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);

    List<HostEntity> hostEntities = new ArrayList<>();
    for (String hostName : hostNames) {
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);
      host.setHostAttributes(hostAttributes);
    }

    clusterEntity.setHostEntities(hostEntities);
    clusterDAO.merge(clusterEntity);

    clusters.mapAndPublishHostsToCluster(hostNames, clusterName);
  }

  private ServiceComponentHost createNewServiceComponentHost(String clusterName,
      String svc,
      String svcComponent,
      String hostName, boolean isClient) throws AmbariException{
    Cluster c = clusters.getCluster(clusterName);
    Assert.assertNotNull(c.getConfigGroups());
    return createNewServiceComponentHost(c, svc, svcComponent, hostName);
  }

  private ServiceComponentHost createNewServiceComponentHost(
      Cluster c,
      String svc,
      String svcComponent,
      String hostName) throws AmbariException{

    Service s = null;

    try {
      s = c.getService(svc);
    } catch (ServiceNotFoundException e) {
      LOG.debug("Calling service create, serviceName={}", svc);

      s = serviceFactory.createNew(c, svc, repositoryVersion);
      c.addService(s);
    }

    ServiceComponent sc = null;
    try {
      sc = s.getServiceComponent(svcComponent);
    } catch (ServiceComponentNotFoundException e) {
      sc = serviceComponentFactory.createNew(s, svcComponent);
      s.addServiceComponent(sc);
    }

    ServiceComponentHost impl = serviceComponentHostFactory.createNew(
        sc, hostName);

    Assert.assertEquals(State.INIT, impl.getState());
    Assert.assertEquals(State.INIT, impl.getDesiredState());
    Assert.assertEquals(c.getClusterName(), impl.getClusterName());
    Assert.assertEquals(c.getClusterId(), impl.getClusterId());
    Assert.assertEquals(s.getName(), impl.getServiceName());
    Assert.assertEquals(sc.getName(), impl.getServiceComponentName());
    Assert.assertEquals(hostName, impl.getHostName());

    Assert.assertNotNull(c.getServiceComponentHosts(hostName));

    Assert.assertNotNull(sc.getDesiredRepositoryVersion());

    return impl;
  }

  private ServiceComponentHostEvent createEvent(ServiceComponentHostImpl impl,
      long timestamp, ServiceComponentHostEventType eventType)
      throws AmbariException {

    Cluster c = clusters.getCluster(clusterName);
    if (c.getConfig("time", String.valueOf(timestamp)) == null) {
      Config config = configFactory.createNew (c, "time", String.valueOf(timestamp),
        new HashMap<>(), new HashMap<>());
    }

    switch (eventType) {
      case HOST_SVCCOMP_INSTALL:
        return new ServiceComponentHostInstallEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp,
            impl.getServiceComponent().getDesiredStackId().toString());
      case HOST_SVCCOMP_START:
        return new ServiceComponentHostStartEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_STOP:
        return new ServiceComponentHostStopEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_UNINSTALL:
        return new ServiceComponentHostUninstallEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_FAILED:
        return new ServiceComponentHostOpFailedEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_SUCCEEDED:
        return new ServiceComponentHostOpSucceededEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_IN_PROGRESS:
        return new ServiceComponentHostOpInProgressEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_OP_RESTART:
        return new ServiceComponentHostOpRestartedEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_DISABLE:
          return new ServiceComponentHostDisableEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      case HOST_SVCCOMP_WIPEOUT:
        return new ServiceComponentHostWipeoutEvent(
            impl.getServiceComponentName(), impl.getHostName(), timestamp);
      default:
        return null;
    }
  }

  private void runStateChanges(ServiceComponentHostImpl impl,
      ServiceComponentHostEventType startEventType,
      State startState,
      State inProgressState,
      State failedState,
      State completedState)
    throws Exception {
    long timestamp = 0;

    boolean checkStack = false;
    if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL) {
      checkStack = true;
    }

    Assert.assertEquals(startState,
        impl.getState());
    ServiceComponentHostEvent startEvent = createEvent(impl, ++timestamp,
        startEventType);

    long startTime = timestamp;
    impl.handleEvent(startEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());
    if (checkStack) {
      Assert.assertNotNull(impl.getServiceComponent().getDesiredStackId());
    }

    ServiceComponentHostEvent installEvent2 = createEvent(impl, ++timestamp,
        startEventType);

    boolean exceptionThrown = false;
    LOG.info("Transitioning from " + impl.getState() + " " + installEvent2.getType());
    try {
      impl.handleEvent(installEvent2);
    } catch (Exception e) {
      exceptionThrown = true;
    }
    if (impl.getState() == State.INSTALLING || impl.getState() == State.STARTING
      || impl.getState() == State.UNINSTALLING
        || impl.getState() == State.WIPING_OUT
        || impl.getState() == State.STARTED
        ) {
      startTime = timestamp;
    // Exception is not expected on valid event
      Assert.assertTrue("Exception not thrown on invalid event", !exceptionThrown);
    }
    else {
      Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);
    }
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent1 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent1);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent2 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());


    ServiceComponentHostOpFailedEvent failEvent = new
        ServiceComponentHostOpFailedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    long endTime = timestamp;
    impl.handleEvent(failEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(failedState,
        impl.getState());

    ServiceComponentHostOpRestartedEvent restartEvent = new
        ServiceComponentHostOpRestartedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    startTime = timestamp;
    impl.handleEvent(restartEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent3 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent3);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpFailedEvent failEvent2 = new
        ServiceComponentHostOpFailedEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    endTime = timestamp;
    impl.handleEvent(failEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(failedState,
        impl.getState());

    ServiceComponentHostEvent startEvent2 = createEvent(impl, ++timestamp,
        startEventType);
    startTime = timestamp;
    impl.handleEvent(startEvent2);
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostOpInProgressEvent inProgressEvent4 = new
        ServiceComponentHostOpInProgressEvent(impl.getServiceComponentName(),
            impl.getHostName(), ++timestamp);
    impl.handleEvent(inProgressEvent4);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(inProgressState,
        impl.getState());

    ServiceComponentHostEvent succeededEvent;
    if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_START) {
      succeededEvent = new ServiceComponentHostStartedEvent(impl.getServiceComponentName(),
          impl.getHostName(), ++timestamp);
    } else if (startEventType == ServiceComponentHostEventType.HOST_SVCCOMP_STOP) {
      succeededEvent = new ServiceComponentHostStoppedEvent(impl.getServiceComponentName(),
          impl.getHostName(), ++timestamp);
    } else {
      succeededEvent = new
          ServiceComponentHostOpSucceededEvent(impl.getServiceComponentName(),
          impl.getHostName(), ++timestamp);
    }

    endTime = timestamp;
    impl.handleEvent(succeededEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(timestamp, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(endTime, impl.getLastOpEndTime());
    Assert.assertEquals(completedState,
        impl.getState());

  }

  @Test
  public void testClientStateFlow() throws Exception {
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost(clusterName, "HDFS", "HDFS_CLIENT", hostName1, true);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        State.INIT,
        State.INSTALLING,
        State.INSTALL_FAILED,
        State.INSTALLED);

    boolean exceptionThrown = false;
    try {
      runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
        State.INSTALLED,
        State.STARTING,
        State.INSTALLED,
        State.STARTED);
    }
    catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        State.INSTALLED,
        State.UNINSTALLING,
        State.UNINSTALLING,
        State.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
        State.UNINSTALLED,
        State.WIPING_OUT,
        State.WIPING_OUT,
        State.INIT);

    // check can be removed
    for (State state : State.values()) {
      impl.setState(state);

      if (state.isRemovableState()) {
        Assert.assertTrue(impl.canBeRemoved());
      }
      else {
        Assert.assertFalse(impl.canBeRemoved());
      }
    }

  }

  @Test
  public void testDaemonStateFlow() throws Exception {
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl)
        createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
        State.INIT,
        State.INSTALLING,
        State.INSTALL_FAILED,
        State.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_START,
      State.INSTALLED,
      State.STARTING,
      State.INSTALLED,
      State.STARTED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      State.STARTED,
      State.STOPPING,
      State.STARTED,
      State.INSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
        State.INSTALLED,
        State.UNINSTALLING,
        State.UNINSTALLING,
        State.UNINSTALLED);

    runStateChanges(impl, ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
            State.UNINSTALLED,
            State.WIPING_OUT,
            State.WIPING_OUT,
            State.INIT);
  }

  @Ignore
  @Test
  public void testJobHandling() {
    // TODO fix once jobs are handled
  }

  @Ignore
  @Test
  public void testGetAndSetConfigs() {
    // FIXME config handling
    /*
    public Map<String, Config> getDesiredConfigs();
    public void updateDesiredConfigs(Map<String, Config> configs);
    public Map<String, Config> getConfigs();
    public void updateConfigs(Map<String, Config> configs);
    */
  }

  @Test
  public void testGetAndSetBasicInfo() throws AmbariException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "NAMENODE", hostName1, false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);

    Assert.assertEquals(State.INSTALLING, sch.getState());
    Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
  }

  @Test
  @Ignore
  //TODO Should be rewritten after actual configs calculate workflow change.
  public void testActualConfigs() throws Exception {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "NAMENODE", hostName1, false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);

    Cluster cluster = clusters.getCluster(clusterName);

    final ConfigGroup configGroup = configGroupFactory.createNew(cluster, "HDFS",
      "cg1", "t1", "", new HashMap<>(), new HashMap<>());

    cluster.addConfigGroup(configGroup);

    Map<String, Map<String,String>> actual =
        new HashMap<String, Map<String, String>>() {{
          put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
          put("core-site", new HashMap<String,String>() {{ put("tag", "version1");
            put(configGroup.getId().toString(), "version2"); }});
        }};

    //sch.updateActualConfigs(actual);

    Map<String, HostConfig> confirm = sch.getActualConfigs();

    Assert.assertEquals(2, confirm.size());
    Assert.assertTrue(confirm.containsKey("global"));
    Assert.assertTrue(confirm.containsKey("core-site"));
    Assert.assertEquals(1, confirm.get("core-site").getConfigGroupOverrides().size());
  }

  @Test
  public void testConvertToResponse() throws AmbariException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLING);
    ServiceComponentHostResponse r = sch.convertToResponse(null);
    Assert.assertEquals("HDFS", r.getServiceName());
    Assert.assertEquals("DATANODE", r.getComponentName());
    Assert.assertEquals(hostName1, r.getHostname());
    Assert.assertEquals(clusterName, r.getClusterName());
    Assert.assertEquals(State.INSTALLED.toString(), r.getDesiredState());
    Assert.assertEquals(State.INSTALLING.toString(), r.getLiveState());
    Assert.assertEquals(repositoryVersion.getStackId().toString(), r.getDesiredStackVersion());

    Assert.assertFalse(r.isStaleConfig());

    // TODO check configs

    StringBuilder sb = new StringBuilder();
    sch.debugDump(sb);
    Assert.assertFalse(sb.toString().isEmpty());
  }

  @Test
  public void testStopInVariousStates() throws AmbariException, InvalidStateTransitionException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);
    ServiceComponentHostImpl impl =  (ServiceComponentHostImpl) sch;

    sch.setDesiredState(State.STARTED);
    sch.setState(State.INSTALLED);

    long timestamp = 0;

    ServiceComponentHostEvent stopEvent = createEvent(impl, ++timestamp,
        ServiceComponentHostEventType.HOST_SVCCOMP_STOP);

    long startTime = timestamp;
    impl.handleEvent(stopEvent);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(State.STOPPING,
        impl.getState());

    sch.setState(State.INSTALL_FAILED);

    boolean exceptionThrown = false;
    try {
      impl.handleEvent(stopEvent);
    } catch (Exception e) {
      exceptionThrown = true;
    }
    Assert.assertTrue("Exception not thrown on invalid event", exceptionThrown);

    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());

    sch.setState(State.INSTALLED);
    ServiceComponentHostEvent stopEvent2 = createEvent(impl, ++timestamp,
        ServiceComponentHostEventType.HOST_SVCCOMP_STOP);

    startTime = timestamp;
    impl.handleEvent(stopEvent2);
    Assert.assertEquals(startTime, impl.getLastOpStartTime());
    Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
    Assert.assertEquals(-1, impl.getLastOpEndTime());
    Assert.assertEquals(State.STOPPING,
            impl.getState());
  }

  @Test
  public void testDisableInVariousStates() throws AmbariException, InvalidStateTransitionException {
    ServiceComponentHost sch = createNewServiceComponentHost(clusterName, "HDFS",
        "DATANODE", hostName1, false);
    ServiceComponentHostImpl impl = (ServiceComponentHostImpl) sch;

    // Test valid states in which host component can be disabled
    long timestamp = 0;
    HashSet<State> validStates = new HashSet<>();
    validStates.add(State.INSTALLED);
    validStates.add(State.INSTALL_FAILED);
    validStates.add(State.UNKNOWN);
    validStates.add(State.DISABLED);
    for (State state : validStates) {
      sch.setState(state);
      ServiceComponentHostEvent disableEvent = createEvent(impl, ++timestamp,
          ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE);
      impl.handleEvent(disableEvent);
      // TODO: At present operation timestamps are not getting updated.
      Assert.assertEquals(-1, impl.getLastOpStartTime());
      Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
      Assert.assertEquals(-1, impl.getLastOpEndTime());
      Assert.assertEquals(State.DISABLED, impl.getState());
    }

    // Test invalid states in which host component cannot be disabled
    HashSet<State> invalidStates = new HashSet<>();
    invalidStates.add(State.INIT);
    invalidStates.add(State.INSTALLING);
    invalidStates.add(State.STARTING);
    invalidStates.add(State.STARTED);
    invalidStates.add(State.STOPPING);
    invalidStates.add(State.UNINSTALLING);
    invalidStates.add(State.UNINSTALLED);
    invalidStates.add(State.UPGRADING);

    for (State state : invalidStates) {
      sch.setState(state);
      ServiceComponentHostEvent disableEvent = createEvent(impl, ++timestamp,
          ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE);
      boolean exceptionThrown = false;
      try {
        impl.handleEvent(disableEvent);
      } catch (Exception e) {
        exceptionThrown = true;
      }
      Assert.assertTrue("Exception not thrown on invalid event",
          exceptionThrown);
      // TODO: At present operation timestamps are not getting updated.
      Assert.assertEquals(-1, impl.getLastOpStartTime());
      Assert.assertEquals(-1, impl.getLastOpLastUpdateTime());
      Assert.assertEquals(-1, impl.getLastOpEndTime());
    }
  }


  @Test
  @Ignore
  //TODO Should be rewritten after stale configs calculate workflow change.
  public void testStaleConfigs() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    final HostEntity hostEntity = hostDAO.findByName(hostName);
    Assert.assertNotNull(hostEntity.getHostId());

    Cluster cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);
    ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName);

    sch1.getServiceComponent().setDesiredRepositoryVersion(repositoryVersion);

    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);

    sch2.setDesiredState(State.INSTALLED);
    sch2.setState(State.INSTALLING);

    sch3.setDesiredState(State.INSTALLED);
    sch3.setState(State.INSTALLING);

    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    makeConfig(cluster, "hdfs-site", "version0",
        new HashMap<String,String>() {{
          put("a", "b");
        }}, new HashMap<>());

    Map<String, Map<String, String>> actual = new HashMap<String, Map<String, String>>() {{
      put("hdfs-site", new HashMap<String,String>() {{ put("tag", "version0"); }});
    }};

    /*sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    sch3.updateActualConfigs(actual);*/

    makeConfig(cluster, "foo", "version1",
        new HashMap<String,String>() {{ put("a", "c"); }}, new HashMap<>());

    // HDP-x/HDFS does not define type 'foo', so changes do not count to stale
    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    makeConfig(cluster, "hdfs-site", "version1",
        new HashMap<String,String>() {{ put("a1", "b1"); }}, new HashMap<>());

    // HDP-x/HDFS/hdfs-site is not on the actual, but it is defined, so it is stale
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());

    actual.put("hdfs-site", new HashMap<String, String>() {{ put ("tag", "version1"); }});

    //sch1.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site up to date, only for sch1
    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());

    //sch2.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site up to date for both
    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    makeConfig(cluster, "hdfs-site", "version2",
        new HashMap<String, String>() {{ put("dfs.journalnode.http-address", "http://foo"); }},
      new HashMap<>());

    // HDP-x/HDFS/hdfs-site updated to changed property
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());

    actual.get("hdfs-site").put("tag", "version2");
    /*sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);*/
    //reset restartRequired flag + invalidating isStale cache
    // after start/restart command execution completed
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    // HDP-x/HDFS/hdfs-site updated to changed property
    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    // make a host override
    final Host host = clusters.getHostsForCluster(clusterName).get(hostName);
    Assert.assertNotNull(host);

    final Config c = configFactory.createNew(cluster, "hdfs-site", "version3",
        new HashMap<String, String>() {{ put("dfs.journalnode.http-address", "http://goo"); }},
      new HashMap<>());

    host.addDesiredConfig(cluster.getClusterId(), true, "user", c);
    ConfigGroup configGroup = configGroupFactory.createNew(cluster, "HDFS", "g1",
      "t1", "", new HashMap<String, Config>() {{ put("hdfs-site", c); }},
      new HashMap<Long, Host>() {{ put(hostEntity.getHostId(), host); }});
    cluster.addConfigGroup(configGroup);

    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());

    actual.get("hdfs-site").put(configGroup.getId().toString(), "version3");
    //sch2.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    //sch1.updateActualConfigs(actual);
    // HDP-x/HDFS/hdfs-site updated host to changed property
    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    makeConfig(cluster, "mapred-site", "version1",
      new HashMap<String, String>() {{ put("a", "b"); }},
      new HashMap<>());

    actual.put("mapred-site", new HashMap<String, String>() {{ put ("tag", "version1"); }});

    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse(null).isStaleConfig());

    //sch3.updateActualConfigs(actual);

    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());

    // Change core-site property, only HDFS property
    makeConfig(cluster, "core-site", "version1",
      new HashMap<String,String>() {{
        put("a", "b");
        put("fs.trash.interval", "360"); // HDFS only
      }}, new HashMap<>());

    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse(null).isStaleConfig());

    actual.put("core-site", new HashMap<String, String>() {{
      put("tag", "version1");
    }});

    //sch1.updateActualConfigs(actual);

    final Config c1 = configFactory.createNew(cluster, "core-site", "version2",
      new HashMap<String, String>() {{ put("fs.trash.interval", "400"); }},
      new HashMap<>());
    configGroup = configGroupFactory.createNew(cluster, "HDFS", "g2",
      "t2", "", new HashMap<String, Config>() {{ put("core-site", c1); }},
      new HashMap<Long, Host>() {{ put(hostEntity.getHostId(), host); }});
    cluster.addConfigGroup(configGroup);

    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse(null).isStaleConfig());

    // Test actual configs are updated for deleted config group
    Long id = configGroup.getId();
    HashMap<String, String> tags = new HashMap<>(2);
    tags.put("tag", "version1");
    tags.put(id.toString(), "version2");
    actual.put("core-site", tags);
    //sch3.updateActualConfigs(actual);

    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());

    cluster.deleteConfigGroup(id);
    Assert.assertNull(cluster.getConfigGroups().get(id));

    //sch3.updateActualConfigs(actual);
    Assert.assertTrue(sch3.convertToResponse(null).isStaleConfig());

    tags.remove(id.toString());
    //sch3.updateActualConfigs(actual);
    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());
  }

  @Test
  @Ignore
  //TODO Should be rewritten after stale configs calculate workflow change.
  public void testStaleConfigsAttributes() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    Cluster cluster = clusters.getCluster(clusterName);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);
    ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName);

    sch1.getServiceComponent().setDesiredRepositoryVersion(repositoryVersion);

    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);

    sch2.setDesiredState(State.INSTALLED);
    sch2.setState(State.INSTALLING);

    sch3.setDesiredState(State.INSTALLED);
    sch3.setState(State.INSTALLING);

    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());

    makeConfig(cluster, "global", "version1",
        new HashMap<String,String>() {{
          put("a", "b");
          put("dfs_namenode_name_dir", "/foo1"); // HDFS only
          put("mapred_log_dir_prefix", "/foo2"); // MR2 only
        }}, new HashMap<>());
    makeConfig(cluster, "hdfs-site", "version1",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, new HashMap<>());
    Map<String, Map<String, String>> actual = new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
      put("hdfs-site", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }};

    /*sch1.updateActualConfigs(actual);
    sch2.updateActualConfigs(actual);
    sch3.updateActualConfigs(actual);*/

    makeConfig(cluster, "mapred-site", "version1",
      new HashMap<String,String>() {{ put("a", "c"); }},new HashMap<String, Map<String,String>>(){{
       put("final", new HashMap<String, String>(){{
         put("a", "true");
       }});
      }});
    // HDP-x/HDFS does not define type 'foo', so changes do not count to stale
    Assert.assertFalse(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch3.convertToResponse(null).isStaleConfig());
    actual = new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
      put("mapred-site", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }};
    sch3.setRestartRequired(false);
    //sch3.updateActualConfigs(actual);
    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());

    // Now add config-attributes
    Map<String, Map<String, String>> c1PropAttributes = new HashMap<>();
    c1PropAttributes.put("final", new HashMap<>());
    c1PropAttributes.get("final").put("hdfs1", "true");
    makeConfig(cluster, "hdfs-site", "version2",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, c1PropAttributes);
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    sch3.setRestartRequired(false);
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());

    // Now change config-attributes
    Map<String, Map<String, String>> c2PropAttributes = new HashMap<>();
    c2PropAttributes.put("final", new HashMap<>());
    c2PropAttributes.get("final").put("hdfs1", "false");
    makeConfig(cluster, "hdfs-site", "version3",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, c2PropAttributes);
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    sch3.setRestartRequired(false);
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());

    // Now change config-attributes
    makeConfig(cluster, "hdfs-site", "version4",
        new HashMap<String,String>() {{
          put("hdfs1", "hdfs1value1");
        }}, new HashMap<>());
    sch1.setRestartRequired(false);
    sch2.setRestartRequired(false);
    sch3.setRestartRequired(false);
    Assert.assertTrue(sch1.convertToResponse(null).isStaleConfig());
    Assert.assertTrue(sch2.convertToResponse(null).isStaleConfig());
    Assert.assertFalse(sch3.convertToResponse(null).isStaleConfig());

  }

  /**
   * Helper method to create a configuration
   * @param cluster the cluster
   * @param type the config type
   * @param tag the config tag
   * @param values the values for the config
   */
  private void makeConfig(Cluster cluster, String type, String tag, Map<String, String> values, Map<String, Map<String, String>> attributes) throws AmbariException {
    Config config = configFactory.createNew(cluster, type, tag, values, attributes);
    cluster.addDesiredConfig("user", Collections.singleton(config));
  }

  @Test
  public void testMaintenance() throws Exception {
    String stackVersion = "HDP-2.0.6";
    StackId stackId = new StackId(stackVersion);
    String clusterName = "c2";
    createCluster(stackId, clusterName);

    final String hostName = "h3";
    Set<String> hostNames = new HashSet<>();
    hostNames.add(hostName);
    addHostsToCluster(clusterName, hostAttributes, hostNames);

    Cluster cluster = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    HostEntity hostEntity = hostDAO.findByName(hostName);
    Assert.assertNotNull(hostEntity);

    ServiceComponentHost sch1 = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    //ServiceComponentHost sch2 = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);
    //ServiceComponentHost sch3 = createNewServiceComponentHost(cluster, "MAPREDUCE2", "HISTORYSERVER", hostName);

    HostComponentDesiredStateEntity entity = hostComponentDesiredStateDAO.findByIndex(
      cluster.getClusterId(),
      sch1.getServiceName(),
      sch1.getServiceComponentName(),
      hostEntity.getHostId()
    );
    Assert.assertEquals(MaintenanceState.OFF, entity.getMaintenanceState());
    Assert.assertEquals(MaintenanceState.OFF, sch1.getMaintenanceState());

    sch1.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON, sch1.getMaintenanceState());

    entity = hostComponentDesiredStateDAO.findByIndex(
      cluster.getClusterId(),
      sch1.getServiceName(),
      sch1.getServiceComponentName(),
      hostEntity.getHostId()
    );
    Assert.assertEquals(MaintenanceState.ON, entity.getMaintenanceState());
  }

  /**
   * Tests that the host version for a repository can transition properly to
   * CURRENT even if other components on that host have not reported in correct
   * for their own repo versions. This assures that the host version logic is
   * scoped to the repo that is transitioning and is not affected by other
   * components.
   *
   * @throws Exception
   */
  @Test
  public void testHostVersionTransitionIsScopedByRepository() throws Exception {
    // put the existing host versions OUT_OF_SYNC
    HostEntity hostEntity = hostDAO.findByName(hostName1);
    Collection<HostVersionEntity> hostVersions = hostEntity.getHostVersionEntities();
    Assert.assertEquals(1, hostVersions.size());
    hostVersions.iterator().next().setState(RepositoryVersionState.OUT_OF_SYNC);
    hostDAO.merge(hostEntity);

    ServiceComponentHost namenode = createNewServiceComponentHost(clusterName, "HDFS", "NAMENODE", hostName1, false);
    namenode.setDesiredState(State.STARTED);
    namenode.setState(State.STARTED);

    ServiceComponentHost datanode = createNewServiceComponentHost(clusterName, "HDFS", "DATANODE", hostName1, false);
    datanode.setDesiredState(State.STARTED);
    datanode.setState(State.STARTED);

    ServiceComponentHost zkServer = createNewServiceComponentHost(clusterName, "ZOOKEEPER", "ZOOKEEPER_SERVER", hostName1, false);
    zkServer.setDesiredState(State.STARTED);
    zkServer.setState(State.STARTED);

    ServiceComponentHost zkClient = createNewServiceComponentHost(clusterName, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostName1, true);
    zkClient.setDesiredState(State.STARTED);
    zkClient.setState(State.STARTED);

    // put some host components into a bad state
    hostEntity = hostDAO.findByName(hostName1);
    Collection<HostComponentStateEntity> hostComponentStates = hostEntity.getHostComponentStateEntities();
    for( HostComponentStateEntity hostComponentState : hostComponentStates ) {
      if( StringUtils.equals("HDFS", hostComponentState.getServiceName() ) ) {
        hostComponentState.setVersion(State.UNKNOWN.name());
        hostComponentStateDAO.merge(hostComponentState);
      }
    }

    // create the repo just for ZK
    StackId stackId = new StackId("HDP-2.2.0");
    RepositoryVersionEntity patchRepositoryVersion = helper.getOrCreateRepositoryVersion(stackId, "2.2.0.0-1");

    // create the new host version
    zkServer.getServiceComponent().setDesiredRepositoryVersion(patchRepositoryVersion);
    zkClient.getServiceComponent().setDesiredRepositoryVersion(patchRepositoryVersion);

    helper.createHostVersion(hostName1, patchRepositoryVersion, RepositoryVersionState.INSTALLED);

    // move ZK components to UPGRADED and reporting the new version
    hostEntity = hostDAO.findByName(hostName1);
    hostComponentStates = hostEntity.getHostComponentStateEntities();
    for( HostComponentStateEntity hostComponentState : hostComponentStates ) {
      if( StringUtils.equals("ZOOKEEPER", hostComponentState.getServiceName() ) ) {
        hostComponentState.setVersion(patchRepositoryVersion.getVersion());
        hostComponentState.setUpgradeState(UpgradeState.COMPLETE);
        hostComponentStateDAO.merge(hostComponentState);
      }
    }

    hostEntity = hostDAO.merge(hostEntity);

    zkServer.recalculateHostVersionState();

    // very transition to CURRENT
    hostVersions = hostEntity.getHostVersionEntities();
    Assert.assertEquals(2, hostVersions.size());

    for (HostVersionEntity hostVersion : hostVersions) {
      if (hostVersion.getRepositoryVersion().equals(repositoryVersion)) {
        Assert.assertEquals(RepositoryVersionState.OUT_OF_SYNC, hostVersion.getState());
      } else if (hostVersion.getRepositoryVersion().equals(patchRepositoryVersion)) {
        Assert.assertEquals(RepositoryVersionState.CURRENT, hostVersion.getState());
      } else {
        Assert.fail("Unexpected repository version");
      }
    }
  }
}