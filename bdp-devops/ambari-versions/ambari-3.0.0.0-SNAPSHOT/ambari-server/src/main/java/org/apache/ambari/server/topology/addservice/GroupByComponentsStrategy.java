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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;


public class GroupByComponentsStrategy implements HostGroupStrategy {
  @Override
  public Map<String, Set<String>> calculateHostGroups(Map<String, Set<String>> hostComponentMap) {
    // create components -> hosts map, the values will be the host groups
    List<Set<String>> hostGroups = newArrayList(
      hostComponentMap.entrySet().stream().collect(
        groupingBy(Map.Entry::getValue, mapping(Map.Entry::getKey, toCollection(TreeSet::new))) ).values()); // hosts names are sorted in the host group
    hostGroups.sort(comparing(hosts -> hosts.iterator().next())); // alphabetical order by the first hostname in the group to have consistent outcome

    // give a name to each host group and add to a map
    Map<String, Set<String>> hostgroupMap = IntStream.range(0, hostGroups.size()).boxed()
      .collect(toMap(i -> "host_group_" + (i + 1), i -> hostGroups.get(i)));

    return hostgroupMap;
  }
}
