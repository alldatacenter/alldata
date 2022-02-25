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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

public class RootServiceComponentPropertyProviderTest {
  @Test
  public void testPopulateResources_AmbariServer_None() throws Exception {
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), false, false, false, false);
  }

  @Test
  public void testPopulateResources_AmbariServer_CiphersAndJCEPolicy() throws Exception {
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), true, true, true, true);
  }

  @Test
  public void testPopulateResources_AmbariServer_JCEPolicy() throws Exception {
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), false, true, false, true);
  }

  @Test
  public void testPopulateResources_AmbariServer_Ciphers() throws Exception {
    testPopulateResources(RootComponent.AMBARI_SERVER.name(), true, false, true, false);
  }

  @Test
  public void testPopulateResources_AmbariAgent_CiphersAndJCEPolicy() throws Exception {
    testPopulateResources(RootComponent.AMBARI_AGENT.name(), true, true, false, false);
  }

  public void testPopulateResources(String componentName,
                                    boolean requestCiphers, boolean requestJCEPolicy,
                                    boolean expectCiphers, boolean expectJCEPolicy) throws Exception {
    RootServiceComponentPropertyProvider propertyProvider = new RootServiceComponentPropertyProvider();
    Resource resource = new ResourceImpl(Resource.Type.RootService);

    resource.setProperty(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID, componentName);
    resource.setProperty(RootServiceComponentResourceProvider.SERVICE_NAME_PROPERTY_ID, RootService.AMBARI.name());

    HashSet<String> requestIds = new HashSet<>();

    if (requestCiphers) {
      requestIds.add(RootServiceComponentPropertyProvider.CIPHER_PROPERTIES_PROPERTY_ID);
    }

    if (requestJCEPolicy) {
      requestIds.add(RootServiceComponentPropertyProvider.JCE_POLICY_PROPERTY_ID);
    }

    Request request = PropertyHelper.getReadRequest(requestIds, new HashMap<>());

    Set<Resource> resources = propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());

    resource = resources.iterator().next();

    Map<String, Map<String, Object>> values = resource.getPropertiesMap();

    Assert.assertEquals(expectCiphers, values.containsKey(RootServiceComponentPropertyProvider.CIPHER_PROPERTIES_PROPERTY_ID));
    Assert.assertEquals(expectJCEPolicy, values.containsKey(RootServiceComponentPropertyProvider.JCE_POLICY_PROPERTY_ID));
  }
}