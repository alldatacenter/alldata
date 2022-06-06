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
import org.apache.ambari.server.state.State;
import org.junit.Assert;
import org.junit.Test;

public class ClusterResponseTest {

  @Test
  public void testBasicGetAndSet() {
    long clusterId = 10L;
    String clusterName = "foo";
    State provisioningState = State.INSTALLED;
    SecurityType securityType = SecurityType.KERBEROS;
    Set<String> hostNames = new HashSet<>();
    hostNames.add("h1");

    ClusterResponse r1 =
        new ClusterResponse(clusterId, clusterName, provisioningState, securityType,
          hostNames, hostNames.size(), "bar", null);
    
    Assert.assertEquals(clusterId, r1.getClusterId());
    Assert.assertEquals(clusterName, r1.getClusterName());
    Assert.assertEquals(provisioningState, r1.getProvisioningState());
    Assert.assertEquals(securityType, r1.getSecurityType());
    Assert.assertEquals(1, r1.getTotalHosts());
    Assert.assertEquals("bar", r1.getDesiredStackVersion());
  }

  @Test
  public void testToString() {
    ClusterResponse r =
      new ClusterResponse(0, null, null, null, null, 0, null, null);
    r.toString();
  }
}
