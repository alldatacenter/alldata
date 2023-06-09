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

package org.apache.drill.exec.store.splunk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.planner.logical.DrillTableSelection;

@JsonTypeName("splunk-scan-spec")
public class SplunkScanSpec implements DrillTableSelection {
  private final String pluginName;
  private final String indexName;
  private final SplunkPluginConfig config;

  @JsonCreator
  public SplunkScanSpec(@JsonProperty("pluginName") String pluginName,
                        @JsonProperty("indexName") String indexName,
                        @JsonProperty("config") SplunkPluginConfig config) {
    this.pluginName = pluginName;
    this.indexName = indexName;
    this.config = config;
  }

  @JsonProperty("pluginName")
  public String getPluginName() { return pluginName; }

  @JsonProperty("indexName")
  public String getIndexName() { return indexName; }

  @JsonProperty("config")
  public SplunkPluginConfig getConfig() { return config; }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("config", config)
      .field("schemaName", pluginName)
      .field("indexName", indexName)
      .toString();
  }

  @Override
  public String digest() {
    return toString();
  }
}
