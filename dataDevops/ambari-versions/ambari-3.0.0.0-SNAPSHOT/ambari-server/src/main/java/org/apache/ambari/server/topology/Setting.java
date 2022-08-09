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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Setting {
  /**
   * Settings for this configuration instance
   */
  private Map<String, Set<HashMap<String, String>>> properties;

  public static final String SETTING_NAME_RECOVERY_SETTINGS = "recovery_settings";

  public static final String SETTING_NAME_SERVICE_SETTINGS = "service_settings";

  public static final String SETTING_NAME_COMPONENT_SETTINGS = "component_settings";

  public static final String SETTING_NAME_DEPLOYMENT_SETTINGS = "deployment_settings";

  public static final String SETTING_NAME_RECOVERY_ENABLED = "recovery_enabled";

  public static final String SETTING_NAME_SKIP_FAILURE = "skip_failure";

  public static final String SETTING_NAME_NAME = "name";

  public static final String SETTING_NAME_REPOSITORY_SETTINGS = "repository_settings";

  /**
   * When specified under the "service_settings" section, it indicates whether credential store
   * use is enabled for that service. Value is "true" or "false". Specify a value of "true"
   * only if the stack definition for the service has a credential_store_supported value of "true".
   * If credential_store_enabled is not specified, value will be taken as null and default value
   * will be picked up from the stack definition, if available.
   *   <pre>
   *     {@code
   *       {
   *         "service_settings" : [
   *           { "name" : "RANGER",
   *             "recovery_enabled" : "true",
   *             "credential_store_enabled" : "true"
   *           },
   *           :
   *       }
   *     }
   *   </pre>
   */
  public static final String SETTING_NAME_CREDENTIAL_STORE_ENABLED = "credential_store_enabled";

  /**
   * Settings.
   *
   * @param properties  setting name-->Set(property name-->property value)
   */
  public Setting(Map<String, Set<HashMap<String, String>>> properties) {

    this.properties = properties;
  }

  /**
   * Get the properties for this instance.
   *
   * @return map of properties for this settings instance keyed by setting name.
   */
  public Map<String, Set<HashMap<String, String>>> getProperties() {
    return properties;
  }

  /**
   * Get the setting properties for a specified setting name.
   *
   * @param settingName
   * @return Set of Map of properties.
   */
  public Set<HashMap<String, String>> getSettingValue(String settingName) {
    if (properties.containsKey(settingName)) {
      return properties.get(settingName);
    }

    return Collections.emptySet();
  }
}
