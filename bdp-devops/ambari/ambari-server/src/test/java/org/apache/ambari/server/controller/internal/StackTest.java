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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackLevelConfigurationRequest;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Configuration;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Stack unit tests.
 */
public class StackTest {

  @Test
  public void testTestXmlExtensionStrippedOff() throws Exception {
    AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
    StackServiceResponse stackServiceResponse = createNiceMock(StackServiceResponse.class);
    Capture<Set<StackServiceComponentRequest>> stackComponentRequestCapture = EasyMock.newCapture();
    StackServiceComponentResponse stackComponentResponse = createNiceMock(StackServiceComponentResponse.class);
    Capture<Set<StackConfigurationRequest>> stackConfigurationRequestCapture = EasyMock.newCapture();
    Capture<Set<StackLevelConfigurationRequest>> stackLevelConfigurationRequestCapture = EasyMock.newCapture();
    StackConfigurationResponse stackConfigurationResponse = EasyMock.createNiceMock(StackConfigurationResponse.class);

    expect(controller.getStackServices(capture(stackServiceRequestCapture))).
        andReturn(Collections.singleton(stackServiceResponse)).anyTimes();

    expect(controller.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();

    expect(stackServiceResponse.getServiceName()).andReturn("service1").anyTimes();
    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.emptySet());
    expect(stackServiceResponse.getConfigTypes()).andReturn(Collections.emptyMap());

    expect(controller.getStackComponents(capture(stackComponentRequestCapture))).
        andReturn(Collections.singleton(stackComponentResponse)).anyTimes();

    expect(stackComponentResponse.getComponentName()).andReturn("component1").anyTimes();
    expect(stackComponentResponse.getComponentCategory()).andReturn("test-site.xml").anyTimes();

    expect(controller.getStackConfigurations(capture(stackConfigurationRequestCapture))).
        andReturn(Collections.singleton(stackConfigurationResponse)).anyTimes();

    // no stack level configs for this test
    expect(controller.getStackLevelConfigurations(capture(stackLevelConfigurationRequestCapture))).
        andReturn(Collections.emptySet()).anyTimes();

    expect(stackConfigurationResponse.getPropertyName()).andReturn("prop1").anyTimes();
    expect(stackConfigurationResponse.getPropertyValue()).andReturn("prop1Val").anyTimes();
    expect(stackConfigurationResponse.getType()).andReturn("test-site.xml").anyTimes();
    expect(stackConfigurationResponse.getPropertyType()).andReturn(
        Collections.emptySet()).anyTimes();
    expect(stackConfigurationResponse.getPropertyAttributes()).andReturn(Collections.emptyMap()).anyTimes();
    expect(stackConfigurationResponse.isRequired()).andReturn(true).anyTimes();

    expect(metaInfo.getComponentDependencies("test", "1.0", "service1", "component1")).
        andReturn(Collections.emptyList()).anyTimes();


    replay(controller, stackServiceResponse, stackComponentResponse, stackConfigurationResponse, metaInfo);


    Stack stack = new Stack("test", "1.0", controller);
    Configuration configuration = stack.getConfiguration(Collections.singleton("service1"));
    assertEquals("prop1Val", configuration.getProperties().get("test-site").get("prop1"));

    assertEquals("test-site", stack.getRequiredConfigurationProperties("service1").iterator().next().getType());

    // assertions
    StackServiceRequest stackServiceRequest = stackServiceRequestCapture.getValue().iterator().next();
    assertEquals("test", stackServiceRequest.getStackName());
    assertEquals("1.0", stackServiceRequest.getStackVersion());

    StackServiceComponentRequest stackComponentRequest = stackComponentRequestCapture.getValue().iterator().next();
    assertEquals("service1", stackComponentRequest.getServiceName());
    assertEquals("test", stackComponentRequest.getStackName());
    assertEquals("1.0", stackComponentRequest.getStackVersion());
    assertNull(stackComponentRequest.getComponentName());
  }

  @Test
  public void testConfigPropertyReadsInDependencies() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    Set<PropertyDependencyInfo> setOfDependencyInfo = new HashSet<>();

    StackConfigurationResponse mockResponse = mockSupport.createMock(StackConfigurationResponse.class);
    expect(mockResponse.getPropertyName()).andReturn("test-property-one");
    expect(mockResponse.getPropertyValue()).andReturn("test-value-one");
    expect(mockResponse.getPropertyAttributes()).andReturn(Collections.emptyMap());
    expect(mockResponse.getPropertyType()).andReturn(Collections.emptySet());
    expect(mockResponse.getType()).andReturn("test-type-one");
    expect(mockResponse.getDependsOnProperties()).andReturn(setOfDependencyInfo);
    expect(mockResponse.getPropertyValueAttributes()).andReturn(new ValueAttributesInfo());

    mockSupport.replayAll();

    Stack.ConfigProperty configProperty =
      new Stack.ConfigProperty(mockResponse);

    assertSame("DependencyInfo was not properly parsed from the stack response object",
          setOfDependencyInfo, configProperty.getDependsOnProperties());


    mockSupport.verifyAll();

  }

  @Test
  public void testGetRequiredProperties_serviceAndPropertyType() throws Exception {
    AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
    StackServiceResponse stackServiceResponse = createNiceMock(StackServiceResponse.class);
    Capture<Set<StackServiceComponentRequest>> stackComponentRequestCapture = EasyMock.newCapture();
    StackServiceComponentResponse stackComponentResponse = createNiceMock(StackServiceComponentResponse.class);
    Capture<Set<StackConfigurationRequest>> stackConfigurationRequestCapture = EasyMock.newCapture();
    Capture<Set<StackLevelConfigurationRequest>> stackLevelConfigurationRequestCapture = EasyMock.newCapture();
    StackConfigurationResponse stackConfigurationResponse = EasyMock.createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse2 = EasyMock.createNiceMock(StackConfigurationResponse.class);

    expect(controller.getStackServices(capture(stackServiceRequestCapture))).
        andReturn(Collections.singleton(stackServiceResponse)).anyTimes();

    expect(controller.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();

    expect(stackServiceResponse.getServiceName()).andReturn("service1").anyTimes();
    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.emptySet());
    expect(stackServiceResponse.getConfigTypes()).andReturn(Collections.emptyMap());

    expect(controller.getStackComponents(capture(stackComponentRequestCapture))).
        andReturn(Collections.singleton(stackComponentResponse)).anyTimes();

    expect(stackComponentResponse.getComponentName()).andReturn("component1").anyTimes();
    expect(stackComponentResponse.getComponentCategory()).andReturn("test-site.xml").anyTimes();

    expect(controller.getStackConfigurations(capture(stackConfigurationRequestCapture))).
        andReturn(new HashSet<>(Arrays.asList(
          stackConfigurationResponse, stackConfigurationResponse2))).anyTimes();

    // no stack level configs for this test
    expect(controller.getStackLevelConfigurations(capture(stackLevelConfigurationRequestCapture))).
        andReturn(Collections.emptySet()).anyTimes();

    expect(stackConfigurationResponse.getPropertyName()).andReturn("prop1").anyTimes();
    expect(stackConfigurationResponse.getPropertyValue()).andReturn(null).anyTimes();
    expect(stackConfigurationResponse.getType()).andReturn("test-site.xml").anyTimes();
    expect(stackConfigurationResponse.getPropertyType()).andReturn(
        Collections.singleton(PropertyInfo.PropertyType.PASSWORD)).anyTimes();
    expect(stackConfigurationResponse.getPropertyAttributes()).andReturn(Collections.emptyMap()).anyTimes();
    expect(stackConfigurationResponse.isRequired()).andReturn(true).anyTimes();

    // not a PASSWORD property type so shouldn't be returned
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("prop2").anyTimes();
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn(null).anyTimes();
    expect(stackConfigurationResponse2.getType()).andReturn("test-site.xml").anyTimes();
    expect(stackConfigurationResponse2.getPropertyType()).andReturn(
        Collections.singleton(PropertyInfo.PropertyType.USER)).anyTimes();
    expect(stackConfigurationResponse2.getPropertyAttributes()).andReturn(Collections.emptyMap()).anyTimes();
    expect(stackConfigurationResponse2.isRequired()).andReturn(true).anyTimes();

    expect(metaInfo.getComponentDependencies("test", "1.0", "service1", "component1")).
        andReturn(Collections.emptyList()).anyTimes();

    replay(controller, stackServiceResponse, stackComponentResponse, stackConfigurationResponse,
        stackConfigurationResponse2, metaInfo);

    // test
    Stack stack = new Stack("test", "1.0", controller);
    // get required password properties
    Collection<Stack.ConfigProperty> requiredPasswordProperties = stack.getRequiredConfigurationProperties(
        "service1", PropertyInfo.PropertyType.PASSWORD);

    // assertions
    assertEquals(1, requiredPasswordProperties.size());
    Stack.ConfigProperty requiredPasswordConfigProperty = requiredPasswordProperties.iterator().next();
    assertEquals("test-site", requiredPasswordConfigProperty.getType());
    assertEquals("prop1", requiredPasswordConfigProperty.getName());
    assertTrue(requiredPasswordConfigProperty.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD));

    StackServiceRequest stackServiceRequest = stackServiceRequestCapture.getValue().iterator().next();
    assertEquals("test", stackServiceRequest.getStackName());
    assertEquals("1.0", stackServiceRequest.getStackVersion());

    StackServiceComponentRequest stackComponentRequest = stackComponentRequestCapture.getValue().iterator().next();
    assertEquals("service1", stackComponentRequest.getServiceName());
    assertEquals("test", stackComponentRequest.getStackName());
    assertEquals("1.0", stackComponentRequest.getStackVersion());
    assertNull(stackComponentRequest.getComponentName());
  }

  // Test that getAllConfigurationTypes returns beside the configuration types that have
  // service config properties defined also the empty ones that doesn't have any config
  // property defined.
  @Test
  public void testGetAllConfigurationTypesWithEmptyStackServiceConfigType() throws Exception {
    // Given
    AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    StackServiceResponse stackServiceResponse = createNiceMock(StackServiceResponse.class);
    StackServiceComponentResponse stackComponentResponse = createNiceMock(StackServiceComponentResponse.class);
    StackConfigurationResponse stackConfigurationResponse1 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse2 = createNiceMock(StackConfigurationResponse.class);

    String testServiceName = "service1";
    String testEmptyConfigType = "test-empty-config-type";
    String testSiteConfigFile = "test-site.xml";
    String testSiteConfigType = "test-site";


    expect(controller.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();

    expect(controller.getStackServices(EasyMock.anyObject())).andReturn(Collections.singleton(stackServiceResponse)).anyTimes();
    expect(stackServiceResponse.getServiceName()).andReturn(testServiceName).anyTimes();
    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.emptySet());

    // stack components
    expect(stackComponentResponse.getComponentName()).andReturn("component1").anyTimes();
    expect(stackComponentResponse.getComponentCategory()).andReturn(testSiteConfigFile).anyTimes();
    expect(controller.getStackComponents(EasyMock.anyObject())).andReturn(Collections.singleton(stackComponentResponse)).anyTimes();

    // stack configurations

    // two properties with config type 'test-site'
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("prop1").anyTimes();
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn(null).anyTimes();
    expect(stackConfigurationResponse1.getType()).andReturn(testSiteConfigFile).anyTimes();
    expect(stackConfigurationResponse1.getPropertyType()).andReturn(Collections.singleton(PropertyInfo.PropertyType.TEXT)).anyTimes();
    expect(stackConfigurationResponse1.getPropertyAttributes()).andReturn(Collections.emptyMap()).anyTimes();
    expect(stackConfigurationResponse1.isRequired()).andReturn(true).anyTimes();

    expect(stackConfigurationResponse2.getPropertyName()).andReturn("prop2").anyTimes();
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn(null).anyTimes();
    expect(stackConfigurationResponse2.getType()).andReturn(testSiteConfigFile).anyTimes();
    expect(stackConfigurationResponse2.getPropertyType()).andReturn(Collections.singleton(PropertyInfo.PropertyType.USER)).anyTimes();
    expect(stackConfigurationResponse2.getPropertyAttributes()).andReturn(Collections.emptyMap()).anyTimes();
    expect(stackConfigurationResponse2.isRequired()).andReturn(true).anyTimes();

    expect(controller.getStackConfigurations(EasyMock.anyObject())).andReturn(Sets.newHashSet(stackConfigurationResponse1, stackConfigurationResponse2)).anyTimes();

    // empty stack service config type
    expect(stackServiceResponse.getConfigTypes()).andReturn(Collections.singletonMap(testEmptyConfigType, Collections.emptyMap()));

    // no stack level configs for this test
    expect(controller.getStackLevelConfigurations(EasyMock.anyObject())).andReturn(Collections.emptySet()).anyTimes();

    expect(metaInfo.getComponentDependencies("test", "1.0", "service1", "component1")).andReturn(Collections.emptyList()).anyTimes();

    replay(controller, stackServiceResponse, stackComponentResponse, stackConfigurationResponse1, stackConfigurationResponse2, metaInfo);


    Stack stack = new Stack("test", "1.0", controller);

    // When
    Collection<String> allServiceConfigTypes = stack.getAllConfigurationTypes(testServiceName);

    // Then

    assertTrue(allServiceConfigTypes.containsAll(ImmutableSet.of(testSiteConfigType, testEmptyConfigType)));
    assertEquals(2, allServiceConfigTypes.size());

    verifyAll();
  }

  // Test that getServiceForConfigType skips excluded config types.
  @Test
  public void testGetServiceForConfigTypeWithExcludedConfigs() throws Exception {
    // Given
    AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    StackServiceResponse stackServiceResponse = createNiceMock(StackServiceResponse.class);
    StackServiceComponentResponse stackComponentResponse = createNiceMock(StackServiceComponentResponse.class);
    StackConfigurationResponse stackConfigurationResponse1 = createNiceMock(StackConfigurationResponse.class);

    String testServiceName = "service1";
    String testEmptyConfigType = "test-empty-config-type";
    String testSiteConfigFile = "test-site.xml";
    String testSiteConfigType = "test-site";

    expect(controller.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();

    expect(controller.getStackServices(EasyMock.anyObject())).andReturn(Collections.singleton(stackServiceResponse)).anyTimes();
    expect(stackServiceResponse.getServiceName()).andReturn(testServiceName).anyTimes();

    // Config type test-site is excluded for the service service1
    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.singleton(testSiteConfigType));

    // stack components
    expect(stackComponentResponse.getComponentName()).andReturn("component1").anyTimes();
    expect(stackComponentResponse.getComponentCategory()).andReturn(testSiteConfigFile).anyTimes();
    expect(controller.getStackComponents(EasyMock.anyObject())).andReturn(Collections.singleton(stackComponentResponse)).anyTimes();

    expect(stackConfigurationResponse1.getPropertyName()).andReturn("prop1").anyTimes();
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn(null).anyTimes();
    expect(stackConfigurationResponse1.getType()).andReturn(testSiteConfigFile).anyTimes();
    expect(stackConfigurationResponse1.getPropertyType()).andReturn(Collections.singleton(PropertyInfo.PropertyType.TEXT)).anyTimes();
    expect(stackConfigurationResponse1.getPropertyAttributes()).andReturn(Collections.emptyMap()).anyTimes();
    expect(stackConfigurationResponse1.isRequired()).andReturn(true).anyTimes();

    expect(controller.getStackConfigurations(EasyMock.anyObject())).andReturn(Collections.singleton(stackConfigurationResponse1)).anyTimes();

    // empty stack service config type
    expect(stackServiceResponse.getConfigTypes()).andReturn(Collections.singletonMap(testEmptyConfigType, Collections.emptyMap()));

    // no stack level configs for this test
    expect(controller.getStackLevelConfigurations(EasyMock.anyObject())).andReturn(Collections.emptySet()).anyTimes();
    expect(metaInfo.getComponentDependencies("test", "1.0", "service1", "component1")).andReturn(Collections.emptyList()).anyTimes();

    replay(controller, stackServiceResponse, stackComponentResponse, stackConfigurationResponse1, metaInfo);

    Stack stack = new Stack("test", "1.0", controller);

    // When
    try {
      stack.getServiceForConfigType(testSiteConfigType);
      fail("Exception not thrown");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    // Not excluded config type
    assertEquals(testServiceName, stack.getServiceForConfigType(testEmptyConfigType));

    verifyAll();
  }

}
