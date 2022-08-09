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
import java.util.Set;

import org.apache.ambari.server.controller.internal.JobResourceProvider.JobFetcher;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * JobResourceProvider tests.
 */
public class JobResourceProviderTest {
  @Test
  public void testGetResources() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<Resource> expected = new HashSet<>();
    expected.add(createJobResponse("Cluster100", "workflow1", "job1"));
    expected.add(createJobResponse("Cluster100", "workflow2", "job2"));
    expected.add(createJobResponse("Cluster100", "workflow2", "job3"));


    Set<String> propertyIds = JobResourceProvider.propertyIds;

    JobFetcher jobFetcher = createMock(JobFetcher.class);
    expect(jobFetcher.fetchJobDetails(propertyIds, null, "workflow2", null))
        .andReturn(expected).once();
    replay(jobFetcher);

    ResourceProvider provider = new JobResourceProvider(jobFetcher);

    Request request = PropertyHelper.getReadRequest(propertyIds);
    Predicate predicate = new PredicateBuilder()
        .property(JobResourceProvider.JOB_WORKFLOW_ID_PROPERTY_ID)
        .equals("workflow2").toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> names = new HashSet<>();
    for (Resource resource : resources) {
      String clusterName = (String) resource
          .getPropertyValue(JobResourceProvider.JOB_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource
          .getPropertyValue(JobResourceProvider.JOB_ID_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (Resource resource : expected) {
      Assert.assertTrue(names.contains(resource
          .getPropertyValue(JobResourceProvider.JOB_ID_PROPERTY_ID)));
    }

    verify(jobFetcher);
  }

  @Test
  public void testJobFetcher1() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<String> requestedIds = new HashSet<>();
    requestedIds.add(JobResourceProvider.JOB_ID_PROPERTY_ID);

    ResourceProvider provider = new TestJobResourceProvider(1);

    Request request = PropertyHelper.getReadRequest(requestedIds);
    Predicate predicate = new PredicateBuilder()
        .property(JobResourceProvider.JOB_ID_PROPERTY_ID).equals("job1")
        .toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String workflowId = (String) resource
          .getPropertyValue(JobResourceProvider.JOB_ID_PROPERTY_ID);
      Assert.assertEquals("job1", workflowId);
    }
  }

  @Test
  public void testJobFetcher2() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<String> requestedIds = new HashSet<>();
    requestedIds.add(JobResourceProvider.JOB_ID_PROPERTY_ID);
    requestedIds.add(JobResourceProvider.JOB_SUBMIT_TIME_PROPERTY_ID);

    ResourceProvider provider = new TestJobResourceProvider(2);

    Request request = PropertyHelper.getReadRequest(requestedIds);
    Predicate predicate = new PredicateBuilder()
        .property(JobResourceProvider.JOB_ID_PROPERTY_ID).equals("job1")
        .toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String workflowId = (String) resource
          .getPropertyValue(JobResourceProvider.JOB_ID_PROPERTY_ID);
      Assert.assertEquals("job1", workflowId);
      Assert.assertEquals(42l, resource
          .getPropertyValue(JobResourceProvider.JOB_SUBMIT_TIME_PROPERTY_ID));
    }
  }

  @Test
  public void testJobFetcher3() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<String> requestedIds = new HashSet<>();
    requestedIds.add(JobResourceProvider.JOB_ID_PROPERTY_ID);
    requestedIds.add(JobResourceProvider.JOB_ELAPSED_TIME_PROPERTY_ID);

    ResourceProvider provider = new TestJobResourceProvider(3);

    Request request = PropertyHelper.getReadRequest(requestedIds);
    Predicate predicate = new PredicateBuilder()
        .property(JobResourceProvider.JOB_ID_PROPERTY_ID).equals("job1")
        .toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String workflowId = (String) resource
          .getPropertyValue(JobResourceProvider.JOB_ID_PROPERTY_ID);
      Assert.assertEquals("job1", workflowId);
      Assert.assertEquals(1l, resource
          .getPropertyValue(JobResourceProvider.JOB_ELAPSED_TIME_PROPERTY_ID));
    }
  }

  @Test
  public void testJobFetcher4() throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    Set<String> requestedIds = new HashSet<>();
    requestedIds.add(JobResourceProvider.JOB_ID_PROPERTY_ID);
    requestedIds.add(JobResourceProvider.JOB_SUBMIT_TIME_PROPERTY_ID);
    requestedIds.add(JobResourceProvider.JOB_ELAPSED_TIME_PROPERTY_ID);


    ResourceProvider provider = new TestJobResourceProvider(4);

    Request request = PropertyHelper.getReadRequest(requestedIds);
    Predicate predicate = new PredicateBuilder()
        .property(JobResourceProvider.JOB_ID_PROPERTY_ID).equals("job1")
        .toPredicate();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String workflowId = (String) resource
          .getPropertyValue(JobResourceProvider.JOB_ID_PROPERTY_ID);
      Assert.assertEquals("job1", workflowId);
      Assert.assertEquals(42l, resource
          .getPropertyValue(JobResourceProvider.JOB_SUBMIT_TIME_PROPERTY_ID));
      Assert.assertEquals(0l, resource
          .getPropertyValue(JobResourceProvider.JOB_ELAPSED_TIME_PROPERTY_ID));
    }
  }

  private static Resource createJobResponse(String clusterName,
      String workflowId, String jobId) {
    Resource r = new ResourceImpl(Resource.Type.Job);
    r.setProperty(JobResourceProvider.JOB_CLUSTER_NAME_PROPERTY_ID, clusterName);
    r.setProperty(JobResourceProvider.JOB_WORKFLOW_ID_PROPERTY_ID, workflowId);
    r.setProperty(JobResourceProvider.JOB_ID_PROPERTY_ID, jobId);
    return r;
  }

  private static class TestJobResourceProvider extends JobResourceProvider {
    protected TestJobResourceProvider(int type) {
      super();
      this.jobFetcher = new TestJobFetcher(type);
    }

    private class TestJobFetcher extends PostgresJobFetcher {
      ResultSet rs = null;
      int type;

      public TestJobFetcher(int type) {
        super((ConnectionFactory) null);
        this.type = type;
      }

      @Override
      protected ResultSet getResultSet(Set<String> requestedIds,
          String workflowId, String jobId) throws SQLException {
        rs = createMock(ResultSet.class);
        expect(rs.next()).andReturn(true).once();
        expect(rs.getString(getDBField(JOB_ID_PROPERTY_ID).toString()))
            .andReturn("job1").once();
        if (type > 1)
          expect(rs.getLong(getDBField(JOB_SUBMIT_TIME_PROPERTY_ID).toString()))
              .andReturn(42l).once();
        if (type == 3)
          expect(rs.getLong(JobFields.FINISHTIME.toString())).andReturn(43l)
              .once();
        if (type == 4)
          expect(rs.getLong(JobFields.FINISHTIME.toString())).andReturn(41l)
              .once();
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
