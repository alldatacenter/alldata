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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Assert;
import org.junit.Test;

/**
 * PermissionResourceDefinition tests.
 */
public class PermissionResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    PermissionResourceDefinition permissionResourceDefinition = new PermissionResourceDefinition();
    Assert.assertEquals("permissions", permissionResourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    PermissionResourceDefinition permissionResourceDefinition = new PermissionResourceDefinition();
    Assert.assertEquals("permission", permissionResourceDefinition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    PermissionResourceDefinition permissionResourceDefinition = new PermissionResourceDefinition();
    Set<SubResourceDefinition> subResourceDefinitions = permissionResourceDefinition.getSubResourceDefinitions ();
    Set<Resource.Type> expectedSubTypes = new HashSet<>();
    expectedSubTypes.add(Resource.Type.RoleAuthorization);

    Assert.assertEquals(1, subResourceDefinitions.size());

    for(SubResourceDefinition subResourceDefinition:subResourceDefinitions) {
      Assert.assertTrue(expectedSubTypes.contains(subResourceDefinition.getType()));
    }
  }
}



