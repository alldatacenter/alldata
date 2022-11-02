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
* Unit tests for ExtensionsService.
*/
public class ExtensionsServiceTest extends BaseServiceTest {

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    // getExtension
    ExtensionsService service = new TestExtensionsService("extensionName", null);
    Method m = service.getClass().getMethod("getExtension", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "extensionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getExtensions
    service = new TestExtensionsService(null, null);
    m = service.getClass().getMethod("getExtensions", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // getExtensionVersion
    service = new TestExtensionsService("extensionName", "extensionVersion");
    m = service.getClass().getMethod("getExtensionVersion", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "extensionName", "extensionVersion"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // getExtensionVersions
    service = new TestExtensionsService("extensionName", null);
    m = service.getClass().getMethod("getExtensionVersions", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "extensionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    return listInvocations;
  }

  private class TestExtensionsService extends ExtensionsService {

    private String m_extensionId;
    private String m_extensionVersion;

    private TestExtensionsService(String extensionName, String extensionVersion) {
      m_extensionId = extensionName;
      m_extensionVersion = extensionVersion;
    }

    @Override
    ResourceInstance createExtensionResource(String extensionName) {
      assertEquals(m_extensionId, extensionName);
      return getTestResource();
    }

    @Override
    ResourceInstance createExtensionVersionResource(String extensionName, String extensionVersion) {
      assertEquals(m_extensionId, extensionName);
      assertEquals(m_extensionVersion, extensionVersion);
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
