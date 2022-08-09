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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * LdapSyncEventResourceProvider tests.
 */
public class LdapSyncEventResourceProviderTest {

  @BeforeClass
  public static void setupAuthentication() {
    // Set authenticated user so that authorization checks will pass
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    LdapSyncEventResourceProvider provider = new TestLdapSyncEventResourceProvider(amc);

    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();

    Set<Map<String, String>> specs = new HashSet<>();

    propertyMap.put(LdapSyncEventResourceProvider.EVENT_SPECS_PROPERTY_ID, specs);
    properties.add(propertyMap);

    provider.createResources(PropertyHelper.getCreateRequest(properties, null));

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(1, resources.size());
  }

  @Test
  public void testGetResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    LdapSyncEventResourceProvider provider = new TestLdapSyncEventResourceProvider(amc);

    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();

    Set<Map<String, String>> specs = new HashSet<>();

    propertyMap.put(LdapSyncEventResourceProvider.EVENT_SPECS_PROPERTY_ID, specs);
    properties.add(propertyMap);

    provider.createResources(PropertyHelper.getCreateRequest(properties, null));

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(1, resources.size());
  }

  @Test
  public void testUpdateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    LdapSyncEventResourceProvider provider = new TestLdapSyncEventResourceProvider(amc);

    Request request = createNiceMock(Request.class);

    try {
      provider.updateResources(request, null);
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  public void testDeleteResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);

    LdapSyncEventResourceProvider provider = new TestLdapSyncEventResourceProvider(amc);

    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();

    Set<Map<String, String>> specs = new HashSet<>();

    propertyMap.put(LdapSyncEventResourceProvider.EVENT_SPECS_PROPERTY_ID, specs);
    properties.add(propertyMap);

    provider.createResources(PropertyHelper.getCreateRequest(properties, null));

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(1, resources.size());

    provider.deleteResources(new RequestImpl(null, null, null, null), null);

    resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    Assert.assertEquals(0, resources.size());
  }


  protected static class TestLdapSyncEventResourceProvider extends LdapSyncEventResourceProvider {
    /**
     * Construct a event resource provider.
     */
    public TestLdapSyncEventResourceProvider(AmbariManagementController managementController) {
      super(managementController);
    }

    @Override
    protected void ensureEventProcessor() {
    }
  }
}
