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

import static junit.framework.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

public class ConfigGroupServiceTest extends BaseServiceTest {

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    // Get Config Groups
    ConfigGroupService configGroupService = new TestConfigGroupService
      ("clusterName", null);
    Method m = configGroupService.getClass().getMethod("getConfigGroups",
      String.class, HttpHeaders.class, UriInfo.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET,
      configGroupService, m, args, null));

    // Get Config Group
    configGroupService = new TestConfigGroupService("clusterName", "groupId");
    m = configGroupService.getClass().getMethod("getConfigGroup",
      String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "groupId"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET,
      configGroupService, m, args, null));

    // Create Config group
    configGroupService = new TestConfigGroupService("clusterName", null);
    m = configGroupService.getClass().getMethod("createConfigGroup",
      String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST,
      configGroupService, m, args, "body"));

    // Delete Config group
    configGroupService = new TestConfigGroupService("clusterName", "groupId");
    m = configGroupService.getClass().getMethod("deleteConfigGroup",
      HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "groupId"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE,
      configGroupService, m, args, null));

    // Update Config group
    configGroupService = new TestConfigGroupService("clusterName", "groupId");
    m = configGroupService.getClass().getMethod("updateConfigGroup",
      String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "groupId"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT,
      configGroupService, m, args, "body"));

    return listInvocations;
  }

  private class TestConfigGroupService extends ConfigGroupService {
    private String m_clusterName;
    private String m_groupId;

    public TestConfigGroupService(String m_clusterName, String groupId) {
      super(m_clusterName);
      this.m_clusterName = m_clusterName;
      this.m_groupId = groupId;
    }

    @Override
    ResourceInstance createConfigGroupResource(String clusterName,
                                               String groupId) {
      assertEquals(m_clusterName, clusterName);
      assertEquals(m_groupId, groupId);
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
