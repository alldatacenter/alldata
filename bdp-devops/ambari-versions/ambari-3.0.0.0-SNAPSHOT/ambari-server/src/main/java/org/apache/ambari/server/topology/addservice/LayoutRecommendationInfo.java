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
package org.apache.ambari.server.topology.addservice;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates information from layout recommendation that can be used or reused for config recommendation.
 */
public class LayoutRecommendationInfo {
  private final Map<String, Set<String>> hostGroups;
  private final Map<String, Map<String, Set<String>>> allServiceComponentHosts;

  public LayoutRecommendationInfo(Map<String, Set<String>> hostGroups, Map<String, Map<String, Set<String>>> allServiceComponentHosts) {
    this.hostGroups = hostGroups;
    this.allServiceComponentHosts = allServiceComponentHosts;
  }

  public Map<String, Set<String>> getHostGroups() {
    return hostGroups;
  }

  public Map<String, Map<String, Set<String>>> getAllServiceLayouts() {
    return allServiceComponentHosts;
  }

  public List<String> getHosts() {
    return getHostsFromHostGroups(hostGroups);
  }

  public static List<String> getHostsFromHostGroups(Map<String, Set<String>> hostGroups) {
    return hostGroups.values().stream().flatMap(Set::stream).collect(toList());
  }
}
