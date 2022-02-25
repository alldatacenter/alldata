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

import java.util.Set;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

public class FilterPredicateTest {
  private static final String IP_ADDRESS_PATTERN =
    "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

  @Test
  public void testApply() throws Exception {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    String propertyId = PropertyHelper.getPropertyId("category1", "ip");

    Predicate predicate = new FilterPredicate(propertyId, IP_ADDRESS_PATTERN);

    resource.setProperty(propertyId, "monkey");
    Assert.assertFalse(predicate.evaluate(resource));

    resource.setProperty(propertyId, "10.0.0.1");
    Assert.assertTrue(predicate.evaluate(resource));

    resource.setProperty(propertyId, "127.0.0.1");
    Assert.assertTrue(predicate.evaluate(resource));

    resource.setProperty(propertyId, "0.0.0.0");
    Assert.assertTrue(predicate.evaluate(resource));

    propertyId = PropertyHelper.getPropertyId("category1", "fun");
    predicate = new FilterPredicate(propertyId, IP_ADDRESS_PATTERN);

    Assert.assertFalse(predicate.evaluate(resource));
  }

  @Test
  public void testApplyNullValue() {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    String propertyId = PropertyHelper.getPropertyId("category1", "foo");
    Predicate predicate = new FilterPredicate(propertyId, null);

    resource.setProperty(propertyId, "monkey");
    Assert.assertFalse(predicate.evaluate(resource));

    resource.setProperty(propertyId, null);
    Assert.assertTrue(predicate.evaluate(resource));
  }

  @Test
  public void testApplyEmptyValue() {
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    String propertyId = PropertyHelper.getPropertyId("category1", "foo");
    Predicate predicate = new FilterPredicate(propertyId, "");

    resource.setProperty(propertyId, "monkey");
    Assert.assertFalse(predicate.evaluate(resource));

    predicate = new FilterPredicate(propertyId, "monkey");
    Assert.assertTrue(predicate.evaluate(resource));
  }

  @Test
  public void testGetProperties() {
    String propertyId = PropertyHelper.getPropertyId("category1", "foo");
    FilterPredicate predicate = new FilterPredicate(propertyId, "bar");

    Set<String> ids = predicate.getPropertyIds();

    Assert.assertEquals(1, ids.size());
    Assert.assertTrue(ids.contains(propertyId));
  }
}
