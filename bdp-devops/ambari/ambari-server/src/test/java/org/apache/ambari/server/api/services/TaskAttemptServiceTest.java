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
package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

/**
 * Unit tests for TaskAttemptService.
 */
public class TaskAttemptServiceTest extends BaseServiceTest {

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    // getTaskAttempt
    TestTaskAttemptService service = new TestTaskAttemptService("clusterName",
        "workflowId", "jobId");
    Method m = service.getClass().getMethod("getTaskAttempt",
      String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "jobId"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m,
        args, null));

    // getTaskAttempts
    service = new TestTaskAttemptService("clusterName", "workflowId", "jobId");
    m = service.getClass().getMethod("getTaskAttempts", String.class, HttpHeaders.class,
        UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m,
        args, null));

    return listInvocations;
  }

  private class TestTaskAttemptService extends TaskAttemptService {
    private String clusterName;
    private String workflowId;
    private String jobId;

    public TestTaskAttemptService(String clusterName, String workflowId,
        String jobId) {
      super(clusterName, workflowId, jobId);
      this.clusterName = clusterName;
      this.workflowId = workflowId;
      this.jobId = jobId;
    }

    @Override
    ResourceInstance createTaskAttemptResource(String clusterName,
        String workflowId, String jobId, String taskAttemptId) {
      assertEquals(this.clusterName, clusterName);
      assertEquals(this.workflowId, workflowId);
      assertEquals(this.jobId, jobId);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }
}
