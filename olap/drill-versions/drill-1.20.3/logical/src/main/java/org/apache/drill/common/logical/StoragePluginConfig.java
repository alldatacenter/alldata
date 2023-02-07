/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.logical;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public abstract class StoragePluginConfig {

  // DO NOT include enabled status in equality and hash
  // comparisons; doing so will break the plugin registry.
  private Boolean enabled;

  /**
   * Check for enabled status of the plugin
   *
   * @return true, when enabled. False, when disabled or status is absent
   */
  public boolean isEnabled() {
    return enabled != null && enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Allows to check whether the enabled status is present in config
   *
   * @return true if enabled status is present, false otherwise
   */
  @JsonIgnore
  public boolean isEnabledStatusPresent() {
    return enabled != null;
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  public String getValue(String key) {
    return null;
  }
}
