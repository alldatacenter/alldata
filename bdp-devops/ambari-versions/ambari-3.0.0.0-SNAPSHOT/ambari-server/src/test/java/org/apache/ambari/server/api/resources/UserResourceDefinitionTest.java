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
 * UserResourceDefinition tests.
 */
public class UserResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    final UserResourceDefinition userResourceDefinition = new UserResourceDefinition();
    Assert.assertEquals("users", userResourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    final UserResourceDefinition userResourceDefinition = new UserResourceDefinition();
    Assert.assertEquals("user", userResourceDefinition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    Set<Resource.Type> expectedSubResourceDefinitionTypes = new HashSet<>();
    expectedSubResourceDefinitionTypes.add(Resource.Type.UserAuthenticationSource);
    expectedSubResourceDefinitionTypes.add(Resource.Type.UserPrivilege);
    expectedSubResourceDefinitionTypes.add(Resource.Type.ActiveWidgetLayout);

    final UserResourceDefinition userResourceDefinition = new UserResourceDefinition();
    Set<SubResourceDefinition> subResourceDefinitions = userResourceDefinition.getSubResourceDefinitions();
    Assert.assertEquals(expectedSubResourceDefinitionTypes.size(), subResourceDefinitions.size());

    for(SubResourceDefinition subResourceDefinition : subResourceDefinitions) {
      Assert.assertTrue(expectedSubResourceDefinitionTypes.contains(subResourceDefinition.getType()));
    }
  }
}
