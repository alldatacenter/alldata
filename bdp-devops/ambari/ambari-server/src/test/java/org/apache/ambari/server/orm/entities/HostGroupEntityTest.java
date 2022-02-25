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

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

/**
 * HostGroupEntity unit tests.
 */
public class HostGroupEntityTest {
  @Test
  public void testSetGetName() {
    HostGroupEntity entity = new HostGroupEntity();
    entity.setName("foo");
    assertEquals("foo", entity.getName());
  }

  @Test
  public void testSetGetBlueprintName() {
    HostGroupEntity entity = new HostGroupEntity();
    entity.setBlueprintName("foo");
    assertEquals("foo", entity.getBlueprintName());
  }

  @Test
  public void testSetGetCardinality() {
    HostGroupEntity entity = new HostGroupEntity();
    entity.setCardinality("foo");
    assertEquals("foo", entity.getCardinality());
  }

  @Test
  public void testSetGetBlueprintEntity() {
    HostGroupEntity entity = new HostGroupEntity();
    BlueprintEntity bp = new BlueprintEntity();
    entity.setBlueprintEntity(bp);
    assertSame(bp, entity.getBlueprintEntity());
  }

  @Test
  public void testSetGetComponents() {
    HostGroupEntity entity = new HostGroupEntity();
    Collection<HostGroupComponentEntity> components = Collections.emptyList();
    entity.setComponents(components);
    assertSame(components, entity.getComponents());
  }
}
