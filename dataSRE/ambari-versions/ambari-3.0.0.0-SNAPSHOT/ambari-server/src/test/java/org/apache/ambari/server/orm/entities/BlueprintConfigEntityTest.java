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
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * BlueprintConfigEntity unit tests.
 */
public class BlueprintConfigEntityTest {
  @Test
  public void testSetGetType() {
    BlueprintConfigEntity entity = new BlueprintConfigEntity();
    entity.setType("foo");
    assertEquals("foo", entity.getType());
  }

  @Test
  public void testSetGetBlueprintEntity() {
    BlueprintEntity bp = new BlueprintEntity();

    BlueprintConfigEntity entity = new BlueprintConfigEntity();
    entity.setBlueprintEntity(bp);
    assertSame(bp, entity.getBlueprintEntity());
  }

  @Test
  public void testSetGetBlueprintName() {
    BlueprintConfigEntity entity = new BlueprintConfigEntity();
    entity.setBlueprintName("foo");
    assertEquals("foo", entity.getBlueprintName());
  }

  @Test
  public void testSetGetConfigData() {
    BlueprintConfigEntity entity = new BlueprintConfigEntity();
    entity.setConfigData("foo");
    assertEquals("foo", entity.getConfigData());
  }
}
