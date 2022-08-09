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
import org.apache.ambari.server.controller.StackResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * StackResourceProvider Test
 */
public class StackResourceProviderTest {
  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Stack;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<StackResponse> allResponse = new HashSet<>();
    allResponse.add(new StackResponse("Stack1"));
    allResponse.add(new StackResponse("Stack2"));

    // set expectations
    expect(managementController.getStacks(EasyMock.anyObject())).andReturn(allResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(StackResourceProvider.STACK_NAME_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(2, resources.size());


    Set<String> stackNames = new HashSet<>();
    stackNames.add("Stack1");
    stackNames.add("Stack2");

    for (Resource resource : resources) {
      String stackName = (String) resource.getPropertyValue(StackResourceProvider.STACK_NAME_PROPERTY_ID);
      Assert.assertTrue(stackNames.contains(stackName));
    }

    // verify
    verify(managementController);
  }
}
