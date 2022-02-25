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
 * HostGroupComponentEntity unit tests.
 */
public class HostGroupComponentEntityTest {
  @Test
  public void testSetGetName() {
    HostGroupComponentEntity entity = new HostGroupComponentEntity();
    entity.setName("foo");
    assertEquals("foo", entity.getName());
  }

  @Test
  public void testSetGetHostGroupEntity() {
    HostGroupComponentEntity entity = new HostGroupComponentEntity();
    HostGroupEntity hg = new HostGroupEntity();
    entity.setHostGroupEntity(hg);
    assertSame(hg, entity.getHostGroupEntity());
  }

  @Test
  public void testSetGetHostGroupName() {
    HostGroupComponentEntity entity = new HostGroupComponentEntity();
    entity.setHostGroupName("foo");
    assertEquals("foo", entity.getHostGroupName());
  }

  @Test
  public void testSetGetBlueprintName() {
    HostGroupComponentEntity entity = new HostGroupComponentEntity();
    entity.setBlueprintName("foo");
    assertEquals("foo", entity.getBlueprintName());
  }

  @Test
  public void testSetGetProvisionAction() {
    HostGroupComponentEntity entity = new HostGroupComponentEntity();
    entity.setProvisionAction("INSTALL_ONLY");
    assertEquals("INSTALL_ONLY", entity.getProvisionAction());
  }

}
