/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.stomp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.events.listeners.tasks.TaskStatusListener;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

public class NamedTasksSubscriptionsTest {
  private static final String SESSION_ID_1 = "fdsg3";
  private static final String SESSION_ID_2 = "idfg6";

  private NamedTasksSubscriptions tasksSubscriptions;
  private Provider<TaskStatusListener> taskStatusListenerProvider;
  private TaskStatusListener taskStatusListener;

  @Before
  public void setupTest() {
    taskStatusListenerProvider = createMock(Provider.class);
    taskStatusListener = createMock(TaskStatusListener.class);

    Map<Long, HostRoleCommand> hostRoleCommands = new HashMap<>();
    HostRoleCommand hostRoleCommand1 = createMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand4 = createMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand5 = createMock(HostRoleCommand.class);

    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();
    expect(hostRoleCommand4.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();
    expect(hostRoleCommand5.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();

    hostRoleCommands.put(1L, hostRoleCommand1);
    hostRoleCommands.put(4L, hostRoleCommand4);
    hostRoleCommands.put(5L, hostRoleCommand5);
    expect(taskStatusListener.getActiveTasksMap()).andReturn(hostRoleCommands).anyTimes();
    expect(taskStatusListenerProvider.get()).andReturn(taskStatusListener).anyTimes();

    replay(taskStatusListenerProvider, taskStatusListener, hostRoleCommand1, hostRoleCommand4, hostRoleCommand5);
    tasksSubscriptions = new NamedTasksSubscriptions(taskStatusListenerProvider);
    tasksSubscriptions.addTaskId(SESSION_ID_1, 1L, "sub-1");
    tasksSubscriptions.addTaskId(SESSION_ID_1, 5L, "sub-5");
    tasksSubscriptions.addTaskId(SESSION_ID_2, 1L, "sub-1");
    tasksSubscriptions.addTaskId(SESSION_ID_2, 4L, "sub-4");
  }

  @Test
  public void testMatching() {
    Optional<Long> taskIdOpt = tasksSubscriptions.matchDestination("/events/tasks/1");
    assertTrue(taskIdOpt.isPresent());
    assertEquals(1L, taskIdOpt.get().longValue());
    assertFalse(tasksSubscriptions.matchDestination("/events/topologies").isPresent());
  }

  @Test
  public void testCheckId() {
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertTrue(tasksSubscriptions.checkTaskId(4L));
    assertTrue(tasksSubscriptions.checkTaskId(5L));
    assertFalse(tasksSubscriptions.checkTaskId(2L));
  }

  @Test
  public void testRemoveBySessionId() {
    tasksSubscriptions.removeSession(SESSION_ID_1);
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertTrue(tasksSubscriptions.checkTaskId(4L));
    assertFalse(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.removeSession(SESSION_ID_2);
    assertFalse(tasksSubscriptions.checkTaskId(1L));
    assertFalse(tasksSubscriptions.checkTaskId(4L));
    assertFalse(tasksSubscriptions.checkTaskId(5L));
  }

  @Test
  public void testRemoveById() {
    tasksSubscriptions.removeId(SESSION_ID_1, "sub-1");
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertTrue(tasksSubscriptions.checkTaskId(4L));
    assertTrue(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.removeId(SESSION_ID_1, "sub-5");
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertTrue(tasksSubscriptions.checkTaskId(4L));
    assertFalse(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.removeId(SESSION_ID_2, "sub-1");
    assertFalse(tasksSubscriptions.checkTaskId(1L));
    assertTrue(tasksSubscriptions.checkTaskId(4L));
    assertFalse(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.removeId(SESSION_ID_2, "sub-4");
    assertFalse(tasksSubscriptions.checkTaskId(1L));
    assertFalse(tasksSubscriptions.checkTaskId(4L));
    assertFalse(tasksSubscriptions.checkTaskId(5L));
  }

  @Test
  public void testAddDestination() {
    tasksSubscriptions = new NamedTasksSubscriptions(taskStatusListenerProvider);
    tasksSubscriptions.addDestination(SESSION_ID_1, "/events/tasks/1", "sub-1");
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertFalse(tasksSubscriptions.checkTaskId(4L));
    assertFalse(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.addDestination(SESSION_ID_1, "/events/tasks/5", "sub-5");
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertFalse(tasksSubscriptions.checkTaskId(4L));
    assertTrue(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.addDestination(SESSION_ID_2, "/events/tasks/1", "sub-1");
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertFalse(tasksSubscriptions.checkTaskId(4L));
    assertTrue(tasksSubscriptions.checkTaskId(5L));

    tasksSubscriptions.addDestination(SESSION_ID_2, "/events/tasks/4", "sub-4");
    assertTrue(tasksSubscriptions.checkTaskId(1L));
    assertTrue(tasksSubscriptions.checkTaskId(4L));
    assertTrue(tasksSubscriptions.checkTaskId(5L));
  }
}
