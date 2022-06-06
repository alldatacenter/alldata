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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.RootServiceComponentResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RootServiceComponentResourceProviderTest {
  
  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.RootServiceComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<RootServiceComponentResponse> allResponse = new HashSet<>();
    String serviceName = RootService.AMBARI.name();
    Map<String, String> emptyMap = Collections.emptyMap();
    allResponse.add(new RootServiceComponentResponse(serviceName, "component1", "1.1.1", emptyMap));
    allResponse.add(new RootServiceComponentResponse(serviceName, "component2", "1.1.1", emptyMap));
    allResponse.add(new RootServiceComponentResponse(serviceName, "component3", "1.1.1", emptyMap));
    allResponse.add(new RootServiceComponentResponse(serviceName, RootComponent.AMBARI_SERVER.name(), "1.1.1", emptyMap));

    Set<RootServiceComponentResponse> nameResponse = new HashSet<>();
    nameResponse.add(new RootServiceComponentResponse(serviceName, "component4", "1.1.1", emptyMap));


    // set expectations
    expect(managementController.getRootServiceComponents(EasyMock.anyObject())).andReturn(allResponse).once();
    expect(managementController.getRootServiceComponents(EasyMock.anyObject())).andReturn(nameResponse).once();
    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RootServiceComponentResourceProvider.SERVICE_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceComponentResourceProvider.PROPERTIES_PROPERTY_ID);
    propertyIds.add(RootServiceComponentResourceProvider.COMPONENT_VERSION_PROPERTY_ID);
    propertyIds.add(RootServiceComponentResourceProvider.SERVER_CLOCK_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    for (Resource resource : resources) {
      String componentName = (String) resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID);
      String componentVersion = (String) resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_VERSION_PROPERTY_ID);
      Long server_clock = (Long) resource.getPropertyValue(RootServiceComponentResourceProvider.SERVER_CLOCK_PROPERTY_ID);
      if (componentName.equals(RootComponent.AMBARI_SERVER.name())){
        Assert.assertNotNull(server_clock);
      } else {
        Assert.assertNull(server_clock);
      }
      
      Assert.assertTrue(allResponse.contains(new RootServiceComponentResponse(serviceName, componentName, componentVersion, emptyMap)));
    }

    // get service named service4
    Predicate predicate =
        new PredicateBuilder().property(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID).
        equals("component4").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("component4", resources.iterator().next().
        getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID));


    // verify
    verify(managementController);
  }

}
