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
 * Unit tests for PrivilegeService.
 */
public class PrivilegeServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getPrivilege
    PrivilegeService service = new TestPrivilegeService("id");
    Method m = service.getClass().getMethod("getPrivilege", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getPrivileges
    service = new TestPrivilegeService(null);
    m = service.getClass().getMethod("getPrivileges", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createPrivilege
    service = new TestPrivilegeService(null);
    m = service.getClass().getMethod("createPrivilege", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //updatePrivilege
    service = new TestPrivilegeService("id");
    m = service.getClass().getMethod("updatePrivilege", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //updatePrivileges
    service = new TestPrivilegeService(null);
    m = service.getClass().getMethod("updatePrivileges", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //deletePrivilege
    service = new TestPrivilegeService("id");
    m = service.getClass().getMethod("deletePrivilege", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    //deletePrivileges
    service = new TestPrivilegeService(null);
    m = service.getClass().getMethod("deletePrivileges", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, "body"));

    return listInvocations;
  }


  private class TestPrivilegeService extends PrivilegeService {
    private String id;

    private TestPrivilegeService(String id) {
      this.id = id;
    }

    @Override
    protected ResourceInstance createPrivilegeResource(String id) {
      assertEquals(this.id, id);
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
