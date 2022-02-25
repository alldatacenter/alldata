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

package org.apache.ambari.server.controller;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.Assert;
import org.junit.Test;

public class ClusterRequestTest {

  @Test
  public void testBasicGetAndSet() {
    Long clusterId = new Long(10);
    String clusterName = "foo";
    String provisioningState = State.INIT.name();
    SecurityType securityType = SecurityType.NONE;
    StackId stackVersion = new StackId("HDP-1.0.1");
    Set<String> hostNames = new HashSet<>();
    hostNames.add("h1");

    ClusterRequest r1 =
        new ClusterRequest(clusterId, clusterName, provisioningState, securityType,
            stackVersion.getStackId(), hostNames);

    Assert.assertEquals(clusterId, r1.getClusterId());
    Assert.assertEquals(clusterName, r1.getClusterName());
    Assert.assertEquals(provisioningState, r1.getProvisioningState());
    Assert.assertEquals(securityType, r1.getSecurityType());
    Assert.assertEquals(stackVersion.getStackId(),
        r1.getStackVersion());
    Assert.assertArrayEquals(hostNames.toArray(), r1.getHostNames().toArray());
  }

  @Test
  public void testToString() {
    ClusterRequest r1 = new ClusterRequest(null, null, null, null, null, null);
    r1.toString();
  }

}
