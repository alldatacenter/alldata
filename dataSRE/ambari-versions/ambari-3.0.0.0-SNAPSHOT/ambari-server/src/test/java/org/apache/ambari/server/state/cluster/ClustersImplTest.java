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

package org.apache.ambari.server.state.cluster;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.state.Cluster;
import org.junit.Test;

public class ClustersImplTest {

  @Test
  public void testAddSessionAttributes() throws Exception {

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("foo", "bar");

    Cluster cluster = createMock(Cluster.class);
    ClustersImpl clusters =
        createMockBuilder(ClustersImpl.class).addMockedMethod("findCluster", String.class).createMock();

    expect(clusters.findCluster("c1")).andReturn(cluster);
    cluster.addSessionAttributes(attributes);
    replay(clusters, cluster);

    clusters.addSessionAttributes("c1", attributes);

    verify(clusters, cluster);
  }

  @Test
  public void testGetSessionAttributes() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("foo", "bar");

    Cluster cluster = createMock(Cluster.class);
    ClustersImpl clusters =
        createMockBuilder(ClustersImpl.class).addMockedMethod("findCluster", String.class).createMock();

    expect(clusters.findCluster("c1")).andReturn(cluster);
    expect(cluster.getSessionAttributes()).andReturn(attributes);
    replay(clusters, cluster);

    assertEquals(attributes, clusters.getSessionAttributes("c1"));

    verify(clusters, cluster);
  }
}
