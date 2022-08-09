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

/**
 * A quicklinks profile is essentially a set of quick link filters defined on three levels:
 * <ul>
 *   <li>global level</li>
 *   <li>service level</li>
 *   <li>component level (within a service)</li>
 * </ul>
 * <p>For each link, filters are evaluated bottom up: component level filters take priority to service level filters
 * and service level filters take priority to global filters.</p>
 * <p>When a quick link profile is set in Ambari, then each quick link's visibility flag is updated according to the profile
 * before being returned by {@link org.apache.ambari.server.controller.internal.QuickLinkArtifactResourceProvider}.</p>
 * <p>When no profile is set, all quick link's visibility flat will be set to {@code true} by the provider</p>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuickLinksProfile {

  /**
   * The name of the Ambari setting that stores the quick links profile
   */
  public static final String SETTING_NAME_QUICKLINKS_PROFILE = "QuickLinksProfile";
  /**
   * The type of the Ambari setting that stores the quick links profile
   */
  public static final String SETTING_TYPE_AMBARI_SERVER = "ambari-server";

  @JsonProperty("filters")
  private List<Filter> filters;

  @JsonProperty("services")
  private List<Service> services;

  static QuickLinksProfile create(List<Filter> globalFilters, List<Service> services) {
    QuickLinksProfile profile = new QuickLinksProfile();
    profile.setFilters(globalFilters);
    profile.setServices(services);
    return profile;
  }

  /**
   * @return service-specific quicklink filter definitions
   */
  public List<Service> getServices() {
    return services != null ? services : emptyList();
  }

  public void setServices(List<Service> services) {
    this.services = services;
  }

  /**
   * @return the global quicklink filters
   */
  public List<Filter> getFilters() {
    return null != filters ? filters : emptyList();
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

}
