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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {
  @JsonProperty("name")
  private String name;

  @JsonProperty("components")
  private List<Component> components;

  @JsonProperty("filters")
  private List<Filter> filters;

  static Service create(String name, List<Filter> filters, List<Component> components) {
    Preconditions.checkNotNull(name, "Service name must not be null");
    Service service = new Service();
    service.setName(name);
    service.setFilters(filters);
    service.setComponents(components);
    return service;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return component-specific quicklink filter definitions for components of this service
   */
  public List<Component> getComponents() {
    return null != components ? components : emptyList();
  }

  public void setComponents(List<Component> components) {
    this.components = components;
  }

  /**
   * @return service-specific filters for this service
   */
  public List<Filter> getFilters() {
    return null != filters ? filters : emptyList();
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

}
