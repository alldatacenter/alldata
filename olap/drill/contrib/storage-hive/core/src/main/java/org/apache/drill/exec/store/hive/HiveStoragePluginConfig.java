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
package org.apache.drill.exec.store.hive;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.apache.drill.common.logical.StoragePluginConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(HiveStoragePluginConfig.NAME)
public class HiveStoragePluginConfig extends StoragePluginConfig {

  public static final String NAME = "hive";

  private final Map<String, String> configProps;

  @JsonCreator
  public HiveStoragePluginConfig(@JsonProperty("configProps")
                                 // previously two names were allowed due to incorrectly written ser / der logic
                                 // allowing to use both during deserialization for backward compatibility
                                 @JsonAlias("config") Map<String, String> configProps) {
    this.configProps = configProps;
  }

  @JsonProperty
  public Map<String, String> getConfigProps() {
    return configProps;
  }

  @Override
  public int hashCode() {
    return configProps != null ? configProps.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HiveStoragePluginConfig that = (HiveStoragePluginConfig) o;

    if (configProps != null ? !configProps.equals(that.configProps) : that.configProps != null) {
      return false;
    }

    return true;
  }

}
