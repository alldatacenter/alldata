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

package org.apache.ambari.server.api.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

/**
 * StackConfigurationResourceDefinition unit tests.
 */
public class StackConfigurationDependencyDefinitionTest {

  @Test
  public void testGetPluralName() {
    assertEquals("dependencies", new StackConfigurationDependencyResourceDefinition().getPluralName());
  }

  @Test
  public void testGetSingularName() {
    assertEquals("dependency", new StackConfigurationDependencyResourceDefinition().getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    ResourceDefinition resource = new StackConfigurationDependencyResourceDefinition();
    Set<SubResourceDefinition> subResources = resource.getSubResourceDefinitions();

    assertTrue(subResources.isEmpty());
  }

}
