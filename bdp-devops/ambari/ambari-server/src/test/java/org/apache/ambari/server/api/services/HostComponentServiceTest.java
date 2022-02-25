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
 * Unit tests for HostComponentService.
 */
public class HostComponentServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getHostComponent
    HostComponentService componentService = new TestHostComponentService("clusterName", "serviceName", "componentName");
    Method m = componentService.getClass().getMethod("getHostComponent", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    Object[] args = new Object[]{null, getHttpHeaders(), getUriInfo(), "componentName", null};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, componentService, m, args, null));

    //getHostComponents
    componentService = new TestHostComponentService("clusterName", "serviceName", null);
    m = componentService.getClass().getMethod("getHostComponents", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), null};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, componentService, m, args, null));

    //createHostComponent
    componentService = new TestHostComponentService("clusterName", "serviceName", "componentName");
    m = componentService.getClass().getMethod("createHostComponent", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "componentName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, componentService, m, args, "body"));

    //createHostComponents
    componentService = new TestHostComponentService("clusterName", "serviceName", null);
    m = componentService.getClass().getMethod("createHostComponents", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, componentService, m, args, "body"));

    //updateHostComponent
    componentService = new TestHostComponentService("clusterName", "serviceName", "componentName");
    m = componentService.getClass().getMethod("updateHostComponent", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "componentName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, componentService, m, args, "body"));

    //updateHostComponents
    componentService = new TestHostComponentService("clusterName", "serviceName", null);
    m = componentService.getClass().getMethod("updateHostComponents", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, componentService, m, args, "body"));

    //deleteHostComponent
    componentService = new TestHostComponentService("clusterName", "serviceName", "componentName");
    m = componentService.getClass().getMethod("deleteHostComponent", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "componentName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, componentService, m, args, null));

    return listInvocations;
  }

  private class TestHostComponentService extends HostComponentService {
    private String m_clusterId;
    private String m_hostId;
    private String m_hostComponentId;

    private TestHostComponentService(String clusterId, String hostId, String hostComponentId) {
      super(clusterId, hostId);
      m_clusterId = clusterId;
      m_hostId = hostId;
      m_hostComponentId = hostComponentId;
    }

    @Override
    ResourceInstance createHostComponentResource(String clusterName, String hostName, String hostComponentName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_hostId, hostName);
      assertEquals(m_hostComponentId, hostComponentName);
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
