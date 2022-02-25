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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Test the SettingFactory class
 */
public class SettingFactoryTest {
  /**
   * Test collection of recovery_settings
   */
  @Test
  public void testGetSettingWithSetOfProperties() {
    SettingFactory settingFactory = new SettingFactory();
    Map<String, Set<HashMap<String, String>>> properties;

    Setting setting = settingFactory.getSetting(createSettingWithSetOfProperties());
    Set<HashMap<String, String>> propertyValues = setting.getSettingValue(Setting.SETTING_NAME_RECOVERY_SETTINGS);
    assertEquals(propertyValues.size(), 1);

    assertEquals(propertyValues.iterator().next().get(Setting.SETTING_NAME_RECOVERY_ENABLED), "true");
  }

  /**
   * Test single recovery_settings at root level
   */
  @Test
  public void testGetSettingWithoutSetOfProperties() {
    SettingFactory settingFactory = new SettingFactory();
    Map<String, Set<HashMap<String, String>>> properties;

    Setting setting = settingFactory.getSetting(createSettingWithoutSetOfProperties());
    Set<HashMap<String, String>> propertyValues = setting.getSettingValue(Setting.SETTING_NAME_RECOVERY_SETTINGS);
    assertEquals(propertyValues.size(), 1);

    assertEquals(propertyValues.iterator().next().get(Setting.SETTING_NAME_RECOVERY_ENABLED), "true");
  }

  /**
   * {
   *   "recovery_settings":[
   *     {
   *     "recovery_enabled":"true"
   *     }
   *   ]
   * }
   *
   * @return
   */
  private Collection<Map<String, Object>> createSettingWithSetOfProperties() {

    Set<HashMap<String, String>> setting1 = new HashSet<>();
    Set<HashMap<String, String>> setting2 = new HashSet<>();

    // Setting 1: Property1
    HashMap<String, String> setting1Properties1 = new HashMap<>();
    setting1Properties1.put(Setting.SETTING_NAME_RECOVERY_ENABLED, "true");
    setting1.add(setting1Properties1);

    // Setting 2: Property1 and Property2
    HashMap<String, String> setting2Properties1 = new HashMap<>();
    setting2Properties1.put(Setting.SETTING_NAME_NAME, "HDFS");
    setting2Properties1.put(Setting.SETTING_NAME_RECOVERY_ENABLED, "false");

    HashMap<String, String> setting2Properties2 = new HashMap<>();
    setting2Properties2.put(Setting.SETTING_NAME_NAME, "TEZ");
    setting2Properties2.put(Setting.SETTING_NAME_RECOVERY_ENABLED, "false");

    setting2.add(setting2Properties1);
    setting2.add(setting2Properties2);

    Collection<Map<String, Object>> setting = new ArrayList<>();
    Map<String, Object> properties;
    properties = new HashMap<>();
    properties.put(Setting.SETTING_NAME_RECOVERY_SETTINGS, setting1);
    setting.add(properties);

    properties = new HashMap<>();
    properties.put(Setting.SETTING_NAME_SERVICE_SETTINGS, setting2);
    setting.add(properties);

    return setting;
  }

  /**
   * {
   *   "recovery_settings":
   *     {
   *     "recovery_enabled":"true"
   *     }
   * }
   *
   * @return
   */
  private Collection<Map<String, Object>> createSettingWithoutSetOfProperties() {
    // Setting 2: Property1 and Property2
    HashMap<String, String> setting2Properties1 = new HashMap<>();
    setting2Properties1.put(Setting.SETTING_NAME_NAME, "HDFS");
    setting2Properties1.put(Setting.SETTING_NAME_RECOVERY_ENABLED, "false");

    HashMap<String, String> setting2Properties2 = new HashMap<>();
    setting2Properties2.put(Setting.SETTING_NAME_NAME, "TEZ");
    setting2Properties2.put(Setting.SETTING_NAME_RECOVERY_ENABLED, "false");

    Set<HashMap<String, String>> setting2 = new HashSet<>();
    setting2.add(setting2Properties1);
    setting2.add(setting2Properties2);

    Collection<Map<String, Object>> setting = new ArrayList<>();
    Map<String, Object> properties;
    properties = new HashMap<>();
    properties.put(Setting.SETTING_NAME_RECOVERY_SETTINGS + "/" + Setting.SETTING_NAME_RECOVERY_ENABLED, "true");
    setting.add(properties);

    properties = new HashMap<>();
    properties.put(Setting.SETTING_NAME_SERVICE_SETTINGS, setting2);
    setting.add(properties);

    return setting;
  }
}
