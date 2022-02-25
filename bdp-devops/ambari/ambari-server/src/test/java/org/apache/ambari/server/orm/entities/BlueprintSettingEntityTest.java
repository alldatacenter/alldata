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
 * Test all the methods of BlueprintSettingEntity
 */
public class BlueprintSettingEntityTest {
  /**
   * Verify setSettingName()
   */
  @Test
  public void testSetGetSettingName() {
    BlueprintSettingEntity entity = new BlueprintSettingEntity();
    entity.setSettingName("component_settings");
    assertEquals("component_settings", entity.getSettingName());
  }

  /**
   * Verify referencing BlueprintEntity object
   */
  @Test
  public void testSetGetBlueprintEntity() {
    BlueprintEntity bp = new BlueprintEntity();

    BlueprintSettingEntity entity = new BlueprintSettingEntity();
    entity.setBlueprintEntity(bp);
    assertSame(bp, entity.getBlueprintEntity());
  }

  /**
   * Verify setBlueprintName()
   */
  @Test
  public void testSetGetBlueprintName() {
    BlueprintSettingEntity entity = new BlueprintSettingEntity();
    entity.setBlueprintName("bp2");
    assertEquals("bp2", entity.getBlueprintName());
  }

  /**
   * Verify setSettingData()
   */
  @Test
  public void testSetGetSettingData() {
    BlueprintSettingEntity entity = new BlueprintSettingEntity();
    String settingData = "[{'recovery_settings':[{'recovery_enabled':'true'}]}]";
    entity.setSettingData(settingData);
    assertEquals(settingData, entity.getSettingData());
  }
}
