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
package org.apache.ambari.server.state;

import java.util.HashMap;
import java.util.Map;

public class RefreshCommandConfiguration {

  public static final String RELOAD_CONFIGS = "reload_configs";
  public static final String REFRESH_CONFIGS = "refresh_configs";

  private Map<String, Map<String, String>> propertyComponentCommandMap;

  public RefreshCommandConfiguration() {
  }

  private String findKey(String propertyName) {
    for (String keyName : propertyComponentCommandMap.keySet()) {
      if (propertyName.startsWith(keyName)) {
        return keyName;
      }
    }
    return null;
  }

  /**
   * If no command is defined for a component then the default command will be REFRESH_CONFIGS in case of a client component or
   * if there's only one command defined for an another component. This is because if RELOAD_CONFIGS is defined for NAMENODE then
   * presumably other dependent components will need just a refresh.
   */
  public String getRefreshCommandForComponent(ServiceComponentHost sch, String propertyName) {
    if (sch.isClientComponent()) {
      return REFRESH_CONFIGS;
    }
    String keyName = findKey(propertyName);
    Map<String, String> componentCommandMap = propertyComponentCommandMap.get(keyName);
    if (componentCommandMap != null) {
      String commandForComponent = componentCommandMap.get(sch.getServiceComponentName());
      if (commandForComponent != null) {
        return commandForComponent;
      } else if(componentCommandMap.size() == 1) {
        return REFRESH_CONFIGS;
      }
    }
    return null;
  }

  public void addRefreshCommands(Map<String, Map<String, String>> refreshCommands) {
    if (propertyComponentCommandMap == null) {
      propertyComponentCommandMap = new HashMap();
    }
    propertyComponentCommandMap.putAll(refreshCommands);
  }

}
