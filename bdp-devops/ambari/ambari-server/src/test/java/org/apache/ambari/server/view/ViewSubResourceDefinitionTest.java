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

package org.apache.ambari.server.view;

import java.util.Set;

import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ResourceConfigTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * ViewSubResourceDefinition tests.
 */
public class ViewSubResourceDefinitionTest {
  @Test
  public void testGetPluralName() throws Exception {
    ViewSubResourceDefinition viewSubResourceDefinition = getViewSubResourceDefinition();

    Assert.assertEquals("resources", viewSubResourceDefinition.getPluralName());
  }

  @Test
  public void testGetSingularName() throws Exception {
    ViewSubResourceDefinition viewSubResourceDefinition = getViewSubResourceDefinition();

    Assert.assertEquals("resource", viewSubResourceDefinition.getSingularName());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    ViewSubResourceDefinition viewSubResourceDefinition = getViewSubResourceDefinition();

    new Resource.Type("MY_VIEW{1.0.0}/resource");
    new Resource.Type("MY_VIEW{1.0.0}/subresource");

    Set<SubResourceDefinition> subResourceDefinitions = viewSubResourceDefinition.getSubResourceDefinitions ();

    Assert.assertEquals(1, subResourceDefinitions.size());

    Assert.assertEquals("MY_VIEW{1.0.0}/subresource", subResourceDefinitions.iterator().next().getType().name());
  }

  public static ViewSubResourceDefinition getViewSubResourceDefinition() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ResourceConfig resourceConfig = ResourceConfigTest.getResourceConfigs().get(0);

    return new ViewSubResourceDefinition(viewDefinition, resourceConfig);
  }
}
