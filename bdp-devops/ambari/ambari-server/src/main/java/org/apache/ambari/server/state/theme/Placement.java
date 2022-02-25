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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;


@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Placement{
	@JsonProperty("configs")
	private List<ConfigPlacement> configs;
	@JsonProperty("configuration-layout")
	private String configurationLayout;

  public List<ConfigPlacement> getConfigs() {
    return configs;
  }

  public void setConfigs(List<ConfigPlacement> configs) {
    this.configs = configs;
  }

  public String getConfigurationLayout() {
    return configurationLayout;
  }

  public void setConfigurationLayout(String configurationLayout) {
    this.configurationLayout = configurationLayout;
  }

  public void mergeWithParent(Placement parent) {
    if (configurationLayout == null) {
      configurationLayout = parent.configurationLayout;
    }

    if (configs == null) {
      configs = parent.configs;
    }else if (parent.configs != null) {
      configs = mergeConfigs(parent.configs, configs);
    }

  }

  private List<ConfigPlacement> mergeConfigs(List<ConfigPlacement> parentConfigs, List<ConfigPlacement> childConfigs) {
    Map<String, ConfigPlacement> mergedConfigPlacements = new LinkedHashMap<>();
    for (ConfigPlacement parentConfigPlacement : parentConfigs) {
      mergedConfigPlacements.put(parentConfigPlacement.getConfig(), parentConfigPlacement);
    }

    for (ConfigPlacement childConfigPlacement : childConfigs) {
      if (childConfigPlacement.getConfig() != null) {
        if (childConfigPlacement.isRemoved()) {
          mergedConfigPlacements.remove(childConfigPlacement.getConfig());
        } else {
          ConfigPlacement parentConfigPlacement = mergedConfigPlacements.get(childConfigPlacement.getConfig());
          childConfigPlacement.mergeWithParent(parentConfigPlacement);
          mergedConfigPlacements.put(childConfigPlacement.getConfig(), childConfigPlacement);
        }
      }
    }
    return new ArrayList<>(mergedConfigPlacements.values());
  }
}
