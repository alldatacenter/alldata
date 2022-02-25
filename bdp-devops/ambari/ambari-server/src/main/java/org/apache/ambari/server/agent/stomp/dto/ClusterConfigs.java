/**
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

package org.apache.ambari.server.agent.stomp.dto;

import java.util.SortedMap;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterConfigs {
  private SortedMap<String, SortedMap<String, String>> configurations;
  private SortedMap<String, SortedMap<String, SortedMap<String, String>>> configurationAttributes;

  public ClusterConfigs(SortedMap<String, SortedMap<String, String>> configurations, SortedMap<String, SortedMap<String, SortedMap<String, String>>> configurationAttributes) {
    this.configurations = configurations;
    this.configurationAttributes = configurationAttributes;
  }

  public SortedMap<String, SortedMap<String, String>> getConfigurations() {
    return configurations;
  }


  public SortedMap<String, SortedMap<String, SortedMap<String, String>>> getConfigurationAttributes() {
    return configurationAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClusterConfigs that = (ClusterConfigs) o;

    if (configurations != null ? !configurations.equals(that.configurations) : that.configurations != null)
      return false;
    return configurationAttributes != null ? configurationAttributes.equals(that.configurationAttributes) : that.configurationAttributes == null;
  }

  @Override
  public int hashCode() {
    int result = configurations != null ? configurations.hashCode() : 0;
    result = 31 * result + (configurationAttributes != null ? configurationAttributes.hashCode() : 0);
    return result;
  }
}
