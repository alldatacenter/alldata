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
 * ViewInstanceResourceDefinition tests.
 */
public class ViewInstanceResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    ViewInstanceResourceDefinition viewInstanceResourceDefinition = getViewInstanceResourceDefinition();
    Assert.assertEquals("instances", viewInstanceResourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    ViewInstanceResourceDefinition viewInstanceResourceDefinition = getViewInstanceResourceDefinition();
    Assert.assertEquals("instance", viewInstanceResourceDefinition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    ViewInstanceResourceDefinition viewInstanceResourceDefinition = getViewInstanceResourceDefinition();
    Set<SubResourceDefinition> subResourceDefinitions = viewInstanceResourceDefinition.getSubResourceDefinitions();

    Assert.assertEquals(3, subResourceDefinitions.size());

    for (SubResourceDefinition subResourceDefinition : subResourceDefinitions) {
      Resource.Type type = subResourceDefinition.getType();
      Assert.assertTrue(type.name().equals("sub1") || type.name().equals("sub2") || type.equals(Resource.Type.ViewPrivilege));
    }
  }

  public static ViewInstanceResourceDefinition getViewInstanceResourceDefinition() {
    Set<SubResourceDefinition> subResourceDefinitions = new HashSet<>();

    subResourceDefinitions.add(new SubResourceDefinition(new Resource.Type("sub1")));
    subResourceDefinitions.add(new SubResourceDefinition(new Resource.Type("sub2")));

    return new ViewInstanceResourceDefinition(subResourceDefinitions);
  }
}
