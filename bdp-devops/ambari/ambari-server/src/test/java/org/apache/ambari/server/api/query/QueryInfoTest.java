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

package org.apache.ambari.server.api.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.api.resources.ClusterResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.junit.Test;

/**
 * QueryInfo unit tests.
 */
public class QueryInfoTest {
  @Test
  public void testGetProperties() {
    Set<String> properties = new HashSet<>();
    QueryInfo info = new QueryInfo(new ClusterResourceDefinition(), properties);

    assertEquals(properties, info.getProperties());
  }

  @Test
  public void testGetResource() {
    ResourceDefinition resource = new ClusterResourceDefinition();
    QueryInfo info = new QueryInfo(resource, new HashSet<>());

    assertSame(resource, info.getResource());
  }
}
