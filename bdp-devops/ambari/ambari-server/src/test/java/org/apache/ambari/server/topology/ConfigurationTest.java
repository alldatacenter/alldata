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

package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Configuration unit tests.
 */
public class ConfigurationTest {

  private static final Map<String, Map<String, String>> EMPTY_PROPERTIES = new HashMap<>();
  private static final Map<String, Map<String, Map<String, String>>>  EMPTY_ATTRIBUTES = new HashMap<>();

  @Test
  public void testGetProperties_noParent() {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProperties1 = new HashMap<>();
    typeProperties1.put("prop1", "val1");
    typeProperties1.put("prop2", "val2");
    Map<String, String> typeProperties2 = new HashMap<>();
    typeProperties2.put("prop1", "val1");
    typeProperties2.put("prop3", "val3");

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);

    Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES);
    assertEquals(properties, configuration.getProperties());
    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetFullProperties_noParent() {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProperties1 = new HashMap<>();
    typeProperties1.put("prop1", "val1");
    typeProperties1.put("prop2", "val2");
    Map<String, String> typeProperties2 = new HashMap<>();
    typeProperties2.put("prop1", "val1");
    typeProperties2.put("prop3", "val3");

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);

    Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES);
    assertEquals(properties, configuration.getFullProperties());
    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetProperties_withParent() {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProperties1 = new HashMap<>();
    typeProperties1.put("prop1", "val1");
    typeProperties1.put("prop2", "val2");
    Map<String, String> typeProperties2 = new HashMap<>();
    typeProperties2.put("prop1", "val1");
    typeProperties2.put("prop3", "val3");

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);

    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Map<String, String> parentTypeProperties1 = new HashMap<>();
    parentTypeProperties1.put("prop5", "val5");
    Map<String, String> parentTypeProperties3 = new HashMap<>();
    parentTypeProperties3.put("prop6", "val6");

    parentProperties.put("type1", parentTypeProperties1);
    parentProperties.put("type3", parentTypeProperties3);

    Configuration parentConfiguration = new Configuration(parentProperties, EMPTY_ATTRIBUTES);

    Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES, parentConfiguration);
    // parent should not be reflected in getProperties() result
    assertEquals(properties, configuration.getProperties());
    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetFullProperties_withParent() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    // all parents should be reflected in getFullProperties() result
    Map<String, Map<String, String>> fullProperties = configuration.getFullProperties();

    // type1, type2, type3, type4
    assertEquals(4, fullProperties.size());
    // type1
    Map<String, String> type1Props = fullProperties.get("type1");
    assertEquals(5, type1Props.size());
    assertEquals("val1.3", type1Props.get("prop1"));
    assertEquals("val2.2", type1Props.get("prop2"));
    assertEquals("val3.1", type1Props.get("prop3"));
    assertEquals("val6.2", type1Props.get("prop6"));
    assertEquals("val9.3", type1Props.get("prop9"));

    //type2
    Map<String, String> type2Props = fullProperties.get("type2");
    assertEquals(2, type2Props.size());
    assertEquals("val4.3", type2Props.get("prop4"));
    assertEquals("val5.1", type2Props.get("prop5"));

    //type3
    Map<String, String> type3Props = fullProperties.get("type3");
    assertEquals(2, type3Props.size());
    assertEquals("val7.3", type3Props.get("prop7"));
    assertEquals("val8.2", type3Props.get("prop8"));

    //type4
    Map<String, String> type4Props = fullProperties.get("type4");
    assertEquals(2, type4Props.size());
    assertEquals("val10.3", type4Props.get("prop10"));
    assertEquals("val11.3", type4Props.get("prop11"));

    // ensure that underlying property map is not modified in getFullProperties
    Configuration expectedConfiguration = createConfigurationWithParents_PropsOnly();
    assertEquals(expectedConfiguration.getProperties(), configuration.getProperties());
    assertEquals(expectedConfiguration.getParentConfiguration().getProperties(), configuration.getParentConfiguration().getProperties());
    assertEquals(expectedConfiguration.getParentConfiguration().getParentConfiguration().getProperties(), configuration.getParentConfiguration().getParentConfiguration().getProperties());

    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());

    Collection<String> configTypes = configuration.getAllConfigTypes();
    assertEquals(4, configTypes.size());
    assertTrue(configTypes.containsAll(Arrays.asList("type1", "type2", "type3", "type4")));
  }

  @Test
  public void containsConfigType() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    assertTrue(configuration.containsConfigType("type1"));
    assertTrue(configuration.containsConfigType("type2"));
    assertTrue(configuration.containsConfigType("type3"));
    assertTrue(configuration.containsConfigType("type4"));
    assertFalse(configuration.containsConfigType("type5"));

    configuration = createConfigurationWithParents_AttributesOnly();
    assertTrue(configuration.containsConfigType("type1"));
    assertTrue(configuration.containsConfigType("type2"));
    assertFalse(configuration.containsConfigType("type3"));
  }

  @Test
  public void containsConfig() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    assertTrue(configuration.containsConfig("type1", "prop1"));
    assertTrue(configuration.containsConfig("type1", "prop2"));
    assertTrue(configuration.containsConfig("type1", "prop3"));
    assertTrue(configuration.containsConfig("type2", "prop4"));
    assertTrue(configuration.containsConfig("type2", "prop5"));
    assertTrue(configuration.containsConfig("type1", "prop6"));
    assertTrue(configuration.containsConfig("type1", "prop9"));
    assertTrue(configuration.containsConfig("type3", "prop7"));
    assertTrue(configuration.containsConfig("type3", "prop8"));
    assertTrue(configuration.containsConfig("type4", "prop10"));
    assertTrue(configuration.containsConfig("type4", "prop11"));
    assertFalse(configuration.containsConfig("type1", "prop99"));
    assertFalse(configuration.containsConfig("core-site", "io.file.buffer.size"));

    configuration = createConfigurationWithParents_AttributesOnly();
    assertTrue(configuration.containsConfig("type1", "prop1"));
    assertTrue(configuration.containsConfig("type1", "prop2"));
    assertTrue(configuration.containsConfig("type1", "prop3"));
    assertTrue(configuration.containsConfig("type1", "prop6"));
    assertTrue(configuration.containsConfig("type1", "prop7"));
    assertTrue(configuration.containsConfig("type1", "prop8"));
    assertTrue(configuration.containsConfig("type1", "prop9"));
    assertTrue(configuration.containsConfig("type1", "prop10"));
    assertTrue(configuration.containsConfig("type1", "prop11"));
    assertTrue(configuration.containsConfig("type2", "prop100"));
    assertTrue(configuration.containsConfig("type2", "prop101"));
    assertTrue(configuration.containsConfig("type2", "prop102"));
    assertFalse(configuration.containsConfig("type1", "prop99"));
    assertFalse(configuration.containsConfig("core-site", "io.file.buffer.size"));
  }

  @Test
  public void testGetFullProperties_withParent_specifyDepth() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    // specify a depth of 1 which means to include only 1 level up the parent chain
    Map<String, Map<String, String>> fullProperties = configuration.getFullProperties(1);

    // type1, type2, type3, type4
    assertEquals(4, fullProperties.size());
    // type1
    Map<String, String> type1Props = fullProperties.get("type1");
    assertEquals(4, type1Props.size());
    assertEquals("val1.3", type1Props.get("prop1"));
    assertEquals("val2.2", type1Props.get("prop2"));
    assertEquals("val6.2", type1Props.get("prop6"));
    assertEquals("val9.3", type1Props.get("prop9"));

    //type2
    Map<String, String> type2Props = fullProperties.get("type2");
    assertEquals(1, type2Props.size());
    assertEquals("val4.3", type2Props.get("prop4"));

    //type3
    Map<String, String> type3Props = fullProperties.get("type3");
    assertEquals(2, type3Props.size());
    assertEquals("val7.3", type3Props.get("prop7"));
    assertEquals("val8.2", type3Props.get("prop8"));

    //type4
    Map<String, String> type4Props = fullProperties.get("type4");
    assertEquals(2, type4Props.size());
    assertEquals("val10.3", type4Props.get("prop10"));
    assertEquals("val11.3", type4Props.get("prop11"));

    // ensure that underlying property maps are not modified in getFullProperties
    Configuration expectedConfiguration = createConfigurationWithParents_PropsOnly();
    assertEquals(expectedConfiguration.getProperties(), configuration.getProperties());
    assertEquals(expectedConfiguration.getParentConfiguration().getProperties(), configuration.getParentConfiguration().getProperties());
    assertEquals(expectedConfiguration.getParentConfiguration().getParentConfiguration().getProperties(), configuration.getParentConfiguration().getParentConfiguration().getProperties());

    assertEquals(EMPTY_ATTRIBUTES, configuration.getAttributes());
  }

  @Test
  public void testGetAttributes_noParent() {
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    Map<String, Map<String, String>> attributeProperties = new HashMap<>();
    Map<String, String> properties1 = new HashMap<>();
    properties1.put("prop1", "val1");
    properties1.put("prop2", "val2");
    Map<String, String> properties2 = new HashMap<>();
    properties2.put("prop1", "val3");
    attributeProperties.put("attribute1", properties1);
    attributeProperties.put("attribute2", properties2);

    attributes.put("type1", attributeProperties);

    //test
    Configuration configuration = new Configuration(EMPTY_PROPERTIES, attributes);
    // assert attributes
    assertEquals(attributes, configuration.getAttributes());
    // assert empty properties
    assertEquals(EMPTY_PROPERTIES, configuration.getProperties());
  }

  @Test
  public void testGetFullAttributes_withParent() {
    Configuration configuration = createConfigurationWithParents_AttributesOnly();
    // all parents should be reflected in getFullAttributes() result
    Map<String, Map<String, Map<String, String>>> fullAttributes = configuration.getFullAttributes();
    assertEquals(2, fullAttributes.size());

    // type 1
    Map<String, Map<String, String>> type1Attributes = fullAttributes.get("type1");
    // attribute1, attribute2, attribute3, attribute4
    assertEquals(4, type1Attributes.size());
    // attribute1
    Map<String, String> attribute1Properties = type1Attributes.get("attribute1");
    assertEquals(5, attribute1Properties.size());
    assertEquals("val1.3", attribute1Properties.get("prop1"));
    assertEquals("val2.2", attribute1Properties.get("prop2"));
    assertEquals("val3.1", attribute1Properties.get("prop3"));
    assertEquals("val6.2", attribute1Properties.get("prop6"));
    assertEquals("val9.3", attribute1Properties.get("prop9"));

    //attribute2
    Map<String, String> attribute2Properties = type1Attributes.get("attribute2");
    assertEquals(2, attribute2Properties.size());
    assertEquals("val4.3", attribute2Properties.get("prop4"));
    assertEquals("val5.1", attribute2Properties.get("prop5"));

    //attribute3
    Map<String, String> attribute3Properties = type1Attributes.get("attribute3");
    assertEquals(2, attribute3Properties.size());
    assertEquals("val7.3", attribute3Properties.get("prop7"));
    assertEquals("val8.2", attribute3Properties.get("prop8"));

    //attribute4
    Map<String, String> attribute4Properties = type1Attributes.get("attribute4");
    assertEquals(2, attribute4Properties.size());
    assertEquals("val10.3", attribute4Properties.get("prop10"));
    assertEquals("val11.3", attribute4Properties.get("prop11"));

    // type 2
    Map<String, Map<String, String>> type2Attributes = fullAttributes.get("type2");
    // attribute100, attribute101
    assertEquals(2, type2Attributes.size());

    Map<String, String> attribute100Properties = type2Attributes.get("attribute100");
    assertEquals(3, attribute100Properties.size());
    assertEquals("val100.3", attribute100Properties.get("prop100"));
    assertEquals("val101.1", attribute100Properties.get("prop101"));
    assertEquals("val102.3", attribute100Properties.get("prop102"));

    Map<String, String> attribute101Properties = type2Attributes.get("attribute101");
    assertEquals(2, attribute101Properties.size());
    assertEquals("val100.2", attribute101Properties.get("prop100"));
    assertEquals("val101.1", attribute101Properties.get("prop101"));

    // ensure that underlying attribute maps are not modified in getFullProperties
    Configuration expectedConfiguration = createConfigurationWithParents_AttributesOnly();
    assertEquals(expectedConfiguration.getAttributes(), configuration.getAttributes());
    assertEquals(expectedConfiguration.getParentConfiguration().getAttributes(), configuration.getParentConfiguration().getAttributes());
    assertEquals(expectedConfiguration.getParentConfiguration().getParentConfiguration().getAttributes(), configuration.getParentConfiguration().getParentConfiguration().getAttributes());

    assertEquals(EMPTY_PROPERTIES, configuration.getProperties());

    Collection<String> configTypes = configuration.getAllConfigTypes();
    assertEquals(2, configTypes.size());
    assertTrue(configTypes.containsAll(Arrays.asList("type1", "type2")));
  }

  @Test
  public void testGetPropertyValue() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();

    assertEquals("val1.3", configuration.getPropertyValue("type1", "prop1"));
    assertEquals("val2.2", configuration.getPropertyValue("type1", "prop2"));
    assertEquals("val3.1", configuration.getPropertyValue("type1", "prop3"));
    assertEquals("val4.3", configuration.getPropertyValue("type2", "prop4"));
    assertEquals("val5.1", configuration.getPropertyValue("type2", "prop5"));
    assertEquals("val6.2", configuration.getPropertyValue("type1", "prop6"));
    assertEquals("val7.3", configuration.getPropertyValue("type3", "prop7"));
    assertEquals("val8.2", configuration.getPropertyValue("type3", "prop8"));
    assertEquals("val9.3", configuration.getPropertyValue("type1", "prop9"));
    assertEquals("val10.3", configuration.getPropertyValue("type4", "prop10"));
    assertEquals("val11.3", configuration.getPropertyValue("type4", "prop11"));
  }

  @Test
  public void testGetAttributeValue() {
    Configuration configuration = createConfigurationWithParents_AttributesOnly();

    assertEquals("val1.3", configuration.getAttributeValue("type1", "prop1", "attribute1"));
    assertEquals("val2.2", configuration.getAttributeValue("type1", "prop2", "attribute1"));
    assertEquals("val3.1", configuration.getAttributeValue("type1", "prop3", "attribute1"));
    assertEquals("val4.3", configuration.getAttributeValue("type1", "prop4", "attribute2"));
    assertEquals("val5.1", configuration.getAttributeValue("type1", "prop5", "attribute2"));
    assertEquals("val6.2", configuration.getAttributeValue("type1", "prop6", "attribute1"));
    assertEquals("val7.3", configuration.getAttributeValue("type1", "prop7", "attribute3"));
    assertEquals("val8.2", configuration.getAttributeValue("type1", "prop8", "attribute3"));
    assertEquals("val100.3", configuration.getAttributeValue("type2", "prop100", "attribute100"));
    assertEquals("val101.1", configuration.getAttributeValue("type2", "prop101", "attribute100"));
    assertEquals("val102.3", configuration.getAttributeValue("type2", "prop102", "attribute100"));
    assertEquals("val100.2", configuration.getAttributeValue("type2", "prop100", "attribute101"));
    assertEquals("val101.1", configuration.getAttributeValue("type2", "prop101", "attribute101"));
  }

  @Test
  public void testRemoveProperty() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    // property only exists in root level config
    assertEquals("val3.1", configuration.removeProperty("type1", "prop3"));
    assertNull(configuration.getPropertyValue("type1", "prop3"));

    // property only exists in configuration instance
    assertEquals("val9.3", configuration.removeProperty("type1", "prop9"));
    assertNull(configuration.getPropertyValue("type1", "prop9"));

    // property at multiple levels
    assertEquals("val1.3", configuration.removeProperty("type1", "prop1"));
    assertNull(configuration.getPropertyValue("type1", "prop1"));

    assertEquals("val4.3", configuration.removeProperty("type2", "prop4"));
    assertNull(configuration.getPropertyValue("type2", "prop4"));

    assertEquals("val2.2", configuration.removeProperty("type1", "prop2"));
    assertNull(configuration.getPropertyValue("type1", "prop2"));

    // type and property don't exist
    assertNull(configuration.getPropertyValue("typeXXX", "XXXXX"));

    // type exists but property doesn't
    assertNull(configuration.getPropertyValue("type1", "XXXXX"));
  }

  @Test
  public void testRemoveConfigTypes() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    configuration.removeConfigType("type1");
    assertNull(configuration.getProperties().get("type1"));
  }

  @Test
  public void testRemoveConfigTypesForAttributes() {
    Configuration configuration = createConfigurationWithParents_PropsOnly();
    configuration.removeConfigType("type1");
    assertNull(configuration.getAttributes().get("type1"));
  }

  private Configuration createConfigurationWithParents_PropsOnly() {
    // parents parent config properties
    Map<String, Map<String, String>> parentParentProperties = new HashMap<>();
    Map<String, String> parentParentTypeProperties1 = new HashMap<>();
    parentParentTypeProperties1.put("prop1", "val1.1");
    parentParentTypeProperties1.put("prop2", "val2.1");
    parentParentTypeProperties1.put("prop3", "val3.1");
    Map<String, String> parentParentTypeProperties2 = new HashMap<>();
    parentParentTypeProperties2.put("prop4", "val4.1");
    parentParentTypeProperties2.put("prop5", "val5.1");

    parentParentProperties.put("type1", parentParentTypeProperties1);
    parentParentProperties.put("type2", parentParentTypeProperties2);
    Configuration parentParentConfiguration = new Configuration(parentParentProperties, EMPTY_ATTRIBUTES);

    // parent config properties
    Map<String, Map<String, String>> parentProperties = new HashMap<>();
    Map<String, String> parentTypeProperties1 = new HashMap<>(); // override
    parentTypeProperties1.put("prop1", "val1.2"); // override parent
    parentTypeProperties1.put("prop2", "val2.2"); // override parent
    parentTypeProperties1.put("prop6", "val6.2"); // new
    Map<String, String> parentTypeProperties3 = new HashMap<>(); // new
    parentTypeProperties3.put("prop7", "val7.2"); // new
    parentTypeProperties3.put("prop8", "val8.2"); // new

    parentProperties.put("type1", parentTypeProperties1);
    parentProperties.put("type3", parentTypeProperties3);
    Configuration parentConfiguration = new Configuration(parentProperties, EMPTY_ATTRIBUTES, parentParentConfiguration);

    // leaf config properties
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProperties1 = new HashMap<>();
    typeProperties1.put("prop1", "val1.3"); // overrides both parent and parents parent
    typeProperties1.put("prop9", "val9.3"); // new
    Map<String, String> typeProperties2 = new HashMap<>(); // overrides
    typeProperties2.put("prop4", "val4.3"); // overrides parents parent value
    Map<String, String> typeProperties3 = new HashMap<>(); // overrides
    typeProperties3.put("prop7", "val7.3"); // overrides parents parent value
    Map<String, String> typeProperties4 = new HashMap<>(); // new
    typeProperties4.put("prop10", "val10.3"); // new
    typeProperties4.put("prop11", "val11.3"); // new

    properties.put("type1", typeProperties1);
    properties.put("type2", typeProperties2);
    properties.put("type3", typeProperties3);
    properties.put("type4", typeProperties4);
    return new Configuration(properties, EMPTY_ATTRIBUTES, parentConfiguration);
  }

  private Configuration createConfigurationWithParents_AttributesOnly() {
    // parents parent config attributes.
    Map<String, Map<String, Map<String, String>>> parentParentAttributes = new HashMap<>();
    Map<String, Map<String, String>> parentParentTypeAttributes1 = new HashMap<>();
    Map<String, Map<String, String>> parentParentTypeAttributes2 = new HashMap<>();
    parentParentAttributes.put("type1", parentParentTypeAttributes1);
    parentParentAttributes.put("type2", parentParentTypeAttributes2);

    Map<String, String> parentParentAttributeProperties1 = new HashMap<>();
    parentParentAttributeProperties1.put("prop1", "val1.1");
    parentParentAttributeProperties1.put("prop2", "val2.1");
    parentParentAttributeProperties1.put("prop3", "val3.1");
    Map<String, String> parentParentAttributeProperties2 = new HashMap<>();
    parentParentAttributeProperties2.put("prop4", "val4.1");
    parentParentAttributeProperties2.put("prop5", "val5.1");

    parentParentTypeAttributes1.put("attribute1", parentParentAttributeProperties1);
    parentParentTypeAttributes1.put("attribute2", parentParentAttributeProperties2);

    Map<String, String> parentParentAttributeProperties100 = new HashMap<>();
    parentParentAttributeProperties100.put("prop100", "val100.1");
    parentParentAttributeProperties100.put("prop101", "val101.1");

    Map<String, String> parentParentAttributeProperties101 = new HashMap<>();
    parentParentAttributeProperties101.put("prop100", "val100.1");
    parentParentAttributeProperties101.put("prop101", "val101.1");

    parentParentTypeAttributes2.put("attribute100", parentParentAttributeProperties100);
    parentParentTypeAttributes2.put("attribute101", parentParentAttributeProperties101);
    Configuration parentParentConfiguration = new Configuration(EMPTY_PROPERTIES,
      new HashMap<>(parentParentAttributes));

    // parent config attributes
    Map<String, Map<String, Map<String, String>>> parentAttributes = new HashMap<>();
    Map<String, Map<String, String>> parentTypeAttributes1 = new HashMap<>();
    Map<String, Map<String, String>> parentTypeAttributes2 = new HashMap<>();
    parentAttributes.put("type1", parentTypeAttributes1);
    parentAttributes.put("type2", parentTypeAttributes2);

    Map<String, String> parentAttributeProperties1 = new HashMap<>(); // override
    parentAttributeProperties1.put("prop1", "val1.2"); // override parent
    parentAttributeProperties1.put("prop2", "val2.2"); // override parent
    parentAttributeProperties1.put("prop6", "val6.2"); // new
    Map<String, String> parentAttributeProperties3 = new HashMap<>(); // new
    parentAttributeProperties3.put("prop7", "val7.2"); // new
    parentAttributeProperties3.put("prop8", "val8.2"); // new

    parentTypeAttributes1.put("attribute1", parentAttributeProperties1);
    parentTypeAttributes1.put("attribute3", parentAttributeProperties3);

    Map<String, String> parentAttributeProperties101 = new HashMap<>();
    parentAttributeProperties101.put("prop100", "val100.2");
    parentTypeAttributes2.put("attribute101", parentAttributeProperties101);
    Configuration parentConfiguration = new Configuration(EMPTY_PROPERTIES,
      new HashMap<>(parentAttributes), parentParentConfiguration);

    // leaf config attributes
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    Map<String, Map<String, String>> typeAttributes1 = new HashMap<>();
    Map<String, Map<String, String>> typeAttributes2 = new HashMap<>();
    attributes.put("type1", typeAttributes1);
    attributes.put("type2", typeAttributes2);

    Map<String, String> attributeProperties1 = new HashMap<>();
    attributeProperties1.put("prop1", "val1.3"); // overrides both parent and parents parent
    attributeProperties1.put("prop9", "val9.3"); // new
    Map<String, String> attributeProperties2 = new HashMap<>(); // overrides
    attributeProperties2.put("prop4", "val4.3"); // overrides parents parent value
    Map<String, String> attributeProperties3 = new HashMap<>(); // overrides
    attributeProperties3.put("prop7", "val7.3"); // overrides parents parent value
    Map<String, String> attributeProperties4 = new HashMap<>(); // new
    attributeProperties4.put("prop10", "val10.3"); // new
    attributeProperties4.put("prop11", "val11.3"); // new

    typeAttributes1.put("attribute1", attributeProperties1);
    typeAttributes1.put("attribute2", attributeProperties2);
    typeAttributes1.put("attribute3", attributeProperties3);
    typeAttributes1.put("attribute4", attributeProperties4);

    Map<String, String> attributeProperties100 = new HashMap<>(); // overrides parents parent
    attributeProperties100.put("prop100", "val100.3"); // overrides parents parent
    attributeProperties100.put("prop102", "val102.3"); // new

    typeAttributes1.put("attribute1", attributeProperties1);
    typeAttributes2.put("attribute100", attributeProperties100);
    return new Configuration(EMPTY_PROPERTIES,
      new HashMap<>(attributes), parentConfiguration);
  }

  @Test
  public void moveProperties() {
    // GIVEN
    String sourceType = "source";
    String targetType = "target";
    String sourceValue = "source value";
    String targetValue = "target value";
    Map<String, String> keepers = ImmutableMap.of("keep1", "v1", "keep2", "v3");
    Map<String, String> movers = ImmutableMap.of("move1", "v2", "move2", "v4");
    Set<String> common = ImmutableSet.of("common1", "common2");
    Configuration config = new Configuration(new HashMap<>(), new HashMap<>());
    for (Map.Entry<String, String> e : keepers.entrySet()) {
      config.setProperty(sourceType, e.getKey(), e.getValue());
    }
    for (Map.Entry<String, String> e : movers.entrySet()) {
      config.setProperty(sourceType, e.getKey(), e.getValue());
    }
    for (String key : common) {
      config.setProperty(sourceType, key, sourceValue);
      config.setProperty(targetType, key, targetValue);
    }

    // WHEN
    Sets.SetView<String> propertiesToMove = Sets.union(movers.keySet(), common);
    Set<String> moved = config.moveProperties(sourceType, targetType, propertiesToMove);

    // THEN
    for (Map.Entry<String, String> e : keepers.entrySet()) {
      assertEquals(e.getValue(), config.getPropertyValue(sourceType, e.getKey()));
    }
    for (Map.Entry<String, String> e : movers.entrySet()) {
      assertEquals(e.getValue(), config.getPropertyValue(targetType, e.getKey()));
      assertFalse(config.isPropertySet(sourceType, e.getKey()));
    }
    for (String key : common) {
      assertEquals(targetValue, config.getPropertyValue(targetType, key));
      assertFalse(config.isPropertySet(sourceType, key));
    }
    assertEquals(propertiesToMove, moved);
  }
}
