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
package org.apache.ambari.server.actionmanager;

import static org.apache.ambari.server.orm.DBAccessor.DbType.ORACLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NamedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.serveraction.MockServerAction;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.CommandUtils;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class TestActionDBAccessorImpl {
  private static final Logger log = LoggerFactory.getLogger(TestActionDBAccessorImpl.class);

  private long requestId = 23;
  private long stageId = 31;
  private String hostName = "host1";
  private String clusterName = "cluster1";
  private String actionName = "validate_kerberos";

  private String serverHostName = StageUtils.getHostName(); // "_localhost_";
  private String serverActionName = MockServerAction.class.getName();

  private Injector injector;
  ActionDBAccessor db;
  ActionManager am;

  @Inject
  private Clusters clusters;

  @Inject
  private ExecutionCommandDAO executionCommandDAO;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private StageFactory stageFactory;


  @Before
  public void setup() throws AmbariException {
    InMemoryDefaultTestModule defaultTestModule = new InMemoryDefaultTestModule();
    injector  = Guice.createInjector(Modules.override(defaultTestModule)
      .with(new TestActionDBAccessorModule()));

    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);

    // initialize AmbariMetaInfo so that the stacks are populated into the DB
    injector.getInstance(AmbariMetaInfo.class);

    injector.injectMembers(this);

    // Add this host's name since it is needed for server-side actions.
    clusters.addHost(serverHostName);
    clusters.addHost(hostName);

    StackId stackId = new StackId("HDP-0.1");
    clusters.addCluster(clusterName, stackId);
    db = injector.getInstance(ActionDBAccessorImpl.class);

    am = injector.getInstance(ActionManager.class);

    EasyMock.replay(injector.getInstance(AuditLogger.class));
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testActionResponse() throws AmbariException {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId, false);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setTaskId(1);
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    reports.add(cr);
    am.processTaskResponse(hostname, reports, CommandUtils.convertToTaskIdCommandMap(stage.getOrderedHostRoleCommands()));
    am.processTaskResponse(hostname, reports, CommandUtils.convertToTaskIdCommandMap(stage.getOrderedHostRoleCommands()));
    assertEquals(215,
        am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
        .getHostRoleStatus(hostname, "HBASE_MASTER"));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals(HostRoleStatus.COMPLETED,s.getHostRoleStatus(hostname, "HBASE_MASTER"));
  }

  @Test
  public void testCancelCommandReport() throws AmbariException {
    String hostname = "host1";
    populateActionDB(db, hostname, requestId, stageId, false);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.ABORTED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<>();
    CommandReport cr = new CommandReport();
    cr.setTaskId(1);
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(0);
    reports.add(cr);
    am.processTaskResponse(hostname, reports, CommandUtils.convertToTaskIdCommandMap(stage.getOrderedHostRoleCommands()));
    assertEquals(0,
            am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals("HostRoleStatus should remain ABORTED " +
            "(command report status should be ignored)",
            HostRoleStatus.ABORTED, am.getAction(requestId, stageId)
            .getHostRoleStatus(hostname, "HBASE_MASTER"));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals("HostRoleStatus should remain ABORTED " +
            "(command report status should be ignored)",
            HostRoleStatus.ABORTED,s.getHostRoleStatus(hostname, "HBASE_MASTER"));
  }

  @Test
  public void testGetStagesInProgress() throws AmbariException {
    List<Stage> stages = new ArrayList<>();
    stages.add(createStubStage(hostName, requestId, stageId, false));
    stages.add(createStubStage(hostName, requestId, stageId + 1, false));
    Request request = new Request(stages, "", clusters);
    db.persistActions(request);
    assertEquals(2, stages.size());
  }

  @Test
  public void testGetStagesInProgressWithFailures() throws AmbariException {
    populateActionDB(db, hostName, requestId, stageId, false);
    populateActionDB(db, hostName, requestId + 1, stageId, false);
    List<Stage> stages = db.getFirstStageInProgressPerRequest();
    assertEquals(2, stages.size());

    db.abortOperation(requestId);
    stages = db.getFirstStageInProgressPerRequest();
    assertEquals(1, stages.size());
    assertEquals(requestId+1, stages.get(0).getRequestId());
  }

  @Test
  public void testGetStagesInProgressWithManyStages() throws AmbariException {
    // create 3 request; each request will have 3 stages, each stage 2 commands
    populateActionDBMultipleStages(3, db, hostName, requestId, stageId);
    populateActionDBMultipleStages(3, db, hostName, requestId + 1, stageId + 3);
    populateActionDBMultipleStages(3, db, hostName, requestId + 2, stageId + 3);

    // verify stages and proper ordering
    int commandsInProgressCount = db.getCommandsInProgressCount();
    List<Stage> stages = db.getFirstStageInProgressPerRequest();
    assertEquals(18, commandsInProgressCount);
    assertEquals(3, stages.size());

    long lastRequestId = Integer.MIN_VALUE;
    for (Stage stage : stages) {
      assertTrue(stage.getRequestId() >= lastRequestId);
      lastRequestId = stage.getRequestId();
    }

    // cancel the first one, removing 3 stages
    db.abortOperation(requestId);

    // verify stages and proper ordering
    commandsInProgressCount = db.getCommandsInProgressCount();
    stages = db.getFirstStageInProgressPerRequest();
    assertEquals(12, commandsInProgressCount);
    assertEquals(2, stages.size());

    // find the first stage, and change one command to COMPLETED
    stages.get(0).setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(),
        HostRoleStatus.COMPLETED);

    db.hostRoleScheduled(stages.get(0), hostName, Role.HBASE_MASTER.toString());

    // the first stage still has at least 1 command IN_PROGRESS
    commandsInProgressCount = db.getCommandsInProgressCount();
    stages = db.getFirstStageInProgressPerRequest();
    assertEquals(11, commandsInProgressCount);
    assertEquals(2, stages.size());

    // find the first stage, and change the other command to COMPLETED
    stages.get(0).setHostRoleStatus(hostName,
        Role.HBASE_REGIONSERVER.toString(), HostRoleStatus.COMPLETED);

    db.hostRoleScheduled(stages.get(0), hostName,
        Role.HBASE_REGIONSERVER.toString());

    // verify stages and proper ordering
    commandsInProgressCount = db.getCommandsInProgressCount();
    stages = db.getFirstStageInProgressPerRequest();
    assertEquals(10, commandsInProgressCount);
    assertEquals(2, stages.size());
  }

  @Test
  public void testGetStagesInProgressWithManyCommands() throws AmbariException {
    // 1000 hosts
    for (int i = 0; i < 1000; i++) {
      String hostName = "c64-" + i;
      clusters.addHost(hostName);
    }

    // create 1 request, 3 stages per host, each with 2 commands
    int requestCount = 1000;
    for (int i = 0; i < requestCount; i++) {
      String hostName = "c64-" + i;
      populateActionDBMultipleStages(3, db, hostName, requestId + i, stageId);
    }

    int commandsInProgressCount = db.getCommandsInProgressCount();
    List<Stage> stages = db.getFirstStageInProgressPerRequest();
    assertEquals(6000, commandsInProgressCount);
    assertEquals(requestCount, stages.size());
  }


  @Test
  public void testPersistActions() throws AmbariException {
    populateActionDB(db, hostName, requestId, stageId, false);
    for (Stage stage : db.getAllStages(requestId)) {
      log.info("taskId={}" + stage.getExecutionCommands(hostName).get(0).
          getExecutionCommand().getTaskId());
      assertTrue(stage.getExecutionCommands(hostName).get(0).
          getExecutionCommand().getTaskId() > 0);
      assertTrue(executionCommandDAO.findByPK(stage.getExecutionCommands(hostName).
          get(0).getExecutionCommand().getTaskId()) != null);
    }
  }

  @Test
  public void testHostRoleScheduled() throws InterruptedException, AmbariException {
    populateActionDB(db, hostName, requestId, stageId, false);
    Stage stage = db.getStage(StageUtils.getActionId(requestId, stageId));
    assertEquals(HostRoleStatus.PENDING, stage.getHostRoleStatus(hostName, Role.HBASE_MASTER.toString()));
    List<HostRoleCommandEntity> entities=
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());

    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    stage.setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(), HostRoleStatus.QUEUED);

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals(HostRoleStatus.QUEUED, stage.getHostRoleStatus(hostName, Role.HBASE_MASTER.toString()));
    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    db.hostRoleScheduled(stage, hostName, Role.HBASE_MASTER.toString());

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals(HostRoleStatus.QUEUED, entities.get(0).getStatus());

    Thread thread = new Thread(){
      @Override
      public void run() {
        Stage stage1 = db.getStage("23-31");
        stage1.setHostRoleStatus(hostName, Role.HBASE_MASTER.toString(), HostRoleStatus.COMPLETED);
        db.hostRoleScheduled(stage1, hostName, Role.HBASE_MASTER.toString());
        injector.getInstance(EntityManager.class).clear();
      }
    };

    thread.start();
    thread.join();

    injector.getInstance(EntityManager.class).clear();
    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals("Concurrent update failed", HostRoleStatus.COMPLETED, entities.get(0).getStatus());
  }

  @Test
  public void testCustomActionScheduled() throws InterruptedException, AmbariException {
    populateActionDBWithCustomAction(db, hostName, requestId, stageId);
    Stage stage = db.getStage(StageUtils.getActionId(requestId, stageId));
    assertEquals(HostRoleStatus.PENDING, stage.getHostRoleStatus(hostName, actionName));
    List<HostRoleCommandEntity> entities =
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, actionName);

    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    stage.setHostRoleStatus(hostName, actionName, HostRoleStatus.QUEUED);

    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, actionName);
    assertEquals(HostRoleStatus.QUEUED, stage.getHostRoleStatus(hostName, actionName));
    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());

    db.hostRoleScheduled(stage, hostName, actionName);

    entities = hostRoleCommandDAO.findByHostRole(
        hostName, requestId, stageId, actionName);
    assertEquals(HostRoleStatus.QUEUED, entities.get(0).getStatus());


    Thread thread = new Thread() {
      @Override
      public void run() {
        Stage stage1 = db.getStage("23-31");
        stage1.setHostRoleStatus(hostName, actionName, HostRoleStatus.COMPLETED);
        db.hostRoleScheduled(stage1, hostName, actionName);
        injector.getInstance(EntityManager.class).clear();
      }
    };

    thread.start();
    thread.join();

    injector.getInstance(EntityManager.class).clear();
    entities = hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, actionName);
    assertEquals("Concurrent update failed", HostRoleStatus.COMPLETED, entities.get(0).getStatus());
  }

  @Test
  public void testServerActionScheduled() throws InterruptedException, AmbariException {
    populateActionDBWithServerAction(db, null, requestId, stageId);

    final String roleName = Role.AMBARI_SERVER_ACTION.toString();
    Stage stage = db.getStage(StageUtils.getActionId(requestId, stageId));
    assertEquals(HostRoleStatus.PENDING, stage.getHostRoleStatus(null, roleName));
    List<HostRoleCommandEntity> entities =
        hostRoleCommandDAO.findByHostRole(null, requestId, stageId, roleName);

    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());
    stage.setHostRoleStatus(null, roleName, HostRoleStatus.QUEUED);

    entities = hostRoleCommandDAO.findByHostRole(null, requestId, stageId, roleName);
    assertEquals(HostRoleStatus.QUEUED, stage.getHostRoleStatus(null, roleName));
    assertEquals(HostRoleStatus.PENDING, entities.get(0).getStatus());

    db.hostRoleScheduled(stage, null, roleName);

    entities = hostRoleCommandDAO.findByHostRole(
        null, requestId, stageId, roleName);
    assertEquals(HostRoleStatus.QUEUED, entities.get(0).getStatus());


    Thread thread = new Thread() {
      @Override
      public void run() {
        Stage stage1 = db.getStage("23-31");
        stage1.setHostRoleStatus(null, roleName, HostRoleStatus.COMPLETED);
        db.hostRoleScheduled(stage1, null, roleName);
        injector.getInstance(EntityManager.class).clear();
      }
    };

    thread.start();
    thread.join();

    injector.getInstance(EntityManager.class).clear();
    entities = hostRoleCommandDAO.findByHostRole(null, requestId, stageId, roleName);
    assertEquals("Concurrent update failed", HostRoleStatus.COMPLETED, entities.get(0).getStatus());
  }

  @Test
  public void testUpdateHostRole() throws Exception {
    populateActionDB(db, hostName, requestId, stageId, false);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 50000; i++) {
      sb.append("1234567890");
    }
    String largeString = sb.toString();

    CommandReport commandReport = new CommandReport();
    commandReport.setStatus(HostRoleStatus.COMPLETED.toString());
    commandReport.setStdOut(largeString);
    commandReport.setStdErr(largeString);
    commandReport.setStructuredOut(largeString);
    commandReport.setExitCode(123);
    db.updateHostRoleState(hostName, requestId, stageId, Role.HBASE_MASTER.toString(), commandReport);

    List<HostRoleCommandEntity> commandEntities =
        hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());
    assertEquals(1, commandEntities.size());
    HostRoleCommandEntity commandEntity = commandEntities.get(0);
    HostRoleCommand command = db.getTask(commandEntity.getTaskId());
    assertNotNull(command);

    assertEquals(largeString, command.getStdout());
    assertEquals(largeString, command.getStructuredOut());

    //endTime for completed commands should be set
    assertTrue(command.getEndTime() != -1);

  }

  @Test
  public void testUpdateHostRoleTimeoutRetry() throws Exception {
    populateActionDB(db, hostName, requestId, stageId, true);

    CommandReport commandReport = new CommandReport();
    commandReport.setStatus(HostRoleStatus.TIMEDOUT.toString());
    commandReport.setStdOut("");
    commandReport.setStdErr("");
    commandReport.setStructuredOut("");
    commandReport.setExitCode(123);
    db.updateHostRoleState(hostName, requestId, stageId, Role.HBASE_MASTER.toString(), commandReport);

    List<HostRoleCommandEntity> commandEntities =
      hostRoleCommandDAO.findByHostRole(hostName, requestId, stageId, Role.HBASE_MASTER.toString());

    HostRoleCommandEntity commandEntity = commandEntities.get(0);
    HostRoleCommand command = db.getTask(commandEntity.getTaskId());
    assertNotNull(command);
    assertEquals(HostRoleStatus.HOLDING_TIMEDOUT, command.getStatus());

  }


  @Test
  public void testGetRequestsByStatus() throws AmbariException {
    List<Long> requestIds = new ArrayList<>();
    requestIds.add(requestId + 1);
    requestIds.add(requestId);
    populateActionDB(db, hostName, requestId, stageId, false);
    clusters.addHost("host2");
    populateActionDB(db, hostName, requestId + 1, stageId, false);
    List<Long> requestIdsResult =
      db.getRequestsByStatus(null, BaseRequest.DEFAULT_PAGE_SIZE, false);

    assertNotNull("List of request IDs is null", requestIdsResult);
    assertEquals("Request IDs not matches", requestIds, requestIdsResult);
  }

  /**
   * Tests getting requests which are fully COMPLETED out the database. This
   * will test for partial completions as well.
   *
   * @throws AmbariException
   */
  @Test
  public void testGetCompletedRequests() throws AmbariException {
    List<Long> requestIds = new ArrayList<>();
    requestIds.add(requestId);
    requestIds.add(requestId + 1);

    // populate with a completed request
    populateActionDBWithCompletedRequest(db, hostName, requestId, stageId);

    // only 1 should come back
    List<Long> requestIdsResult = db.getRequestsByStatus(RequestStatus.COMPLETED,
        BaseRequest.DEFAULT_PAGE_SIZE, false);

    assertEquals(1, requestIdsResult.size());
    assertTrue(requestIdsResult.contains(requestId));

    // populate with a partially completed request
    populateActionDBWithPartiallyCompletedRequest(db, hostName, requestId + 1, stageId);

    // the new request should not come back
    requestIdsResult = db.getRequestsByStatus(RequestStatus.COMPLETED,
        BaseRequest.DEFAULT_PAGE_SIZE, false);

    assertEquals(1, requestIdsResult.size());
    assertTrue(requestIdsResult.contains(requestId));
  }

  @Test
  public void testGetRequestsByStatusWithParams() throws AmbariException {
    List<Long> ids = new ArrayList<>();

    for (long l = 1; l <= 10; l++) {
      ids.add(l);
    }

    for (Long id : ids) {
      populateActionDB(db, hostName, id, stageId, false);
    }

    List<Long> expected = null;
    List<Long> actual = null;

    // Select all requests
    actual = db.getRequestsByStatus(null, BaseRequest.DEFAULT_PAGE_SIZE, false);
    expected = reverse(new ArrayList<>(ids));
    assertEquals("Request IDs not matches", expected, actual);

    actual = db.getRequestsByStatus(null, 4, false);
    expected = reverse(new ArrayList<>(ids.subList(ids.size() - 4, ids.size())));
    assertEquals("Request IDs not matches", expected, actual);

    actual = db.getRequestsByStatus(null, 7, true);
    expected = new ArrayList<>(ids.subList(0, 7));
    assertEquals("Request IDs not matches", expected, actual);
  }

  private <T> List<T> reverse(List<T> list) {
    List<T> result = new ArrayList<>(list);

    Collections.reverse(result);

    return result;
  }

  @Test
  public void testAbortRequest() throws AmbariException {
    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);

    clusters.addHost("host2");
    clusters.addHost("host3");
    clusters.addHost("host4");

    s.addHostRoleExecutionCommand("host1", Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            "host1", System.currentTimeMillis()), "cluster1", "HBASE", false, false);
    s.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            "host2", System.currentTimeMillis()), "cluster1", "HBASE", false, false);
    s.addHostRoleExecutionCommand(
        "host3",
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), "host3", System.currentTimeMillis()), "cluster1", "HBASE", false, false);
    s.addHostRoleExecutionCommand(
        "host4",
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), "host4", System.currentTimeMillis()), "cluster1", "HBASE", false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    s.getOrderedHostRoleCommands().get(0).setStatus(HostRoleStatus.PENDING);
    s.getOrderedHostRoleCommands().get(1).setStatus(HostRoleStatus.IN_PROGRESS);
    s.getOrderedHostRoleCommands().get(2).setStatus(HostRoleStatus.QUEUED);

    HostRoleCommand cmd = s.getOrderedHostRoleCommands().get(3);
    String hostName = cmd.getHostName();
    cmd.setStatus(HostRoleStatus.COMPLETED);

    Request request = new Request(stages, "", clusters);
    request.setClusterHostInfo("clusterHostInfo");
    db.persistActions(request);
    db.abortOperation(requestId);

    List<Long> aborted = new ArrayList<>();

    List<HostRoleCommand> commands = db.getRequestTasks(requestId);
    for(HostRoleCommand command : commands) {
      if(command.getHostName().equals(hostName)) {
        assertEquals(HostRoleStatus.COMPLETED, command.getStatus());
      } else {
        assertEquals(HostRoleStatus.ABORTED, command.getStatus());
        aborted.add(command.getTaskId());
      }
    }

    db.resubmitTasks(aborted);

    commands = db.getRequestTasks(requestId);

    for(HostRoleCommand command : commands) {
      if(command.getHostName().equals(hostName)) {
        assertEquals(HostRoleStatus.COMPLETED, command.getStatus());
      } else {
        assertEquals(HostRoleStatus.PENDING, command.getStatus());
      }
    }

  }

  /**
   * Tests that entities created int he {@link ActionDBAccessor} can be
   * retrieved with their IDs intact. EclipseLink seems to execute the
   * {@link NamedQuery} but then use its cached entities to fill in the data.
   *
   * @throws Exception
   */
  @Test
  public void testEntitiesCreatedWithIDs() throws Exception {
    List<Stage> stages = new ArrayList<>();
    Stage stage = createStubStage(hostName, requestId, stageId, false);

    stages.add(stage);

    Request request = new Request(stages, "", clusters);

    // persist entities
    db.persistActions(request);

    // query entities immediately to ensure IDs are populated
    List<HostRoleCommandEntity> commandEntities = hostRoleCommandDAO.findByRequest(requestId);
    Assert.assertEquals(2, commandEntities.size());

    for (HostRoleCommandEntity entity : commandEntities) {
      Assert.assertEquals(Long.valueOf(requestId), entity.getRequestId());
      Assert.assertEquals(Long.valueOf(stageId), entity.getStageId());
    }
  }

  private static class TestActionDBAccessorModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(DBAccessor.class).to(TestDBAccessorImpl.class);
    }
  }

  @Singleton
  static class TestDBAccessorImpl extends DBAccessorImpl {
    private DbType dbTypeOverride = null;

    @Inject
    public TestDBAccessorImpl(Configuration configuration) {
      super(configuration);
    }

    @Override
    public DbType getDbType() {
      if (dbTypeOverride != null) {
        return dbTypeOverride;
      }

      return super.getDbType();
    }

    public void setDbTypeOverride(DbType dbTypeOverride) {
      this.dbTypeOverride = dbTypeOverride;
    }
  }

  @Test
  public void testGet1000TasksFromOracleDB() throws Exception {
    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    for (int i = 1000; i < 2002; i++) {
      String host = "host" + i;

      clusters.addHost(host);

      s.addHostRoleExecutionCommand("host" + i, Role.HBASE_MASTER,
        RoleCommand.START, null, "cluster1", "HBASE", false, false);
    }

    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "", clusters);
    request.setClusterHostInfo("clusterHostInfo");
    db.persistActions(request);

    List<HostRoleCommandEntity> entities =
      hostRoleCommandDAO.findByRequest(request.getRequestId());

    assertEquals(1002, entities.size());
    List<Long> taskIds = new ArrayList<>();
    for (HostRoleCommandEntity entity : entities) {
      taskIds.add(entity.getTaskId());
    }

    TestDBAccessorImpl testDBAccessorImpl =
      (TestDBAccessorImpl) injector.getInstance(DBAccessor.class);

    testDBAccessorImpl.setDbTypeOverride(ORACLE);

    assertEquals(ORACLE, injector.getInstance(DBAccessor.class).getDbType());
    entities = hostRoleCommandDAO.findByPKs(taskIds);
    assertEquals("Tasks returned from DB match the ones created",
      taskIds.size(), entities.size());
  }

  private void populateActionDB(ActionDBAccessor db, String hostname,
      long requestId, long stageId, boolean retryAllowed) throws AmbariException {
    Stage s = createStubStage(hostname, requestId, stageId, retryAllowed);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "", clusters);
    db.persistActions(request);
  }

  private void populateActionDBMultipleStages(int numberOfStages,
      ActionDBAccessor db, String hostname, long requestId, long stageId)
      throws AmbariException {

    List<Stage> stages = new ArrayList<>();
    for (int i = 0; i < numberOfStages; i++) {
      Stage stage = createStubStage(hostname, requestId, stageId + i, false);
      stages.add(stage);
    }

    Request request = new Request(stages, "", clusters);
    db.persistActions(request);
  }

  private void populateActionDBWithCompletedRequest(ActionDBAccessor db, String hostname,
      long requestId, long stageId) throws AmbariException {

    Stage s = createStubStage(hostname, requestId, stageId, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "", clusters);

    s.setHostRoleStatus(hostname, Role.HBASE_REGIONSERVER.name(), HostRoleStatus.COMPLETED);
    s.setHostRoleStatus(hostname, Role.HBASE_MASTER.name(), HostRoleStatus.COMPLETED);
    db.persistActions(request);
  }

  private void populateActionDBWithPartiallyCompletedRequest(ActionDBAccessor db, String hostname,
      long requestId, long stageId) throws AmbariException {

    Stage s = createStubStage(hostname, requestId, stageId, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);

    Request request = new Request(stages, "", clusters);

    s.setHostRoleStatus(hostname, Role.HBASE_REGIONSERVER.name(), HostRoleStatus.PENDING);
    s.setHostRoleStatus(hostname, Role.HBASE_MASTER.name(), HostRoleStatus.COMPLETED);
    db.persistActions(request);
  }

  private Stage createStubStage(String hostname, long requestId, long stageId, boolean retryAllowed) {
    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis()), "cluster1", "HBASE", retryAllowed, false);
    s.addHostRoleExecutionCommand(
        hostname,
        Role.HBASE_REGIONSERVER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_REGIONSERVER
            .toString(), hostname, System.currentTimeMillis()), "cluster1", "HBASE", false, false);
    return s;
  }

  private void populateActionDBWithCustomAction(ActionDBAccessor db, String hostname,
                                long requestId, long stageId) throws AmbariException {
    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
      "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.valueOf(actionName),
        RoleCommand.ACTIONEXECUTE,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis()), "cluster1", "HBASE", false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    final RequestResourceFilter resourceFilter = new RequestResourceFilter("HBASE", "HBASE_MASTER", null);
    List<RequestResourceFilter> resourceFilters = new
      ArrayList<RequestResourceFilter>() {{ add(resourceFilter); }};
    Request request = new Request(stages, "", clusters);
    request.setClusterHostInfo("");
    db.persistActions(request);
  }

  private void populateActionDBWithServerAction(ActionDBAccessor db, String hostname,
                                                long requestId, long stageId) throws AmbariException {
    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action db accessor test",
        "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addServerActionCommand(serverActionName, null, Role.AMBARI_SERVER_ACTION,
        RoleCommand.ACTIONEXECUTE, clusterName, null, null, "command details", null, 300, false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "", clusters);
    request.setClusterHostInfo("");
    db.persistActions(request);
  }
}
