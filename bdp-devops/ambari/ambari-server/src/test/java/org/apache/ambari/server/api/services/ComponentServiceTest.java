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
 * Unit tests for ComponentService.
 */
public class ComponentServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getComponent
    ComponentService service = new TestComponentService("clusterName", "serviceName", "componentName");
    Method m = service.getClass().getMethod("getComponent", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "componentName", null};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getComponents
    service = new TestComponentService("clusterName", "serviceName", null);
    m = service.getClass().getMethod("getComponents", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), null};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createComponent
    service = new TestComponentService("clusterName", "serviceName", "componentName");
    m = service.getClass().getMethod("createComponent", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "componentName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //createComponentsAndHosts
    service = new TestComponentService("clusterName", "serviceName", null);
    m = service.getClass().getMethod("createComponents", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //updateComponent
    service = new TestComponentService("clusterName", "serviceName", "componentName");
    m = service.getClass().getMethod("updateComponent", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "componentName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //updateComponents
    service = new TestComponentService("clusterName", "serviceName", null);
    m = service.getClass().getMethod("updateComponents", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //deleteComponent
    service = new TestComponentService("clusterName", "serviceName", "componentName");
    m = service.getClass().getMethod("deleteComponent", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "componentName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    return listInvocations;
  }

  private class TestComponentService extends ComponentService {
    private String m_clusterId;
    private String m_serviceId;
    private String m_componentId;

    private TestComponentService(String clusterId, String serviceId, String componentId) {
      super(clusterId, serviceId);
      m_clusterId = clusterId;
      m_serviceId = serviceId;
      m_componentId = componentId;
    }

    @Override
    ResourceInstance createComponentResource(String clusterName, String serviceName, String componentName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
      assertEquals(m_componentId, componentName);
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
