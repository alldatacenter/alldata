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
 * Unit tests for TargetClusterService.
 */
public class TargetClusterServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getTargetCluster
    TargetClusterService targetClusterService = new TestTargetClusterService("targetClusterName");
    Method m = targetClusterService.getClass().getMethod("getTargetCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "targetClusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, targetClusterService, m, args, null));

    //getTargetClusters
    targetClusterService = new TestTargetClusterService(null);
    m = targetClusterService.getClass().getMethod("getTargetClusters", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, targetClusterService, m, args, null));

    //createTargetCluster
    targetClusterService = new TestTargetClusterService("targetClusterName");
    m = targetClusterService.getClass().getMethod("createTargetCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "targetClusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, targetClusterService, m, args, "body"));

    //updateTargetCluster
    targetClusterService = new TestTargetClusterService("targetClusterName");
    m = targetClusterService.getClass().getMethod("updateTargetCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "targetClusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, targetClusterService, m, args, "body"));

    //deleteTargetCluster
    targetClusterService = new TestTargetClusterService("targetClusterName");
    m = targetClusterService.getClass().getMethod("deleteTargetCluster", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "targetClusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, targetClusterService, m, args, null));

    return listInvocations;
  }


  private class TestTargetClusterService extends TargetClusterService {
    private String m_targetClusterId;

    private TestTargetClusterService(String targetClusterId) {
      m_targetClusterId = targetClusterId;
    }

    @Override
    ResourceInstance createTargetClusterResource(String targetClusterName) {
      assertEquals(m_targetClusterId, targetClusterName);
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

  //todo: test getHostHandler, getServiceHandler, getHostComponentHandler
}
