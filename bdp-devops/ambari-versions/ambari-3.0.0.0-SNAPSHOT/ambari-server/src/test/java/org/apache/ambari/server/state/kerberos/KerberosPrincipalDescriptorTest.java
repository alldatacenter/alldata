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

import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

@Category({category.KerberosTest.class})
public class KerberosPrincipalDescriptorTest {
  static final String JSON_VALUE =
      "{" +
          "\"value\": \"service/_HOST@_REALM\"," +
          "\"configuration\": \"service-site/service.component.kerberos.principal\"," +
          "\"type\": \"service\"," +
          "\"local_username\": \"localUser\"" +
          "}";

  private static final String JSON_VALUE_SPARSE =
      "{" +
          "\"value\": \"serviceOther/_HOST@_REALM\"" +
          "}";

  public static final Map<String, Object> MAP_VALUE;
  private static final Map<String, Object> MAP_VALUE_SPARSE;

  static {
    MAP_VALUE = new TreeMap<>();
    MAP_VALUE.put(KerberosPrincipalDescriptor.KEY_VALUE, "user@_REALM");
    MAP_VALUE.put(KerberosPrincipalDescriptor.KEY_CONFIGURATION, "service-site/service.component.kerberos.https.principal");
    MAP_VALUE.put(KerberosPrincipalDescriptor.KEY_TYPE, "user");
    MAP_VALUE.put(KerberosPrincipalDescriptor.KEY_LOCAL_USERNAME, null);

    MAP_VALUE_SPARSE = new TreeMap<>();
    MAP_VALUE_SPARSE.put(KerberosPrincipalDescriptor.KEY_VALUE, "userOther@_REALM");
  }


  static void validateFromJSON(KerberosPrincipalDescriptor principalDescriptor) {
    Assert.assertNotNull(principalDescriptor);
    Assert.assertFalse(principalDescriptor.isContainer());
    Assert.assertEquals("service/_HOST@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.SERVICE, principalDescriptor.getType());
    Assert.assertEquals("localUser", principalDescriptor.getLocalUsername());
  }

  static void validateFromMap(KerberosPrincipalDescriptor principalDescriptor) {
    Assert.assertNotNull(principalDescriptor);
    Assert.assertFalse(principalDescriptor.isContainer());
    Assert.assertEquals("user@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.https.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.USER, principalDescriptor.getType());
    Assert.assertNull(principalDescriptor.getLocalUsername());
  }

  static void validateUpdatedData(KerberosPrincipalDescriptor principalDescriptor) {
    Assert.assertNotNull(principalDescriptor);
    Assert.assertEquals("user@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.https.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.USER, principalDescriptor.getType());
    Assert.assertEquals("localUser", principalDescriptor.getLocalUsername());
  }

  private static KerberosPrincipalDescriptor createFromJSON() {
    Map<?, ?> map = new Gson().fromJson(JSON_VALUE,
        new TypeToken<Map<?, ?>>() {
        }.getType());
    return new KerberosPrincipalDescriptor(map);
  }

  private static KerberosPrincipalDescriptor createFromJSONSparse() {
    Map<?, ?> map = new Gson().fromJson(JSON_VALUE_SPARSE,
        new TypeToken<Map<?, ?>>() {
        }.getType());
    return new KerberosPrincipalDescriptor(map);
  }

  private static KerberosPrincipalDescriptor createFromMap() {
    return new KerberosPrincipalDescriptor(MAP_VALUE);
  }

  private static KerberosPrincipalDescriptor createFromMapSparse() {
    return new KerberosPrincipalDescriptor(MAP_VALUE_SPARSE);
  }

  @Test
  public void testJSONDeserialize() {
    validateFromJSON(createFromJSON());
  }

  @Test
  public void testMapDeserialize() {
    validateFromMap(createFromMap());
  }

  @Test
  public void testEquals() throws AmbariException {
    Assert.assertTrue(createFromJSON().equals(createFromJSON()));
    Assert.assertFalse(createFromJSON().equals(createFromMap()));
  }

  @Test
  public void testToMap() throws AmbariException {
    KerberosPrincipalDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(MAP_VALUE, descriptor.toMap());
  }

  @Test
  public void testUpdate() {
    KerberosPrincipalDescriptor principalDescriptor = createFromJSON();
    KerberosPrincipalDescriptor updatedPrincipalDescriptor = createFromMap();

    Assert.assertNotNull(principalDescriptor);
    Assert.assertNotNull(updatedPrincipalDescriptor);

    principalDescriptor.update(updatedPrincipalDescriptor);

    validateUpdatedData(principalDescriptor);
  }

  @Test
  public void testUpdateSparse() {
    KerberosPrincipalDescriptor principalDescriptor;
    KerberosPrincipalDescriptor updatedPrincipalDescriptor;

    /* ****************************************
     * Test updating a service principal
     * **************************************** */
    principalDescriptor = createFromJSON();
    updatedPrincipalDescriptor = createFromJSONSparse();

    Assert.assertNotNull(principalDescriptor);
    Assert.assertNotNull(updatedPrincipalDescriptor);

    // The original value
    Assert.assertEquals("service/_HOST@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.SERVICE, principalDescriptor.getType());
    Assert.assertEquals("localUser", principalDescriptor.getLocalUsername());

    principalDescriptor.update(updatedPrincipalDescriptor);

    // The updated value
    Assert.assertEquals("serviceOther/_HOST@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.SERVICE, principalDescriptor.getType());
    Assert.assertEquals("localUser", principalDescriptor.getLocalUsername());

    /* ****************************************
     * Test updating a user principal
     * **************************************** */
    principalDescriptor = createFromMap();
    updatedPrincipalDescriptor = createFromMapSparse();

    Assert.assertNotNull(principalDescriptor);
    Assert.assertNotNull(updatedPrincipalDescriptor);

    Assert.assertEquals("user@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.https.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.USER, principalDescriptor.getType());
    Assert.assertNull(principalDescriptor.getLocalUsername());

    principalDescriptor.update(updatedPrincipalDescriptor);

    Assert.assertEquals("userOther@_REALM", principalDescriptor.getValue());
    Assert.assertEquals("service-site/service.component.kerberos.https.principal", principalDescriptor.getConfiguration());
    Assert.assertEquals(KerberosPrincipalType.USER, principalDescriptor.getType());
    Assert.assertNull(principalDescriptor.getLocalUsername());
  }
}
