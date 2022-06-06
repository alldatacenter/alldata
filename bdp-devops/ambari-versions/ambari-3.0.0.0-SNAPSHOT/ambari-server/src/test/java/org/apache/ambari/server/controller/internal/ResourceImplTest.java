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

package org.apache.ambari.server.controller.internal;

import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

/**
 *
 */
public class ResourceImplTest {

  @Test
  public void testGetType() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    Assert.assertEquals(Resource.Type.Cluster, resource.getType());

    resource = new ResourceImpl(Resource.Type.Service);
    Assert.assertEquals(Resource.Type.Service, resource.getType());

    resource = new ResourceImpl(Resource.Type.Host);
    Assert.assertEquals(Resource.Type.Host, resource.getType());

    resource = new ResourceImpl(Resource.Type.Component);
    Assert.assertEquals(Resource.Type.Component, resource.getType());

    resource = new ResourceImpl(Resource.Type.HostComponent);
    Assert.assertEquals(Resource.Type.HostComponent, resource.getType());
  }

  @Test
  public void testSetGetProperty() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    String propertyId = PropertyHelper.getPropertyId("c1", "p1");
    resource.setProperty(propertyId, "foo");
    Assert.assertEquals("foo", resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, 1);
    Assert.assertEquals(1, resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, (float) 1.99);
    Assert.assertEquals((float) 1.99, resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, 1.99);
    Assert.assertEquals(1.99, resource.getPropertyValue(propertyId));

    resource.setProperty(propertyId, 65L);
    Assert.assertEquals(65L, resource.getPropertyValue(propertyId));
  }

  @Test
  public void testAddCategory() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    resource.addCategory("c1");
    resource.addCategory("c2/sub2");
    resource.addCategory("c3/sub3/sub3a");

    Assert.assertTrue(resource.getPropertiesMap().containsKey("c1"));
    Assert.assertTrue(resource.getPropertiesMap().containsKey("c2/sub2"));
    Assert.assertTrue(resource.getPropertiesMap().containsKey("c3/sub3/sub3a"));
  }

  @Test
  public void testCopyConstructor() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    String p1 = PropertyHelper.getPropertyId(null, "p1");
    String p2 = PropertyHelper.getPropertyId("c1", "p2");
    String p3 = PropertyHelper.getPropertyId("c1/c2", "p3");
    String p4 = PropertyHelper.getPropertyId("c1/c2/c3", "p4");
    String p5 = PropertyHelper.getPropertyId("c1", "p5");

    resource.setProperty(p1, "foo");
    Assert.assertEquals("foo", resource.getPropertyValue(p1));

    resource.setProperty(p2, 1);
    Assert.assertEquals(1, resource.getPropertyValue(p2));

    resource.setProperty(p3, (float) 1.99);
    Assert.assertEquals((float) 1.99, resource.getPropertyValue(p3));

    resource.setProperty(p4, 1.99);
    Assert.assertEquals(1.99, resource.getPropertyValue(p4));

    resource.setProperty(p5, 65L);
    Assert.assertEquals(65L, resource.getPropertyValue(p5));

    Resource copy = new ResourceImpl(resource);

    Assert.assertEquals("foo", copy.getPropertyValue(p1));
    Assert.assertEquals(1, copy.getPropertyValue(p2));
    Assert.assertEquals((float) 1.99, copy.getPropertyValue(p3));
    Assert.assertEquals(1.99, copy.getPropertyValue(p4));
    Assert.assertEquals(65L, copy.getPropertyValue(p5));
  }

  @Test
  public void testGetPropertiesMap() {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    String p1 = PropertyHelper.getPropertyId(null, "p1");
    String p2 = PropertyHelper.getPropertyId("c1", "p2");
    String p3 = PropertyHelper.getPropertyId("c1/c2", "p3");
    String p4 = PropertyHelper.getPropertyId("c1/c2/c3", "p4");
    String p5 = PropertyHelper.getPropertyId("c1", "p5");

    resource.setProperty(p1, "foo");
    resource.setProperty(p2, 1);
    resource.setProperty(p3, (float) 1.99);
    resource.setProperty(p4, 1.99);
    resource.setProperty(p5, 65L);

    Map<String, Map<String, Object>> map = resource.getPropertiesMap();

    Assert.assertEquals(4, map.keySet().size());
    Assert.assertTrue(map.containsKey(""));
    Assert.assertTrue(map.containsKey("c1"));
    Assert.assertTrue(map.containsKey("c1/c2"));
    Assert.assertTrue(map.containsKey("c1/c2/c3"));

    // Check order of categories and properties ...
    String lastCategory = null;

    for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
      String category = entry.getKey();

      if (lastCategory != null) {
        Assert.assertTrue(category.compareTo(lastCategory) > 0);
      }
      lastCategory = category;

      String lastProperty = null;
      for (String property : entry.getValue().keySet()) {
        if (lastProperty != null) {
          Assert.assertTrue(property.compareTo(lastProperty) > 0);
        }
        lastProperty = property;
      }
    }
  }

  @Test
  public void testEquals() {
    Resource resource1 = new ResourceImpl(Resource.Type.Cluster);
    Resource resource2 = new ResourceImpl(Resource.Type.Cluster);
    Resource resource3 = new ResourceImpl(Resource.Type.Host);

    Assert.assertTrue(resource1.equals(resource2));
    Assert.assertTrue(resource2.equals(resource1));
    Assert.assertFalse(resource1.equals(resource3));
    Assert.assertFalse(resource3.equals(resource1));
    Assert.assertFalse(resource2.equals(resource3));
    Assert.assertFalse(resource3.equals(resource2));

    resource1.setProperty("p1", "foo");
    resource2.setProperty("p1", "bar");
    Assert.assertFalse(resource1.equals(resource2));
    Assert.assertFalse(resource2.equals(resource1));

    resource2.setProperty("p1", "foo");
    Assert.assertTrue(resource1.equals(resource2));
    Assert.assertTrue(resource2.equals(resource1));
  }
}

