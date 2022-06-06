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

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewParameterEntity;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.junit.Assert;
import org.junit.Test;

/**
 * ViewContextImpl tests.
 */
public class ViewContextImplTest {
  @Test
  public void testGetViewName() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals("MY_VIEW", viewContext.getViewName());
  }

  @Test
  public void testGetInstanceName() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals("INSTANCE1", viewContext.getInstanceName());
  }

  @Test
  public void testGetProperties() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    expect(viewRegistry.getCluster(viewInstanceDefinition)).andReturn(null).anyTimes();

    viewInstanceDefinition.putProperty("p1", "v1");
    viewInstanceDefinition.putProperty("p2", new DefaultMasker().mask("v2"));
    viewInstanceDefinition.putProperty("p3", "v3");

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Map<String, String> properties = viewContext.getProperties();
    Assert.assertEquals(3, properties.size());

    Assert.assertEquals("v1", properties.get("p1"));
    Assert.assertEquals("v2", properties.get("p2"));
    Assert.assertEquals("v3", properties.get("p3"));
  }

  @Test
  public void testGetPropertiesWithParameters() throws Exception {
    InstanceConfig instanceConfig = createNiceMock(InstanceConfig.class);
    expect(instanceConfig.getName()).andReturn("Instance").anyTimes();
    replay(instanceConfig);
    ViewEntity viewDefinition = createNiceMock(ViewEntity.class);
    expect(viewDefinition.getName()).andReturn("View").anyTimes();
    expect(viewDefinition.getCommonName()).andReturn("View").times(2);
    expect(viewDefinition.getClassLoader()).andReturn(ViewContextImplTest.class.getClassLoader()).anyTimes();
    expect(viewDefinition.getConfiguration()).andReturn(ViewConfigTest.getConfig()).anyTimes();

    ViewParameterEntity parameter1 = createNiceMock(ViewParameterEntity.class);
    expect(parameter1.getName()).andReturn("p1").anyTimes();
    ViewParameterEntity parameter2 = createNiceMock(ViewParameterEntity.class);
    expect(parameter2.getName()).andReturn("p2").anyTimes();
    expect(viewDefinition.getParameters()).andReturn(Arrays.asList(parameter1, parameter2)).anyTimes();

    replay(viewDefinition, parameter1, parameter2);
    ViewInstanceEntity viewInstanceDefinition = createMockBuilder(ViewInstanceEntity.class)
        .addMockedMethod("getUsername")
        .addMockedMethod("getName")
        .addMockedMethod("getViewEntity")
        .withConstructor(viewDefinition, instanceConfig).createMock();
    expect(viewInstanceDefinition.getUsername()).andReturn("User").times(1);
    expect(viewInstanceDefinition.getUsername()).andReturn("User2").times(1);
    expect(viewInstanceDefinition.getName()).andReturn("Instance").anyTimes();
    expect(viewInstanceDefinition.getViewEntity()).andReturn(viewDefinition).anyTimes();
    replay(viewInstanceDefinition);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    expect(viewRegistry.getCluster(viewInstanceDefinition)).andReturn(null).anyTimes();
    replay(viewRegistry);
    viewInstanceDefinition.putProperty("p1", "/tmp/some/path/${username}");
    viewInstanceDefinition.putProperty("p2", new DefaultMasker().mask("/tmp/path/$viewName"));
    viewInstanceDefinition.putProperty("p3", "/path/$instanceName");
    viewInstanceDefinition.putProperty("p4", "/path/to/${unspecified_parameter}");
    viewInstanceDefinition.putProperty("p5", "/path/to/${incorrect_parameter");
    viewInstanceDefinition.putProperty("p6", "/path/to/\\${username}");
    viewInstanceDefinition.putProperty("p7", "/path/to/\\$viewName");
    viewInstanceDefinition.putProperty("p8", null);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Map<String, String> properties = viewContext.getProperties();
    Assert.assertEquals(8, properties.size());
    Assert.assertEquals("/tmp/some/path/User", properties.get("p1"));
    Assert.assertEquals("/tmp/path/View", properties.get("p2"));
    Assert.assertEquals("/path/Instance", properties.get("p3"));
    Assert.assertEquals("/path/to/${unspecified_parameter}", properties.get("p4"));
    Assert.assertEquals("/path/to/${incorrect_parameter", properties.get("p5"));
    Assert.assertEquals("/path/to/${username}", properties.get("p6"));
    Assert.assertEquals("/path/to/$viewName", properties.get("p7"));
    Assert.assertNull(properties.get("p8"));

    properties = viewContext.getProperties();
    Assert.assertEquals(8, properties.size());
    Assert.assertEquals("/tmp/some/path/User2", properties.get("p1"));
    Assert.assertEquals("/tmp/path/View", properties.get("p2"));
    Assert.assertEquals("/path/Instance", properties.get("p3"));
    Assert.assertEquals("/path/to/${unspecified_parameter}", properties.get("p4"));
    Assert.assertEquals("/path/to/${incorrect_parameter", properties.get("p5"));
    Assert.assertEquals("/path/to/${username}", properties.get("p6"));
    Assert.assertEquals("/path/to/$viewName", properties.get("p7"));
    Assert.assertNull(properties.get("p8"));
  }

  @Test
  public void testGetResourceProvider() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW{1.0.0}/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals(provider, viewContext.getResourceProvider("myType"));
  }

  @Test
  public void testGetURLStreamProvider() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    ViewURLStreamProvider urlStreamProvider = createNiceMock(ViewURLStreamProvider.class);
    ViewURLStreamProvider urlStreamProvider2 = createNiceMock(ViewURLStreamProvider.class);

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    expect(viewRegistry.createURLStreamProvider(viewContext)).andReturn(urlStreamProvider);
    expect(viewRegistry.createURLStreamProvider(viewContext)).andReturn(urlStreamProvider2);

    replay(viewRegistry);

    Assert.assertEquals(urlStreamProvider, viewContext.getURLStreamProvider());
    // make sure the the provider is not cached
    Assert.assertEquals(urlStreamProvider2, viewContext.getURLStreamProvider());

    verify(viewRegistry);
  }

  @Test
  public void testGetURLConnectionProvider() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    ViewURLStreamProvider urlStreamProvider = createNiceMock(ViewURLStreamProvider.class);

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    ViewContext viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    expect(viewRegistry.createURLStreamProvider(viewContext)).andReturn(urlStreamProvider);

    replay(viewRegistry);

    Assert.assertEquals(urlStreamProvider, viewContext.getURLConnectionProvider());

    verify(viewRegistry);
  }

  @Test
  public void testGetAmbariStreamProvider() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    ViewAmbariStreamProvider ambariStreamProvider = createNiceMock(ViewAmbariStreamProvider.class);

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    expect(viewRegistry.createAmbariStreamProvider()).andReturn(ambariStreamProvider);

    replay(viewRegistry);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals(ambariStreamProvider, viewContext.getAmbariStreamProvider());

    verify(viewRegistry);
  }

  @Test
  public void testGetCluster() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);

    Cluster cluster = createNiceMock(Cluster.class);

    expect(viewRegistry.getCluster(viewInstanceDefinition)).andReturn(cluster);

    replay(viewRegistry);

    ViewContextImpl viewContext = new ViewContextImpl(viewInstanceDefinition, viewRegistry);

    Assert.assertEquals(cluster, viewContext.getCluster());

    verify(viewRegistry);
  }
}
