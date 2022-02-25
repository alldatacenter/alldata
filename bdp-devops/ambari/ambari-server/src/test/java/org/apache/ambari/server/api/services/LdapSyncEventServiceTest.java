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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * LdapSyncEventService tests.
 */
public class LdapSyncEventServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getEvent
    LdapSyncEventService permissionService = new TestLdapSyncEventService();
    Method m = permissionService.getClass().getMethod("getEvent", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "1"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, permissionService, m, args, null));

    //getEvents
    permissionService = new TestLdapSyncEventService();
    m = permissionService.getClass().getMethod("getEvents", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, permissionService, m, args, null));

    //createEvent
    permissionService = new TestLdapSyncEventService();
    m = permissionService.getClass().getMethod("createEvent", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, permissionService, m, args, "body"));

    //deleteEvent
    permissionService = new TestLdapSyncEventService();
    m = permissionService.getClass().getMethod("deleteEvent", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "1"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, permissionService, m, args, null));

    return listInvocations;
  }


  private class TestLdapSyncEventService extends LdapSyncEventService {

    private TestLdapSyncEventService() {
      super();
    }

    @Override
    protected ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {
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
