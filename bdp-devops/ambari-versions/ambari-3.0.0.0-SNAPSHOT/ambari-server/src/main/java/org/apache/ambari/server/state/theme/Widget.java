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
import java.util.Map;

import org.apache.ambari.server.controller.ApiModel;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Widget implements ApiModel {
	@JsonProperty("type")
	private String type;

	@JsonProperty("units")
	private List<Unit> units;

  @JsonProperty("required-properties")
  private Map<String,String> requiredProperties;

  @JsonProperty("display-name")
  private String displayName;

  @ApiModelProperty(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @ApiModelProperty(name = "units")
  public List<Unit> getUnits() {
    return units;
  }

  public void setUnits(List<Unit> units) {
    this.units = units;
  }

  @ApiModelProperty(name = "required-properties")
  public Map<String, String> getRequiredProperties() {
    return requiredProperties;
  }

  public void setRequiredProperties(Map<String, String> requiredProperties) {
    this.requiredProperties = requiredProperties;
  }

  @ApiModelProperty(name = "display-name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
