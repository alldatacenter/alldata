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

package org.apache.ambari.server.serveraction;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.RequestStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.serveraction.upgrades.ManualStageAction;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

// TODO, fix this test later.
@Ignore
public class ServerActionExecutorTest {
  private static final int MAX_CYCLE_ITERATIONS = 1000;
  private static final String SERVER_HOST_NAME = StageUtils.getHostName();
  private static final String CLUSTER_HOST_INFO = "{all_hosts=["
      + SERVER_HOST_NAME + "], slave_hosts=["
      + SERVER_HOST_NAME + "]}";

  private static Injector injector;

  @Inject
  static StageFactory stageFactory;

  @BeforeClass
  public static void beforeClass() throws Exception {
    injector = Guice.createInjector(new MockModule());
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

    /**
     * Test a normal server action
     */
  @Test
  public void testServerAction() throws Exception {
    final Request request = createMockRequest();
    final Stage s = getStageWithServerAction(1, 977, null, "test", 300);
    final List<Stage> stages = new ArrayList<Stage>() {
      {
        add(s);
      }
    };
    ActionDBAccessor db = createMockActionDBAccessor(request, stages);
    ServerActionExecutor.init(injector);
    ServerActionExecutor executor = new ServerActionExecutor(db, 10000);

    // Force the task to be QUEUED
    s.getHostRoleCommand(SERVER_HOST_NAME, Role.AMBARI_SERVER_ACTION.toString()).setStatus(HostRoleStatus.QUEUED);

    int cycleCount = 0;
    while (!getTaskStatus(s).isCompletedState() && (cycleCount++ <= MAX_CYCLE_ITERATIONS)) {
      executor.doWork();
    }

    assertEquals(HostRoleStatus.COMPLETED, getTaskStatus(s));
  }

  /**
   * Test a manual stage
   */
  @Test
  public void testServerActionManualStage() throws Exception {
    final Request request = createMockRequest();
    stageFactory = createNiceMock(StageFactory.class);

    final Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 978, "context",
        "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");

    stage.addServerActionCommand(ManualStageAction.class.getName(),
        null, Role.AMBARI_SERVER_ACTION, RoleCommand.EXECUTE, "cluster1",
        new ServiceComponentHostServerActionEvent(StageUtils.getHostName(), System.currentTimeMillis()),
        Collections.emptyMap(), null, null, 1200, false, false);

    final List<Stage> stages = new ArrayList<Stage>() {
      {
        add(stage);
      }
    };
    ActionDBAccessor db = createMockActionDBAccessor(request, stages);
    ServerActionExecutor.init(injector);
    ServerActionExecutor executor = new ServerActionExecutor(db, 10000);

    // Force the task to be QUEUED
    stage.getHostRoleCommand(SERVER_HOST_NAME, Role.AMBARI_SERVER_ACTION.toString()).setStatus(HostRoleStatus.QUEUED);

    int cycleCount = 0;
    while (!getTaskStatus(stage).isHoldingState() && (cycleCount++ <= MAX_CYCLE_ITERATIONS)) {
      executor.doWork();
    }
    assertEquals(HostRoleStatus.HOLDING, getTaskStatus(stage));
  }

  /**
   * Test a timeout server action
   */
  @Test
  public void testServerActionTimeout() throws Exception {
    final Request request = createMockRequest();
    final Stage s = getStageWithServerAction(1,
        977,
        new HashMap<String, String>() {{
          put(MockServerAction.PAYLOAD_FORCE_FAIL, "timeout");
        }},
        "test",
        1);
    final List<Stage> stages = new ArrayList<Stage>() {
      {
        add(s);
      }
    };
    ActionDBAccessor db = createMockActionDBAccessor(request, stages);
    ServerActionExecutor.init(injector);
    ServerActionExecutor executor = new ServerActionExecutor(db, 10000);

    // Force the task to be QUEUED
    s.getHostRoleCommand(SERVER_HOST_NAME, Role.AMBARI_SERVER_ACTION.toString()).setStatus(HostRoleStatus.QUEUED);

    int cycleCount = 0;
    while (!getTaskStatus(s).isCompletedState() && (cycleCount++ <= MAX_CYCLE_ITERATIONS)) {
      executor.doWork();
    }

    assertEquals(HostRoleStatus.TIMEDOUT, getTaskStatus(s));
  }


  /**
   * Test a timeout server action
   */
  @Test
  public void testServerActionFailedException() throws Exception {
    final Request request = createMockRequest();
    final Stage s = getStageWithServerAction(1,
        977,
        new HashMap<String, String>() {{
          put(MockServerAction.PAYLOAD_FORCE_FAIL, "exception");
        }},
        "test",
        1);
    final List<Stage> stages = new ArrayList<Stage>() {
      {
        add(s);
      }
    };
    ActionDBAccessor db = createMockActionDBAccessor(request, stages);
    ServerActionExecutor.init(injector);
    ServerActionExecutor executor = new ServerActionExecutor(db, 10000);

    // Force the task to be QUEUED
    s.getHostRoleCommand(SERVER_HOST_NAME, Role.AMBARI_SERVER_ACTION.toString()).setStatus(HostRoleStatus.QUEUED);

    int cycleCount = 0;
    while (!getTaskStatus(s).isCompletedState() && (cycleCount++ <= MAX_CYCLE_ITERATIONS)) {
      executor.doWork();
    }

    assertEquals(HostRoleStatus.FAILED, getTaskStatus(s));
  }

  /**
   * Test a timeout server action
   */
  @Test
  public void testServerActionFailedReport() throws Exception {
    final Request request = createMockRequest();
    final Stage s = getStageWithServerAction(1,
        977,
        new HashMap<String, String>() {{
          put(MockServerAction.PAYLOAD_FORCE_FAIL, "report");
        }},
        "test",
        1);
    final List<Stage> stages = new ArrayList<Stage>() {
      {
        add(s);
      }
    };
    ActionDBAccessor db = createMockActionDBAccessor(request, stages);
    ServerActionExecutor.init(injector);
    ServerActionExecutor executor = new ServerActionExecutor(db, 10000);

    // Force the task to be QUEUED
    s.getHostRoleCommand(SERVER_HOST_NAME, Role.AMBARI_SERVER_ACTION.toString()).setStatus(HostRoleStatus.QUEUED);

    int cycleCount = 0;
    while (!getTaskStatus(s).isCompletedState() && (cycleCount++ <= MAX_CYCLE_ITERATIONS)) {
      executor.doWork();
    }

    assertEquals(HostRoleStatus.FAILED, getTaskStatus(s));
  }

  private HostRoleStatus getTaskStatus(Stage stage) {
    return stage.getHostRoleStatus(SERVER_HOST_NAME, "AMBARI_SERVER_ACTION");
  }

  private Request createMockRequest() {
    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(request.getRequestId()).thenReturn(1L);
    return request;
  }

  private ActionDBAccessor createMockActionDBAccessor(final Request request, final List<Stage> stages) {
    ActionDBAccessor db = mock(ActionDBAccessor.class);

    when(db.getFirstStageInProgressPerRequest()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        RequestStatus status = (RequestStatus) invocation.getArguments()[0];

        if (status == RequestStatus.IN_PROGRESS) {
          return Arrays.asList(request);
        } else {
          return Collections.emptyList();
        }
      }
    }).when(db).getRequestsByStatus(any(RequestStatus.class), anyInt(), anyBoolean());

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];
        HostRoleCommand command = stages.get(0).getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[1];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[2];

        HostRoleCommand task = stages.get(0).getHostRoleCommand(host, role);

        if (task.getStatus() == status) {
          return Arrays.asList(task);
        } else {
          return null;
        }
      }
    }).when(db).getTasksByHostRoleAndStatus(anyString(), anyString(), any(HostRoleStatus.class));

    return db;
  }

  private static Stage getStageWithServerAction(final long requestId, final long stageId,
                                                final Map<String, String> payload, final String requestContext,
                                                final int timeout) {
    stageFactory = createNiceMock(StageFactory.class);
    expect(stageFactory.createNew(anyLong(), anyObject(String.class), anyObject(String.class),
        anyLong(), anyObject(String.class), anyObject(String.class), anyObject(String.class))).
        andAnswer(new IAnswer<Stage>() {

          @Override
          public Stage answer() throws Throwable {
            Stage stage = stageFactory.createNew(requestId, "/tmp", "cluster1",
                1L, requestContext, "{}", "{}");

            stage.setStageId(stageId);
            stage.addServerActionCommand(MockServerAction.class.getName(), null,
                Role.AMBARI_SERVER_ACTION, RoleCommand.EXECUTE, "cluster1",
                new ServiceComponentHostServerActionEvent(SERVER_HOST_NAME, System.currentTimeMillis()),
                payload, "command detail", null, timeout, false, false);

            // TODO, take a look at KerberosHelperTest.java as an example
            return stage;
          }
        });

    Stage stage = stageFactory.createNew(requestId, "", "", 1L, "", "", "");
    return stage;
  }

  public static class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Clusters.class).toInstance(mock(Clusters.class));
    }
  }
}
