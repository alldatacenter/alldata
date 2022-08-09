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

package org.apache.ambari.server.state.scheduler;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.captureLong;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;

public class BatchRequestJobTest {

  @Test
  public void testDoWork() throws Exception {
    ExecutionScheduleManager scheduleManagerMock = createMock(ExecutionScheduleManager.class);
    BatchRequestJob batchRequestJob = new BatchRequestJob(scheduleManagerMock, 100L);
    String clusterName = "mycluster";
    long requestId = 11L;
    long executionId = 31L;
    long batchId = 1L;

    Map<String, Object> properties = new HashMap<>();
    properties.put(BatchRequestJob.BATCH_REQUEST_EXECUTION_ID_KEY, executionId);
    properties.put(BatchRequestJob.BATCH_REQUEST_BATCH_ID_KEY, batchId);
    properties.put(BatchRequestJob.BATCH_REQUEST_CLUSTER_NAME_KEY, clusterName);

    HashMap<String, Integer> taskCounts = new HashMap<String, Integer>()
    {{ put(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY, 0);
      put(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_IN_CURRENT_BATCH_KEY, 0);
      put(BatchRequestJob.BATCH_REQUEST_TOTAL_TASKS_KEY, 0); }};


    BatchRequestResponse pendingResponse = new BatchRequestResponse();
    pendingResponse.setStatus(HostRoleStatus.PENDING.toString());
    BatchRequestResponse inProgressResponse = new BatchRequestResponse();
    inProgressResponse.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    BatchRequestResponse completedResponse = new BatchRequestResponse();
    completedResponse.setStatus(HostRoleStatus.COMPLETED.toString());

    Capture<Long> executionIdCapture = EasyMock.newCapture();
    Capture<Long> batchIdCapture = EasyMock.newCapture();
    Capture<String> clusterNameCapture = EasyMock.newCapture();


    expect(scheduleManagerMock.executeBatchRequest(captureLong(executionIdCapture),
      captureLong(batchIdCapture),
      capture(clusterNameCapture))).andReturn(requestId);

    expect(scheduleManagerMock.getBatchRequestResponse(requestId, clusterName)).
      andReturn(pendingResponse).times(2);
    expect(scheduleManagerMock.getBatchRequestResponse(requestId, clusterName)).
      andReturn(inProgressResponse).times(4);
    expect(scheduleManagerMock.getBatchRequestResponse(requestId, clusterName)).
      andReturn(completedResponse).once();
    expect(scheduleManagerMock.hasToleranceThresholdExceeded(executionId,
      clusterName, taskCounts)).andReturn(false);

    scheduleManagerMock.updateBatchRequest(eq(executionId), eq(batchId), eq(clusterName),
        anyObject(BatchRequestResponse.class), eq(true));
    expectLastCall().anyTimes();

    replay(scheduleManagerMock);

    batchRequestJob.doWork(properties);

    verify(scheduleManagerMock);

    Assert.assertEquals(executionId, executionIdCapture.getValue().longValue());
    Assert.assertEquals(batchId, batchIdCapture.getValue().longValue());
    Assert.assertEquals(clusterName, clusterNameCapture.getValue());
  }

  @Test
  public void testTaskCountsPersistedWithTrigger() throws Exception {
    ExecutionScheduleManager scheduleManagerMock = createNiceMock
      (ExecutionScheduleManager.class);
    BatchRequestJob batchRequestJobMock = createMockBuilder
      (BatchRequestJob.class).withConstructor(scheduleManagerMock, 100L)
      .addMockedMethods("doWork")
      .createMock();
    JobExecutionContext executionContext = createNiceMock(JobExecutionContext.class);
    JobDataMap jobDataMap = createNiceMock(JobDataMap.class);
    JobDetail jobDetail = createNiceMock(JobDetail.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY, 10);
    properties.put(BatchRequestJob.BATCH_REQUEST_TOTAL_TASKS_KEY, 20);

    expect(scheduleManagerMock.continueOnMisfire(executionContext)).andReturn(true);
    expect(executionContext.getMergedJobDataMap()).andReturn(jobDataMap);
    expect(executionContext.getJobDetail()).andReturn(jobDetail);
    expect(jobDetail.getKey()).andReturn(JobKey.jobKey("testJob", "testGroup"));
    expect(jobDataMap.getWrappedMap()).andReturn(properties);
    expect(jobDataMap.getString((String) anyObject())).andReturn("testJob").anyTimes();

    Capture<Trigger> triggerCapture = EasyMock.newCapture();
    scheduleManagerMock.scheduleJob(capture(triggerCapture));
    expectLastCall().once();

    replay(scheduleManagerMock, executionContext, jobDataMap, jobDetail);

    batchRequestJobMock.execute(executionContext);

    verify(scheduleManagerMock, executionContext, jobDataMap, jobDetail);

    Trigger trigger = triggerCapture.getValue();
    Assert.assertNotNull(trigger);
    JobDataMap savedMap = trigger.getJobDataMap();
    Assert.assertNotNull(savedMap);
    Assert.assertEquals(10, savedMap.getIntValue
      (BatchRequestJob.BATCH_REQUEST_FAILED_TASKS_KEY));
    Assert.assertEquals(20, savedMap.getIntValue
      (BatchRequestJob.BATCH_REQUEST_TOTAL_TASKS_KEY));
  }
}
