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
package org.apache.ambari.server.state;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.Sets;

public class PropertyInfoTest {

  @Test
  public void testProperty() {
    PropertyInfo property = new PropertyInfo();
    property.setName("name");
    property.setValue("value");
    property.setDescription("desc");
    property.setFilename("filename");
    PropertyDependencyInfo pdi = new PropertyDependencyInfo("type", "name");
    property.getDependsOnProperties().add(pdi);

    assertEquals("name", property.getName());
    assertEquals("value", property.getValue());
    assertEquals("desc", property.getDescription());
    assertEquals("filename", property.getFilename());

    assertEquals(1, property.getDependsOnProperties().size());
    assertTrue(property.getDependsOnProperties().contains(pdi));
  }

  @Test
  public void testAttributes() throws Exception {
    PropertyInfo property = new PropertyInfo();

    List<Element> elements = new ArrayList<>();
    Element e1 = createNiceMock(Element.class);
    Element e2 = createNiceMock(Element.class);
    Node n1 = createNiceMock(Node.class);
    Node n2 = createNiceMock(Node.class);

    elements.add(e1);
    elements.add(e2);

    // set mock expectations
    expect(e1.getTagName()).andReturn("foo").anyTimes();
    expect(e1.getFirstChild()).andReturn(n1).anyTimes();
    expect(n1.getNodeValue()).andReturn("value1").anyTimes();

    expect(e2.getTagName()).andReturn("bar").anyTimes();
    expect(e2.getFirstChild()).andReturn(n2).anyTimes();
    expect(n2.getNodeValue()).andReturn("value2").anyTimes();

    replay(e1, e2, n1, n2);

    // set attributes
    Field f = property.getClass().getDeclaredField("propertyAttributes");
    f.setAccessible(true);
    f.set(property, elements);

    Map<String, String> attributes = property.getAttributesMap();
    assertEquals(2, attributes.size());
    assertEquals("value1", attributes.get("foo"));
    assertEquals("value2", attributes.get("bar"));
  }

  @Test
  public void testUpgradeBehaviorTag() throws JAXBException {
    // given
    String xml =
      "<property>\n" +
      "  <name>prop_name</name>\n" +
      "  <value>prop_val</value>\n" +
      "  <on-ambari-upgrade add=\"true\" update=\"true\" delete=\"true\"/>\n" +
      "</property>";

    // when
    PropertyInfo propertyInfo = propertyInfoFrom(xml);

    // then
    assertTrue(propertyInfo.getPropertyAmbariUpgradeBehavior().isAdd());
    assertTrue(propertyInfo.getPropertyAmbariUpgradeBehavior().isUpdate());
    assertTrue(propertyInfo.getPropertyAmbariUpgradeBehavior().isDelete());
  }

  @Test
  public void testBehaviorWithoutUpgradeTags() throws JAXBException {
    // given
    String xml =
        "<property>\n" +
            "  <name>prop_name</name>\n" +
            "  <value>prop_val</value>\n" +
            "</property>";

    // when
    PropertyInfo propertyInfo = propertyInfoFrom(xml);

    // then

    assertTrue(propertyInfo.getPropertyAmbariUpgradeBehavior().isAdd());
    assertFalse(propertyInfo.getPropertyAmbariUpgradeBehavior().isUpdate());
    assertFalse(propertyInfo.getPropertyAmbariUpgradeBehavior().isDelete());
  }

  @Test
  public void testBehaviorWithSupportedRefreshCommandsTags() throws JAXBException {
    // given
    String xml =
    "<property>\n" +
    " <name>prop_name</name>\n" +
    " <value>prop_val</value>\n" +
    " <supported-refresh-commands>\n" +
    "   <refresh-command componentName=\"NAMENODE\" command=\"reload_configs\" />\n" +
    " </supported-refresh-commands>\n" +
    "</property>";

    // when
    PropertyInfo propertyInfo = propertyInfoFrom(xml);

    // then
    assertEquals(propertyInfo.getSupportedRefreshCommands().iterator().next().getCommand(), "reload_configs");
    assertEquals(propertyInfo.getSupportedRefreshCommands().iterator().next().getComponentName(), "NAMENODE");
  }

  @Test
  public void testUnknownPropertyType() throws Exception {
    // Given
    String xml =
      "<property>\n"+
      "  <name>prop_name</name>\n" +
      "  <value>prop_val</value>\n" +
      "  <property-type>PASSWORD USER UNRECOGNIZED_TYPE</property-type>\n" +
      "  <description>test description</description>\n" +
      "</property>";


    // When
    PropertyInfo propertyInfo = propertyInfoFrom(xml);

    // Then
    Set<PropertyInfo.PropertyType> expectedPropertyTypes = Sets.newHashSet(PropertyInfo.PropertyType.PASSWORD, PropertyInfo.PropertyType.USER);

    assertEquals(expectedPropertyTypes, propertyInfo.getPropertyTypes());
  }

  public static PropertyInfo propertyInfoFrom(String xml) throws JAXBException {
    JAXBContext jaxbCtx = JAXBContext.newInstance(PropertyInfo.class, PropertyUpgradeBehavior.class);
    Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();

    return unmarshaller.unmarshal(
      new StreamSource(
        new ByteArrayInputStream(xml.getBytes())
      ),
      PropertyInfo.class
    ).getValue();
  }
}
