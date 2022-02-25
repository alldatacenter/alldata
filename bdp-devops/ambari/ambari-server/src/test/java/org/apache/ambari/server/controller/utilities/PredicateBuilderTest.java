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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import junit.framework.Assert;

/**
 *
 */
public class PredicateBuilderTest {

  @Test
  public void testSimple() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("bar").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testSimpleNot() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    /*  ! p1 == "foo" */
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.not().property(p1).equals("foo").toPredicate();

    Assert.assertFalse(predicate1.evaluate(resource));

    /*  ! p1 == "bar" */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.not().property(p1).equals("bar").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));
  }

  @Test
  public void testDone() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property(p1).equals("foo").toPredicate();

    // can't reuse a builder after toPredicate is called.
    try {
      pb.property(p1).equals("foo").toPredicate();
      Assert.fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    Assert.assertSame(predicate, pb.toPredicate());
  }

  @Test
  public void testSimpleAnd() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").and().property(p2).equals("bar").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().property(p2).equals("car").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testSimpleAndNot() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");

    /* p1 == foo and !p2 == bar */
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").and().not().property(p2).equals("bar").toPredicate();

    Assert.assertFalse(predicate1.evaluate(resource));

    /* p1 == foo and !p2 == car */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().not().property(p2).equals("car").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));
  }

  @Test
  public void testLongAnd() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");
    String p3 = PropertyHelper.getPropertyId("cat1", "prop3");
    String p4 = PropertyHelper.getPropertyId("cat1", "prop4");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").and().property(p2).equals("bar").and().property(p3).equals("cat").and().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().property(p2).equals("bar").and().property(p3).equals("cat").and().property(p4).equals("dot").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testSimpleOr() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").or().property(p2).equals("bar").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").or().property(p2).equals("car").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));

    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.property(p1).equals("fun").or().property(p2).equals("car").toPredicate();

    Assert.assertFalse(predicate3.evaluate(resource));
  }

  @Test
  public void testLongOr() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");
    String p3 = PropertyHelper.getPropertyId("cat1", "prop3");
    String p4 = PropertyHelper.getPropertyId("cat1", "prop4");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").or().property(p2).equals("bar").or().property(p3).equals("cat").or().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").or().property(p2).equals("car").or().property(p3).equals("cat").or().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));

    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.property(p1).equals("fun").or().property(p2).equals("car").or().property(p3).equals("bat").or().property(p4).equals("dot").toPredicate();

    Assert.assertFalse(predicate3.evaluate(resource));
  }

  @Test
  public void testAndOr() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");
    String p3 = PropertyHelper.getPropertyId("cat1", "prop3");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");

    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.property(p1).equals("foo").and().property(p2).equals("bar").or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));


    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("foo").and().property(p2).equals("car").or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));


    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.property(p1).equals("foo").and().property(p2).equals("bar").or().property(p3).equals("can").toPredicate();

    Assert.assertTrue(predicate3.evaluate(resource));


    PredicateBuilder pb4 = new PredicateBuilder();
    Predicate predicate4 = pb4.property(p1).equals("foo").and().property(p2).equals("bat").or().property(p3).equals("can").toPredicate();

    Assert.assertFalse(predicate4.evaluate(resource));
  }


  @Test
  public void testBlocks() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");
    String p3 = PropertyHelper.getPropertyId("cat1", "prop3");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");


    /*   (p1==foo && p2==bar) || p3 == cat   */
    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.begin().property(p1).equals("foo").and().property(p2).equals("bar").end().or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    /*   (p1==foo && p2==bat) || p3 == cat   */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.begin().property(p1).equals("foo").and().property(p2).equals("bat").end().or().property(p3).equals("cat").toPredicate();

    Assert.assertTrue(predicate2.evaluate(resource));

    /*   (p1==foo && p2==bar) || p3 == can   */
    PredicateBuilder pb3 = new PredicateBuilder();
    Predicate predicate3 = pb3.begin().property(p1).equals("foo").and().property(p2).equals("bar").end().or().property(p3).equals("can").toPredicate();

    Assert.assertTrue(predicate3.evaluate(resource));

    /*   (p1==foo && p2==bat) || p3 == can   */
    PredicateBuilder pb4 = new PredicateBuilder();
    Predicate predicate4 = pb4.begin().property(p1).equals("foo").and().property(p2).equals("bat").end().or().property(p3).equals("can").toPredicate();

    Assert.assertFalse(predicate4.evaluate(resource));


    /*   p1==foo && (p2==bar || p3 == cat)   */
    PredicateBuilder pb5 = new PredicateBuilder();
    Predicate predicate5 = pb5.property(p1).equals("foo").and().begin().property(p2).equals("bar").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertTrue(predicate5.evaluate(resource));

    /*   p1==foo && (p2==bat || p3 == cat)   */
    PredicateBuilder pb6 = new PredicateBuilder();
    Predicate predicate6 = pb6.property(p1).equals("foo").and().begin().property(p2).equals("bat").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertTrue(predicate6.evaluate(resource));

    /*   p1==foo && (p2==bat || p3 == can)   */
    PredicateBuilder pb7 = new PredicateBuilder();
    Predicate predicate7 = pb7.property(p1).equals("foo").and().begin().property(p2).equals("bat").or().property(p3).equals("can").end().toPredicate();

    Assert.assertFalse(predicate7.evaluate(resource));

    /*   p1==fat && (p2==bar || p3 == cat)   */
    PredicateBuilder pb8 = new PredicateBuilder();
    Predicate predicate8 = pb8.property(p1).equals("fat").and().begin().property(p2).equals("bar").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertFalse(predicate8.evaluate(resource));

    /*   p1==foo && !(p2==bar || p3 == cat)   */
    PredicateBuilder pb9 = new PredicateBuilder();
    Predicate predicate9 = pb9.property(p1).equals("foo").and().not().begin().property(p2).equals("bar").or().property(p3).equals("cat").end().toPredicate();

    Assert.assertFalse(predicate9.evaluate(resource));


    /*   p1==foo && !(p2==bat || p3 == car)   */
    PredicateBuilder pb10 = new PredicateBuilder();
    Predicate predicate10 = pb10.property(p1).equals("foo").and().not().begin().property(p2).equals("bat").or().property(p3).equals("car").end().toPredicate();

    Assert.assertTrue(predicate10.evaluate(resource));
  }

  @Test
  public void testNestedBlocks() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");
    String p3 = PropertyHelper.getPropertyId("cat1", "prop3");
    String p4 = PropertyHelper.getPropertyId("cat1", "prop4");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    /*   (p1==foo && (p2==bar || p3==cat)) || p4 == dog   */
    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.
        begin().
        property(p1).equals("foo").and().
        begin().
        property(p2).equals("bar").or().property(p3).equals("cat").
        end().
        end().
        or().property(p4).equals("dog").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));


    /*   (p1==fat && (p2==bar || p3==cat)) || p4 == dot   */
    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.
        begin().
        property(p1).equals("fat").and().
        begin().
        property(p2).equals("bar").or().property(p3).equals("cat").
        end().
        end().
        or().property(p4).equals("dot").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }


  @Test
  public void testUnbalancedBlocks() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");
    String p2 = PropertyHelper.getPropertyId("cat1", "prop2");
    String p3 = PropertyHelper.getPropertyId("cat1", "prop3");
    String p4 = PropertyHelper.getPropertyId("cat1", "prop4");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");
    resource.setProperty(p4, "dog");

    /*   (p1==foo && (p2==bar || p3==cat) || p4 == dog   */
    PredicateBuilder pb1 = new PredicateBuilder();
    try {
      pb1.
          begin().
          property(p1).equals("foo").and().
          begin().
          property(p2).equals("bar").or().property(p3).equals("cat").
          end().
          or().property(p4).equals("dog").toPredicate();
      Assert.fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    /*   (p1==foo && p2==bar || p3==cat)) || p4 == dog   */
    PredicateBuilder pb2 = new PredicateBuilder();
    try {
      pb2.
          begin().
          property(p1).equals("foo").and().
          property(p2).equals("bar").or().property(p3).equals("cat").
          end().
          end().
          or().property(p4).equals("dog").toPredicate();
      Assert.fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void testAltProperty() {
    String p1 = "cat1/prop1";
    String p2 = "cat1/prop2";
    String p3 = "prop3";

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");
    resource.setProperty(p2, "bar");
    resource.setProperty(p3, "cat");


    /*   (p1==foo && p2==bar) || p3 == cat   */
    PredicateBuilder pb1 = new PredicateBuilder();
    Predicate predicate1 = pb1.begin().property("cat1/prop1").equals("foo").and().property("cat1/prop2").equals("bar").end().or().property("prop3").equals("cat").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));
  }


  @Test
  public void testEqualsString() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, "foo");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals("foo").toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals("bar").toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsInteger() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 1);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsFloat() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 1);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(Float.valueOf(1)).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(Float.valueOf(99)).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsDouble() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 1.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(Double.valueOf(1.999)).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(Double.valueOf(99.998)).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testEqualsLong() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 1L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).equals(1L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).equals(99L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterInteger() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan(1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan(99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterFloat() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan((float) 1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan((float) 99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterDouble() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan(1.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan(99.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterLong() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThan(1L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThan(99L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToInteger() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo(1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo(99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToFloat() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo((float) 1).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo((float) 99).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToDouble() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo(1.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo(99.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testGreaterThanEqualToLong() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).greaterThanEqualTo(1L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).greaterThanEqualTo(99L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessInteger() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan(99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan(1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessFloat() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan((float) 99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan((float) 1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessDouble() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan(99.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan(1.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessLong() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThan(99L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThan(1L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToInteger() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo(99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo(1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToFloat() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, (float) 2);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo((float) 99).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo((float) 1).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToDouble() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2.999);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo(99.999).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo(1.998).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }

  @Test
  public void testLessThanEqualToLong() {
    String p1 = PropertyHelper.getPropertyId("cat1", "prop1");

    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(p1, 2L);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate1 = pb.property(p1).lessThanEqualTo(99L).toPredicate();

    Assert.assertTrue(predicate1.evaluate(resource));

    PredicateBuilder pb2 = new PredicateBuilder();
    Predicate predicate2 = pb2.property(p1).lessThanEqualTo(1L).toPredicate();

    Assert.assertFalse(predicate2.evaluate(resource));
  }
}
