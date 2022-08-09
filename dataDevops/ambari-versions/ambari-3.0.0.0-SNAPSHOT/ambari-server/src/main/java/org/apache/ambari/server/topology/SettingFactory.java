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

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Singleton;

/**
 * Creates the Setting object from the parsed blueprint. Expects the settings JSON
 * that was parsed to follow the schema here:
 *
 *   "settings" : [
 *     {
 *     "recovery_settings" : [{
 *     "recovery_enabled" : "true"
 *     }
 *    ]
 *    },
 *    {
 *      "service_settings" : [
 *      {
 *        "name" : "HDFS",
 *        "recovery_enabled" : "false"
 *      },
 *      {
 *        "name" : "TEZ",
 *        "recovery_enabled" : "false"
 *      }
 *      ]
 *     },
 *     {
 *       "component_settings" : [
 *       {
 *         "name" : "DATANODE",
 *         "recovery_enabled" : "true"
 *       }
 *       ]
 *     }
 *    ]
 */
@Singleton
public class SettingFactory {
  /**
   * Attempts to build the list of settings in the following format:
   * setting_name1-->[propertyName1-->propertyValue1, propertyName2-->propertyValue2]
   * @param blueprintSetting
   * @return
   */
  public static Setting getSetting(Collection<Map<String, Object>> blueprintSetting) {
    Map<String, Set<HashMap<String, String>>> properties = new HashMap<>();
    Setting setting = new Setting(properties);

    if (blueprintSetting != null) {
      for (Map<String, Object> settingMap : blueprintSetting) {
        for (Map.Entry<String, Object> entry : settingMap.entrySet()) {
          final String[] propertyNames = entry.getKey().split("/");
          Set<HashMap<String, String>> settingValue;
          if (entry.getValue() instanceof Set) {
            settingValue = (HashSet<HashMap<String, String>>)entry.getValue();
          }
          else if (propertyNames.length > 1){
            HashMap<String, String> property = new HashMap<>();
            property.put(propertyNames[1], String.valueOf(entry.getValue()));
            settingValue = properties.get(propertyNames[0]);
            if (settingValue == null) {
              settingValue = new HashSet<>();
            }
            settingValue.add(property);
          }
          else {
            throw new IllegalArgumentException("Invalid setting schema: " + String.valueOf(entry.getValue()));
          }
          properties.put(propertyNames[0], settingValue);
        }
      }
    }

    return setting;
  }
}
