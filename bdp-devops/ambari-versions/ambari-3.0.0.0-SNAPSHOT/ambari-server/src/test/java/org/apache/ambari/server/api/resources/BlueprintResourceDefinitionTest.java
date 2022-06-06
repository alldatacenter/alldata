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

import java.util.Collection;

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

/**
 * BlueprintResourceDefinition tests.
 */
public class BlueprintResourceDefinitionTest {

  @Test
  public void testGetType() throws Exception {
    BlueprintResourceDefinition definition = new BlueprintResourceDefinition();
    Assert.assertEquals(Resource.Type.Blueprint, definition.getType());
  }

  @Test
  public void testGetPluralName() throws Exception {
    BlueprintResourceDefinition definition = new BlueprintResourceDefinition();
    Assert.assertEquals("blueprints", definition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    BlueprintResourceDefinition definition = new BlueprintResourceDefinition();
    Assert.assertEquals("blueprint", definition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    BlueprintResourceDefinition definition = new BlueprintResourceDefinition();
    Assert.assertTrue(definition.getSubResourceDefinitions().isEmpty());
  }

  @Test
  public void testGetCreateDirectives() {
    BlueprintResourceDefinition definition = new BlueprintResourceDefinition();
    Collection<String> directives = definition.getCreateDirectives();
    Assert.assertEquals(1, directives.size());
    Assert.assertTrue(directives.contains("validate_topology"));
  }
}
