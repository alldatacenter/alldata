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

import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 * HostResourceDefinition unit tests.
 */
public class HostResourceDefinitionTest {

  @Test
  public void testGetPluralName() {
    assertEquals("hosts", new HostResourceDefinition().getPluralName());
  }

  @Test
  public void testGetSingularName() {
    assertEquals("host", new HostResourceDefinition().getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    final ResourceDefinition resource = new HostResourceDefinition();
    Set<SubResourceDefinition> subResources = resource.getSubResourceDefinitions();

    assertEquals(4, subResources.size());
    assertTrue(includesType(subResources, Resource.Type.HostComponent));
    assertTrue(includesType(subResources, Resource.Type.Alert));
    assertTrue(includesType(subResources, Resource.Type.HostStackVersion));
    assertTrue(includesType(subResources, Resource.Type.HostKerberosIdentity));
  }

  private boolean includesType(Set<SubResourceDefinition> resources, Resource.Type type) {
    for (SubResourceDefinition subResource : resources) {
      if (subResource.getType() == type) {
        return true;
      }
    }
    return false;
  }
}
