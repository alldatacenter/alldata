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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RootServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RootServiceResourceProviderTest {
  
  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.RootService;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<RootServiceResponse> allResponse = new HashSet<>();
    allResponse.add(new RootServiceResponse("service1"));
    allResponse.add(new RootServiceResponse("service2"));
    allResponse.add(new RootServiceResponse("service3"));

    Set<RootServiceResponse> nameResponse = new HashSet<>();
    nameResponse.add(new RootServiceResponse("service4"));


    // set expectations
    expect(managementController.getRootServices(EasyMock.anyObject())).andReturn(allResponse).once();
    expect(managementController.getRootServices(EasyMock.anyObject())).andReturn(nameResponse).once();
    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RootServiceResourceProvider.SERVICE_NAME_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    for (Resource resource : resources) {
      String serviceName = (String) resource.getPropertyValue(RootServiceResourceProvider.SERVICE_NAME_PROPERTY_ID);
      Assert.assertTrue(allResponse.contains(new RootServiceResponse(serviceName)));
    }

    // get service named service4
    Predicate predicate =
        new PredicateBuilder().property(RootServiceResourceProvider.SERVICE_NAME_PROPERTY_ID).
        equals("service4").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("service4", resources.iterator().next().
        getPropertyValue(RootServiceResourceProvider.SERVICE_NAME_PROPERTY_ID));


    // verify
    verify(managementController);
  }

}
