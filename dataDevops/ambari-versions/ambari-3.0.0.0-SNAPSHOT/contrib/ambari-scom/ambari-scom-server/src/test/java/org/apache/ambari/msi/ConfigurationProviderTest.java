/**
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

package org.apache.ambari.msi;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;

/**
 * Tests for ConfigurationProvider.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationProvider.class, StaxDriver.class, XStream.class, ClassLoader.class, InputStream.class})
public class ConfigurationProviderTest {

  @Test
  public void testConfigurationProvider_init_method_file_doesnt_exists() throws Exception {
    ClusterDefinition clusterDefinitionMock = createStrictMock(ClusterDefinition.class);
    PowerMock.suppress(PowerMock.methods(ConfigurationProvider.class, "initConfigurationResources"));

    StaxDriver staxDriver = PowerMock.createStrictMock(StaxDriver.class);
    XStream xstream = PowerMock.createStrictMock(XStream.class);

    PowerMock.expectNew(StaxDriver.class).andReturn(staxDriver);
    PowerMock.expectNew(XStream.class, staxDriver).andReturn(xstream);
    xstream.alias("configuration", Map.class);
    expectLastCall();
    xstream.registerConverter(anyObject(ConfigurationProvider.ScomConfigConverter.class));
    expectLastCall();

    PowerMock.replay(staxDriver, StaxDriver.class, xstream, XStream.class);
    replay(clusterDefinitionMock);
    new ConfigurationProvider(clusterDefinitionMock);
    PowerMock.verify(staxDriver, StaxDriver.class, xstream, XStream.class);
    verify(clusterDefinitionMock);
  }

  @Test
  public void testConfigurationProvider_init_method_file_exists() throws Exception {
    ClusterDefinition clusterDefinitionMock = createStrictMock(ClusterDefinition.class);
    PowerMock.suppress(PowerMock.methods(ConfigurationProvider.class, "initConfigurationResources"));

    StaxDriver staxDriver = PowerMock.createStrictMock(StaxDriver.class);
    XStream xstream = PowerMock.createStrictMock(XStream.class);
    PowerMock.mockStatic(ClassLoader.class);
    InputStream mockInputStream = createMock(InputStream.class);


    PowerMock.expectNew(StaxDriver.class).andReturn(staxDriver);
    PowerMock.expectNew(XStream.class, staxDriver).andReturn(xstream);
    xstream.alias("configuration", Map.class);
    expectLastCall();
    xstream.registerConverter(anyObject(ConfigurationProvider.ScomConfigConverter.class));
    expectLastCall();
    expect(ClassLoader.getSystemResourceAsStream(anyObject(String.class))).andReturn(mockInputStream).times(5);
    expect(xstream.fromXML(mockInputStream)).andReturn(new HashMap<String, String>()).times(5);

    PowerMock.replay(staxDriver, StaxDriver.class, xstream, XStream.class, ClassLoader.class);
    replay(clusterDefinitionMock, mockInputStream);

    new ConfigurationProvider(clusterDefinitionMock);

    PowerMock.verify(staxDriver, StaxDriver.class, xstream, XStream.class, ClassLoader.class);
    verify(clusterDefinitionMock, mockInputStream);
  }

  @Test
  public void testConfigurationProvider_initConfigurationResources_method() throws Exception {
    ClusterDefinition clusterDefinitionMock = createStrictMock(ClusterDefinition.class);
    StaxDriver staxDriver = PowerMock.createStrictMock(StaxDriver.class);
    XStream xstream = PowerMock.createStrictMock(XStream.class);
    PowerMock.mockStatic(ClassLoader.class);
    InputStream mockInputStream = createMock(InputStream.class);


    PowerMock.expectNew(StaxDriver.class).andReturn(staxDriver);
    PowerMock.expectNew(XStream.class, staxDriver).andReturn(xstream);
    xstream.alias("configuration", Map.class);
    expectLastCall();
    xstream.registerConverter(anyObject(ConfigurationProvider.ScomConfigConverter.class));
    expectLastCall();
    expect(ClassLoader.getSystemResourceAsStream(anyObject(String.class))).andReturn(mockInputStream).times(5);
    expect(xstream.fromXML(mockInputStream)).andReturn(new HashMap<String, String>() {{
      put("property_key", "propery_value");
    }}).times(5);

    expect(clusterDefinitionMock.getClusterName()).andReturn("ambari");

    PowerMock.replay(staxDriver, StaxDriver.class, xstream, XStream.class, ClassLoader.class);
    replay(clusterDefinitionMock, mockInputStream);

    ConfigurationProvider configurationProvider = new ConfigurationProvider(clusterDefinitionMock);

    PowerMock.verify(staxDriver, StaxDriver.class, xstream, XStream.class, ClassLoader.class);
    verify(clusterDefinitionMock, mockInputStream);

    Assert.assertEquals(5, configurationProvider.getResources().size());
  }

  @Test
  public void testGetResourcesWithPredicate() throws Exception {
    ClusterDefinition clusterDefinitionMock = createStrictMock(ClusterDefinition.class);
    StaxDriver staxDriver = PowerMock.createStrictMock(StaxDriver.class);
    XStream xstream = PowerMock.createStrictMock(XStream.class);
    PowerMock.mockStatic(ClassLoader.class);
    InputStream mockInputStream = createMock(InputStream.class);


    PowerMock.expectNew(StaxDriver.class).andReturn(staxDriver);
    PowerMock.expectNew(XStream.class, staxDriver).andReturn(xstream);
    xstream.alias("configuration", Map.class);
    expectLastCall();
    xstream.registerConverter(anyObject(ConfigurationProvider.ScomConfigConverter.class));
    expectLastCall();
    expect(ClassLoader.getSystemResourceAsStream(anyObject(String.class))).andReturn(mockInputStream).times(5);
    expect(xstream.fromXML(mockInputStream)).andReturn(new HashMap<String, String>() {{
      put("property_key", "propery_value");
    }}).times(5);

    expect(clusterDefinitionMock.getClusterName()).andReturn("ambari");

    PowerMock.replay(staxDriver, StaxDriver.class, xstream, XStream.class, ClassLoader.class);
    replay(clusterDefinitionMock, mockInputStream);

    ConfigurationProvider configurationProvider = new ConfigurationProvider(clusterDefinitionMock);

    PowerMock.verify(staxDriver, StaxDriver.class, xstream, XStream.class, ClassLoader.class);
    verify(clusterDefinitionMock, mockInputStream);

    Predicate configPredicate = new PredicateBuilder().property
            (ConfigurationProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID).equals("ambari").and()
            .property(ConfigurationProvider.CONFIGURATION_CONFIG_TYPE_PROPERTY_ID).equals("yarn-site").and()
            .property(ConfigurationProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID).equals("version1").toPredicate();

    Set<Resource> resources = configurationProvider.getResources(PropertyHelper.getReadRequest(), configPredicate);
    Assert.assertNotNull(resources);
    Assert.assertEquals(1, resources.size());
  }
}
