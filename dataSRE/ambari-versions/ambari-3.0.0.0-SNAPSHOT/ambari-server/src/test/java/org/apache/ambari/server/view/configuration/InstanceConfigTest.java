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

package org.apache.ambari.server.view.configuration;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;

/**
 * InstanceConfig tests.
 */
public class InstanceConfigTest {

  private static String xml_no_properties = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <resource>\n" +
      "        <name>resource</name>\n" +
      "        <plural-name>resources</plural-name>\n" +
      "        <id-property>id</id-property>\n" +
      "        <resource-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResource</resource-class>\n" +
      "        <provider-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResourceProvider</provider-class>\n" +
      "        <service-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResourceService</service-class>\n" +
      "        <sub-resource-name>subresource</sub-resource-name>\n" +
      "    </resource>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_visible = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <visible>true</visible>\n" +
      "    </instance>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE2</name>\n" +
      "        <visible>false</visible>\n" +
      "    </instance>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE3</name>\n" +
      "    </instance>\n" +
      "</view>";


  @Test
  public void testGetName() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("INSTANCE1", instances.get(0).getName());
    Assert.assertEquals("INSTANCE2", instances.get(1).getName());
  }

  @Test
  public void testGetLabel() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("My Instance 1!", instances.get(0).getLabel());
    Assert.assertEquals("My Instance 2!", instances.get(1).getLabel());
  }

  @Test
  public void testGetDescription() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("This is a description.", instances.get(0).getDescription());
    Assert.assertNull(instances.get(1).getDescription());
  }

  @Test
  public void testIsVisible() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs(xml_visible);

    Assert.assertEquals(3, instances.size());
    Assert.assertTrue(instances.get(0).isVisible());
    Assert.assertFalse(instances.get(1).isVisible());
    Assert.assertTrue(instances.get(2).isVisible());
  }

  @Test
  public void testGetIcon() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("/this/is/the/icon/url/instance_1_icon.png", instances.get(0).getIcon());
    Assert.assertNull(instances.get(1).getIcon());
  }

  @Test
  public void testGetIcon64() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("/this/is/the/icon/url/instance_1_icon64.png", instances.get(0).getIcon64());
    Assert.assertNull(instances.get(1).getIcon());
  }

  @Test
  public void testGetProperties() throws Exception {
    List<InstanceConfig> instances = getInstanceConfigs();

    Assert.assertEquals(2, instances.size());
    List<PropertyConfig> properties = instances.get(0).getProperties();
    Assert.assertEquals(2, properties.size());

    properties = instances.get(1).getProperties();
    Assert.assertEquals(1, properties.size());

    // check the case where no properties are specified for the instance...
    instances = getInstanceConfigs(xml_no_properties);

    Assert.assertEquals(1, instances.size());
    properties = instances.get(0).getProperties();
    Assert.assertNotNull(properties);
    Assert.assertEquals(0, properties.size());
  }

  public static List<InstanceConfig> getInstanceConfigs() throws JAXBException {
    ViewConfig config = ViewConfigTest.getConfig();
    return config.getInstances();
  }

  public static List<InstanceConfig> getInstanceConfigs(String xml) throws JAXBException {
    ViewConfig config = ViewConfigTest.getConfig(xml);
    return config.getInstances();
  }
}
