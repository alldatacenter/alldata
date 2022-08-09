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
public class KerberosKeytabDescriptorTest {
  static final String JSON_VALUE =
      "{" +
          "  \"file\": \"/etc/security/keytabs/${host}/subject.service.keytab\"," +
          "  \"owner\": {" +
          "      \"name\": \"subject\"," +
          "      \"access\": \"rw\"" +
          "  }," +
          "  \"group\": {" +
          "      \"name\": \"hadoop\"," +
          "      \"access\": \"r\"" +
          "  }," +
          "  \"configuration\": \"service-site/service.component.keytab.file\"" +
          "}";

  static final Map<String, Object> MAP_VALUE;

  static {
    TreeMap<String, Object> ownerMap = new TreeMap<>();
    ownerMap.put(KerberosKeytabDescriptor.KEY_ACL_NAME, "root");
    ownerMap.put(KerberosKeytabDescriptor.KEY_ACL_ACCESS, "rw");

    TreeMap<String, Object> groupMap = new TreeMap<>();
    groupMap.put(KerberosKeytabDescriptor.KEY_ACL_NAME, "hadoop");
    groupMap.put(KerberosKeytabDescriptor.KEY_ACL_ACCESS, "r");

    MAP_VALUE = new TreeMap<>();
    MAP_VALUE.put(KerberosKeytabDescriptor.KEY_FILE, "/etc/security/keytabs/subject.service.keytab");
    MAP_VALUE.put(KerberosKeytabDescriptor.KEY_OWNER, ownerMap);
    MAP_VALUE.put(KerberosKeytabDescriptor.KEY_GROUP, groupMap);
    MAP_VALUE.put(KerberosKeytabDescriptor.KEY_CONFIGURATION, "service-site/service2.component.keytab.file");
  }

  static void validateFromJSON(KerberosKeytabDescriptor keytabDescriptor) {
    Assert.assertNotNull(keytabDescriptor);
    Assert.assertFalse(keytabDescriptor.isContainer());

    Assert.assertEquals("/etc/security/keytabs/${host}/subject.service.keytab", keytabDescriptor.getFile());
    Assert.assertEquals("subject", keytabDescriptor.getOwnerName());
    Assert.assertEquals("rw", keytabDescriptor.getOwnerAccess());
    Assert.assertEquals("hadoop", keytabDescriptor.getGroupName());
    Assert.assertEquals("r", keytabDescriptor.getGroupAccess());
    Assert.assertEquals("service-site/service.component.keytab.file", keytabDescriptor.getConfiguration());
  }

  static void validateFromMap(KerberosKeytabDescriptor keytabDescriptor) {
    Assert.assertNotNull(keytabDescriptor);
    Assert.assertFalse(keytabDescriptor.isContainer());

    Assert.assertEquals("/etc/security/keytabs/subject.service.keytab", keytabDescriptor.getFile());
    Assert.assertEquals("root", keytabDescriptor.getOwnerName());
    Assert.assertEquals("rw", keytabDescriptor.getOwnerAccess());
    Assert.assertEquals("hadoop", keytabDescriptor.getGroupName());
    Assert.assertEquals("r", keytabDescriptor.getGroupAccess());
    Assert.assertEquals("service-site/service2.component.keytab.file", keytabDescriptor.getConfiguration());
  }

  static void validateUpdatedData(KerberosKeytabDescriptor keytabDescriptor) {
    Assert.assertNotNull(keytabDescriptor);

    Assert.assertEquals("/etc/security/keytabs/subject.service.keytab", keytabDescriptor.getFile());
    Assert.assertEquals("root", keytabDescriptor.getOwnerName());
    Assert.assertEquals("rw", keytabDescriptor.getOwnerAccess());
    Assert.assertEquals("hadoop", keytabDescriptor.getGroupName());
    Assert.assertEquals("r", keytabDescriptor.getGroupAccess());
    Assert.assertEquals("service-site/service2.component.keytab.file", keytabDescriptor.getConfiguration());
  }

  private static KerberosKeytabDescriptor createFromJSON() {
    Map<?, ?> map = new Gson().fromJson(JSON_VALUE,
        new TypeToken<Map<?, ?>>() {
        }.getType());
    return new KerberosKeytabDescriptor(map);

  }

  private static KerberosKeytabDescriptor createFromMap() {
    return new KerberosKeytabDescriptor(MAP_VALUE);
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
    KerberosKeytabDescriptor descriptor = createFromMap();
    Assert.assertNotNull(descriptor);
    Assert.assertEquals(MAP_VALUE, descriptor.toMap());
  }

  @Test
  public void testUpdate() {
    KerberosKeytabDescriptor keytabDescriptor = createFromJSON();

    KerberosKeytabDescriptor updatedKeytabDescriptor = createFromMap();

    Assert.assertNotNull(keytabDescriptor);
    Assert.assertNotNull(updatedKeytabDescriptor);

    keytabDescriptor.update(updatedKeytabDescriptor);

    validateUpdatedData(keytabDescriptor);
  }
}
