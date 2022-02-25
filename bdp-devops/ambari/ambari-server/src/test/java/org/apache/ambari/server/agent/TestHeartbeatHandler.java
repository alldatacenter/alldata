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
package org.apache.ambari.server.agent;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyClusterId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCurrentPingPort;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostStatus;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOSRelease;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOs;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOsType;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyStackId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS_CLIENT;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.SECONDARY_NAMENODE;
import static org.apache.ambari.server.controller.KerberosHelperImpl.SET_KEYTAB;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionManagerTestHelper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.HostStatus.Status;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.publishers.AgentCommandsPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriter;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class TestHeartbeatHandler {

  private static final Logger log = LoggerFactory.getLogger(TestHeartbeatHandler.class);
  private Injector injector;
  private Clusters clusters;
  long requestId = 23;
  long stageId = 31;

  @Inject
  AmbariMetaInfo metaInfo;

  @Inject
  Configuration config;

  @Inject
  ActionDBAccessor actionDBAccessor;

  @Inject
  StageFactory stageFactory;

  @Inject
  HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  ActionManagerTestHelper actionManagerTestHelper;

  @Inject
  AuditLogger auditLogger;

  @Inject
  private OrmTestHelper helper;

  @Inject
  private AgentCommandsPublisher agentCommandsPublisher;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();


  private InMemoryDefaultTestModule module;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    EasyMock.replay(auditLogger);
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    EasyMock.reset(auditLogger);
  }

  @Test
  public void testHeartbeat() throws Exception {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(new ArrayList<>());
    replay(am);

    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);
    Collection<Host> hosts = cluster.getHosts();
    assertEquals(hosts.size(), 1);

    Host hostObject = hosts.iterator().next();
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");
    hostObject.setOsType(DummyOsType);

    String hostname = hostObject.getHostName();

    HeartBeatHandler handler = createHeartBeatHandler();
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(hostname);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);

    hostObject.setState(HostState.UNHEALTHY);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    HostStatus hs = new HostStatus(Status.HEALTHY, DummyHostStatus);
    List<Alert> al = new ArrayList<>();
    al.add(new Alert());
    hb.setNodeStatus(hs);
    hb.setHostname(hostname);

    handler.handleHeartBeat(hb);
    assertEquals(HostState.HEALTHY, hostObject.getState());
  }

  @Test
  public void testStatusHeartbeatWithAnnotation() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<>();
    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartBeatResponse resp = handler.handleHeartBeat(hb);
    Assert.assertFalse(resp.hasMappedComponents());

    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INIT);

    hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());
    hb.setComponentStatus(componentStatuses);

    resp = handler.handleHeartBeat(hb);
    Assert.assertTrue(resp.hasMappedComponents());
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testLiveStatusUpdateAfterStopFailed() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).
        addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).
        addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.
            getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).
            getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.
            getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).
            getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.STARTED);
    serviceComponentHost2.setState(State.STARTED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<>();

    ComponentStatus componentStatus1 = new ComponentStatus();
    //componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(DATANODE);
    componentStatuses.add(componentStatus1);

    ComponentStatus componentStatus2 = new ComponentStatus();
    //componentStatus2.setClusterName(DummyCluster);
    componentStatus2.setServiceName(HDFS);
    componentStatus2.setMessage(DummyHostStatus);
    componentStatus2.setStatus(State.INSTALLED.name());
    componentStatus2.setComponentName(NAMENODE);
    componentStatuses.add(componentStatus2);

    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
  }


  @Test
  public void testRegistration() throws Exception,
      InvalidStateTransitionException {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setCurrentPingPort(DummyCurrentPingPort);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    reg.setPrefix(Configuration.PREFIX_DIR);
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertEquals(DummyCurrentPingPort, hostObject.getCurrentPingPort());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
  }

  private HeartBeatHandler createHeartBeatHandler() {
    return new HeartBeatHandler(clusters, actionManagerTestHelper.getMockActionManager(), Encryptor.NONE, injector);
  }

  @Test
  @Ignore
  // TODO should be rewritten after STOMP protocol implementation.
  public void testRegistrationRecoveryConfig() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);

    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);

    // Create helper after creating service to avoid race condition caused by asynchronous recovery configs
    // timestamp invalidation (RecoveryConfigHelper.handleServiceComponentInstalledEvent())
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    handler.start();

    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setCurrentPingPort(DummyCurrentPingPort);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse rr = handler.handleRegistration(reg);
    RecoveryConfig rc = rr.getRecoveryConfig();
    assertEquals(rc.getEnabledComponents(), "DATANODE,NAMENODE");

    // Send a heart beat with the recovery timestamp set to the
    // recovery timestamp from registration. The heart beat
    // response should not contain a recovery config since
    // nothing changed between the registration and heart beat.
    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));

    HeartBeatResponse hbr = handler.handleHeartBeat(hb);
    assertNull(hbr.getRecoveryConfig());
    handler.stop();
  }

  //
  // Same as testRegistrationRecoveryConfig but will test
  // maintenance mode set to ON for a service component host
  //
  @Test
  @Ignore
  // TODO should be rewritten after STOMP protocol implementation.
  public void testRegistrationRecoveryConfigMaintenanceMode()
      throws Exception, InvalidStateTransitionException {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);

    /*
     * Add three service components enabled for auto start.
     */
    hdfs.addServiceComponent(DATANODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(NAMENODE).setRecoveryEnabled(true);
    hdfs.getServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);

    hdfs.addServiceComponent(HDFS_CLIENT).setRecoveryEnabled(true);
    hdfs.getServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);

    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    // set maintenance mode on HDFS_CLIENT on host1 to true
    ServiceComponentHost schHdfsClient = hdfs.getServiceComponent(HDFS_CLIENT).getServiceComponentHost(DummyHostname1);
    schHdfsClient.setMaintenanceState(MaintenanceState.ON);


    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setCurrentPingPort(DummyCurrentPingPort);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse rr = handler.handleRegistration(reg);
    RecoveryConfig rc = rr.getRecoveryConfig();
    assertEquals(rc.getEnabledComponents(), "DATANODE,NAMENODE"); // HDFS_CLIENT is in maintenance mode
  }

  @Test
  @Ignore
  // TODO should be rewritten after STOMP protocol implementation.
  public void testRegistrationAgentConfig() throws Exception,
      InvalidStateTransitionException {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setCurrentPingPort(DummyCurrentPingPort);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse rr = handler.handleRegistration(reg);
    Map<String, String> config = rr.getAgentConfig();
    assertFalse(config.isEmpty());
    assertTrue(config.containsKey(Configuration.CHECK_REMOTE_MOUNTS.getKey()));
    assertTrue("false".equals(config.get(Configuration.CHECK_REMOTE_MOUNTS.getKey())));
    assertTrue(config.containsKey(Configuration.CHECK_MOUNTS_TIMEOUT.getKey()));
    assertTrue("0".equals(config.get(Configuration.CHECK_MOUNTS_TIMEOUT.getKey())));
    assertTrue("true".equals(config.get(Configuration.ENABLE_AUTO_AGENT_CACHE_UPDATE.getKey())));
  }

  @Test
  public void testRegistrationWithBadVersion() throws Exception,
      InvalidStateTransitionException {

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);

    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(""); // Invalid agent version
    reg.setPrefix(Configuration.PREFIX_DIR);
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non compatible agent version");
    } catch (AmbariException e) {
      log.debug("Error:{}", e.getMessage());
      Assert.assertTrue(e.getMessage().contains(
          "Cannot register host with non compatible agent version"));
    }

    reg.setAgentVersion(null); // Invalid agent version
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non compatible agent version");
    } catch (AmbariException e) {
      log.debug("Error:{}", e.getMessage());
      Assert.assertTrue(e.getMessage().contains(
          "Cannot register host with non compatible agent version"));
    }
  }

  @Test
  public void testRegistrationPublicHostname() throws Exception, InvalidStateTransitionException {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setPublicHostname(DummyHostname1 + "-public");
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());

    Host verifyHost = clusters.getHost(DummyHostname1);
    assertEquals(verifyHost.getPublicHostName(), reg.getPublicHostname());
  }


  @Test
  public void testInvalidOSRegistration() throws Exception,
      InvalidStateTransitionException {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non matching os type");
    } catch (AmbariException e) {
      // Expected
    }
  }

  @Test
  public void testIncompatibleAgentRegistration() throws Exception,
          InvalidStateTransitionException {

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion("0.0.0"); // Invalid agent version
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non compatible agent version");
    } catch (AmbariException e) {
      // Expected
    }
  }

  @Test
  public void testRegisterNewNode()
      throws Exception, InvalidStateTransitionException {
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    Clusters fsm = clusters;
    fsm.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    HeartBeatHandler handler = createHeartBeatHandler();
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS("redhat5");
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse response = handler.handleRegistration(reg);

    assertEquals(hostObject.getState(), HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    assertEquals("redhat5", hostObject.getOsType());
    assertEquals(0, response.getResponseId());
    assertEquals(reg.getPrefix(), hostObject.getPrefix());
  }

  @Test
  public void testRequestId() throws IOException,
      InvalidStateTransitionException, JsonGenerationException, JAXBException {
    HeartBeatHandler heartBeatHandler = injector.getInstance(
        HeartBeatHandler.class);

    Register register = new Register();
    register.setHostname("newHost");
    register.setTimestamp(new Date().getTime());
    register.setResponseId(123);
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS("redhat5");
    register.setHardwareProfile(hi);
    register.setAgentVersion(metaInfo.getServerVersion());
    RegistrationResponse registrationResponse = heartBeatHandler.handleRegistration(register);

    assertEquals("ResponseId should start from zero", 0L, registrationResponse.getResponseId());

    HeartBeat heartBeat = constructHeartBeat("newHost", registrationResponse.getResponseId(), Status.HEALTHY);
    HeartBeatResponse hbResponse = heartBeatHandler.handleHeartBeat(heartBeat);

    assertEquals("responseId was not incremented", 1L, hbResponse.getResponseId());
    assertTrue("Not cached response returned", hbResponse == heartBeatHandler.handleHeartBeat(heartBeat));

    heartBeat.setResponseId(1L);
    hbResponse = heartBeatHandler.handleHeartBeat(heartBeat);
    assertEquals("responseId was not incremented", 2L, hbResponse.getResponseId());
    assertTrue("Agent is flagged for restart", hbResponse.isRestartAgent() == null);

    log.debug(StageUtils.jaxbToString(hbResponse));

    heartBeat.setResponseId(20L);
    hbResponse = heartBeatHandler.handleHeartBeat(heartBeat);
//    assertEquals("responseId was not incremented", 2L, hbResponse.getResponseId());
    assertTrue("Agent is not flagged for restart", hbResponse.isRestartAgent());

    log.debug(StageUtils.jaxbToString(hbResponse));

  }

  private HeartBeat constructHeartBeat(String hostName, long responseId, Status status) {
    HeartBeat heartBeat = new HeartBeat();
    heartBeat.setHostname(hostName);
    heartBeat.setTimestamp(new Date().getTime());
    heartBeat.setResponseId(responseId);
    HostStatus hs = new HostStatus();
    hs.setCause("");
    hs.setStatus(status);
    heartBeat.setNodeStatus(hs);
    heartBeat.setReports(Collections.emptyList());

    return heartBeat;
  }

  @Test
  @Ignore
  //TODO should be rewritten, statusCommand already is not actual as a part of heartbeat.
  public void testStateCommandsAtRegistration() throws Exception, InvalidStateTransitionException {
    List<StatusCommand> dummyCmds = new ArrayList<>();
    StatusCommand statusCmd1 = new StatusCommand();
    statusCmd1.setClusterName(DummyCluster);
    statusCmd1.setServiceName(HDFS);
    dummyCmds.add(statusCmd1);
    HeartbeatMonitor hm = mock(HeartbeatMonitor.class);
    when(hm.generateStatusCommands(anyString())).thenReturn(dummyCmds);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    handler.setHeartbeatMonitor(hm);
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    RegistrationResponse registrationResponse = handler.handleRegistration(reg);
    registrationResponse.getStatusCommands();
    assertTrue(registrationResponse.getStatusCommands().size() == 1);
    assertTrue(registrationResponse.getStatusCommands().get(0).equals(statusCmd1));
  }

  @Test
  public void testTaskInProgressHandling() throws Exception, InvalidStateTransitionException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setClusterId(DummyClusterId);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setRoleCommand("INSTALL");
    cr.setStatus("IN_PROGRESS");
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<>());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, RoleCommand.INSTALL);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    handler.handleHeartBeat(hb);
    handler.getHeartbeatProcessor().processHeartbeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING, componentState1);
  }

  @Test
  public void testOPFailedEventForAbortedTask() throws Exception, InvalidStateTransitionException {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
      getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    Stage s = stageFactory.createNew(1, "/a/b", "cluster1", 1L, "action manager test",
      "commandParamsStage", "hostParamsStage");
    s.setStageId(1);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.DATANODE, RoleCommand.INSTALL,
      new ServiceComponentHostInstallEvent(Role.DATANODE.toString(),
        DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
          DummyCluster, "HDFS", false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "clusterHostInfo", clusters);
    actionDBAccessor.persistActions(request);
    actionDBAccessor.abortHostRole(DummyHostname1, 1, 1, Role.DATANODE.name());

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(1, 1));
    cr.setTaskId(1);
    cr.setClusterId(DummyClusterId);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setRoleCommand("INSTALL");
    cr.setStatus("FAILED");
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<>());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    handler.handleHeartBeat(hb);
    handler.getHeartbeatProcessor().processHeartbeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING,
      componentState1);
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testStatusHeartbeat() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost3 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(HDFS_CLIENT).getServiceComponentHost(DummyHostname1);

    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.STARTED);
    serviceComponentHost3.setState(State.STARTED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());
    hb.setAgentEnv(new AgentEnv());
    hb.setMounts(new ArrayList<>());

    ArrayList<ComponentStatus> componentStatuses = new ArrayList<>();
    ComponentStatus componentStatus1 = createComponentStatus(new Long(DummyClusterId), HDFS, DummyHostStatus, State.STARTED,
      DATANODE, "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.0\"}");
    ComponentStatus componentStatus2 =
        createComponentStatus(new Long(DummyClusterId), HDFS, DummyHostStatus, State.STARTED, NAMENODE, "");
    ComponentStatus componentStatus3 = createComponentStatus(new Long(DummyClusterId), HDFS, DummyHostStatus, State.INSTALLED,
      HDFS_CLIENT, "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.0\"}");

    componentStatuses.add(componentStatus1);
    componentStatuses.add(componentStatus2);
    componentStatuses.add(componentStatus3);
    hb.setComponentStatus(componentStatuses);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(new ArrayList<>());
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    handler.handleHeartBeat(hb);
    heartbeatProcessor.processHeartbeat(hb);

    assertTrue(hb.getAgentEnv().getHostHealth().getServerTimeStampAtReporting() >= hb.getTimestamp());
  }

  @Test
  public void testRecoveryStatusReports() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Host hostObject = clusters.getHost(DummyHostname1);
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1, Role.DATANODE, null, null);
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
          add(command);
        }}).anyTimes();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);

    hostObject.setState(HostState.UNHEALTHY);

    //aq.enqueue(DummyHostname1, new StatusCommand());

    //All components are up
    HeartBeat hb1 = new HeartBeat();
    hb1.setResponseId(0);
    hb1.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb1.setHostname(DummyHostname1);
    RecoveryReport rr = new RecoveryReport();
    rr.setSummary("RECOVERABLE");
    List<ComponentRecoveryReport> compRecReports = new ArrayList<>();
    ComponentRecoveryReport compRecReport = new ComponentRecoveryReport();
    compRecReport.setLimitReached(Boolean.FALSE);
    compRecReport.setName("DATANODE");
    compRecReport.setNumAttempts(2);
    compRecReports.add(compRecReport);
    rr.setComponentReports(compRecReports);
    hb1.setRecoveryReport(rr);
    handler.handleHeartBeat(hb1);
    assertEquals("RECOVERABLE", hostObject.getRecoveryReport().getSummary());
    assertEquals(1, hostObject.getRecoveryReport().getComponentReports().size());
    assertEquals(2, hostObject.getRecoveryReport().getComponentReports().get(0).getNumAttempts());

    HeartBeat hb2 = new HeartBeat();
    hb2.setResponseId(1);
    hb2.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb2.setHostname(DummyHostname1);
    rr = new RecoveryReport();
    rr.setSummary("UNRECOVERABLE");
    compRecReports = new ArrayList<>();
    compRecReport = new ComponentRecoveryReport();
    compRecReport.setLimitReached(Boolean.TRUE);
    compRecReport.setName("DATANODE");
    compRecReport.setNumAttempts(5);
    compRecReports.add(compRecReport);
    rr.setComponentReports(compRecReports);
    hb2.setRecoveryReport(rr);
    handler.handleHeartBeat(hb2);
    assertEquals("UNRECOVERABLE", hostObject.getRecoveryReport().getSummary());
    assertEquals(1, hostObject.getRecoveryReport().getComponentReports().size());
    assertEquals(5, hostObject.getRecoveryReport().getComponentReports().get(0).getNumAttempts());
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testProcessStatusReports() throws Exception {
    Clusters fsm = clusters;

    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Host hostObject = clusters.getHost(DummyHostname1);
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);
    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }}).anyTimes();
    replay(am);
    HeartBeatHandler handler = createHeartBeatHandler();
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);

    hostObject.setState(HostState.UNHEALTHY);

    //aq.enqueue(DummyHostname1, new StatusCommand());

    //All components are up
    HeartBeat hb1 = new HeartBeat();
    hb1.setResponseId(0);
    hb1.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb1.setHostname(DummyHostname1);
    List<ComponentStatus> componentStatus = new ArrayList<>();
    ComponentStatus dataNodeStatus = new ComponentStatus();
    //dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("STARTED");
    componentStatus.add(dataNodeStatus);
    ComponentStatus nameNodeStatus = new ComponentStatus();
    //nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("STARTED");
    componentStatus.add(nameNodeStatus);
    hb1.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb1);
    heartbeatProcessor.processHeartbeat(hb1);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    //Some slaves are down, masters are up
    HeartBeat hb2 = new HeartBeat();
    hb2.setResponseId(1);
    hb2.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb2.setHostname(DummyHostname1);
    componentStatus = new ArrayList<>();
    dataNodeStatus = new ComponentStatus();
    //dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("INSTALLED");
    componentStatus.add(dataNodeStatus);
    nameNodeStatus = new ComponentStatus();
    //nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("STARTED");
    componentStatus.add(nameNodeStatus);
    hb2.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb2);
    heartbeatProcessor.processHeartbeat(hb2);
    assertEquals(HostHealthStatus.HealthStatus.ALERT.name(), hostObject.getStatus());

    // mark the installed DN as maintenance
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(
        DummyHostname1).setMaintenanceState(MaintenanceState.ON);
    HeartBeat hb2a = new HeartBeat();
    hb2a.setResponseId(2);
    hb2a.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb2a.setHostname(DummyHostname1);
    componentStatus = new ArrayList<>();
    dataNodeStatus = new ComponentStatus();
    //dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("INSTALLED");
    componentStatus.add(dataNodeStatus);
    nameNodeStatus = new ComponentStatus();
    //nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("STARTED");
    componentStatus.add(nameNodeStatus);
    hb2a.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb2a);
    heartbeatProcessor.processHeartbeat(hb2a);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(
        DummyHostname1).setMaintenanceState(MaintenanceState.OFF);

    //Some masters are down
    HeartBeat hb3 = new HeartBeat();
    hb3.setResponseId(3);
    hb3.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb3.setHostname(DummyHostname1);
    componentStatus = new ArrayList<>();
    dataNodeStatus = new ComponentStatus();
    //dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("INSTALLED");
    componentStatus.add(dataNodeStatus);
    nameNodeStatus = new ComponentStatus();
    //nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("INSTALLED");
    componentStatus.add(nameNodeStatus);
    hb3.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb3);
    heartbeatProcessor.processHeartbeat(hb3);
    assertEquals(HostHealthStatus.HealthStatus.UNHEALTHY.name(), hostObject.getStatus());

    //All are up
    hb1.setResponseId(4);
    handler.handleHeartBeat(hb1);
    heartbeatProcessor.processHeartbeat(hb1);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    reset(am);
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    //Only one component reported status
    hdfs.getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1).setState(State.INSTALLED);
    HeartBeat hb4 = new HeartBeat();
    hb4.setResponseId(5);
    hb4.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb4.setHostname(DummyHostname1);
    componentStatus = new ArrayList<>();
    dataNodeStatus = new ComponentStatus();
    //dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("STARTED");
    componentStatus.add(dataNodeStatus);
    hb4.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb4);
    heartbeatProcessor.processHeartbeat(hb4);
    assertEquals(HostHealthStatus.HealthStatus.UNHEALTHY.name(), hostObject.getStatus());

    hb1.setResponseId(6);
    handler.handleHeartBeat(hb1);
    heartbeatProcessor.processHeartbeat(hb1);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    //Some command reports
    HeartBeat hb5 = new HeartBeat();
    hb5.setResponseId(7);
    hb5.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb5.setHostname(DummyHostname1);
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setServiceName(HDFS);
    cr1.setTaskId(1);
    cr1.setRole(DATANODE);
    cr1.setStatus("COMPLETED");
    cr1.setStdErr("");
    cr1.setStdOut("");
    cr1.setExitCode(215);
    cr1.setRoleCommand("STOP");
    //cr1.setClusterName(DummyCluster);
    ArrayList<CommandReport> reports = new ArrayList<>();
    reports.add(cr1);
    hb5.setReports(reports);
    handler.handleHeartBeat(hb5);
    heartbeatProcessor.processHeartbeat(hb5);
    assertEquals(HostHealthStatus.HealthStatus.ALERT.name(), hostObject.getStatus());
  }

  @Test
  @Ignore
  //TODO should be rewritten, commandReports already are not actual as a part of heartbeat.
  public void testIgnoreCustomActionReport() throws Exception, InvalidStateTransitionException {
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    //cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(NAMENODE);
    cr1.setStatus(HostRoleStatus.FAILED.toString());
    cr1.setRoleCommand("CUSTOM_COMMAND");
    cr1.setStdErr("none");
    cr1.setStdOut("dummy output");
    cr1.setExitCode(0);
    CommandReport cr2 = new CommandReport();
    cr2.setActionId(StageUtils.getActionId(requestId, stageId));
    cr2.setTaskId(2);
    //cr2.setClusterName(DummyCluster);
    cr2.setServiceName(HDFS);
    cr2.setRole(NAMENODE);
    cr2.setStatus(HostRoleStatus.FAILED.toString());
    cr2.setRoleCommand("ACTIONEXECUTE");
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(0);

    ArrayList<CommandReport> reports = new ArrayList<>();
    reports.add(cr1);
    reports.add(cr2);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(reports);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);

    // CUSTOM_COMMAND and ACTIONEXECUTE reports are ignored
    // they should not change the host component state
    try {
      handler.handleHeartBeat(hb);
      handler.getHeartbeatProcessor().processHeartbeat(hb);
    } catch (Exception e) {
      fail();
    }

  }

  @Test @Ignore
  public void testComponents() throws Exception {

    ComponentsResponse expected = new ComponentsResponse();
    StackId dummyStackId = new StackId(DummyStackId);
    Map<String, Map<String, String>> dummyComponents = new HashMap<>();

    Map<String, String> dummyCategoryMap = new HashMap<>();
    dummyCategoryMap.put("NAMENODE", "MASTER");
    dummyComponents.put("HDFS", dummyCategoryMap);

    expected.setClusterName(DummyCluster);
    expected.setStackName(dummyStackId.getStackName());
    expected.setStackVersion(dummyStackId.getStackVersion());
    expected.setComponents(dummyComponents);

    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service service = EasyMock.createNiceMock(Service.class);
    expect(service.getName()).andReturn("HDFS").atLeastOnce();

    Map<String, ServiceComponent> componentMap = new HashMap<>();
    ServiceComponent nnComponent = EasyMock.createNiceMock(ServiceComponent.class);
    expect(nnComponent.getName()).andReturn("NAMENODE").atLeastOnce();
    expect(nnComponent.getDesiredStackId()).andReturn(dummyStackId).atLeastOnce();
    componentMap.put("NAMENODE", nnComponent);

    expect(service.getServiceComponents()).andReturn(componentMap).atLeastOnce();

    ActionManager am = actionManagerTestHelper.getMockActionManager();

    replay(service, nnComponent, am);

    cluster.addService(service);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    // Make sure handler is not null, this has possibly been an intermittent problem in the past
    assertNotNull(handler);

    ComponentsResponse actual = handler.handleComponents(DummyCluster);

    if (log.isDebugEnabled()) {
      log.debug(actual.toString());
    }

    assertEquals(expected.getClusterName(), actual.getClusterName());
    assertEquals(expected.getComponents(), actual.getComponents());
  }




  private ComponentStatus createComponentStatus(Long clusterId, String serviceName, String message,
                                                State state,
                                                String componentName, String stackVersion) {
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterId(clusterId);
    componentStatus1.setServiceName(serviceName);
    componentStatus1.setMessage(message);
    componentStatus1.setStatus(state.name());
    componentStatus1.setComponentName(componentName);
    componentStatus1.setStackVersion(stackVersion);
    return componentStatus1;
  }

  @Test
  public void testCommandStatusProcesses_empty() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());

    ArrayList<ComponentStatus> componentStatuses = new ArrayList<>();
    ComponentStatus componentStatus1 = new ComponentStatus();
    //componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(DATANODE);

    componentStatuses.add(componentStatus1);
    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    ServiceComponentHost sch = hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);

    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(sch.getProcesses().size()));
  }


  @Test
  public void testInjectKeytabApplicableHost() throws Exception {
    List<Map<String, String>> kcp;
    Map<String, String> properties;

    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);

    kcp = testInjectKeytabSetKeytab("c6403.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertEquals(1, kcp.size());

    properties = kcp.get(0);
    Assert.assertNotNull(properties);
    Assert.assertEquals("c6403.ambari.apache.org", properties.get(KerberosIdentityDataFileWriter.HOSTNAME));
    Assert.assertEquals("dn/_HOST@_REALM", properties.get(KerberosIdentityDataFileWriter.PRINCIPAL));
    Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_PATH));
    Assert.assertEquals("hdfs", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_NAME));
    Assert.assertEquals("r", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_ACCESS));
    Assert.assertEquals("hadoop", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_NAME));
    Assert.assertEquals("", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_ACCESS));

    Assert.assertEquals(Base64.encodeBase64String("hello".getBytes()), kcp.get(0).get(KerberosServerAction.KEYTAB_CONTENT_BASE64));


    kcp = testInjectKeytabRemoveKeytab("c6403.ambari.apache.org");

    Assert.assertNotNull(kcp);
    Assert.assertEquals(1, kcp.size());

    properties = kcp.get(0);
    Assert.assertNotNull(properties);
    Assert.assertEquals("c6403.ambari.apache.org", properties.get(KerberosIdentityDataFileWriter.HOSTNAME));
    Assert.assertEquals("dn/_HOST@_REALM", properties.get(KerberosIdentityDataFileWriter.PRINCIPAL));
    Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_PATH));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_NAME));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_ACCESS));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_NAME));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_ACCESS));
    Assert.assertFalse(properties.containsKey(KerberosServerAction.KEYTAB_CONTENT_BASE64));
  }

  @Test
  public void testInjectKeytabNotApplicableHost() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);

    List<Map<String, String>> kcp;
    kcp = testInjectKeytabSetKeytab("c6401.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertTrue(kcp.isEmpty());

    kcp = testInjectKeytabRemoveKeytab("c6401.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertTrue(kcp.isEmpty());
  }

  private List<Map<String, String>> testInjectKeytabSetKeytab(String targetHost) throws Exception {

    ExecutionCommand executionCommand = new ExecutionCommand();

    Map<String, String> hlp = new HashMap<>();
    hlp.put("custom_command", SET_KEYTAB);
    executionCommand.setHostLevelParams(hlp);

    Map<String, String> commandparams = new HashMap<>();
    commandparams.put(KerberosServerAction.AUTHENTICATED_USER_NAME, "admin");
    executionCommand.setCommandParams(commandparams);
    executionCommand.setClusterName(DummyCluster);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    Method injectKeytabMethod = agentCommandsPublisher.getClass().getDeclaredMethod("injectKeytab",
        ExecutionCommand.class, String.class, String.class);
    injectKeytabMethod.setAccessible(true);
    commandparams.put(KerberosServerAction.DATA_DIRECTORY, createTestKeytabData(agentCommandsPublisher, false).getAbsolutePath());
    injectKeytabMethod.invoke(agentCommandsPublisher, executionCommand, "SET_KEYTAB", targetHost);

    return executionCommand.getKerberosCommandParams();
  }


  private List<Map<String, String>> testInjectKeytabRemoveKeytab(String targetHost) throws Exception {

    ExecutionCommand executionCommand = new ExecutionCommand();

    Map<String, String> hlp = new HashMap<>();
    hlp.put("custom_command", "REMOVE_KEYTAB");
    executionCommand.setHostLevelParams(hlp);

    Map<String, String> commandparams = new HashMap<>();
    commandparams.put(KerberosServerAction.AUTHENTICATED_USER_NAME, "admin");
    executionCommand.setCommandParams(commandparams);
    executionCommand.setClusterName(DummyCluster);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    Method injectKeytabMethod = agentCommandsPublisher.getClass().getDeclaredMethod("injectKeytab",
        ExecutionCommand.class, String.class, String.class);
    injectKeytabMethod.setAccessible(true);
    commandparams.put(KerberosServerAction.DATA_DIRECTORY, createTestKeytabData(agentCommandsPublisher, true).getAbsolutePath());
    injectKeytabMethod.invoke(agentCommandsPublisher, executionCommand, "REMOVE_KEYTAB", targetHost);

    return executionCommand.getKerberosCommandParams();
  }


  private File createTestKeytabData(AgentCommandsPublisher agentCommandsPublisher, boolean removeKeytabs) throws Exception {
    KerberosKeytabController kerberosKeytabControllerMock = createMock(KerberosKeytabController.class);
    Map<String, Collection<String>> filter;

    if(removeKeytabs) {
      filter = null;

      Multimap<String, String> serviceMapping = ArrayListMultimap.create();
      serviceMapping.put("HDFS", "DATANODE");

      ResolvedKerberosPrincipal resolvedKerberosPrincipal = createMock(ResolvedKerberosPrincipal.class);
      expect(resolvedKerberosPrincipal.getHostName()).andReturn("c6403.ambari.apache.org");
      expect(resolvedKerberosPrincipal.getPrincipal()).andReturn("dn/_HOST@_REALM");
      expect(resolvedKerberosPrincipal.getServiceMapping()).andReturn(serviceMapping);
      replay(resolvedKerberosPrincipal);

      ResolvedKerberosKeytab resolvedKerberosKeytab = createMock(ResolvedKerberosKeytab.class);
      expect(resolvedKerberosKeytab.getPrincipals()).andReturn(Collections.singleton(resolvedKerberosPrincipal));
      replay(resolvedKerberosKeytab);

      expect(kerberosKeytabControllerMock.getKeytabByFile("/etc/security/keytabs/dn.service.keytab")).andReturn(resolvedKerberosKeytab).once();
    }
    else {
      filter = Collections.singletonMap("HDFS", Collections.singletonList("*"));
    }

    expect(kerberosKeytabControllerMock.adjustServiceComponentFilter(anyObject(), eq(false), anyObject())).andReturn(filter).once();
    expect(kerberosKeytabControllerMock.getFilteredKeytabs((Collection<KerberosIdentityDescriptor>) EasyMock.anyObject(), EasyMock.eq(null), EasyMock.eq(null))).andReturn(
      Sets.newHashSet(
        new ResolvedKerberosKeytab(
          "/etc/security/keytabs/dn.service.keytab",
          "hdfs",
          "r",
          "hadoop",
          "",
          Sets.newHashSet(new ResolvedKerberosPrincipal(
              1L,
              "c6403.ambari.apache.org",
              "dn/_HOST@_REALM",
              false,
              "/tmp",
              "HDFS",
              "DATANODE",
              "/etc/security/keytabs/dn.service.keytab"
            )
          ),
          false,
          false
        )
      )
    ).once();

    expect(kerberosKeytabControllerMock.getServiceIdentities(EasyMock.anyString(), EasyMock.anyObject())).andReturn(Collections.emptySet()).anyTimes();

    replay(kerberosKeytabControllerMock);

    Field controllerField = agentCommandsPublisher.getClass().getDeclaredField("kerberosKeytabController");
    controllerField.setAccessible(true);
    controllerField.set(agentCommandsPublisher, kerberosKeytabControllerMock);

    File dataDirectory = temporaryFolder.newFolder();
    File hostDirectory = new File(dataDirectory, "c6403.ambari.apache.org");
    File keytabFile;
    if(hostDirectory.mkdirs()) {
      keytabFile = new File(hostDirectory, DigestUtils.sha256Hex("/etc/security/keytabs/dn.service.keytab"));
      FileWriter fw = new FileWriter(keytabFile);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write("hello");
      bw.close();
    } else {
      throw new Exception("Failed to create " + hostDirectory.getAbsolutePath());
    }

    return dataDirectory;
  }

  /**
   * Adds the service to the cluster using the current cluster version as the
   * repository version for the service.
   *
   * @param cluster
   *          the cluster.
   * @param serviceName
   *          the service name.
   * @return the newly added service.
   * @throws AmbariException
   */
  private Service addService(Cluster cluster, String serviceName) throws AmbariException {
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster);
    return cluster.addService(serviceName, repositoryVersion);
  }

}
