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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 * SimpleResourceDefinition tests.
 */
public class SimpleResourceDefinitionTest {

  @Test
  public void testGetPluralName() throws Exception {
    ResourceDefinition resourceDefinition =
        new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages", Resource.Type.Task);

    assertEquals("stages", resourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    ResourceDefinition resourceDefinition =
        new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages", Resource.Type.Task);

    assertEquals("stage", resourceDefinition.getSingularName());
  }

  @Test
  public void testDirectives() {
    ResourceDefinition resourceDefinition;

    resourceDefinition = new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages",
        Resource.Type.Task);

    validateDirectives(Collections.emptySet(), resourceDefinition.getCreateDirectives());
    validateDirectives(Collections.emptySet(), resourceDefinition.getReadDirectives());
    validateDirectives(Collections.emptySet(), resourceDefinition.getUpdateDirectives());
    validateDirectives(Collections.emptySet(), resourceDefinition.getDeleteDirectives());

    HashMap<BaseResourceDefinition.DirectiveType, Collection<String>> directives = new HashMap<>();
    directives.put(BaseResourceDefinition.DirectiveType.CREATE, Arrays.asList("POST1", "POST2"));
    directives.put(BaseResourceDefinition.DirectiveType.READ, Arrays.asList("GET1", "GET2"));
    directives.put(BaseResourceDefinition.DirectiveType.UPDATE, Arrays.asList("PUT1", "PUT2"));
    directives.put(BaseResourceDefinition.DirectiveType.DELETE, Arrays.asList("DEL1", "DEL2"));

    resourceDefinition = new SimpleResourceDefinition(Resource.Type.Stage, "stage", "stages",
        Collections.singleton(Resource.Type.Task), directives);

    validateDirectives(directives.get(BaseResourceDefinition.DirectiveType.CREATE), resourceDefinition.getCreateDirectives());
    validateDirectives(directives.get(BaseResourceDefinition.DirectiveType.READ), resourceDefinition.getReadDirectives());
    validateDirectives(directives.get(BaseResourceDefinition.DirectiveType.UPDATE), resourceDefinition.getUpdateDirectives());
    validateDirectives(directives.get(BaseResourceDefinition.DirectiveType.DELETE), resourceDefinition.getDeleteDirectives());
  }

  private void validateDirectives(Collection<String> expected, Collection<String> actual) {
    int actualSize = actual.size();

    // Ensure the collection is empty...
    assertEquals(expected.size(), actual.size());
    for (String actualItem : actual) {
      assertTrue(expected.contains(actualItem));
    }

    // Ensure the collection is modifiable...
    assertTrue(actual.add("DIRECTIVE"));
    assertEquals(actualSize + 1, actual.size());
  }
}
