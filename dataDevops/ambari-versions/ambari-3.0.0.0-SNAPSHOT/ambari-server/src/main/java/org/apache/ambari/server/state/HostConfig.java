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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Objects;

/**
 * Data structure that hangs off of the Host and points to what tags are
 * applicable to a config derived at the host-level
 */

public class HostConfig {
  private final Map<Long, String> configGroupOverrides = new ConcurrentHashMap<>();
  private String defaultVersionTag;

  public HostConfig() {
  }

  @JsonProperty("default")
  public String getDefaultVersionTag() {
    return defaultVersionTag;
  }

  public void setDefaultVersionTag(String defaultVersionTag) {
    this.defaultVersionTag = defaultVersionTag;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("overrides")
  public Map<Long, String> getConfigGroupOverrides() {
    return configGroupOverrides;
  }

  @Override
  public int hashCode(){
    return Objects.hashCode(defaultVersionTag.hashCode(), configGroupOverrides.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (defaultVersionTag != null) {
      sb.append("default = ").append(defaultVersionTag);
    }
    if (!configGroupOverrides.isEmpty()) {
      sb.append(", overrides = [ ");
      int i = 0;
      for (Map.Entry<Long, String> entry : configGroupOverrides.entrySet()) {
        if (i++ != 0) {
          sb.append(", ");
        }
        sb.append(entry.getKey()).append(" : ").append(entry.getValue());
      }
      sb.append("]");
    }
    sb.append("}");

    return sb.toString();
  }
}
