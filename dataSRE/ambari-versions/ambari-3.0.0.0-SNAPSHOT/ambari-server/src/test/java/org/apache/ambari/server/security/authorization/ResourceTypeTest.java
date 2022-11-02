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

package org.apache.ambari.server.security.authorization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ResourceTypeTest {

  @Test
  public void testGetId() throws Exception {
    assertEquals(1, ResourceType.AMBARI.getId());
    assertEquals(2, ResourceType.CLUSTER.getId());
    assertEquals(3, ResourceType.VIEW.getId());
  }

  @Test
  public void testTranslate() throws Exception {
    assertEquals(ResourceType.AMBARI, ResourceType.translate("ambari"));
    assertEquals(ResourceType.AMBARI, ResourceType.translate("Ambari"));
    assertEquals(ResourceType.AMBARI, ResourceType.translate("AMBARI"));
    assertEquals(ResourceType.AMBARI, ResourceType.translate(" AMBARI "));

    assertEquals(ResourceType.CLUSTER, ResourceType.translate("cluster"));
    assertEquals(ResourceType.CLUSTER, ResourceType.translate("Cluster"));
    assertEquals(ResourceType.CLUSTER, ResourceType.translate("CLUSTER"));
    assertEquals(ResourceType.CLUSTER, ResourceType.translate(" CLUSTER "));

    assertEquals(ResourceType.VIEW, ResourceType.translate("view"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("View"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("VIEW"));
    assertEquals(ResourceType.VIEW, ResourceType.translate(" VIEW "));

    assertEquals(ResourceType.VIEW, ResourceType.translate("CAPACITY-SCHEDULER{1.0.0}"));

    assertEquals(ResourceType.VIEW, ResourceType.translate("ADMIN_VIEW{2.1.2}"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("FILES{1.0.0}"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("PIG{1.0.0}"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("CAPACITY-SCHEDULER{1.0.0}"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("TEZ{0.7.0.2.3.2.0-377}"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("SLIDER{2.0.0}"));
    assertEquals(ResourceType.VIEW, ResourceType.translate("HIVE{1.0.0}"));
  }
}