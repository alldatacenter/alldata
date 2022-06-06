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
package org.apache.ambari.server.api.query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * ExtendedResourcePredicateVisitor tests.
 */
public class ExtendedResourcePredicateVisitorTest {
  @Test
  public void testGetExtendedPredicate() throws Exception {

    Resource resource1 = new ResourceImpl(Resource.Type.Service);
    resource1.setProperty("name", "service1");
    Resource resource2 = new ResourceImpl(Resource.Type.Service);
    resource2.setProperty("name", "service2");

    Map<String, Object> resource1SubProperties1 = new HashMap<>();
    resource1SubProperties1.put("sub1/category/p1", 1);
    resource1SubProperties1.put("sub1/category/p2", 2);
    resource1SubProperties1.put("sub1/category/p3", 3);

    Map<String, Object> resource1SubProperties2 = new HashMap<>();
    resource1SubProperties2.put("sub1/category/p1", 1);
    resource1SubProperties2.put("sub1/category/p2", 4);
    resource1SubProperties2.put("sub1/category/p3", 6);

    Map<String, Object> resource1SubProperties3 = new HashMap<>();
    resource1SubProperties3.put("sub1/category/p1", 1);
    resource1SubProperties3.put("sub1/category/p2", 8);
    resource1SubProperties3.put("sub1/category/p3", 12);

    Set<Map<String, Object>> resource1SubPropertiesSet = new HashSet<>();

    resource1SubPropertiesSet.add(resource1SubProperties1);
    resource1SubPropertiesSet.add(resource1SubProperties2);
    resource1SubPropertiesSet.add(resource1SubProperties3);

    Map<String, Object> resource2SubProperties1 = new HashMap<>();
    resource2SubProperties1.put("sub1/category/p1", 2);
    resource2SubProperties1.put("sub1/category/p2", 2);
    resource2SubProperties1.put("sub1/category/p3", 3);

    Map<String, Object> resource2SubProperties2 = new HashMap<>();
    resource2SubProperties2.put("sub1/category/p1", 2);
    resource2SubProperties2.put("sub1/category/p2", 4);
    resource2SubProperties2.put("sub1/category/p3", 6);

    Map<String, Object> resource2SubProperties3 = new HashMap<>();
    resource2SubProperties3.put("sub1/category/p1", 2);
    resource2SubProperties3.put("sub1/category/p2", 8);
    resource2SubProperties3.put("sub1/category/p3", 12);

    Set<Map<String, Object>> resource2SubPropertiesSet = new HashSet<>();

    resource2SubPropertiesSet.add(resource2SubProperties1);
    resource2SubPropertiesSet.add(resource2SubProperties2);
    resource2SubPropertiesSet.add(resource2SubProperties3);

    Map<Resource, Set<Map<String, Object>>> extendedPropertyMap = new HashMap<>();

    extendedPropertyMap.put(resource1, resource1SubPropertiesSet);
    extendedPropertyMap.put(resource2, resource2SubPropertiesSet);

    Predicate predicate = new PredicateBuilder().
        property("sub1/category/p1").equals(1).toPredicate();

    ExtendedResourcePredicateVisitor visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    Predicate extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertTrue(extendedPredicate.evaluate(resource1));
    Assert.assertFalse(extendedPredicate.evaluate(resource2));


    predicate = new PredicateBuilder().
        property("sub1/category/p1").equals(2).toPredicate();

    visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertFalse(extendedPredicate.evaluate(resource1));
    Assert.assertTrue(extendedPredicate.evaluate(resource2));



    predicate = new PredicateBuilder().
        property("sub1/category/p2").equals(4).toPredicate();

    visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertTrue(extendedPredicate.evaluate(resource1));
    Assert.assertTrue(extendedPredicate.evaluate(resource2));



    predicate = new PredicateBuilder().
        property("sub1/category/p2").equals(5).toPredicate();

    visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertFalse(extendedPredicate.evaluate(resource1));
    Assert.assertFalse(extendedPredicate.evaluate(resource2));


    predicate = new PredicateBuilder().not().
        property("sub1/category/p2").equals(5).toPredicate();

    visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertTrue(extendedPredicate.evaluate(resource1));
    Assert.assertTrue(extendedPredicate.evaluate(resource2));


    predicate = new PredicateBuilder().
        property("sub1/category/p1").equals(1).and().
        property("sub1/category/p2").equals(4).toPredicate();

    visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertTrue(extendedPredicate.evaluate(resource1));
    Assert.assertFalse(extendedPredicate.evaluate(resource2));


    predicate = new PredicateBuilder().
        property("sub1/category/p1").equals(1).or().
        property("sub1/category/p2").equals(4).toPredicate();

    visitor = new ExtendedResourcePredicateVisitor(extendedPropertyMap);
    PredicateHelper.visit(predicate, visitor);

    extendedPredicate = visitor.getExtendedPredicate();

    Assert.assertTrue(extendedPredicate.evaluate(resource1));
    Assert.assertTrue(extendedPredicate.evaluate(resource2));
  }
}
