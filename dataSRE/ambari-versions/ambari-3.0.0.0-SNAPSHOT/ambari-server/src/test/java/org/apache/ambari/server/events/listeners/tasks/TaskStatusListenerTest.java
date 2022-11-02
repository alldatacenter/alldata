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

package org.apache.ambari.server.events.listeners.tasks;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.stomp.NamedTasksSubscriptions;
import org.apache.ambari.server.events.NamedTaskUpdateEvent;
import org.apache.ambari.server.events.TaskCreateEvent;
import org.apache.ambari.server.events.TaskUpdateEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.events.publishers.TaskEventPublisher;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class TaskStatusListenerTest extends EasyMockSupport {

  private TaskEventPublisher publisher = new TaskEventPublisher();

  @Inject
  private ExecutionCommandDAO executionCommandDAO;

  @Inject
  private ExecutionCommandWrapperFactory ecwFactory;


  @Test
  public void testOnTaskUpdateEvent() throws ClusterNotFoundException {
    List<HostRoleCommand> hostRoleCommands = new ArrayList<>();
    ServiceComponentHostEvent serviceComponentHostEvent = createNiceMock(ServiceComponentHostEvent.class);
    HostDAO hostDAO = createNiceMock(HostDAO.class);

    EasyMock.replay(hostDAO);
    EasyMock.replay(serviceComponentHostEvent);

    int hostRoleCommandSize = 3;
    int hrcCounter = 1;
    for (int stageCounter = 0; stageCounter < 2; stageCounter++) {
      for (int i = 1; i <= hostRoleCommandSize; i++,hrcCounter++) {
        String hostname = "hostname-" + hrcCounter;
        HostRoleCommand hostRoleCommand = new HostRoleCommand(hostname, Role.DATANODE,
            serviceComponentHostEvent, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
        hostRoleCommand.setStatus(HostRoleStatus.PENDING);
        hostRoleCommand.setRequestId(1L);
        hostRoleCommand.setStageId(stageCounter);
        hostRoleCommand.setTaskId(hrcCounter);
        hostRoleCommands.add(hostRoleCommand);
      }
    }

    HostRoleStatus hostRoleStatus = HostRoleStatus.PENDING;
    StageDAO stageDAO = createNiceMock(StageDAO.class);
    RequestDAO requestDAO = createNiceMock(RequestDAO.class);
    StageEntity stageEntity = createNiceMock(StageEntity.class);
    RequestEntity requestEntity = createNiceMock(RequestEntity.class);
    STOMPUpdatePublisher statePublisher = createNiceMock(STOMPUpdatePublisher.class);
    NamedTasksSubscriptions namedTasksSubscriptions = createNiceMock(NamedTasksSubscriptions.class);
    EasyMock.expect(stageEntity.getStatus()).andReturn(hostRoleStatus).anyTimes();;
    EasyMock.expect(stageEntity.getDisplayStatus()).andReturn(hostRoleStatus).anyTimes();
    EasyMock.expect(stageEntity.isSkippable()).andReturn(Boolean.FALSE).anyTimes();;
    EasyMock.expect(stageEntity.getRoleSuccessCriterias()).andReturn(Collections.emptyList()).anyTimes();
    EasyMock.expect(stageDAO.findByPK(anyObject(StageEntityPK.class))).andReturn(stageEntity).anyTimes();
    EasyMock.expect(requestEntity.getStatus()).andReturn(hostRoleStatus).anyTimes();;
    EasyMock.expect(requestEntity.getDisplayStatus()).andReturn(hostRoleStatus).anyTimes();
    EasyMock.expect(requestDAO.findByPK(anyLong())).andReturn(requestEntity).anyTimes();

    EasyMock.expect(requestDAO.updateStatus(eq(1L), eq(HostRoleStatus.COMPLETED),
        eq(HostRoleStatus.SKIPPED_FAILED))).andReturn(new RequestEntity()).times(1);

    EasyMock.replay(stageEntity);
    EasyMock.replay(requestEntity);
    EasyMock.replay(stageDAO);
    EasyMock.replay(requestDAO);
    EasyMock.replay(statePublisher);
    EasyMock.replay(namedTasksSubscriptions);

    TaskCreateEvent event = new TaskCreateEvent(hostRoleCommands);
    TaskStatusListener listener = new TaskStatusListener(publisher, stageDAO, requestDAO, statePublisher,
        namedTasksSubscriptions);

    Assert.assertTrue(listener.getActiveTasksMap().isEmpty());
    Assert.assertTrue(listener.getActiveStageMap().isEmpty());
    Assert.assertTrue(listener.getActiveRequestMap().isEmpty());

    listener.onTaskCreateEvent(event);
    Assert.assertEquals(listener.getActiveTasksMap().size(),6);
    Assert.assertEquals(listener.getActiveStageMap().size(),2);
    Assert.assertEquals(listener.getActiveRequestMap().size(),1);
    Assert.assertEquals(listener.getActiveRequestMap().get(1L).getStatus(), hostRoleStatus);



    // update of a task status of IN_PROGRESS should cascade into an update of request status
    String hostname = "hostname-1";
    HostRoleCommand hostRoleCommand = new HostRoleCommand(hostname, Role.DATANODE,
        serviceComponentHostEvent, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    hostRoleCommand.setStatus(HostRoleStatus.IN_PROGRESS);
    hostRoleCommand.setRequestId(1L);
    hostRoleCommand.setStageId(0);
    hostRoleCommand.setTaskId(1L);
    listener.onTaskUpdateEvent(new TaskUpdateEvent(Collections.singletonList(hostRoleCommand)));
    Assert.assertEquals(HostRoleStatus.IN_PROGRESS, listener.getActiveRequestMap().get(1L).getStatus());

    // update of all tasks status of skip_failed and  completed states should cascade into request status of completed
    // and request display status to be of skip_failed
    hrcCounter = 1;
    List<HostRoleCommand> finalHostRoleCommands = new ArrayList<>();
    HostRoleStatus finalHostRoleStatus = HostRoleStatus.COMPLETED;
    for (int stageCounter = 0; stageCounter < 2; stageCounter++) {
      for (int i = 1; i <= hostRoleCommandSize; i++,hrcCounter++) {
        String finalHostname = "hostname-" + hrcCounter;
        HostRoleCommand finalHostRoleCommand = new HostRoleCommand(finalHostname, Role.DATANODE,
            serviceComponentHostEvent, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
        finalHostRoleCommand.setStatus(finalHostRoleStatus);
        finalHostRoleCommand.setRequestId(1L);
        finalHostRoleCommand.setStageId(stageCounter);
        finalHostRoleCommand.setTaskId(hrcCounter);
        finalHostRoleCommands.add(finalHostRoleCommand);
      }
      finalHostRoleStatus = HostRoleStatus.SKIPPED_FAILED;
    }

    listener.onTaskUpdateEvent(new TaskUpdateEvent(finalHostRoleCommands));

    //Once request status and display status are in completed state, it should no longer be tracked by TaskStatusListener
    Assert.assertNull(listener.getActiveRequestMap().get(1L));

    // verify request status = completed and display_status = skip_failed
    verifyAll();
  }

  @Test
  public void testNamedTasksEnabled() {
    final Long taskId = 1L;
    final Long requestId = 2L;
    final HostRoleStatus status = HostRoleStatus.COMPLETED;
    final String stderr = "gW$%SGFbhzsdfHBzdffdfd";
    final String stdout = "gW$%gTESJ KHBjzdkfjbgv";
    final String errorLog = " wTHT J YHKtjgsjgbvklfj";
    final String outputLog = "546ky3kt%V$WYk4tgs5xzs";

    Provider<TaskStatusListener> taskStatusListenerProvider = createMock(Provider.class);

    NamedTasksSubscriptions namedTasksSubscriptions = new NamedTasksSubscriptions(taskStatusListenerProvider);

    Capture<NamedTaskUpdateEvent> namedTaskUpdateEventCapture = Capture.newInstance();
    STOMPUpdatePublisher stompUpdatePublisher = createStrictMock(STOMPUpdatePublisher.class);
    stompUpdatePublisher.publish(capture(namedTaskUpdateEventCapture));
    expectLastCall();

    ServiceComponentHostEvent serviceComponentHostEvent = createNiceMock(ServiceComponentHostEvent.class);
    HostDAO hostDAO = createNiceMock(HostDAO.class);

    EasyMock.replay(hostDAO);
    EasyMock.replay(serviceComponentHostEvent);

    List<HostRoleCommand> updateHostRolesCommands = new ArrayList<>();
    HostRoleCommand updateHostRoleCommand = new HostRoleCommand("hostName", Role.DATANODE,
        serviceComponentHostEvent, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    updateHostRoleCommand.setStatus(status);
    updateHostRoleCommand.setRequestId(requestId);
    updateHostRoleCommand.setStageId(3L);
    updateHostRoleCommand.setTaskId(taskId);
    updateHostRoleCommand.setStderr(stderr);
    updateHostRoleCommand.setStdout(stdout);
    updateHostRoleCommand.setErrorLog(errorLog);
    updateHostRoleCommand.setOutputLog(outputLog);
    updateHostRolesCommands.add(updateHostRoleCommand);

    StageDAO stageDAO = createNiceMock(StageDAO.class);
    RequestDAO requestDAO = createNiceMock(RequestDAO.class);

    EasyMock.replay(stageDAO);
    EasyMock.replay(requestDAO);
    EasyMock.replay(stompUpdatePublisher);

    TaskStatusListener listener = new TaskStatusListener(publisher, stageDAO, requestDAO, stompUpdatePublisher,
        namedTasksSubscriptions);

    expect(taskStatusListenerProvider.get()).andReturn(listener);

    EasyMock.replay(taskStatusListenerProvider);

    // subscribe for task
    namedTasksSubscriptions.addTaskId("", taskId, "sub-1");

    // add dummy host role command as active
    // status should be the same to avoid request update event firing
    HostRoleCommand activeHostRoleCommand = new HostRoleCommand("hostName", Role.DATANODE,
        serviceComponentHostEvent, RoleCommand.EXECUTE, hostDAO, executionCommandDAO, ecwFactory);
    activeHostRoleCommand.setStatus(status);
    listener.getActiveTasksMap().put(taskId, activeHostRoleCommand);

    listener.onTaskUpdateEvent(new TaskUpdateEvent(updateHostRolesCommands));

    Assert.assertNotNull(namedTaskUpdateEventCapture.getValues());
    Assert.assertEquals(1L, namedTaskUpdateEventCapture.getValues().size());

    NamedTaskUpdateEvent capturedEvent = namedTaskUpdateEventCapture.getValue();

    Assert.assertEquals(taskId, capturedEvent.getId());
    Assert.assertEquals(requestId, capturedEvent.getRequestId());
    Assert.assertEquals(status, capturedEvent.getStatus());
    Assert.assertEquals(stderr, capturedEvent.getStderr());
    Assert.assertEquals(stdout, capturedEvent.getStdout());
    Assert.assertEquals(errorLog, capturedEvent.getErrorLog());
    Assert.assertEquals(outputLog, capturedEvent.getOutLog());

    verifyAll();
  }

}
