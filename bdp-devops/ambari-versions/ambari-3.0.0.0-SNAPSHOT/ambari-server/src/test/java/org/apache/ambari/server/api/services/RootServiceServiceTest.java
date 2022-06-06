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
* Unit tests for RootServiceService.
*/
public class RootServiceServiceTest extends BaseServiceTest {

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();
    
    //getServices
    RootServiceService service = new TestRootServiceService(null, null, null);
    Method m = service.getClass().getMethod("getRootServices", String.class, HttpHeaders.class, UriInfo.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    //getService
    service = new TestRootServiceService("AMBARI", null, null);
    m = service.getClass().getMethod("getRootService", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "AMBARI"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    //getServiceComponents
    service = new TestRootServiceService("AMBARI", null, null);
    m = service.getClass().getMethod("getRootServiceComponents", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "AMBARI"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    //getServiceComponent
    service = new TestRootServiceService("AMBARI", "AMBARI_SERVER", null);
    m = service.getClass().getMethod("getRootServiceComponent", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "AMBARI", "AMBARI_SERVER"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    //getRootHostComponents
    service = new TestRootServiceService("AMBARI", "AMBARI_SERVER", null);
    m = service.getClass().getMethod("getRootServiceComponentHosts", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "AMBARI", "AMBARI_SERVER"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    //getRootHosts
    service = new TestRootServiceService("AMBARI", "AMBARI_SERVER", null);
    m = service.getClass().getMethod("getRootHosts", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    //getRootHost
    service = new TestRootServiceService("AMBARI", "AMBARI_SERVER", "host1");
    m = service.getClass().getMethod("getRootHost", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "host1"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));
    
    
    return listInvocations;
  }
  
  private class TestRootServiceService extends RootServiceService {

    private String m_serviceName;
    private String m_componentName;
    private String m_hostName;

    private TestRootServiceService(String serviceName, String componentName, String hostName) {
      m_serviceName = serviceName;
      m_componentName = componentName;
      m_hostName = hostName;
    }

    @Override
    protected ResourceInstance createServiceResource(String serviceName) {
      assertEquals(m_serviceName, serviceName);
      return getTestResource();
    }
    
    @Override
    protected ResourceInstance createServiceComponentResource(String serviceName, String componentName) {
      assertEquals(m_serviceName, serviceName);
      assertEquals(m_componentName, componentName);
      return getTestResource();
    }

    @Override
    protected ResourceInstance createHostComponentResource(String serviceName, String hostName, String componentName) {
      assertEquals(m_serviceName, serviceName);
      assertEquals(m_hostName, hostName);
      assertEquals(m_componentName, componentName);
      return getTestResource();
    }
    
    @Override
    protected ResourceInstance createHostResource(String hostName) {
      assertEquals(m_hostName, hostName);
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
