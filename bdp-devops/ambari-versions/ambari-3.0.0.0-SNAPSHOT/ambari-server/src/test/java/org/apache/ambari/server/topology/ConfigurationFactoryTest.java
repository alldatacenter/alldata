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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Creates a configuration instance given user specified properties.
 */
public class ConfigurationFactoryTest {

  @Test
  public void testOldSyntax() throws Exception {
    ConfigurationFactory factory = new ConfigurationFactory();
    Configuration configuration = factory.getConfiguration(getOldSyntaxConfigProps());

    assertEquals(2, configuration.getProperties().size());

    Map<String, String> configProperties1 = configuration.getProperties().get("foo-type");
    assertEquals(2, configProperties1.size());
    assertEquals("prop1Value", configProperties1.get("prop1"));
    assertEquals("prop2Value", configProperties1.get("prop2"));

    Map<String, String> configProperties2 = configuration.getProperties().get("bar-type");
    assertEquals(1, configProperties2.size());
    assertEquals("prop3Value", configProperties2.get("prop3"));

    assertTrue(configuration.getAttributes().isEmpty());
  }

  @Test
  public void testNewSyntax() throws Exception {
    ConfigurationFactory factory = new ConfigurationFactory();
    Configuration configuration = factory.getConfiguration(getNewSyntaxConfigProps());

    // properties
    Map<String, Map<String, String>> properties = configuration.getProperties();
    assertEquals(2, properties.size());

    Map<String, String> configProperties1 = properties.get("foo-type");
    assertEquals(2, configProperties1.size());
    assertEquals("prop1Value", configProperties1.get("prop1"));
    assertEquals("prop2Value", configProperties1.get("prop2"));

    Map<String, String> configProperties2 = properties.get("bar-type");
    assertEquals(1, configProperties2.size());
    assertEquals("prop3Value", configProperties2.get("prop3"));

    // attributes
    Map<String, Map<String, Map<String, String>>> attributes = configuration.getAttributes();
    assertEquals(2, attributes.size());

    // config type foo
    Map<String, Map<String, String>> configType1Attributes = attributes.get("foo-type");
    assertEquals(2, configType1Attributes.size());
    // properties with attribute1
    Map<String, String> configType1Prop1Attributes = configType1Attributes.get("attribute1");
    assertEquals(3, configType1Prop1Attributes.size());
    assertEquals("attribute1-prop1-value", configType1Prop1Attributes.get("prop1"));
    assertEquals("attribute1-prop2-value", configType1Prop1Attributes.get("prop2"));
    assertEquals("attribute1-prop3-value", configType1Prop1Attributes.get("prop3"));
    // properties with attribute2
    Map<String, String> configType1Prop2Attributes = configType1Attributes.get("attribute2");
    assertEquals(1, configType1Prop2Attributes.size());
    assertEquals("attribute2-prop1-value", configType1Prop2Attributes.get("prop1"));

    // config type foobar
    Map<String, Map<String, String>> configType2Attributes = attributes.get("foobar-type");
    assertEquals(2, configType2Attributes.size());
    // properties with attribute1
    Map<String, String> configType2Prop1Attributes = configType2Attributes.get("attribute1");
    assertEquals(1, configType2Prop1Attributes.size());
    assertEquals("attribute1-prop10-value", configType2Prop1Attributes.get("prop10"));
    // properties with attribute10
    Map<String, String> configType2Prop2Attributes = configType2Attributes.get("attribute10");
    assertEquals(1, configType2Prop2Attributes.size());
    assertEquals("attribute10-prop11-value", configType2Prop2Attributes.get("prop11"));
  }

  private Collection<Map<String, String>> getNewSyntaxConfigProps() {
    Collection<Map<String, String>> configurations = new ArrayList<>();

    // type foo has both properties and attributes
    Map<String, String> configProperties1 = new HashMap<>();
    configProperties1.put("foo-type/properties/prop1", "prop1Value");
    configProperties1.put("foo-type/properties/prop2", "prop2Value");
    // foo type attributes
    configProperties1.put("foo-type/properties_attributes/attribute1/prop1", "attribute1-prop1-value");
    configProperties1.put("foo-type/properties_attributes/attribute1/prop2", "attribute1-prop2-value");
    configProperties1.put("foo-type/properties_attributes/attribute1/prop3", "attribute1-prop3-value");
    configProperties1.put("foo-type/properties_attributes/attribute2/prop1", "attribute2-prop1-value");
    configurations.add(configProperties1);

    // type bar has only properties
    Map<String, String> configProperties2 = new HashMap<>();
    configProperties2.put("bar-type/properties/prop3", "prop3Value");
    configurations.add(configProperties2);

    // type foobar has only attributes
    Map<String, String> configProperties3 = new HashMap<>();
    configProperties3.put("foobar-type/properties_attributes/attribute1/prop10", "attribute1-prop10-value");
    configProperties3.put("foobar-type/properties_attributes/attribute10/prop11", "attribute10-prop11-value");
    configurations.add(configProperties3);

    return configurations;
  }

  private Collection<Map<String, String>> getOldSyntaxConfigProps() {
    Collection<Map<String, String>> configurations = new ArrayList<>();

    Map<String, String> configProperties1 = new HashMap<>();
    configProperties1.put("foo-type/prop1", "prop1Value");
    configProperties1.put("foo-type/prop2", "prop2Value");
    configurations.add(configProperties1);

    Map<String, String> configProperties2 = new HashMap<>();
    configProperties2.put("bar-type/prop3", "prop3Value");
    configurations.add(configProperties2);

    return configurations;
  }
}
