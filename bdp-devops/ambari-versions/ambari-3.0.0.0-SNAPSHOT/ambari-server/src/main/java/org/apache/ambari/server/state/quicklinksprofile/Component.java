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

package org.apache.ambari.server.state.quicklinksprofile;

import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

/**
 * Class to represent component-level filter definitions
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Component {
  @JsonProperty("name")
  private String name;

  @JsonProperty("filters")
  private List<Filter> filters;

  static Component create(String name, List<Filter> filters) {
    Preconditions.checkNotNull(name, "Component name must not be null");
    Component component = new Component();
    component.setName(name);
    component.setFilters(filters);
    return component;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the quicklink filters for this component
   */
  public List<Filter> getFilters() {
    return null != filters ? filters : emptyList();
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }
}
