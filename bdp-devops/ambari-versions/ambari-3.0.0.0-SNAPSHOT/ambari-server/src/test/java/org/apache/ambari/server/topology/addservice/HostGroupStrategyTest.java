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


import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class HostGroupStrategyTest {

  public static final Map<String, Set<String>> HOST_COMPONENTS = ImmutableMap.<String, Set<String>>builder()
    .put("c7401", ImmutableSet.of("ZOOKEEPER_SERVER, NAMENODE, HDFS_CLIENT"))
    .put("c7402", ImmutableSet.of("ZOOKEEPER_SERVER, NAMENODE, HDFS_CLIENT"))
    .put("c7403", ImmutableSet.of("ZOOKEEPER_SERVER, NAMENODE, HDFS_CLIENT"))
    .put("c7404", ImmutableSet.of("ZOOKEEPER_SERVER, NAMENODE, HDFS_CLIENT, SECONDARY_NAMENODE"))
    .put("c7405", ImmutableSet.of("HIVE_SERVER, KAFKA_BROKER, ZOOKEEPER_CLIENT"))
    .put("c7406", ImmutableSet.of("DATANODE, HDFS_CLIENT, ZOOKEEPER_CLIENT"))
    .put("c7407", ImmutableSet.of("DATANODE, HDFS_CLIENT, ZOOKEEPER_CLIENT"))
    .put("c7408", ImmutableSet.of("DATANODE, HDFS_CLIENT, ZOOKEEPER_CLIENT"))
    .build();

  Map<String, Set<String>> HOST_GROUPS_FOR_EACH_HOST = ImmutableMap.<String, Set<String>>builder()
    .put("host_group_c7401", ImmutableSet.of("c7401"))
    .put("host_group_c7402", ImmutableSet.of("c7402"))
    .put("host_group_c7403", ImmutableSet.of("c7403"))
    .put("host_group_c7404", ImmutableSet.of("c7404"))
    .put("host_group_c7405", ImmutableSet.of("c7405"))
    .put("host_group_c7406", ImmutableSet.of("c7406"))
    .put("host_group_c7407", ImmutableSet.of("c7407"))
    .put("host_group_c7408", ImmutableSet.of("c7408"))
    .build();

  Map<String, Set<String>> HOST_GROUPS_BY_COMPONENTS = ImmutableMap.of(
    "host_group_1", ImmutableSet.of("c7401", "c7402", "c7403"),
    "host_group_2", ImmutableSet.of("c7404"),
    "host_group_3", ImmutableSet.of("c7405"),
    "host_group_4", ImmutableSet.of("c7406", "c7407", "c7408")
  );


  @Test
  public void hostGroupForEachHostStrategy() {
    assertEquals(HOST_GROUPS_FOR_EACH_HOST, new HostGroupForEachHostStrategy().calculateHostGroups(HOST_COMPONENTS));
  }

  @Test
  public void groupByComponentsStrategy() {
    assertEquals(HOST_GROUPS_BY_COMPONENTS, new GroupByComponentsStrategy().calculateHostGroups(HOST_COMPONENTS));
  }

  @Test
  public void autoHostgroupStrategy() {
    assertEquals(HOST_GROUPS_FOR_EACH_HOST, new AutoHostgroupStrategy().calculateHostGroups(HOST_COMPONENTS));
    assertEquals(HOST_GROUPS_BY_COMPONENTS, new AutoHostgroupStrategy(7).calculateHostGroups(HOST_COMPONENTS));
  }
}