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
 * Unit tests for BlueprintService.
 */
public class BlueprintServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getBlueprint
    BlueprintService BlueprintService = new TestBlueprintService("blueprintName");
    Method m = BlueprintService.getClass().getMethod("getBlueprint", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "blueprintName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, BlueprintService, m, args, null));

    //getBlueprints
    BlueprintService = new TestBlueprintService(null);
    m = BlueprintService.getClass().getMethod("getBlueprints", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, BlueprintService, m, args, null));

    //createBlueprint
    BlueprintService = new TestBlueprintService("blueprintName");
    m = BlueprintService.getClass().getMethod("createBlueprint", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "blueprintName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, BlueprintService, m, args, "body"));

    //deleteBlueprint
    BlueprintService = new TestBlueprintService("blueprintName");
    m = BlueprintService.getClass().getMethod("deleteBlueprint", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "blueprintName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, BlueprintService, m, args, null));

    //deleteBlueprints
    BlueprintService = new TestBlueprintService(null);
    m = BlueprintService.getClass().getMethod("deleteBlueprints", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, BlueprintService, m, args, null));

    return listInvocations;
  }


  private class TestBlueprintService extends BlueprintService {
    private String m_blueprintId;

    private TestBlueprintService(String blueprintId) {
      m_blueprintId = blueprintId;
    }

    @Override
    ResourceInstance createBlueprintResource(String blueprintName) {
      assertEquals(m_blueprintId, blueprintName);
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
