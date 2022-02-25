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

package org.apache.ambari.server.api.services.stackadvisor.recommendations;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RecommendationResponseTest {

  private final RecommendationResponse response = new RecommendationResponse();

  @Before
  public void setUp() {
    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    blueprint.setHostGroups(ImmutableSet.of(
      hostGroup("host_group_1", "NAMENODE", "ZOOKEEPER_SERVER"),
      hostGroup("host_group_2", "DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT")
    ));

    RecommendationResponse.BlueprintClusterBinding clusterBinding = new RecommendationResponse.BlueprintClusterBinding();
    clusterBinding.setHostGroups(ImmutableSet.of(
      hostGroupBinding("host_group_1", "c7401", "c7402"),
      hostGroupBinding("host_group_2", "c7403", "c7404", "c7405")
    ));

    RecommendationResponse.Recommendation recommendation = new RecommendationResponse.Recommendation();
    recommendation.setBlueprint(blueprint);
    recommendation.setBlueprintClusterBinding(clusterBinding);

    response.setRecommendations(recommendation);
  }

  @Test
  public void blueprint_getHostgroupComponentMap() {
    ImmutableMap<String, Set<String>> expected = ImmutableMap.of(
      "host_group_1", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER"),
      "host_group_2", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"));
    assertEquals(expected, response.getRecommendations().getBlueprint().getHostgroupComponentMap());
  }

  @Test
  public void hostgGroup_getComponentNames() {
    Map<String, RecommendationResponse.HostGroup> hostGroups =
      response.getRecommendations().getBlueprint().getHostGroups().stream()
        .collect(toMap(RecommendationResponse.HostGroup::getName, identity()));
    assertEquals(ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER"), hostGroups.get("host_group_1").getComponentNames());
    assertEquals(ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"), hostGroups.get("host_group_2").getComponentNames());
  }

  @Test
  public void blueprintClusterBinding_getHostgroupHostMap() {
    ImmutableMap<String, Set<String>> expected = ImmutableMap.of(
      "host_group_1", ImmutableSet.of("c7401", "c7402"),
      "host_group_2", ImmutableSet.of("c7403", "c7404", "c7405"));
    assertEquals(expected, response.getRecommendations().getBlueprintClusterBinding().getHostgroupHostMap());
  }

  private static final RecommendationResponse.HostGroup hostGroup(String name, String... components) {
    RecommendationResponse.HostGroup hostGroup = new RecommendationResponse.HostGroup();
    hostGroup.setName(name);
    Set<Map<String, String>> hostGroupComponents =
      Arrays.stream(components).map(comp -> ImmutableMap.of("name", comp)).collect(toSet());
    hostGroup.setComponents(hostGroupComponents);
    return hostGroup;
  }

  private static final RecommendationResponse.BindingHostGroup hostGroupBinding(String name, String... hosts) {
    RecommendationResponse.BindingHostGroup hostGroup = new RecommendationResponse.BindingHostGroup();
    hostGroup.setName(name);
    Set<Map<String, String>> hostGroupHosts =
      Arrays.stream(hosts).map(host -> ImmutableMap.of("fqdn", host)).collect(toSet());
    hostGroup.setHosts(hostGroupHosts);
    return hostGroup;
  }
}