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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

@Category({category.KerberosTest.class})
public class KerberosComponentDescriptorTest {
  static final String JSON_VALUE =
      " {" +
          "  \"name\": \"COMPONENT_NAME\"," +
          "  \"identities\": [" +
          KerberosIdentityDescriptorTest.JSON_VALUE +
          "]," +
          "  \"configurations\": [" +
          "    {" +
          "      \"service-site\": {" +
          "        \"service.component.property1\": \"value1\"," +
          "        \"service.component.property2\": \"value2\"" +
          "      }" +
          "    }" +
          "  ]," +
          "  \"auth_to_local_properties\": [" +
          "      component.name.rules1" +
          "    ]" +
          "}";

  static final Map<String, Object> MAP_VALUE;

  static {
    Map<String, Object> identitiesMap = new TreeMap<>();
    identitiesMap.put((String) KerberosIdentityDescriptorTest.MAP_VALUE.get(KerberosIdentityDescriptor.KEY_NAME), KerberosIdentityDescriptorTest.MAP_VALUE);
    identitiesMap.put((String) KerberosIdentityDescriptorTest.MAP_VALUE_ALT.get(KerberosIdentityDescriptor.KEY_NAME), KerberosIdentityDescriptorTest.MAP_VALUE_ALT);
    identitiesMap.put((String) KerberosIdentityDescriptorTest.MAP_VALUE_REFERENCE.get(KerberosIdentityDescriptor.KEY_NAME), KerberosIdentityDescriptorTest.MAP_VALUE_REFERENCE);

    Map<String, Object> serviceSiteProperties = new TreeMap<>();
    serviceSiteProperties.put("service.component.property1", "red");
    serviceSiteProperties.put("service.component.property", "green");

    Map<String, Map<String, Object>> serviceSiteMap = new TreeMap<>();
    serviceSiteMap.put("service-site", serviceSiteProperties);

    TreeMap<String, Map<String, Map<String, Object>>> configurationsMap = new TreeMap<>();
    configurationsMap.put("service-site", serviceSiteMap);

    Collection<String> authToLocalRules = new ArrayList<>();
    authToLocalRules.add("component.name.rules2");

    MAP_VALUE = new TreeMap<>();
    MAP_VALUE.put(KerberosIdentityDescriptor.KEY_NAME, "A_DIFFERENT_COMPONENT_NAME");
    MAP_VALUE.put(KerberosComponentDescriptor.KEY_IDENTITIES, new ArrayList<>(identitiesMap.values()));
    MAP_VALUE.put(KerberosComponentDescriptor.KEY_CONFIGURATIONS, configurationsMap.values());
    MAP_VALUE.put(KerberosComponentDescriptor.KEY_AUTH_TO_LOCAL_PROPERTIES, authToLocalRules);
  }

  static void validateFromJSON(KerberosComponentDescriptor componentDescriptor) {
    Assert.assertNotNull(componentDescriptor);
    Assert.assertTrue(componentDescriptor.isContainer());

    Assert.assertEquals("COMPONENT_NAME", componentDescriptor.getName());

    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities();

    Assert.assertNotNull(identities);
    Assert.assertEquals(1, identities.size());

    Map<String, KerberosConfigurationDescriptor> configurations = componentDescriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("service-site");

    Assert.assertNotNull(configuration);

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("service-site", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("value1", properties.get("service.component.property1"));
    Assert.assertEquals("value2", properties.get("service.component.property2"));

    Set<String> authToLocalProperties = componentDescriptor.getAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(1, authToLocalProperties.size());
    Assert.assertEquals("component.name.rules1", authToLocalProperties.iterator().next());
  }

  static void validateFromMap(KerberosComponentDescriptor componentDescriptor) {
    Assert.assertNotNull(componentDescriptor);
    Assert.assertTrue(componentDescriptor.isContainer());

    Assert.assertEquals("A_DIFFERENT_COMPONENT_NAME", componentDescriptor.getName());

    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities();

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Map<String, KerberosConfigurationDescriptor> configurations = componentDescriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("service-site");

    Assert.assertNotNull(configuration);

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("service-site", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("red", properties.get("service.component.property1"));
    Assert.assertEquals("green", properties.get("service.component.property"));

    Set<String> authToLocalProperties = componentDescriptor.getAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(1, authToLocalProperties.size());
    Assert.assertEquals("component.name.rules2", authToLocalProperties.iterator().next());
  }

  private static void validateUpdatedData(KerberosComponentDescriptor componentDescriptor) {
    Assert.assertNotNull(componentDescriptor);

    Assert.assertEquals("A_DIFFERENT_COMPONENT_NAME", componentDescriptor.getName());

    List<KerberosIdentityDescriptor> identities = componentDescriptor.getIdentities();

    Assert.assertNotNull(identities);
    Assert.assertEquals(3, identities.size());

    Map<String, KerberosConfigurationDescriptor> configurations = componentDescriptor.getConfigurations();

    Assert.assertNotNull(configurations);
    Assert.assertEquals(1, configurations.size());

    KerberosConfigurationDescriptor configuration = configurations.get("service-site");

    Assert.assertNotNull(configuration);

    Map<String, String> properties = configuration.getProperties();

    Assert.assertEquals("service-site", configuration.getType());
    Assert.assertNotNull(properties);
    Assert.assertEquals(3, properties.size());
    Assert.assertEquals("red", properties.get("service.component.property1"));
    Assert.assertEquals("value2", properties.get("service.component.property2"));
    Assert.assertEquals("green", properties.get("service.component.property"));

    Set<String> authToLocalProperties = componentDescriptor.getAuthToLocalProperties();
    Assert.assertNotNull(authToLocalProperties);
    Assert.assertEquals(2, authToLocalProperties.size());
    // guarantee ordering...
    Iterator<String> iterator = new TreeSet<>(authToLocalProperties).iterator();
    Assert.assertEquals("component.name.rules1", iterator.next());
    Assert.assertEquals("component.name.rules2", iterator.next());

  }

  private static KerberosComponentDescriptor createFromJSON() {
    Map<Object, Object> map = new Gson()
        .fromJson(JSON_VALUE, new TypeToken<Map<Object, Object>>() {
        }.getType());
    return new KerberosComponentDescriptor(map);
  }

  private static KerberosComponentDescriptor createFromMap() throws AmbariException {
    return new KerberosComponentDescriptor(MAP_VALUE);
  }

  @Test
  public void testJSONDeserialize() {
    validateFromJSON(createFromJSON());
  }

  @Test
  public void testMapDeserialize() throws AmbariException {
    validateFromMap(createFromMap());
  }

  @Test
  public void testEquals() throws AmbariException {
    Assert.assertTrue(createFromJSON().equals(createFromJSON()));
    Assert.assertFalse(createFromJSON().equals(createFromMap()));
  }

  @Test
  public void testToMap() throws AmbariException {
    Gson gson = new Gson();
    KerberosComponentDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(gson.toJson(MAP_VALUE), gson.toJson(descriptor.toMap()));
  }


  @Test
  public void testUpdate() throws AmbariException {
    KerberosComponentDescriptor componentDescriptor = createFromJSON();
    KerberosComponentDescriptor updatedComponentDescriptor = createFromMap();

    Assert.assertNotNull(componentDescriptor);
    Assert.assertNotNull(updatedComponentDescriptor);

    componentDescriptor.update(updatedComponentDescriptor);

    validateUpdatedData(componentDescriptor);
  }
}
