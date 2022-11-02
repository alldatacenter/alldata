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

package org.apache.ambari.server.controller.predicate;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests for CategoryIsEmptyPredicate.
 */
public class CategoryIsEmptyPredicateTest {

  @Test
  public void testApply() {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    String categoryId = PropertyHelper.getPropertyId("category1", null);
    Predicate predicate = new CategoryIsEmptyPredicate(categoryId);

    Assert.assertTrue(predicate.evaluate(resource));

    resource.addCategory(categoryId);
    Assert.assertTrue(predicate.evaluate(resource));

    String propertyId = PropertyHelper.getPropertyId("category1", "bar");
    resource.setProperty(propertyId, "value1");
    Assert.assertFalse(predicate.evaluate(resource));
  }

  @Test
  public void testApplyWithMap() {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    String propertyId = PropertyHelper.getPropertyId("category1", "mapProperty");
    Predicate predicate = new CategoryIsEmptyPredicate(propertyId);

    Assert.assertTrue(predicate.evaluate(resource));

    Map<String, String> mapProperty = new HashMap<>();

    resource.setProperty(propertyId, mapProperty);
    Assert.assertTrue(predicate.evaluate(resource));

    mapProperty.put("foo", "bar");

    Assert.assertFalse(predicate.evaluate(resource));
  }

}
