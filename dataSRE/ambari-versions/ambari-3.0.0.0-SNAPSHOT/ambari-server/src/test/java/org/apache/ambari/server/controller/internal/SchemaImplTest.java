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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

/**
 *
 */
public class SchemaImplTest {

  private static final Set<String> resourceProviderProperties = new HashSet<>();

  static {
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p1"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p2"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c1", "p3"));
    resourceProviderProperties.add(PropertyHelper.getPropertyId("c2", "p4"));
  }

  private static final ResourceProvider resourceProvider = new ResourceProvider() {
    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return null;
    }

    @Override
    public RequestStatus createResources(Request request)
        throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return new RequestStatusImpl(null);
    }

    @Override
    public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
      return new RequestStatusImpl(null);
    }

    @Override
    public Map<Resource.Type, String> getKeyPropertyIds() {
      return keyPropertyIds;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      if (!resourceProviderProperties.containsAll(propertyIds)) {
        Set<String> unsupportedPropertyIds = new HashSet<>(propertyIds);
        unsupportedPropertyIds.removeAll(resourceProviderProperties);
        return unsupportedPropertyIds;
      }
      return Collections.emptySet();
    }
  };

  private static final Set<String> propertyProviderProperties = new HashSet<>();

  static {
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c3", "p5"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c3", "p6"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c4", "p7"));
    propertyProviderProperties.add(PropertyHelper.getPropertyId("c4", "p8"));
  }

  private static final PropertyProvider propertyProvider = new PropertyProvider() {
    @Override
    public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) {
      return null;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      if (!propertyProviderProperties.containsAll(propertyIds)) {
        Set<String> unsupportedPropertyIds = new HashSet<>(propertyIds);
        unsupportedPropertyIds.removeAll(propertyProviderProperties);
        return unsupportedPropertyIds;
      }
      return Collections.emptySet();
    }
  };

  private static final List<PropertyProvider> propertyProviders = new LinkedList<>();

  static {
    propertyProviders.add(propertyProvider);
  }

  private static final Map<Resource.Type, String> keyPropertyIds = new HashMap<>();

  static {
    keyPropertyIds.put(Resource.Type.Cluster, PropertyHelper.getPropertyId("c1", "p1"));
    keyPropertyIds.put(Resource.Type.Host, PropertyHelper.getPropertyId("c1", "p2"));
    keyPropertyIds.put(Resource.Type.Component, PropertyHelper.getPropertyId("c1", "p3"));
  }

  @Test
  public void testGetKeyPropertyId() {
    Schema schema = new SchemaImpl(resourceProvider);

    Assert.assertEquals(PropertyHelper.getPropertyId("c1", "p1"), schema.getKeyPropertyId(Resource.Type.Cluster));
    Assert.assertEquals(PropertyHelper.getPropertyId("c1", "p2"), schema.getKeyPropertyId(Resource.Type.Host));
    Assert.assertEquals(PropertyHelper.getPropertyId("c1", "p3"), schema.getKeyPropertyId(Resource.Type.Component));
  }

//  @Test
//  public void testGetCategories() {
//    Schema schema = new SchemaImpl(resourceProvider);
//
//    Map<String, Set<String>> categories = schema.getCategoryProperties();
//    Assert.assertEquals(4, categories.size());
//    Assert.assertTrue(categories.containsKey("c1"));
//    Assert.assertTrue(categories.containsKey("c2"));
//    Assert.assertTrue(categories.containsKey("c3"));
//    Assert.assertTrue(categories.containsKey("c4"));
//
//    Set<String> properties = categories.get("c1");
//    Assert.assertEquals(3, properties.size());
//    Assert.assertTrue(properties.contains("p1"));
//    Assert.assertTrue(properties.contains("p2"));
//    Assert.assertTrue(properties.contains("p3"));
//
//    properties = categories.get("c2");
//    Assert.assertEquals(1, properties.size());
//    Assert.assertTrue(properties.contains("p4"));
//
//    properties = categories.get("c3");
//    Assert.assertEquals(2, properties.size());
//    Assert.assertTrue(properties.contains("p5"));
//    Assert.assertTrue(properties.contains("p6"));
//
//    properties = categories.get("c4");
//    Assert.assertEquals(2, properties.size());
//    Assert.assertTrue(properties.contains("p7"));
//    Assert.assertTrue(properties.contains("p8"));
//  }
}
