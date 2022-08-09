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
 * Unit tests for InstanceService.
 */
public class InstanceServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getInstance
    InstanceService service = new TestInstanceService("clusterName", "instanceName");
    Method m = service.getClass().getMethod("getInstance", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "instanceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getInstances
    service = new TestInstanceService("clusterName", null);
    m = service.getClass().getMethod("getInstances", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createInstance
    service = new TestInstanceService("clusterName", "instanceName");
    m = service.getClass().getMethod("createInstance", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "instanceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //updateInstance
    service = new TestInstanceService("clusterName", "instanceName");
    m = service.getClass().getMethod("updateInstance", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "instanceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //deleteInstance
    service = new TestInstanceService("clusterName", "instanceName");
    m = service.getClass().getMethod("deleteInstance", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "instanceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    return listInvocations;
  }

  private class TestInstanceService extends InstanceService {
    private String m_feedName;
    private String m_instanceId;

    private TestInstanceService(String feedName, String instanceId) {
      super(feedName);
      m_feedName = feedName;
      m_instanceId = instanceId;
    }

    @Override
    ResourceInstance createInstanceResource(String feedName, String instanceID, UriInfo ui) {
      assertEquals(m_feedName, feedName);
      assertEquals(m_instanceId, instanceID);
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


