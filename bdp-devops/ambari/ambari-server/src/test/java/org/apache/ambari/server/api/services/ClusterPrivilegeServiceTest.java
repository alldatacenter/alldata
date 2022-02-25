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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

/**
 * Unit tests for ClusterPrivilegeService.
 */
public class ClusterPrivilegeServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getPrivilege
    ClusterPrivilegeService privilegeService = new TestClusterPrivilegeService("c1");
    Method m = privilegeService.getClass().getMethod("getPrivilege", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "privilegename"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, privilegeService, m, args, null));

    //getPrivileges
    privilegeService = new TestClusterPrivilegeService("c1");
    m = privilegeService.getClass().getMethod("getPrivileges", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, privilegeService, m, args, null));

    //createPrivilege
    privilegeService = new TestClusterPrivilegeService("c1");
    m = privilegeService.getClass().getMethod("createPrivilege", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, privilegeService, m, args, "body"));

    //deletePrivilege
    privilegeService = new TestClusterPrivilegeService("c1");
    m = privilegeService.getClass().getMethod("deletePrivilege", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "privilegename"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, privilegeService, m, args, null));

    return listInvocations;
  }


  private class TestClusterPrivilegeService extends ClusterPrivilegeService {

    private TestClusterPrivilegeService(String clusterName) {
      super(clusterName);
    }

    @Override
    protected ResourceInstance createPrivilegeResource(String clusterName) {
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
