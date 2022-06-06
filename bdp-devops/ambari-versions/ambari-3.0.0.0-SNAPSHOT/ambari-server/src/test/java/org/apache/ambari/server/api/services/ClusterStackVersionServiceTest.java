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
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.easymock.EasyMock;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Unit tests for ClusterStackVersionService.
 */
public class ClusterStackVersionServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    ClusterStackVersionService clusterStackVersionService;
    Method m;
    Object[] args;

    //getClusterStackVersions
    clusterStackVersionService = new TestClusterStackVersionService("cluster");
    m = clusterStackVersionService.getClass().getMethod("getClusterStackVersions", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterStackVersionService, m, args, null));

    //getClusterStackVersion
    clusterStackVersionService = new TestClusterStackVersionService("cluster");
    m = clusterStackVersionService.getClass().getMethod("getClusterStackVersion", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "1"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterStackVersionService, m, args, null));

    return listInvocations;
  }

  @Test
  public void testGetRepositoryVersionService() {
    ClusterStackVersionService clusterStackVersionService = new TestClusterStackVersionService("cluster");
    RepositoryVersionService rvs =
            clusterStackVersionService.getRepositoryVersionService(EasyMock.createMock(javax.ws.rs.core.Request.class), "1");
    TestCase.assertNotNull(rvs);
  }

  private class TestClusterStackVersionService extends ClusterStackVersionService {
    public TestClusterStackVersionService(String clusterName) {
      super(clusterName);
    }

    @Override
    protected ResourceInstance createResource(Type type, Map<Type, String> mapIds) {
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
