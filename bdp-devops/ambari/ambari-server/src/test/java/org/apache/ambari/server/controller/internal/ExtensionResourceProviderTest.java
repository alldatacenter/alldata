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
import org.apache.ambari.server.controller.ExtensionResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * ExtensionResourceProvider Test
 */
public class ExtensionResourceProviderTest {
  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Extension;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ExtensionResponse> allResponse = new HashSet<>();
    allResponse.add(new ExtensionResponse("Extension1"));
    allResponse.add(new ExtensionResponse("Extension2"));

    // set expectations
    expect(managementController.getExtensions(EasyMock.anyObject())).andReturn(allResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ExtensionResourceProvider.EXTENSION_NAME_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(2, resources.size());


    Set<String> extensionNames = new HashSet<>();
    extensionNames.add("Extension1");
    extensionNames.add("Extension2");

    for (Resource resource : resources) {
      String extensionName = (String) resource.getPropertyValue(ExtensionResourceProvider.EXTENSION_NAME_PROPERTY_ID);
      Assert.assertTrue(extensionNames.contains(extensionName));
    }

    // verify
    verify(managementController);
  }
}
