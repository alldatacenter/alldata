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
 * Unit tests for CredentialService.
 */
public class CredentialServiceTest extends BaseServiceTest {


  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getCredential
    CredentialService CredentialService = new TestCredentialService("alias");
    Method m = CredentialService.getClass().getMethod("getCredential", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[]{getHttpHeaders(), getUriInfo(), "alias"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, CredentialService, m, args, null));

    //getCredentials
    CredentialService = new TestCredentialService(null);
    m = CredentialService.getClass().getMethod("getCredentials", HttpHeaders.class, UriInfo.class);
    args = new Object[]{getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, CredentialService, m, args, null));

    //createCredential
    CredentialService = new TestCredentialService("alias");
    m = CredentialService.getClass().getMethod("createCredential", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[]{"body", getHttpHeaders(), getUriInfo(), "alias"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, CredentialService, m, args, "body"));

    //deleteCredential
    CredentialService = new TestCredentialService("alias");
    m = CredentialService.getClass().getMethod("deleteCredential", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[]{getHttpHeaders(), getUriInfo(), "alias"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, CredentialService, m, args, null));

    return listInvocations;
  }


  private class TestCredentialService extends CredentialService {
    private String alias;

    private TestCredentialService(String alias) {
      super("C1");
      this.alias = alias;
    }

    @Override
    ResourceInstance createCredentialResource(String alias) {
      assertEquals(this.alias, alias);
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