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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.ServiceComponentHostEventWrapper;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.util.Modules;

public class AutoSkipFailedSummaryActionTest {

  private Injector m_injector;

  private static final StackId HDP_STACK = new StackId("HDP-2.2.0");

  @Inject
  private ExecutionCommandDAO executionCommandDAO;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private ExecutionCommandWrapperFactory ecwFactory;

  // Mocked out values
  private UpgradeDAO upgradeDAOMock;
  private HostRoleCommandDAO hostRoleCommandDAOMock;
  private Clusters clustersMock;
  private Cluster clusterMock;


  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    upgradeDAOMock = createNiceMock(UpgradeDAO.class);
    hostRoleCommandDAOMock = createNiceMock(HostRoleCommandDAO.class);
    clustersMock = createNiceMock(Clusters.class);
    clusterMock = createNiceMock(Cluster.class);

    expect(clustersMock.getCluster(anyString())).andReturn(clusterMock).anyTimes();
    replay(clustersMock);

    expect(clusterMock.getDesiredStackVersion()).andReturn(HDP_STACK).anyTimes();
    replay(clusterMock);

    // Initialize injector
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    m_injector = Guice.createInjector(Modules.override(module).with(new MockModule()));
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
    m_injector.getInstance(UnitOfWork.class).begin();

    // Normally initialized in runtime when loading stack
    m_injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }


  /**
   * Tests successful workflow
   */
  @Test
  public void testAutoSkipFailedSummaryAction__green() throws Exception {
    AutoSkipFailedSummaryAction action = new AutoSkipFailedSummaryAction();
    m_injector.injectMembers(action);

    ServiceComponentHostEvent event = createNiceMock(ServiceComponentHostEvent.class);

    // Set mock for parent's getHostRoleCommand()
    HostRoleCommand hostRoleCommand = new HostRoleCommand("host1", Role.AMBARI_SERVER_ACTION,
        event, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    hostRoleCommand.setRequestId(1l);
    hostRoleCommand.setStageId(1l);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setClusterName("cc");
    executionCommand.setRoleCommand(RoleCommand.EXECUTE);
    executionCommand.setRole("AMBARI_SERVER_ACTION");
    executionCommand.setServiceName("");
    executionCommand.setTaskId(1l);
    ExecutionCommandWrapper wrapper = new ExecutionCommandWrapper(executionCommand);
    hostRoleCommand.setExecutionCommandWrapper(wrapper);

    Field f = AbstractServerAction.class.getDeclaredField("hostRoleCommand");
    f.setAccessible(true);
    f.set(action, hostRoleCommand);

    final UpgradeItemEntity upgradeItem1 = new UpgradeItemEntity();
    upgradeItem1.setStageId(5l);
    final UpgradeItemEntity upgradeItem2 = new UpgradeItemEntity();
    upgradeItem2.setStageId(6l);

    UpgradeGroupEntity upgradeGroupEntity = new UpgradeGroupEntity();
    upgradeGroupEntity.setId(11l);
    // List of upgrade items in group
    List<UpgradeItemEntity> groupUpgradeItems = new ArrayList<UpgradeItemEntity>(){{
      add(upgradeItem1);
      add(upgradeItem2);
    }};
    upgradeGroupEntity.setItems(groupUpgradeItems);

    UpgradeItemEntity upgradeItemEntity = new UpgradeItemEntity();
    upgradeItemEntity.setGroupEntity(upgradeGroupEntity);

    expect(upgradeDAOMock.findUpgradeItemByRequestAndStage(anyLong(), anyLong())).andReturn(upgradeItemEntity).anyTimes();
    expect(upgradeDAOMock.findUpgradeGroup(anyLong())).andReturn(upgradeGroupEntity).anyTimes();
    replay(upgradeDAOMock);

    List<HostRoleCommandEntity> skippedTasks = new ArrayList<HostRoleCommandEntity>() {{
      // It's empty - no skipped tasks
    }};
    expect(hostRoleCommandDAOMock.findByStatusBetweenStages(anyLong(),
      anyObject(HostRoleStatus.class), anyLong(), anyLong())).andReturn(skippedTasks).anyTimes();
    replay(hostRoleCommandDAOMock);

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();
    CommandReport result = action.execute(requestSharedDataContext);

    assertNotNull(result.getStructuredOut());
    assertEquals(0, result.getExitCode());
    assertEquals(HostRoleStatus.COMPLETED.toString(), result.getStatus());
    assertEquals("There were no skipped failures", result.getStdOut());
    assertEquals("{}", result.getStructuredOut());
    assertEquals("", result.getStdErr());
  }


  /**
   * Tests workflow with few skipped tasks
   */
  @Test
  public void testAutoSkipFailedSummaryAction__red() throws Exception {
    AutoSkipFailedSummaryAction action = new AutoSkipFailedSummaryAction();
    m_injector.injectMembers(action);

    EasyMock.reset(clusterMock);

    Service hdfsService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(clusterMock.getServiceByComponentName("DATANODE")).andReturn(hdfsService).anyTimes();

    Service zkService = createNiceMock(Service.class);
    expect(zkService.getName()).andReturn("ZOOKEEPER").anyTimes();
    expect(clusterMock.getServiceByComponentName("ZOOKEEPER_CLIENT")).andReturn(zkService).anyTimes();

    replay(clusterMock, hdfsService, zkService);

    ServiceComponentHostEvent event = createNiceMock(ServiceComponentHostEvent.class);

    // Set mock for parent's getHostRoleCommand()
    final HostRoleCommand hostRoleCommand = new HostRoleCommand("host1", Role.AMBARI_SERVER_ACTION,
        event, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    hostRoleCommand.setRequestId(1l);
    hostRoleCommand.setStageId(1l);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setClusterName("cc");
    executionCommand.setRoleCommand(RoleCommand.EXECUTE);
    executionCommand.setRole("AMBARI_SERVER_ACTION");
    executionCommand.setServiceName("");
    executionCommand.setTaskId(1l);
    ExecutionCommandWrapper wrapper = new ExecutionCommandWrapper(executionCommand);
    hostRoleCommand.setExecutionCommandWrapper(wrapper);

    Field f = AbstractServerAction.class.getDeclaredField("hostRoleCommand");
    f.setAccessible(true);
    f.set(action, hostRoleCommand);

    final UpgradeItemEntity upgradeItem1 = new UpgradeItemEntity();
    upgradeItem1.setStageId(5l);
    final UpgradeItemEntity upgradeItem2 = new UpgradeItemEntity();
    upgradeItem2.setStageId(6l);

    UpgradeGroupEntity upgradeGroupEntity = new UpgradeGroupEntity();
    upgradeGroupEntity.setId(11l);
    // List of upgrade items in group
    List<UpgradeItemEntity> groupUpgradeItems = new ArrayList<UpgradeItemEntity>(){{
      add(upgradeItem1);
      add(upgradeItem2);
    }};
    upgradeGroupEntity.setItems(groupUpgradeItems);

    UpgradeItemEntity upgradeItemEntity = new UpgradeItemEntity();
    upgradeItemEntity.setGroupEntity(upgradeGroupEntity);

    expect(upgradeDAOMock.findUpgradeItemByRequestAndStage(anyLong(), anyLong())).andReturn(upgradeItemEntity).anyTimes();
    expect(upgradeDAOMock.findUpgradeGroup(anyLong())).andReturn(upgradeGroupEntity).anyTimes();
    replay(upgradeDAOMock);

    List<HostRoleCommandEntity> skippedTasks = new ArrayList<HostRoleCommandEntity>() {{
      add(createSkippedTask("DATANODE", "DATANODE", "host1.vm",
        "RESTART HDFS/DATANODE", RoleCommand.CUSTOM_COMMAND,
        "RESTART"
        ));
      add(createSkippedTask("DATANODE", "DATANODE", "host2.vm",
        "RESTART HDFS/DATANODE", RoleCommand.CUSTOM_COMMAND,
        "RESTART"));
      add(createSkippedTask("ZOOKEEPER_QUORUM_SERVICE_CHECK", "ZOOKEEPER_CLIENT", "host2.vm",
        "SERVICE_CHECK ZOOKEEPER", RoleCommand.SERVICE_CHECK, null));
    }};
    expect(hostRoleCommandDAOMock.findByStatusBetweenStages(anyLong(),
      anyObject(HostRoleStatus.class), anyLong(), anyLong())).andReturn(skippedTasks).anyTimes();
    replay(hostRoleCommandDAOMock);

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();
    CommandReport result = action.execute(requestSharedDataContext);

    assertNotNull(result.getStructuredOut());
    assertEquals(0, result.getExitCode());
    assertEquals(HostRoleStatus.HOLDING.toString(), result.getStatus());
    assertEquals("There were 3 skipped failure(s) that must be addressed " +
      "before you can proceed. Please resolve each failure before continuing with the upgrade.",
      result.getStdOut());

    assertEquals("{\"failures\":" +
        "{\"service_check\":[\"ZOOKEEPER\"]," +
        "\"host_component\":{" +
        "\"host1.vm\":[{\"component\":\"DATANODE\",\"service\":\"HDFS\"}]," +
        "\"host2.vm\":[{\"component\":\"DATANODE\",\"service\":\"HDFS\"}]}}," +
        "\"skipped\":[\"service_check\",\"host_component\"]}",
      result.getStructuredOut());
    assertEquals("The following steps failed but were automatically skipped:\n" +
      "DATANODE on host1.vm: RESTART HDFS/DATANODE\n" +
      "DATANODE on host2.vm: RESTART HDFS/DATANODE\n" +
      "ZOOKEEPER_CLIENT on host2.vm: SERVICE_CHECK ZOOKEEPER\n", result.getStdErr());
  }

  /**
   * Tests workflow with failed service check
   */
  @Test
  public void testAutoSkipFailedSummaryAction__red__service_checks_only() throws Exception {
    AutoSkipFailedSummaryAction action = new AutoSkipFailedSummaryAction();
    m_injector.injectMembers(action);

    ServiceComponentHostEvent event = createNiceMock(ServiceComponentHostEvent.class);

    // Set mock for parent's getHostRoleCommand()
    final HostRoleCommand hostRoleCommand = new HostRoleCommand("host1", Role.AMBARI_SERVER_ACTION,
        event, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    hostRoleCommand.setRequestId(1l);
    hostRoleCommand.setStageId(1l);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setClusterName("cc");
    executionCommand.setRoleCommand(RoleCommand.EXECUTE);
    executionCommand.setRole("AMBARI_SERVER_ACTION");
    executionCommand.setServiceName("");
    executionCommand.setTaskId(1l);
    ExecutionCommandWrapper wrapper = new ExecutionCommandWrapper(executionCommand);
    hostRoleCommand.setExecutionCommandWrapper(wrapper);

    Field f = AbstractServerAction.class.getDeclaredField("hostRoleCommand");
    f.setAccessible(true);
    f.set(action, hostRoleCommand);

    final UpgradeItemEntity upgradeItem1 = new UpgradeItemEntity();
    upgradeItem1.setStageId(5l);
    final UpgradeItemEntity upgradeItem2 = new UpgradeItemEntity();
    upgradeItem2.setStageId(6l);

    UpgradeGroupEntity upgradeGroupEntity = new UpgradeGroupEntity();
    upgradeGroupEntity.setId(11l);
    // List of upgrade items in group
    List<UpgradeItemEntity> groupUpgradeItems = new ArrayList<UpgradeItemEntity>(){{
      add(upgradeItem1);
      add(upgradeItem2);
    }};
    upgradeGroupEntity.setItems(groupUpgradeItems);

    UpgradeItemEntity upgradeItemEntity = new UpgradeItemEntity();
    upgradeItemEntity.setGroupEntity(upgradeGroupEntity);

    expect(upgradeDAOMock.findUpgradeItemByRequestAndStage(anyLong(), anyLong())).andReturn(upgradeItemEntity).anyTimes();
    expect(upgradeDAOMock.findUpgradeGroup(anyLong())).andReturn(upgradeGroupEntity).anyTimes();
    replay(upgradeDAOMock);

    List<HostRoleCommandEntity> skippedTasks = new ArrayList<HostRoleCommandEntity>() {{
      add(createSkippedTask("ZOOKEEPER_QUORUM_SERVICE_CHECK", "ZOOKEEPER_CLIENT", "host2.vm",
        "SERVICE_CHECK ZOOKEEPER", RoleCommand.SERVICE_CHECK, null));
    }};
    expect(hostRoleCommandDAOMock.findByStatusBetweenStages(anyLong(),
      anyObject(HostRoleStatus.class), anyLong(), anyLong())).andReturn(skippedTasks).anyTimes();
    replay(hostRoleCommandDAOMock);

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();
    CommandReport result = action.execute(requestSharedDataContext);

    assertNotNull(result.getStructuredOut());
    assertEquals(0, result.getExitCode());
    assertEquals(HostRoleStatus.HOLDING.toString(), result.getStatus());
    assertEquals("There were 1 skipped failure(s) that must be addressed " +
        "before you can proceed. Please resolve each failure before continuing with the upgrade.",
      result.getStdOut());
    assertEquals("{\"failures\":{\"service_check\":[\"ZOOKEEPER\"]},\"skipped\":[\"service_check\"]}",
      result.getStructuredOut());
    assertEquals("The following steps failed but were automatically skipped:\n" +
      "ZOOKEEPER_CLIENT on host2.vm: SERVICE_CHECK ZOOKEEPER\n", result.getStdErr());
  }

  /**
   * Tests workflow with failed host component tasks only
   */
  @Test
  public void testAutoSkipFailedSummaryAction__red__host_components_only() throws Exception {
    AutoSkipFailedSummaryAction action = new AutoSkipFailedSummaryAction();
    m_injector.injectMembers(action);

    EasyMock.reset(clusterMock);

    Service hdfsService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(clusterMock.getServiceByComponentName("DATANODE")).andReturn(hdfsService).anyTimes();

    replay(clusterMock, hdfsService);


    ServiceComponentHostEvent event = createNiceMock(ServiceComponentHostEvent.class);

    // Set mock for parent's getHostRoleCommand()
    final HostRoleCommand hostRoleCommand = new HostRoleCommand("host1", Role.AMBARI_SERVER_ACTION,
        event, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    hostRoleCommand.setRequestId(1l);
    hostRoleCommand.setStageId(1l);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setClusterName("cc");
    executionCommand.setRoleCommand(RoleCommand.EXECUTE);
    executionCommand.setRole("AMBARI_SERVER_ACTION");
    executionCommand.setServiceName("");
    executionCommand.setTaskId(1l);
    ExecutionCommandWrapper wrapper = new ExecutionCommandWrapper(executionCommand);
    hostRoleCommand.setExecutionCommandWrapper(wrapper);

    Field f = AbstractServerAction.class.getDeclaredField("hostRoleCommand");
    f.setAccessible(true);
    f.set(action, hostRoleCommand);

    final UpgradeItemEntity upgradeItem1 = new UpgradeItemEntity();
    upgradeItem1.setStageId(5l);
    final UpgradeItemEntity upgradeItem2 = new UpgradeItemEntity();
    upgradeItem2.setStageId(6l);

    UpgradeGroupEntity upgradeGroupEntity = new UpgradeGroupEntity();
    upgradeGroupEntity.setId(11l);
    // List of upgrade items in group
    List<UpgradeItemEntity> groupUpgradeItems = new ArrayList<UpgradeItemEntity>(){{
      add(upgradeItem1);
      add(upgradeItem2);
    }};
    upgradeGroupEntity.setItems(groupUpgradeItems);

    UpgradeItemEntity upgradeItemEntity = new UpgradeItemEntity();
    upgradeItemEntity.setGroupEntity(upgradeGroupEntity);

    expect(upgradeDAOMock.findUpgradeItemByRequestAndStage(anyLong(), anyLong())).andReturn(upgradeItemEntity).anyTimes();
    expect(upgradeDAOMock.findUpgradeGroup(anyLong())).andReturn(upgradeGroupEntity).anyTimes();
    replay(upgradeDAOMock);

    List<HostRoleCommandEntity> skippedTasks = new ArrayList<HostRoleCommandEntity>() {{
      add(createSkippedTask("DATANODE", "DATANODE", "host1.vm",
        "RESTART HDFS/DATANODE", RoleCommand.CUSTOM_COMMAND,
        "RESTART"
      ));
      add(createSkippedTask("DATANODE", "DATANODE", "host2.vm",
        "RESTART HDFS/DATANODE", RoleCommand.CUSTOM_COMMAND,
        "RESTART"));
    }};
    expect(hostRoleCommandDAOMock.findByStatusBetweenStages(anyLong(),
      anyObject(HostRoleStatus.class), anyLong(), anyLong())).andReturn(skippedTasks).anyTimes();
    replay(hostRoleCommandDAOMock);

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();
    CommandReport result = action.execute(requestSharedDataContext);

    assertNotNull(result.getStructuredOut());
    assertEquals(0, result.getExitCode());
    assertEquals(HostRoleStatus.HOLDING.toString(), result.getStatus());
    assertEquals("There were 2 skipped failure(s) that must be addressed " +
        "before you can proceed. Please resolve each failure before continuing with the upgrade.",
      result.getStdOut());
    assertEquals("{\"failures\":" +
        "{\"host_component\":" +
        "{\"host1.vm\":[{\"component\":\"DATANODE\",\"service\":\"HDFS\"}]," +
        "\"host2.vm\":[{\"component\":\"DATANODE\",\"service\":\"HDFS\"}]}}," +
        "\"skipped\":[\"host_component\"]}",
      result.getStructuredOut());
    assertEquals("The following steps failed but were automatically skipped:\n" +
      "DATANODE on host1.vm: RESTART HDFS/DATANODE\n" +
      "DATANODE on host2.vm: RESTART HDFS/DATANODE\n", result.getStdErr());
  }



  private HostRoleCommandEntity createSkippedTask(String role, String componentName,
                                                  String hostname, String commandDetail,
                                                  RoleCommand roleCommand, String customCommandName) {
    HostRoleCommandEntity result = new HostRoleCommandEntity();

    ServiceComponentHostEvent event = new ServiceComponentHostOpInProgressEvent(componentName, hostname, 77l);
    ServiceComponentHostEventWrapper eventWrapper = new ServiceComponentHostEventWrapper(event);
    result.setEvent(eventWrapper.getEventJson());

    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname);
    result.setHostEntity(hostEntity);

    result.setTaskId(7l);
    result.setExitcode(1);
    result.setOutputLog("/output.log");
    result.setErrorLog("/error.log");
    result.setStdOut("Some stdout".getBytes());
    result.setStdError("Some stderr".getBytes());
    result.setCommandDetail(commandDetail);
    result.setRole(Role.valueOf(role));
    result.setRoleCommand(roleCommand);
    result.setCustomCommandName(customCommandName);
    result.setStatus(HostRoleStatus.SKIPPED_FAILED);

    return result;
  }


  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(UpgradeDAO.class).toInstance(upgradeDAOMock);
      bind(HostRoleCommandDAO.class).toInstance(hostRoleCommandDAOMock);
      bind(Clusters.class).toInstance(clustersMock);
    }
  }

}
