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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.ApiModel;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThemeConfiguration implements ApiModel {
	@JsonProperty("placement")
	private Placement placement;

	@JsonProperty("widgets")
	private List<WidgetEntry> widgets;

	@JsonProperty("layouts")
	private List<Layout> layouts;

  @ApiModelProperty(name = "placement")
  public Placement getPlacement() {
    return placement;
  }

  public void setPlacement(Placement placement) {
    this.placement = placement;
  }

  @ApiModelProperty(name = "widgets")
  public List<WidgetEntry> getWidgets() {
    return widgets;
  }

  public void setWidgets(List<WidgetEntry> widgets) {
    this.widgets = widgets;
  }

  @ApiModelProperty(name = "layouts")
  public List<Layout> getLayouts() {
    return layouts;
  }

  public void setLayouts(List<Layout> layouts) {
    this.layouts = layouts;
  }

  public void mergeWithParent(ThemeConfiguration parent) {
    if (parent == null) {
      return;
    }

    if (placement == null) {
      placement = parent.placement;
    } else {
      placement.mergeWithParent(parent.placement);
    }

    if (widgets == null) {
      widgets = parent.widgets;
    } else if (parent.widgets != null) {
      widgets = mergeWidgets(parent.widgets, widgets);
    }

    if (layouts == null) {
      layouts = parent.layouts;
    } else if (parent.layouts != null) {
      layouts = mergeLayouts(parent.layouts, layouts);
    }


  }

  private List<Layout> mergeLayouts(List<Layout> parentLayouts, List<Layout> childLayouts) {
    Map<String, Layout> mergedLayouts = new LinkedHashMap<>();

    for (Layout parentLayout : parentLayouts) {
      mergedLayouts.put(parentLayout.getName(), parentLayout);
    }

    for (Layout childLayout : childLayouts) {
      if (childLayout.getName() != null) { //skip invalid entries
        if (childLayout.getTabs() == null) {
          mergedLayouts.remove(childLayout.getName());
        } else {
          Layout parentLayout = mergedLayouts.get(childLayout.getName());
          childLayout.mergeWithParent(parentLayout);
          mergedLayouts.put(childLayout.getName(), childLayout);
        }
      }
    }

    return new ArrayList<>(mergedLayouts.values());

  }

  private List<WidgetEntry> mergeWidgets(List<WidgetEntry> parentWidgets, List<WidgetEntry> childWidgets) {
    Map<String, WidgetEntry> mergedWidgets = new LinkedHashMap<>();
    for (WidgetEntry widgetEntry : parentWidgets) {
      mergedWidgets.put(widgetEntry.getConfig(), widgetEntry);
    }

    for (WidgetEntry widgetEntry : childWidgets) {
      if (widgetEntry.getConfig() != null) { //skip invalid entries
        if (widgetEntry.getWidget() == null) {
          mergedWidgets.remove(widgetEntry.getConfig());
        } else {
          mergedWidgets.put(widgetEntry.getConfig(), widgetEntry);
        }
      }

    }

    return new ArrayList<>(mergedWidgets.values());
  }
}
