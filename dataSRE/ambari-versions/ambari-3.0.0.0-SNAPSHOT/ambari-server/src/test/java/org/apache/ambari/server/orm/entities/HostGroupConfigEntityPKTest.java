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

package org.apache.ambari.server.orm.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * HostGroupConfigEntityPK unit tests.
 */
public class HostGroupConfigEntityPKTest {
  @Test
  public void testSetGetBlueprintName() {
    HostGroupConfigEntityPK pk = new HostGroupConfigEntityPK();
    pk.setBlueprintName("foo");
    assertEquals("foo", pk.getBlueprintName());
  }

  @Test
  public void testSetGetHostGroupName() {
    HostGroupConfigEntityPK pk = new HostGroupConfigEntityPK();
    pk.setHostGroupName("foo");
    assertEquals("foo", pk.getHostGroupName());
  }

  @Test
  public void testSetGetType() {
    HostGroupConfigEntityPK pk = new HostGroupConfigEntityPK();
    pk.setType("testType");
    assertEquals("testType", pk.getType());
  }

  @Test
  public void testHashcode() {
    HostGroupConfigEntityPK pk1 = new HostGroupConfigEntityPK();
    HostGroupConfigEntityPK pk2 = new HostGroupConfigEntityPK();

    pk1.setType("foo");
    pk2.setType("foo");
    pk1.setBlueprintName("bp");
    pk2.setBlueprintName("bp");
    pk1.setHostGroupName("hg");
    pk2.setHostGroupName("hg");

    assertEquals(pk1.hashCode(), pk2.hashCode());
  }

  @Test
  public void testEquals() {
    HostGroupConfigEntityPK pk1 = new HostGroupConfigEntityPK();
    HostGroupConfigEntityPK pk2 = new HostGroupConfigEntityPK();

    pk1.setType("foo");
    pk2.setType("foo");
    pk1.setBlueprintName("bp");
    pk2.setBlueprintName("bp");
    pk1.setHostGroupName("hg");
    pk2.setHostGroupName("hg");

    assertEquals(pk1, pk2);

    pk1.setType("something_else");
    assertFalse(pk1.equals(pk2));

    pk2.setType("something_else");
    assertEquals(pk1, pk2);
    pk1.setType("other_type");
    assertFalse(pk1.equals(pk2));

    pk2.setType("other_type");
    assertEquals(pk1, pk2);
    pk1.setHostGroupName("hg2");
    assertFalse(pk1.equals(pk2));
  }
}
