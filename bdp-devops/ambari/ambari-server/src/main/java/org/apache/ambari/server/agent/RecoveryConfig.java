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

package org.apache.ambari.server.agent;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Recovery config to be sent to the agent
 */
public class RecoveryConfig {

  @SerializedName("components")
  @JsonProperty("components")
  private List<RecoveryConfigComponent> enabledComponents;

  public RecoveryConfig(List<RecoveryConfigComponent> enabledComponents) {
    this.enabledComponents = enabledComponents;
  }

  public List<RecoveryConfigComponent> getEnabledComponents() {
    return enabledComponents == null ? null : Collections.unmodifiableList(enabledComponents);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RecoveryConfig that = (RecoveryConfig) o;

    return enabledComponents != null ? enabledComponents.equals(that.enabledComponents) : that.enabledComponents == null;
  }

  @Override
  public int hashCode() {
    int result = (enabledComponents != null ? enabledComponents.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("RecoveryConfig{");
    buffer.append(", components=").append(enabledComponents);
    buffer.append('}');
    return buffer.toString();
  }
}
