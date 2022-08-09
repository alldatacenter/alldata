/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.TaskAttemptResourceProvider.TaskAttemptFetcher;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * TaskAttemptResourceProvider tests
 */
public class TaskAttemptResourceProviderTest {
  @Test
  public void testGetResources() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<Resource> expected = new HashSet<>();
    expected.add(createTaskAttemptResponse("Cluster100", "workflow1", "job1",
        "taskAttempt1"));
    expected.add(createTaskAttemptResponse("Cluster100", "workflow2", "job2",
        "taskAttempt2"));
    expected.add(createTaskAttemptResponse("Cluster100", "workflow2", "job2",
        "taskAttempt3"));

    Resource.Type type = Resource.Type.TaskAttempt;
    Set<String> propertyIds = TaskAttemptResourceProvider.propertyIds;

    TaskAttemptFetcher taskAttemptFetcher = createMock(TaskAttemptFetcher.class);
    expect(
        taskAttemptFetcher.fetchTaskAttemptDetails(propertyIds, null, null,
            "job2", null)).andReturn(expected).once();
    replay(taskAttemptFetcher);

    Map<Resource.Type,String> keyPropertyIds = PropertyHelper
        .getKeyPropertyIds(type);
    ResourceProvider provider = new TaskAttemptResourceProvider(propertyIds,
        keyPropertyIds, taskAttemptFetcher);

    Request request = PropertyHelper.getReadRequest(propertyIds);
    Predicate predicate = new PredicateBuilder()
        .property(TaskAttemptResourceProvider.TASK_ATTEMPT_JOB_ID_PROPERTY_ID)
        .equals("job2").toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> names = new HashSet<>();
    for (Resource resource : resources) {
      String clusterName = (String) resource
          .getPropertyValue(TaskAttemptResourceProvider.TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names
          .add((String) resource
              .getPropertyValue(TaskAttemptResourceProvider.TASK_ATTEMPT_ID_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (Resource resource : expected) {
      Assert
          .assertTrue(names.contains(resource
              .getPropertyValue(TaskAttemptResourceProvider.TASK_ATTEMPT_ID_PROPERTY_ID)));
    }

    verify(taskAttemptFetcher);
  }

  @Test
  public void testTaskAttemptFetcher() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<String> requestedIds = new HashSet<>();
    requestedIds.add(TaskAttemptResourceProvider.TASK_ATTEMPT_ID_PROPERTY_ID);

    Map<Resource.Type,String> keyPropertyIds = PropertyHelper
        .getKeyPropertyIds(Resource.Type.TaskAttempt);
    ResourceProvider provider = new TestTaskAttemptResourceProvider(
        requestedIds, keyPropertyIds);

    Request request = PropertyHelper.getReadRequest(requestedIds);
    Predicate predicate = new PredicateBuilder()
        .property(TaskAttemptResourceProvider.TASK_ATTEMPT_ID_PROPERTY_ID)
        .equals("taskattempt1").toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String workflowId = (String) resource
          .getPropertyValue(TaskAttemptResourceProvider.TASK_ATTEMPT_ID_PROPERTY_ID);
      Assert.assertEquals("taskattempt1", workflowId);
    }
  }

  private static Resource createTaskAttemptResponse(String clusterName,
      String workflowId, String jobId, String taskAttemptId) {
    Resource r = new ResourceImpl(Resource.Type.TaskAttempt);
    r.setProperty(
        TaskAttemptResourceProvider.TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID,
        clusterName);
    r.setProperty(
        TaskAttemptResourceProvider.TASK_ATTEMPT_WORKFLOW_ID_PROPERTY_ID,
        workflowId);
    r.setProperty(TaskAttemptResourceProvider.TASK_ATTEMPT_JOB_ID_PROPERTY_ID,
        jobId);
    r.setProperty(TaskAttemptResourceProvider.TASK_ATTEMPT_ID_PROPERTY_ID,
        taskAttemptId);
    return r;
  }

  private static class TestTaskAttemptResourceProvider extends
      TaskAttemptResourceProvider {
    protected TestTaskAttemptResourceProvider(Set<String> propertyIds,
        Map<Type,String> keyPropertyIds) {
      super(propertyIds, keyPropertyIds, null);
      this.taskAttemptFetcher = new TestTaskAttemptFetcher();
    }

    private class TestTaskAttemptFetcher extends PostgresTaskAttemptFetcher {
      ResultSet rs = null;

      public TestTaskAttemptFetcher() {
        super((ConnectionFactory) null);
      }

      @Override
      protected ResultSet getResultSet(Set<String> requestedIds,
          String workflowId, String jobId, String taskAttemptId)
          throws SQLException {
        rs = createMock(ResultSet.class);
        expect(rs.next()).andReturn(true).once();
        expect(rs.getString(getDBField(TASK_ATTEMPT_ID_PROPERTY_ID).toString()))
            .andReturn("taskattempt1").once();
        expect(rs.next()).andReturn(false).once();
        rs.close();
        expectLastCall().once();
        replay(rs);
        return rs;
      }

      @Override
      protected void close() {
        verify(rs);
      }
    }
  }
}
