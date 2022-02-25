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
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostStatus;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOSRelease;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOs;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOsType;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyStackId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HBASE_MASTER;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS_CLIENT;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.SECONDARY_NAMENODE;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionManagerTestHelper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.CommandUtils;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class HeartbeatProcessorTest {

  private Injector injector;
  private long requestId = 23;
  private long stageId = 31;

  @Inject
  private Clusters clusters;

  @Inject
  Configuration config;

  @Inject
  private ActionDBAccessor actionDBAccessor;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private ActionManagerTestHelper actionManagerTestHelper;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private StageFactory stageFactory;

  @Inject
  private AmbariMetaInfo metaInfo;

  @Inject
  private OrmTestHelper helper;


  public HeartbeatProcessorTest(){
    InMemoryDefaultTestModule module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
  }


  @Before
  public void setup() throws Exception {
    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    EasyMock.replay(injector.getInstance(AuditLogger.class));
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testHeartbeatWithConfigs() throws Exception {
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
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setTaskId(1);
    cr.setRole(DATANODE);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    cr.setRoleCommand("START");
    //cr.setClusterName(DummyCluster);

    reports.add(cr);
    hb.setReports(reports);

    HostEntity host1 = hostDAO.findByName(DummyHostname1);
    Assert.assertNotNull(host1);
    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    // the heartbeat test passed if actual configs is populated
    Assert.assertNotNull(serviceComponentHost1.getActualConfigs());
    Assert.assertEquals(serviceComponentHost1.getActualConfigs().size(), 1);
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testRestartRequiredAfterInstallClient() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(HDFS_CLIENT);
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(HDFS_CLIENT).getServiceComponentHost(DummyHostname1);

    serviceComponentHost.setState(State.INSTALLED);
    serviceComponentHost.setRestartRequired(true);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);


    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setRoleCommand("INSTALL");
    cr.setCustomCommand("EXECUTION_COMMAND");
    cr.setTaskId(1);
    cr.setRole(HDFS_CLIENT);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    //cr.setClusterName(DummyCluster);
    reports.add(cr);
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
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    Assert.assertNotNull(serviceComponentHost.getActualConfigs());
    Assert.assertFalse(serviceComponentHost.isRestartRequired());
    Assert.assertEquals(serviceComponentHost.getActualConfigs().size(), 1);

  }


  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testHeartbeatCustomCommandWithConfigs() throws Exception {
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
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setRoleCommand("CUSTOM_COMMAND");
    cr.setCustomCommand("RESTART");
    cr.setTaskId(1);
    cr.setRole(DATANODE);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    //cr.setClusterName(DummyCluster);
    CommandReport crn = new CommandReport();
    crn.setActionId(StageUtils.getActionId(requestId, stageId));
    crn.setServiceName(HDFS);
    crn.setRoleCommand("CUSTOM_COMMAND");
    crn.setCustomCommand("START");
    crn.setTaskId(1);
    crn.setRole(NAMENODE);
    crn.setStatus("COMPLETED");
    crn.setStdErr("");
    crn.setStdOut("");
    crn.setExitCode(215);
    //crn.setClusterName(DummyCluster);

    reports.add(cr);
    reports.add(crn);
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
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    // the heartbeat test passed if actual configs is populated
    Assert.assertNotNull(serviceComponentHost1.getActualConfigs());
    Assert.assertEquals(serviceComponentHost1.getActualConfigs().size(), 1);
    Assert.assertNotNull(serviceComponentHost2.getActualConfigs());
    Assert.assertEquals(serviceComponentHost2.getActualConfigs().size(), 1);
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testHeartbeatCustomStartStop() throws Exception {
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
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.STARTED);
    serviceComponentHost1.setRestartRequired(true);
    serviceComponentHost2.setRestartRequired(true);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setRoleCommand("CUSTOM_COMMAND");
    cr.setCustomCommand("START");
    cr.setTaskId(1);
    cr.setRole(DATANODE);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    //cr.setClusterName(DummyCluster);
    CommandReport crn = new CommandReport();
    crn.setActionId(StageUtils.getActionId(requestId, stageId));
    crn.setServiceName(HDFS);
    crn.setRoleCommand("CUSTOM_COMMAND");
    crn.setCustomCommand("STOP");
    crn.setTaskId(1);
    crn.setRole(NAMENODE);
    crn.setStatus("COMPLETED");
    crn.setStdErr("");
    crn.setStdOut("");
    crn.setExitCode(215);
    //crn.setClusterName(DummyCluster);

    reports.add(cr);
    reports.add(crn);
    hb.setReports(reports);

    assertTrue(serviceComponentHost1.isRestartRequired());

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

    // the heartbeat test passed if actual configs is populated
    State componentState1 = serviceComponentHost1.getState();
    assertEquals(State.STARTED, componentState1);
    assertFalse(serviceComponentHost1.isRestartRequired());
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.INSTALLED, componentState2);
    assertTrue(serviceComponentHost2.isRestartRequired());
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
    hdfs.addServiceComponent(SECONDARY_NAMENODE);
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost3 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(SECONDARY_NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);
    serviceComponentHost3.setState(State.STARTING);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
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
    componentStatus2.setStatus(State.STARTED.name());
    componentStatus2.setComponentName(SECONDARY_NAMENODE);
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
    State componentState3 = serviceComponentHost3.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
    //starting state will not be overridden by status command
    assertEquals(State.STARTING, componentState3);
  }


  @Test
  public void testCommandReport() throws AmbariException {
    injector.injectMembers(this);
    clusters.addHost(DummyHostname1);

    StackId dummyStackId = new StackId(DummyStackId);
    clusters.addCluster(DummyCluster, dummyStackId);

    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = injector.getInstance(ActionManager.class);
    heartbeatTestHelper.populateActionDB(db, DummyHostname1, requestId, stageId);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(DummyHostname1, HBASE_MASTER, HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, DummyHostname1, HBASE_MASTER);
    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setRole(HBASE_MASTER);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);

    reports.add(cr);
    am.processTaskResponse(DummyHostname1, reports, CommandUtils.convertToTaskIdCommandMap(stage.getOrderedHostRoleCommands()));
    assertEquals(215,
        am.getAction(requestId, stageId).getExitCode(DummyHostname1, HBASE_MASTER));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostRoleStatus(DummyHostname1, HBASE_MASTER));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals(HostRoleStatus.COMPLETED,
        s.getHostRoleStatus(DummyHostname1, HBASE_MASTER));
    assertEquals(215,
        s.getExitCode(DummyHostname1, HBASE_MASTER));
  }

  /**
   * Tests the fact that when START and STOP commands are in progress, and heartbeat
   * forces the host component state to STARTED or INSTALLED, there are no undesired
   * side effects.
   * @throws AmbariException
   * @throws InvalidStateTransitionException
   */
  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testCommandReportOnHeartbeatUpdatedState()
      throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    //cr.setClusterName(DummyCluster);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    cr.setRoleCommand("START");
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<>());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    assertEquals("Host state should  be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.STARTED,
        State.STARTED, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(2);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setRoleCommand("STOP");
    cr.setExitCode(777);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.STARTED,
        State.STARTED, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(3);
    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());

    // validate the transitions when there is no heartbeat
    serviceComponentHost1.setState(State.STARTING);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setExitCode(777);
    cr.setRoleCommand("START");
    hb.setResponseId(4);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.STARTING,
        State.STARTING, serviceComponentHost1.getState());

    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);
    hb.setResponseId(5);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.STARTED,
        State.STARTED, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.STOPPING);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setExitCode(777);
    cr.setRoleCommand("STOP");
    hb.setResponseId(6);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.STOPPING,
        State.STOPPING, serviceComponentHost1.getState());

    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);
    hb.setResponseId(7);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testUpgradeSpecificHandling() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.UPGRADING);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));

    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    //cr.setClusterName(DummyCluster);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setRoleCommand("INSTALL");
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
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
        }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    assertEquals("Host state should  be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.UPGRADING);
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(2);
    cr.setStatus(HostRoleStatus.FAILED.toString());
    cr.setExitCode(3);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.UPGRADING);
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(3);
    cr.setStatus(HostRoleStatus.PENDING.toString());
    cr.setExitCode(55);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.UPGRADING);
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(4);
    cr.setStatus(HostRoleStatus.QUEUED.toString());
    cr.setExitCode(55);

    heartbeatProcessor.processHeartbeat(hb);
    assertEquals("Host state should be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());
  }


  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testCommandStatusProcesses() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());

    List<Map<String, String>> procs = new ArrayList<>();
    Map<String, String> proc1info = new HashMap<>();
    proc1info.put("name", "a");
    proc1info.put("status", "RUNNING");
    procs.add(proc1info);

    Map<String, String> proc2info = new HashMap<>();
    proc2info.put("name", "b");
    proc2info.put("status", "NOT_RUNNING");
    procs.add(proc2info);

    Map<String, Object> extra = new HashMap<>();
    extra.put("processes", procs);

    ArrayList<ComponentStatus> componentStatuses = new ArrayList<>();
    ComponentStatus componentStatus1 = new ComponentStatus();
    //componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(DATANODE);

    componentStatus1.setExtra(extra);
    componentStatuses.add(componentStatus1);
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
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);
    ServiceComponentHost sch = hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);

    Assert.assertEquals(Integer.valueOf(2), Integer.valueOf(sch.getProcesses().size()));

    hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());

    componentStatus1 = new ComponentStatus();
    //componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(DATANODE);
    hb.setComponentStatus(Collections.singletonList(componentStatus1));

    heartbeatProcessor.processHeartbeat(hb);
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testComponentUpgradeFailReport() throws Exception {
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

    StackId stack130 = new StackId("HDP-1.3.0");
    StackId stack120 = new StackId("HDP-1.2.0");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action manager test",
        "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(),
            DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
        DummyCluster, "HDFS", false, false);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.NAMENODE, RoleCommand.INSTALL,
        new ServiceComponentHostInstallEvent(Role.NAMENODE.toString(),
            DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
        DummyCluster, "HDFS", false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "clusterHostInfo", clusters);
    actionDBAccessor.persistActions(request);
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    //cr.setClusterName(DummyCluster);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    actionDBAccessor.updateHostRoleState(DummyHostname1, requestId, stageId,
        Role.DATANODE.name(), cr);
    cr.setRole(NAMENODE);
    cr.setTaskId(2);
    actionDBAccessor.updateHostRoleState(DummyHostname1, requestId, stageId,
        Role.NAMENODE.name(), cr);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    //cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(DATANODE);
    cr1.setRoleCommand("INSTALL");
    cr1.setStatus(HostRoleStatus.FAILED.toString());
    cr1.setStdErr("none");
    cr1.setStdOut("dummy output");
    cr1.setExitCode(0);

    CommandReport cr2 = new CommandReport();
    cr2.setActionId(StageUtils.getActionId(requestId, stageId));
    cr2.setTaskId(2);
    //cr2.setClusterName(DummyCluster);
    cr2.setServiceName(HDFS);
    cr2.setRole(NAMENODE);
    cr2.setRoleCommand("INSTALL");
    cr2.setStatus(HostRoleStatus.FAILED.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(0);
    ArrayList<CommandReport> reports = new ArrayList<>();
    reports.add(cr1);
    reports.add(cr2);
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
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    heartbeatProcessor.processHeartbeat(hb);

    assertEquals("State of SCH should change after fail report",
        State.UPGRADING, serviceComponentHost1.getState());
    assertEquals("State of SCH should change after fail report",
        State.INSTALL_FAILED, serviceComponentHost2.getState());
    assertEquals("Stack version of SCH should not change after fail report",
        State.INSTALL_FAILED, serviceComponentHost2.getState());
  }


  @Test
  public void testComponentUpgradeInProgressReport() throws Exception {
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

    StackId stack130 = new StackId("HDP-1.3.0");
    StackId stack120 = new StackId("HDP-1.2.0");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    //cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(DATANODE);
    cr1.setRoleCommand("INSTALL");
    cr1.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr1.setStdErr("none");
    cr1.setStdOut("dummy output");
    cr1.setExitCode(777);

    CommandReport cr2 = new CommandReport();
    cr2.setActionId(StageUtils.getActionId(requestId, stageId));
    cr2.setTaskId(2);
    //cr2.setClusterName(DummyCluster);
    cr2.setServiceName(HDFS);
    cr2.setRole(NAMENODE);
    cr2.setRoleCommand("INSTALL");
    cr2.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(777);
    ArrayList<CommandReport> reports = new ArrayList<>();
    reports.add(cr1);
    reports.add(cr2);
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
    handler.handleHeartBeat(hb);
    assertEquals("State of SCH not change while operation is in progress",
        State.UPGRADING, serviceComponentHost1.getState());
    assertEquals("State of SCH not change while operation is  in progress",
        State.INSTALLING, serviceComponentHost2.getState());
  }


  /**
   * Tests that if there is an invalid cluster in heartbeat data, the heartbeat
   * doesn't fail.
   *
   * @throws Exception
   */
  @Test
  public void testHeartBeatWithAlertAndInvalidCluster() throws Exception {
    ActionManager am = actionManagerTestHelper.getMockActionManager();

    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(new ArrayList<>());

    replay(am);

    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Clusters fsm = clusters;
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");
    hostObject.setOsType(DummyOsType);

    HeartBeatHandler handler = new HeartBeatHandler(fsm, am, Encryptor.NONE, injector);
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

    ExecutionCommand execCmd = new ExecutionCommand();
    execCmd.setRequestAndStage(2, 34);
    execCmd.setHostname(DummyHostname1);
    //aq.enqueue(DummyHostname1, new ExecutionCommand());

    HeartBeat hb = new HeartBeat();
    HostStatus hs = new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus);

    hb.setResponseId(0);
    hb.setNodeStatus(hs);
    hb.setHostname(DummyHostname1);

    Alert alert = new Alert("foo", "bar", "baz", "foobar", "foobarbaz",
        AlertState.OK);

    alert.setClusterId(-1L);

    List<Alert> alerts = Collections.singletonList(alert);
    hb.setAlerts(alerts);

    // should NOT throw AmbariException from alerts.
    handler.getHeartbeatProcessor().processHeartbeat(hb);
  }


  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testInstallPackagesWithVersion() throws Exception {
    // required since this test method checks the DAO result of handling a
    // heartbeat which performs some async tasks
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        Collections.singletonList(command)).anyTimes();
    replay(am);

    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    HeartBeat hb = new HeartBeat();

    JsonObject json = new JsonObject();
    json.addProperty("actual_version", "2.2.1.0-2222");
    json.addProperty("package_installation_result", "SUCCESS");
    json.addProperty("installed_repository_version", "0.1-1234");
    json.addProperty("stack_id", cluster.getDesiredStackVersion().getStackId());

    CommandReport cmdReport = new CommandReport();
    cmdReport.setActionId(StageUtils.getActionId(requestId, stageId));
    cmdReport.setTaskId(1);
    cmdReport.setCustomCommand("install_packages");
    cmdReport.setStructuredOut(json.toString());
    cmdReport.setRoleCommand(RoleCommand.ACTIONEXECUTE.name());
    cmdReport.setStatus(HostRoleStatus.COMPLETED.name());
    cmdReport.setRole("install_packages");
    //cmdReport.setClusterName(DummyCluster);

    List<CommandReport> reports = new ArrayList<>();
    reports.add(cmdReport);
    hb.setReports(reports);
    hb.setTimestamp(0L);
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);
    hb.setComponentStatus(new ArrayList<>());

    StackId stackId = new StackId("HDP", "0.1");

    RepositoryVersionDAO dao = injector.getInstance(RepositoryVersionDAO.class);
    RepositoryVersionEntity entity = helper.getOrCreateRepositoryVersion(cluster);
    Assert.assertNotNull(entity);

    heartbeatProcessor.processHeartbeat(hb);

    entity = dao.findByStackAndVersion(stackId, "2.2.1.0-2222");
    Assert.assertNull(entity);

    entity = dao.findByStackAndVersion(stackId, "0.1.1");
    Assert.assertNotNull(entity);
  }

  @Test
  public void testInstallPackagesWithId() throws Exception {
    // required since this test method checks the DAO result of handling a
    // heartbeat which performs some async tasks
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        Collections.singletonList(command)).anyTimes();
    replay(am);

    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    RepositoryVersionDAO dao = injector.getInstance(RepositoryVersionDAO.class);
    RepositoryVersionEntity entity = helper.getOrCreateRepositoryVersion(cluster);
    Assert.assertNotNull(entity);

    HeartBeatHandler handler = heartbeatTestHelper.getHeartBeatHandler(am);
    HeartbeatProcessor heartbeatProcessor = handler.getHeartbeatProcessor();
    HeartBeat hb = new HeartBeat();

    JsonObject json = new JsonObject();
    json.addProperty("actual_version", "2.2.1.0-2222");
    json.addProperty("package_installation_result", "SUCCESS");
    json.addProperty("repository_version_id", entity.getId());

    CommandReport cmdReport = new CommandReport();
    cmdReport.setActionId(StageUtils.getActionId(requestId, stageId));
    cmdReport.setTaskId(1);
    cmdReport.setCustomCommand("install_packages");
    cmdReport.setStructuredOut(json.toString());
    cmdReport.setRoleCommand(RoleCommand.ACTIONEXECUTE.name());
    cmdReport.setStatus(HostRoleStatus.COMPLETED.name());
    cmdReport.setRole("install_packages");
    cmdReport.setClusterId(DummyClusterId);

    List<CommandReport> reports = new ArrayList<>();
    reports.add(cmdReport);
    hb.setReports(reports);
    hb.setTimestamp(0L);
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);
    hb.setComponentStatus(new ArrayList<>());

    StackId stackId = new StackId("HDP", "0.1");

    heartbeatProcessor.processHeartbeat(hb);

    entity = dao.findByStackAndVersion(stackId, "2.2.1.0-2222");
    Assert.assertNotNull(entity);

    entity = dao.findByStackAndVersion(stackId, "0.1.1");
    Assert.assertNull(entity);
  }

  @Test
  @Ignore
  //TODO should be rewritten, componentStatuses already are not actual as a part of heartbeat.
  public void testComponentInProgressStatusSafeAfterStatusReport() throws Exception {
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
    serviceComponentHost1.setState(State.STARTING);
    serviceComponentHost2.setState(State.STOPPING);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<>();

    ComponentStatus componentStatus1 = new ComponentStatus();
    //componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.INSTALLED.name());
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
    assertEquals(State.STARTING, componentState1);
    assertEquals(State.STOPPING, componentState2);
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
