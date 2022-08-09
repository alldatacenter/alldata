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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 * StackServiceResourceDefinition unit tests
 */
public class StackServiceResourceDefinitionTest {
  @Test
  public void testDefinitionNames() {
    ResourceDefinition def = new StackServiceResourceDefinition();
    assertEquals("service", def.getSingularName());
    assertEquals("services", def.getPluralName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    ResourceDefinition def = new StackServiceResourceDefinition();

    Set<SubResourceDefinition> subResources = def.getSubResourceDefinitions();
    assertEquals(5, subResources.size());

    boolean configReturned = false;
    boolean componentReturned = false;
    boolean artifactReturned = false;
    boolean themesReturned = false;

    for (SubResourceDefinition subResource : subResources) {
      Resource.Type type = subResource.getType();
      if (type.equals(Resource.Type.StackConfiguration)) {
        configReturned = true;
      } else if (type.equals(Resource.Type.StackServiceComponent)) {
        componentReturned = true;
      } else if (type.equals(Resource.Type.StackArtifact)) {
        artifactReturned = true;
      } else if (type.equals(Resource.Type.Theme)) {
        themesReturned = true;
      }
    }

    assertTrue(configReturned);
    assertTrue(componentReturned);
    assertTrue(artifactReturned);
    assertTrue(themesReturned);
  }
}
