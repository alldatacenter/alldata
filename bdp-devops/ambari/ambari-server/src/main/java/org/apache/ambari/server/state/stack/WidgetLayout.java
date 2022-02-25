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

import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.annotations.SerializedName;

public class WidgetLayout {
  @SerializedName("layout_name")
  private String layoutName;
  @SerializedName("section_name")
  private String sectionName;
  @SerializedName("display_name")
  private String displayName;
  @SerializedName("widgetLayoutInfo")
  private List<WidgetLayoutInfo> widgetLayoutInfoList;

  @JsonProperty("layout_name")
  public String getLayoutName() {
    return layoutName;
  }

  public void setLayoutName(String layoutName) {
    this.layoutName = layoutName;
  }

  public String getSectionName() {
    return sectionName;
  }

  @JsonProperty("section_name")
  public void setSectionName(String sectionName) {
    this.sectionName = sectionName;
  }

  @JsonProperty("display_name")
  public String getDisplayName() {
    return displayName;
  }

  @JsonProperty("widgetLayoutInfo")
  public List<WidgetLayoutInfo> getWidgetLayoutInfoList() {
    return widgetLayoutInfoList;
  }

  public void setWidgetLayoutInfoList(List<WidgetLayoutInfo> widgetLayoutInfoList) {
    this.widgetLayoutInfoList = widgetLayoutInfoList;
  }

  @Override
  public String toString() {
    return "WidgetLayout{" +
      "layoutName='" + layoutName + '\'' +
      ", sectionName='" + sectionName + '\'' +
      ", widgetLayoutInfoList=" + widgetLayoutInfoList +
      '}';
  }
}
