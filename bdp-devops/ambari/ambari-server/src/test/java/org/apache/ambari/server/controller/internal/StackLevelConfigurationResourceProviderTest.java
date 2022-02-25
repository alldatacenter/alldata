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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

public class StackLevelConfigurationResourceProviderTest {
  
  private static final String PROPERTY_NAME = "name";
  private static final String PROPERTY_VALUE = "value";
  private static final String PROPERTY_DESC = "Desc";
  private static final String TYPE = "type.xml";

  @Test
  public void testGetResources() throws Exception{
       
    Map<String, String> attributes = new HashMap<>();
    attributes.put("final", "true");

    Resource.Type type = Resource.Type.StackLevelConfiguration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<StackConfigurationResponse> allResponse = new HashSet<>();
    
    allResponse.add(new StackConfigurationResponse(PROPERTY_NAME, PROPERTY_VALUE, PROPERTY_DESC, TYPE, attributes));
   
    // set expectations
    expect(managementController.getStackLevelConfigurations(
        AbstractResourceProviderTest.Matcher.getStackLevelConfigurationRequestSet(null, null, null))).
        andReturn(allResponse).times(1);
    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(StackLevelConfigurationResourceProvider.STACK_NAME_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.STACK_VERSION_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_NAME_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_VALUE_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_DESCRIPTION_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_TYPE_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_FINAL_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    
    for (Resource resource : resources) {   
      String propertyName = (String) resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_NAME_PROPERTY_ID);
      String propertyValue = (String) resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_VALUE_PROPERTY_ID);
      String propertyDesc = (String) 
          resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_DESCRIPTION_PROPERTY_ID);
      String propertyType = (String) 
          resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_TYPE_PROPERTY_ID);
      String propertyIsFinal = (String)
          resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_FINAL_PROPERTY_ID);
      
      Assert.assertEquals(PROPERTY_NAME, propertyName);
      Assert.assertEquals(PROPERTY_VALUE, propertyValue);
      Assert.assertEquals(PROPERTY_DESC, propertyDesc);
      Assert.assertEquals(TYPE, propertyType);
      Assert.assertEquals("true", propertyIsFinal);

    }

    // verify
    verify(managementController);
  }

  @Test
  public void testGetResources_noFinal() throws Exception{

    Map<String, String> attributes = new HashMap<>();

    Resource.Type type = Resource.Type.StackLevelConfiguration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<StackConfigurationResponse> allResponse = new HashSet<>();

    allResponse.add(new StackConfigurationResponse(PROPERTY_NAME, PROPERTY_VALUE, PROPERTY_DESC, TYPE, attributes));

    // set expectations
    expect(managementController.getStackLevelConfigurations(
        AbstractResourceProviderTest.Matcher.getStackLevelConfigurationRequestSet(null, null, null))).
        andReturn(allResponse).times(1);
    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(StackLevelConfigurationResourceProvider.STACK_NAME_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.STACK_VERSION_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_NAME_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_VALUE_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_DESCRIPTION_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_TYPE_PROPERTY_ID);
    propertyIds.add(StackLevelConfigurationResourceProvider.PROPERTY_FINAL_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());

    for (Resource resource : resources) {
      String propertyName = (String) resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_NAME_PROPERTY_ID);
      String propertyValue = (String) resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_VALUE_PROPERTY_ID);
      String propertyDesc = (String)
          resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_DESCRIPTION_PROPERTY_ID);
      String propertyType = (String)
          resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_TYPE_PROPERTY_ID);
      String propertyIsFinal = (String)
          resource.getPropertyValue(StackLevelConfigurationResourceProvider.PROPERTY_FINAL_PROPERTY_ID);

      Assert.assertEquals(PROPERTY_NAME, propertyName);
      Assert.assertEquals(PROPERTY_VALUE, propertyValue);
      Assert.assertEquals(PROPERTY_DESC, propertyDesc);
      Assert.assertEquals(TYPE, propertyType);
      Assert.assertEquals("false", propertyIsFinal);

    }

    // verify
    verify(managementController);
  } 

}
