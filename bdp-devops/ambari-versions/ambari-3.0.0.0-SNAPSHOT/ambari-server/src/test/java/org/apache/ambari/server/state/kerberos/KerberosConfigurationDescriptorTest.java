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
package org.apache.ambari.server.state.kerberos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

@Category({category.KerberosTest.class})
public class KerberosConfigurationDescriptorTest {
  private static final String JSON_SINGLE_VALUE =
      "{ \"configuration-type\": {" +
          "     \"property1\": \"${property-value1}\"," +
          "     \"property2\": \"${property.value2}\"" +
          "}}";

  private static final String JSON_MULTIPLE_VALUE =
      "[" +
          "{ \"configuration-type\": {" +
          "     \"property1\": \"value1\"," +
          "     \"property2\": \"value2\"" +
          "}}," +
          "{ \"configuration-type2\": {" +
          "     \"property1\": \"value1\"," +
          "     \"property3\": \"value3\"," +
          "     \"property2\": \"value2\"" +
          "}}" +
          "]";

  private static final Map<String, Map<String, Object>> MAP_SINGLE_VALUE;
  private static final Collection<Map<String, Map<String, Object>>> MAP_MULTIPLE_VALUES;


  static {
    TreeMap<String, Object> configuration_data = new TreeMap<>();
    configuration_data.put("property1", "black");
    configuration_data.put("property2", "white");

    MAP_SINGLE_VALUE = new TreeMap<>();
    MAP_SINGLE_VALUE.put("configuration-type", configuration_data);

    TreeMap<String, Object> configurationType2Properties = new TreeMap<>();
    configurationType2Properties.put("property1", "red");
    configurationType2Properties.put("property2", "yellow");
    configurationType2Properties.put("property3", "green");

    Map<String, Map<String, Object>> configurationType2 = new TreeMap<>();
    configurationType2.put("configuration-type2", configurationType2Properties);

    TreeMap<String, Map<String, Map<String, Object>>> multipleValuesMap = new TreeMap<>();
    multipleValuesMap.put("configuration-type", MAP_SINGLE_VALUE);
    multipleValuesMap.put("configuration-type2", configurationType2);

    MAP_MULTIPLE_VALUES = multipleValuesMap.values();
  }

  @Test
  public void testJSONDeserialize() {
    Map<String, Map<String, Object>> jsonData = new Gson().fromJson(JSON_SINGLE_VALUE,
        new TypeToken<Map<String, Map<String, Object>>>() {
        }.getType());

    KerberosConfigurationDescriptor configuration = new KerberosConfigurationDescriptor(jsonData);

    Assert.assertNotNull(configuration);
    Assert.assertFalse(configuration.isContainer());

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("configuration-type", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("${property-value1}", properties.get("property1"));
    Assert.assertEquals("${property.value2}", properties.get("property2"));
  }

  @Test
  public void testJSONDeserializeMultiple() {
    List<Map<String, Object>> jsonData = new Gson().fromJson(JSON_MULTIPLE_VALUE,
        new TypeToken<List<Map<String, Object>>>() {
        }.getType());


    List<KerberosConfigurationDescriptor> configurations = new ArrayList<>();

    for (Map<String, Object> item : jsonData) {
      configurations.add(new KerberosConfigurationDescriptor(item));
    }

    Assert.assertNotNull(configurations);
    Assert.assertEquals(2, configurations.size());

    for (KerberosConfigurationDescriptor configuration : configurations) {
      Assert.assertFalse(configuration.isContainer());
      String type = configuration.getType();
      Assert.assertEquals(2, configurations.size());

      Map<String, String> properties = configuration.getProperties();

      if ("configuration-type".equals(type)) {
        Assert.assertNotNull(properties);
        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("value1", properties.get("property1"));
        Assert.assertEquals("value2", properties.get("property2"));

      } else if ("configuration-type2".equals(type)) {
        Assert.assertNotNull(properties);
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("value1", properties.get("property1"));
        Assert.assertEquals("value2", properties.get("property2"));
        Assert.assertEquals("value3", properties.get("property3"));
        Assert.assertEquals("value1", configuration.getProperty("property1"));
        Assert.assertEquals("value2", configuration.getProperty("property2"));
        Assert.assertEquals("value3", configuration.getProperty("property3"));
      } else {
        Assert.fail("Missing expected configuration type");
      }
    }
  }

  @Test
  public void testMapDeserialize() {
    KerberosConfigurationDescriptor configuration = new KerberosConfigurationDescriptor(MAP_SINGLE_VALUE);
    Map<String, String> properties = configuration.getProperties();

    Assert.assertNotNull(configuration);
    Assert.assertFalse(configuration.isContainer());
    Assert.assertEquals("configuration-type", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("black", properties.get("property1"));
    Assert.assertEquals("white", properties.get("property2"));
  }

  @Test
  public void testMapDeserializeMultiple() {

    List<KerberosConfigurationDescriptor> configurations = new ArrayList<>();

    for (Map<String, Map<String, Object>> item : MAP_MULTIPLE_VALUES) {
      configurations.add(new KerberosConfigurationDescriptor(item));
    }

    Assert.assertNotNull(configurations);
    Assert.assertEquals(2, configurations.size());

    for (KerberosConfigurationDescriptor configuration : configurations) {
      Assert.assertFalse(configuration.isContainer());
      String type = configuration.getType();
      Map<String, String> properties = configuration.getProperties();

      if ("configuration-type".equals(type)) {
        Assert.assertNotNull(properties);
        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("black", properties.get("property1"));
        Assert.assertEquals("white", properties.get("property2"));
        Assert.assertEquals("black", configuration.getProperty("property1"));
        Assert.assertEquals("white", configuration.getProperty("property2"));
      } else if ("configuration-type2".equals(type)) {
        Assert.assertNotNull(properties);
        Assert.assertEquals(3, properties.size());
        Assert.assertEquals("red", properties.get("property1"));
        Assert.assertEquals("yellow", properties.get("property2"));
        Assert.assertEquals("green", properties.get("property3"));
        Assert.assertEquals("red", configuration.getProperty("property1"));
        Assert.assertEquals("yellow", configuration.getProperty("property2"));
        Assert.assertEquals("green", configuration.getProperty("property3"));
      } else {
        Assert.fail("Missing expected configuration type");
      }
    }
  }

  @Test
  public void testToMap() throws AmbariException {
    KerberosConfigurationDescriptor descriptor = new KerberosConfigurationDescriptor(MAP_SINGLE_VALUE);
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(MAP_SINGLE_VALUE, descriptor.toMap());
  }

  @Test
  public void testUpdate() {
    Map<String, Map<String, Object>> jsonData = new Gson().fromJson(JSON_SINGLE_VALUE,
        new TypeToken<Map<String, Map<String, Object>>>() {
        }.getType());

    KerberosConfigurationDescriptor configuration = new KerberosConfigurationDescriptor(jsonData);
    KerberosConfigurationDescriptor updatedConfiguration = new KerberosConfigurationDescriptor(MAP_SINGLE_VALUE);

    Map<String, String> properties;

    properties = configuration.getProperties();

    Assert.assertEquals("configuration-type", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("${property-value1}", properties.get("property1"));
    Assert.assertEquals("${property.value2}", properties.get("property2"));

    configuration.update(updatedConfiguration);

    properties = configuration.getProperties();

    Assert.assertEquals("configuration-type", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("black", properties.get("property1"));
    Assert.assertEquals("white", properties.get("property2"));

    updatedConfiguration.setType("updated-type");

    configuration.update(updatedConfiguration);

    Assert.assertEquals("updated-type", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("black", properties.get("property1"));
    Assert.assertEquals("white", properties.get("property2"));
  }
}
