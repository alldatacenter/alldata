/**
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
 * Unit tests for MpacksService
 */
public class MpacksServiceTest extends BaseServiceTest{
  @Override
  public List<BaseServiceTest.ServiceTestInvocation> getTestInvocations() throws Exception {
    List<BaseServiceTest.ServiceTestInvocation> listInvocations = new ArrayList<>();

    // getMpacks
    MpacksService service = new TestMpacksService("null");
    Method m = service.getClass().getMethod("getMpacks", String.class, HttpHeaders.class, UriInfo.class);
    Object[] args = new Object[]{null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // getMpack
    service = new TestMpacksService("1");
    m = service.getClass().getMethod("getMpack", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[]{null, getHttpHeaders(), getUriInfo(), ""};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createMpacks
    service = new TestMpacksService(null);
    m = service.getClass().getMethod("createMpacks", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[]{"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    return listInvocations;
  }
  private class TestMpacksService extends MpacksService {

    private String m_mpackId;

    private TestMpacksService(String mpackId) {
      super();
      m_mpackId = mpackId;
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
