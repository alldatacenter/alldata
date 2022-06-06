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
 * BlueprintConfigEntityPK unit tests.
 */
public class BlueprintConfigEntityPKTest {
  @Test
  public void testSetGetBlueprintName() {
    BlueprintConfigEntityPK pk = new BlueprintConfigEntityPK();
    pk.setBlueprintName("foo");
    assertEquals("foo", pk.getBlueprintName());
  }

  @Test
  public void testSetGetType() {
    BlueprintConfigEntityPK pk = new BlueprintConfigEntityPK();
    pk.setType("foo");
    assertEquals("foo", pk.getType());
  }

  @Test
  public void testEquals() {
    BlueprintConfigEntityPK pk = new BlueprintConfigEntityPK();
    BlueprintConfigEntityPK pk2 = new BlueprintConfigEntityPK();


    pk.setBlueprintName("foo");
    pk.setType("core-site");
    pk2.setBlueprintName("foo");
    pk2.setType("core-site");
    assertEquals(pk, pk2);
    assertEquals(pk2, pk);

    pk.setBlueprintName("foo2");
    assertFalse(pk.equals(pk2));
    assertFalse(pk2.equals(pk));

    pk2.setBlueprintName("foo2");
    assertEquals(pk, pk2);
    assertEquals(pk2, pk);

    pk.setType("other-type");
    assertFalse(pk.equals(pk2));
    assertFalse(pk2.equals(pk));
  }
}
