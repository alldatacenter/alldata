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
import org.apache.ambari.server.controller.StackConfigurationDependencyResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

public class StackConfigurationDependencyResourceProviderTest {

  @Test
  public void testGetResources() throws Exception{


    Resource.Type type = Resource.Type.StackConfigurationDependency;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<StackConfigurationDependencyResponse> allResponse = new HashSet<>();
    
    allResponse.add(new StackConfigurationDependencyResponse("depName", "depType"));
   
    // set expectations
    expect(managementController.getStackConfigurationDependencies(
        AbstractResourceProviderTest.Matcher.getStackConfigurationDependencyRequestSet(null, null, null, null, null))).
        andReturn(allResponse).times(1);
    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(StackConfigurationDependencyResourceProvider.STACK_NAME_PROPERTY_ID);
    propertyIds.add(StackConfigurationDependencyResourceProvider.STACK_VERSION_PROPERTY_ID);
    propertyIds.add(StackConfigurationDependencyResourceProvider.SERVICE_NAME_PROPERTY_ID);
    propertyIds.add(StackConfigurationDependencyResourceProvider.PROPERTY_NAME_PROPERTY_ID);
    propertyIds.add(StackConfigurationDependencyResourceProvider.DEPENDENCY_NAME_PROPERTY_ID);
    propertyIds.add(StackConfigurationDependencyResourceProvider.DEPENDENCY_TYPE_PROPERTY_ID);


    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    
    for (Resource resource : resources) {   
      String dependencyName = (String) resource.getPropertyValue(StackConfigurationDependencyResourceProvider.DEPENDENCY_NAME_PROPERTY_ID);
      String dependencyType = (String) resource.getPropertyValue(StackConfigurationDependencyResourceProvider.DEPENDENCY_TYPE_PROPERTY_ID);

      Assert.assertEquals("depName", dependencyName);
      Assert.assertEquals("depType", dependencyType);

    }

    // verify
    verify(managementController);
  }

}
