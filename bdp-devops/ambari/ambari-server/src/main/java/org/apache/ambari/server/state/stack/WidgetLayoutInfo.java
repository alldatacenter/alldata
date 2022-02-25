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
package org.apache.ambari.server.state.stack;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.annotations.SerializedName;

public class WidgetLayoutInfo {
  @SerializedName("widget_name")
  private String widgetName;
  @SerializedName("default_section_name")
  private String defaultSectionName = null;
  @SerializedName("description")
  private String description;
  @SerializedName("widget_type")
  private String type;
  @SerializedName("is_visible")
  private boolean visibility;
  @SerializedName("metrics")
  private List<Map<String, String>> metricsInfo;
  @SerializedName("values")
  private List<Map<String, String>> values;
  @SerializedName("properties")
  private Map<String, String> properties;

  @JsonProperty("widget_name")
  public String getWidgetName() {
    return widgetName;
  }

  public void setWidgetName(String widgetName) {
    this.widgetName = widgetName;
  }

  @JsonProperty("default_section_name")
  public String getDefaultSectionName() {
    return defaultSectionName;
  }

  public void setDefaultSectionName(String defaultSectionName) {
    this.defaultSectionName = defaultSectionName;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("widget_type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("is_visible")
  public boolean isVisible() {
    return visibility;
  }

  public void setVisibility(boolean visibility) {
    this.visibility = visibility;
  }

  @JsonProperty("metrics")
  public List<Map<String, String>> getMetricsInfo() {
    return metricsInfo;
  }

  public void setMetricsInfo(List<Map<String, String>> metricsInfo) {
    this.metricsInfo = metricsInfo;
  }

  @JsonProperty("values")
  public List<Map<String, String>> getValues() {
    return values;
  }

  public void setValues(List<Map<String, String>> values) {
    this.values = values;
  }

  @JsonProperty("properties")
  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
