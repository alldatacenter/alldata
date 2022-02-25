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

package org.apache.ambari.server.state.theme;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subsection {
	@JsonProperty("row-index")
	private String rowIndex;
	@JsonProperty("name")
	private String name;
  @JsonProperty("display-name")
	private String displayName;
	@JsonProperty("column-span")
	private String columnSpan;
	@JsonProperty("row-span")
	private String rowSpan;
	@JsonProperty("column-index")
	private String columnIndex;
  @JsonProperty("border")
	private String border;
  @JsonProperty("left-vertical-splitter")
  private Boolean leftVerticalSplitter;
  @JsonProperty("depends-on")
  private List<ConfigCondition> dependsOn;
  @JsonProperty("subsection-tabs")
  private List<SubsectionTab> subsectionTabs;


  @ApiModelProperty( name = "row-index")
  public String getRowIndex() {
    return rowIndex;
  }

  public void setRowIndex(String rowIndex) {
    this.rowIndex = rowIndex;
  }

  @ApiModelProperty( name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @ApiModelProperty( name = "column-span")
  public String getColumnSpan() {
    return columnSpan;
  }

  public void setColumnSpan(String columnSpan) {
    this.columnSpan = columnSpan;
  }

  @ApiModelProperty( name = "row-span")
  public String getRowSpan() {
    return rowSpan;
  }

  public void setRowSpan(String rowSpan) {
    this.rowSpan = rowSpan;
  }

  @ApiModelProperty( name = "column-index")
  public String getColumnIndex() {
    return columnIndex;
  }

  public void setColumnIndex(String columnIndex) {
    this.columnIndex = columnIndex;
  }

  @ApiModelProperty( name = "display-name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @ApiModelProperty( name = "border")
  public String getBorder() {
    return border;
  }

  public void setBorder(String border) {
    this.border = border;
  }

  @ApiModelProperty( name = "left-vertical-splitter")
  public Boolean getLeftVerticalSplitter() {
    return leftVerticalSplitter;
  }

  public void setLeftVerticalSplitter(Boolean leftVerticalSplitter) {
    this.leftVerticalSplitter = leftVerticalSplitter;
  }

  @ApiModelProperty( name = "depends-on")
  public List<ConfigCondition> getDependsOn() {
    return dependsOn;
  }

  public void setDependsOn(List<ConfigCondition> dependsOn) {
    this.dependsOn = dependsOn;
  }

  @ApiModelProperty( name = "subsection-tab")
  public List<SubsectionTab> getSubsectionTabs() {
    return subsectionTabs;
  }

  public void setSubsectionTabs(List<SubsectionTab> subsectionTabs) {
    this.subsectionTabs = subsectionTabs;
  }

  @ApiModelProperty( name = "removed")
  public boolean isRemoved() {
    return rowIndex == null && rowSpan == null && columnIndex == null && columnSpan == null && dependsOn == null && subsectionTabs == null;
  }

  public void mergeWithParent(Subsection parent) {
    if (rowSpan == null) {
      rowSpan = parent.rowSpan;
    }
    if (rowIndex == null) {
      rowIndex = parent.rowIndex;
    }
    if (columnSpan == null) {
      columnSpan = parent.columnSpan;
    }
    if (columnIndex == null) {
      columnIndex = parent.columnIndex;
    }
    if (displayName == null) {
      displayName = parent.displayName;
    }
    if (border == null) {
      border = parent.border;
    }
    if (leftVerticalSplitter == null) {
      leftVerticalSplitter = parent.leftVerticalSplitter;
    }
    if (dependsOn == null) {
      dependsOn = parent.dependsOn;
    }
    if (subsectionTabs == null) {
      subsectionTabs = parent.subsectionTabs;
    }
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class SubsectionTab {
    @JsonProperty("name")
    private String name;
    @JsonProperty("display-name")
    private String displayName;
    @JsonProperty("depends-on")
    private List<ConfigCondition> dependsOn;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public List<ConfigCondition> getDependsOn() {
      return dependsOn;
    }

    public void setDependsOn(List<ConfigCondition> dependsOn) {
      this.dependsOn = dependsOn;
    }

  }
}