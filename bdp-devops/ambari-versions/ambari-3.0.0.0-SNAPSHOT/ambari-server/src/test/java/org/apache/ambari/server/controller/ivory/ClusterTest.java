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

package org.apache.ambari.server.controller.ivory;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * Cluster tests.
 */
public class ClusterTest {
  @Test
  public void testGetName() throws Exception {
    Cluster cluster = new Cluster("Cluster1", "Colo", Collections.singleton(new Cluster.Interface("type", "endpoint", "version")),
        Collections.singleton(new Cluster.Location("name", "path")), Collections.singletonMap("P1", "V1"));
    Assert.assertEquals("Cluster1", cluster.getName());
  }

  @Test
  public void testGetColo() throws Exception {
    Cluster cluster = new Cluster("Cluster1", "Colo", Collections.singleton(new Cluster.Interface("type", "endpoint", "version")),
        Collections.singleton(new Cluster.Location("name", "path")), Collections.singletonMap("P1", "V1"));
    Assert.assertEquals("Colo", cluster.getColo());
  }

  @Test
  public void testGetInterfaces() throws Exception {
    Cluster.Interface interface1 = new Cluster.Interface("type", "endpoint", "version");
    Cluster cluster = new Cluster("Cluster1", "Colo", Collections.singleton(interface1),
        Collections.singleton(new Cluster.Location("name", "path")), Collections.singletonMap("P1", "V1"));
    Assert.assertEquals(Collections.singleton(interface1), cluster.getInterfaces());
  }

  @Test
  public void testGetLocations() throws Exception {
    Cluster.Location location = new Cluster.Location("name", "path");
    Cluster cluster = new Cluster("Cluster1", "Colo", Collections.singleton(new Cluster.Interface("type", "endpoint", "version")),
        Collections.singleton(location), Collections.singletonMap("P1", "V1"));
    Assert.assertEquals(Collections.singleton(location), cluster.getLocations());
  }

  @Test
  public void testGetProperties() throws Exception {
    Cluster cluster = new Cluster("Cluster1", "Colo", Collections.singleton(new Cluster.Interface("type", "endpoint", "version")),
        Collections.singleton(new Cluster.Location("name", "path")), Collections.singletonMap("P1", "V1"));
    Assert.assertEquals(Collections.singletonMap("P1", "V1"), cluster.getProperties());
  }
}
