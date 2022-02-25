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
import static org.junit.Assert.fail;

import java.util.Set;

import org.apache.ambari.server.api.query.render.ClusterBlueprintRenderer;
import org.apache.ambari.server.api.query.render.DefaultRenderer;
import org.apache.ambari.server.api.query.render.MinimalRenderer;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

/**
 * ClusterResourceDefinition unit tests.
 */
public class ClusterResourceDefinitionTest {

  @Test
  public void testGetPluralName() {
    assertEquals("clusters", new ClusterResourceDefinition().getPluralName());
  }

  @Test
  public void testGetSingularName() {
    assertEquals("cluster", new ClusterResourceDefinition().getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() {
    ResourceDefinition resource = new ClusterResourceDefinition();
    Set<SubResourceDefinition> subResources = resource.getSubResourceDefinitions();

    assertEquals(13, subResources.size());
    assertTrue(includesType(subResources, Resource.Type.Service));
    assertTrue(includesType(subResources, Resource.Type.Host));
    assertTrue(includesType(subResources, Resource.Type.Configuration));
    assertTrue(includesType(subResources, Resource.Type.Request));
    assertTrue(includesType(subResources, Resource.Type.Workflow));
    assertTrue(includesType(subResources, Resource.Type.ConfigGroup));
    assertTrue(includesType(subResources, Resource.Type.AlertDefinition));
    assertTrue(includesType(subResources, Resource.Type.ServiceConfigVersion));
    assertTrue(includesType(subResources, Resource.Type.ClusterPrivilege));
    assertTrue(includesType(subResources, Resource.Type.Alert));
    assertTrue(includesType(subResources, Resource.Type.ClusterStackVersion));
    assertTrue(includesType(subResources, Resource.Type.Artifact));
    assertTrue(includesType(subResources, Resource.Type.ClusterKerberosDescriptor));
  }

  @Test
  public void testGetRenderer() {
    ResourceDefinition resource = new ClusterResourceDefinition();

    assertTrue(resource.getRenderer(null) instanceof DefaultRenderer);
    assertTrue(resource.getRenderer("default") instanceof DefaultRenderer);
    assertTrue(resource.getRenderer("minimal") instanceof MinimalRenderer);
    assertTrue(resource.getRenderer("blueprint") instanceof ClusterBlueprintRenderer);

    try {
      resource.getRenderer("foo");
      fail("Should have thrown an exception due to invalid renderer type");
    } catch (IllegalArgumentException e) {
      // expected
    }
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
