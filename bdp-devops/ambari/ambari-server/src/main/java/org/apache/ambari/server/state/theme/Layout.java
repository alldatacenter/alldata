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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Layout{

	@JsonProperty("name")
	private String name;

	@JsonProperty("tabs")
	private List<Tab> tabs;

  @ApiModelProperty(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @ApiModelProperty(name = "tabs")
  public List<Tab> getTabs() {
    return tabs;
  }

  public void setTabs(List<Tab> tabs) {
    this.tabs = tabs;
  }


  public void mergeWithParent(Layout parentLayout) {
    //merge Tabs only due to theme structure
    tabs = mergeTabs(parentLayout.tabs, tabs);
  }

  private List<Tab> mergeTabs(List<Tab> parentTabs, List<Tab> childTabs) {
    Map<String, Tab> mergedTabs = new HashMap<>();
    for (Tab parentTab : parentTabs) {
      mergedTabs.put(parentTab.getName(), parentTab);
    }

    for (Tab childTab : childTabs) {
      if (childTab.getName() != null) {
        if (childTab.getDisplayName() == null && childTab.getTabLayout() == null) {
          mergedTabs.remove(childTab.getName());
        } else {
          Tab parentTab = mergedTabs.get(childTab.getName());
          childTab.mergeWithParent(parentTab);
          mergedTabs.put(childTab.getName(), childTab);
        }
      }
    }
    return new ArrayList<>(mergedTabs.values());
  }

}