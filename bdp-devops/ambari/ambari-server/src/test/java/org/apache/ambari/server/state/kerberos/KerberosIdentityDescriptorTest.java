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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

@Category({category.KerberosTest.class})
public class KerberosIdentityDescriptorTest {
  static final String JSON_VALUE =
      "{" +
          "  \"name\": \"identity_1\"" +
          "," +
          "  \"principal\":" + KerberosPrincipalDescriptorTest.JSON_VALUE +
          "," +
          "  \"keytab\":" + KerberosKeytabDescriptorTest.JSON_VALUE +
          "," +
          "  \"when\": {\"contains\" : [\"services\", \"HIVE\"]}" +
          "}";

  static final Map<String, Object> MAP_VALUE;
  static final Map<String, Object> MAP_VALUE_ALT;
  static final Map<String, Object> MAP_VALUE_REFERENCE;

  static {
    MAP_VALUE = new TreeMap<>();
    MAP_VALUE.put(KerberosIdentityDescriptor.KEY_NAME, "identity_1");
    MAP_VALUE.put(KerberosIdentityDescriptor.KEY_PRINCIPAL, KerberosPrincipalDescriptorTest.MAP_VALUE);
    MAP_VALUE.put(KerberosIdentityDescriptor.KEY_KEYTAB, KerberosKeytabDescriptorTest.MAP_VALUE);

    MAP_VALUE_ALT = new TreeMap<>();
    MAP_VALUE_ALT.put(KerberosIdentityDescriptor.KEY_NAME, "identity_2");
    MAP_VALUE_ALT.put(KerberosIdentityDescriptor.KEY_PRINCIPAL, KerberosPrincipalDescriptorTest.MAP_VALUE);
    MAP_VALUE_ALT.put(KerberosIdentityDescriptor.KEY_KEYTAB, KerberosKeytabDescriptorTest.MAP_VALUE);

    TreeMap<String, Object> ownerMap = new TreeMap<>();
    ownerMap.put(KerberosKeytabDescriptor.KEY_ACL_NAME, "me");
    ownerMap.put(KerberosKeytabDescriptor.KEY_ACL_ACCESS, "rw");

    TreeMap<String, Object> groupMap = new TreeMap<>();
    groupMap.put(KerberosKeytabDescriptor.KEY_ACL_NAME, "nobody");
    groupMap.put(KerberosKeytabDescriptor.KEY_ACL_ACCESS, "");


    TreeMap<String, Object> keytabMap = new TreeMap<>();
    keytabMap.put(KerberosKeytabDescriptor.KEY_FILE, "/home/user/me/subject.service.keytab");
    keytabMap.put(KerberosKeytabDescriptor.KEY_OWNER, ownerMap);
    keytabMap.put(KerberosKeytabDescriptor.KEY_GROUP, groupMap);
    keytabMap.put(KerberosKeytabDescriptor.KEY_CONFIGURATION, "service-site/me.component.keytab.file");

    MAP_VALUE_REFERENCE = new TreeMap<>();
    MAP_VALUE_REFERENCE.put(KerberosIdentityDescriptor.KEY_NAME, "shared_identity");
    MAP_VALUE_REFERENCE.put(KerberosIdentityDescriptor.KEY_REFERENCE, "/shared");
    MAP_VALUE_REFERENCE.put(KerberosIdentityDescriptor.KEY_KEYTAB, keytabMap);
  }


  static void validateFromJSON(KerberosIdentityDescriptor identityDescriptor) {
    Assert.assertNotNull(identityDescriptor);
    Assert.assertFalse(identityDescriptor.isContainer());

    KerberosPrincipalDescriptorTest.validateFromJSON(identityDescriptor.getPrincipalDescriptor());
    KerberosKeytabDescriptorTest.validateFromJSON(identityDescriptor.getKeytabDescriptor());
  }

  static void validateFromMap(KerberosIdentityDescriptor identityDescriptor) {
    Assert.assertNotNull(identityDescriptor);
    Assert.assertFalse(identityDescriptor.isContainer());

    KerberosPrincipalDescriptorTest.validateFromMap(identityDescriptor.getPrincipalDescriptor());
    KerberosKeytabDescriptorTest.validateFromMap(identityDescriptor.getKeytabDescriptor());
  }

  static void validateUpdatedData(KerberosIdentityDescriptor identityDescriptor) {
    Assert.assertNotNull(identityDescriptor);

    KerberosPrincipalDescriptorTest.validateUpdatedData(identityDescriptor.getPrincipalDescriptor());
    KerberosKeytabDescriptorTest.validateUpdatedData(identityDescriptor.getKeytabDescriptor());
  }

  private static KerberosIdentityDescriptor createFromJSON() {
    Map<?, ?> map = new Gson().fromJson(JSON_VALUE, new TypeToken<Map<?, ?>>() {
    }.getType());
    return new KerberosIdentityDescriptor(map);
  }

  private static KerberosIdentityDescriptor createFromMap() {
    return new KerberosIdentityDescriptor(MAP_VALUE);
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
    KerberosIdentityDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(MAP_VALUE, descriptor.toMap());
  }

  @Test
  public void testUpdate() {
    KerberosIdentityDescriptor identityDescriptor = createFromJSON();
    KerberosIdentityDescriptor updatedIdentityDescriptor = createFromMap();

    Assert.assertNotNull(identityDescriptor);
    Assert.assertNotNull(updatedIdentityDescriptor);

    identityDescriptor.update(updatedIdentityDescriptor);

    validateUpdatedData(identityDescriptor);
  }

  @Test
  public void testShouldInclude() {
    KerberosIdentityDescriptor identityDescriptor = createFromJSON();

    Map<String, Object> context = new TreeMap<>();

    context.put("services", new HashSet<>(Arrays.asList("HIVE", "HDFS", "ZOOKEEPER")));
    Assert.assertTrue(identityDescriptor.shouldInclude(context));

    context.put("services", new HashSet<>(Arrays.asList("NOT_HIVE", "HDFS", "ZOOKEEPER")));
    Assert.assertFalse(identityDescriptor.shouldInclude(context));
  }
}
