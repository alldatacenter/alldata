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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.state.ServiceInfo;
import org.junit.Test;

public class ConfigurationRecommendationCommandTest {

  @Test
  public void testProcessHostGroups() throws Exception {
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    File file = mock(File.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ConfigurationRecommendationCommand command =
        new ConfigurationRecommendationCommand(StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS, file,
            "1w", ServiceInfo.ServiceAdvisorType.PYTHON, 1, saRunner, metaInfo,
            null, null);

    StackAdvisorRequest request = mock(StackAdvisorRequest.class);
    SortedMap<String, SortedSet<String>> componentHostGroupMap = new TreeMap<>();
    SortedSet<String> components1 = new TreeSet<>();
    components1.add("component1");
    components1.add("component4");
    components1.add("component5");
    componentHostGroupMap.put("group1", components1);
    SortedSet<String> components2 = new TreeSet<>();
    components2.add("component2");
    components2.add("component3");
    componentHostGroupMap.put("group2", components2);
    doReturn(componentHostGroupMap).when(request).getHostComponents();
    Set<RecommendationResponse.HostGroup> hostGroups = command.processHostGroups(request);

    assertNotNull(hostGroups);
    assertEquals(2, hostGroups.size());
    Map<String, RecommendationResponse.HostGroup> hostGroupMap =
      new HashMap<>();
    for (RecommendationResponse.HostGroup hostGroup : hostGroups) {
      hostGroupMap.put(hostGroup.getName(), hostGroup);
    }
    RecommendationResponse.HostGroup hostGroup1 = hostGroupMap.get("group1");
    assertNotNull(hostGroup1);
    Set<Map<String, String>> host1Components = hostGroup1.getComponents();
    assertNotNull(host1Components);
    assertEquals(3, host1Components.size());
    Set<String> componentNames1 = new HashSet<>();
    for (Map<String, String> host1Component : host1Components) {
      assertNotNull(host1Component);
      assertEquals(1, host1Component.size());
      String componentName = host1Component.get("name");
      assertNotNull(componentName);
      componentNames1.add(componentName);
    }
    assertEquals(3, componentNames1.size());
    assertTrue(componentNames1.contains("component1"));
    assertTrue(componentNames1.contains("component4"));
    assertTrue(componentNames1.contains("component5"));
    RecommendationResponse.HostGroup hostGroup2 = hostGroupMap.get("group2");
    assertNotNull(hostGroup2);
    Set<Map<String, String>> host2Components = hostGroup2.getComponents();
    assertNotNull(host2Components);
    assertEquals(2, host2Components.size());
    Set<String> componentNames2 = new HashSet<>();
    for (Map<String, String> host2Component : host2Components) {
      assertNotNull(host2Component);
      assertEquals(1, host2Component.size());
      String componentName = host2Component.get("name");
      assertNotNull(componentName);
      componentNames2.add(componentName);
    }
    assertEquals(2, componentNames2.size());
    assertTrue(componentNames2.contains("component2"));
    assertTrue(componentNames2.contains("component3"));
  }
}
