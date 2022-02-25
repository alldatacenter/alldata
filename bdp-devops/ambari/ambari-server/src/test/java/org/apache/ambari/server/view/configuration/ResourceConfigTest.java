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

package org.apache.ambari.server.view.configuration;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;

/**
 * ResourceConfig tests.
 */
public class ResourceConfigTest {
  @Test
  public void testGetName() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertEquals("resource", resourceConfigs.get(0).getName());
    Assert.assertEquals("subresource", resourceConfigs.get(1).getName());
  }

  @Test
  public void testGetPluralName() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertEquals("resources", resourceConfigs.get(0).getPluralName());
    Assert.assertEquals("subresources", resourceConfigs.get(1).getPluralName());
  }

  @Test
  public void testGetIdProperty() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertEquals("id", resourceConfigs.get(0).getIdProperty());
    Assert.assertEquals("id", resourceConfigs.get(1).getIdProperty());
  }

  @Test
  public void testGetSubResourceNames() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertEquals(1, resourceConfigs.get(0).getSubResourceNames().size());
    Assert.assertEquals("subresource", resourceConfigs.get(0).getSubResourceNames().get(0));
    Assert.assertEquals(0, resourceConfigs.get(1).getSubResourceNames().size());
  }

  @Test
  public void testGetProviderClass() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertTrue(resourceConfigs.get(0).getProviderClass(getClass().getClassLoader()).equals(ViewConfigTest.MyResourceProvider.class));
    Assert.assertTrue(resourceConfigs.get(1).getProviderClass(getClass().getClassLoader()).equals(ViewConfigTest.MyResourceProvider.class));
  }

  @Test
  public void testGetServiceClass() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertTrue(resourceConfigs.get(0).getResourceClass(getClass().getClassLoader()).equals(ViewConfigTest.MyResource.class));
    Assert.assertTrue(resourceConfigs.get(1).getResourceClass(getClass().getClassLoader()).equals(ViewConfigTest.MyResource.class));
  }

  @Test
  public void testGetResourceClass() throws Exception {
    List<ResourceConfig> resourceConfigs = getResourceConfigs();

    Assert.assertEquals(2, resourceConfigs.size());
    Assert.assertTrue(resourceConfigs.get(0).getServiceClass(getClass().getClassLoader()).equals(ViewConfigTest.MyResourceService.class));
    Assert.assertTrue(resourceConfigs.get(1).getServiceClass(getClass().getClassLoader()).equals(ViewConfigTest.MyResourceService.class));
  }

  public static List<ResourceConfig> getResourceConfigs() throws JAXBException {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    return viewConfig.getResources ();
  }
}
