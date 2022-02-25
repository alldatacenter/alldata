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

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.CategoryIsEmptyPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests for SimplifyingPredicateVisitor
 */
public class SimplifyingPredicateVisitorTest {

  private static final String PROPERTY_A = PropertyHelper.getPropertyId("category", "A");
  private static final String PROPERTY_B = PropertyHelper.getPropertyId("category", "B");
  private static final String PROPERTY_C = PropertyHelper.getPropertyId("category", "C");
  private static final String PROPERTY_D = PropertyHelper.getPropertyId("category", "D");

  private static final Predicate PREDICATE_1 = new PredicateBuilder().property(PROPERTY_A).equals("Monkey").toPredicate();
  private static final Predicate PREDICATE_2 = new PredicateBuilder().property(PROPERTY_B).equals("Runner").toPredicate();
  private static final Predicate PREDICATE_3 = new AndPredicate(PREDICATE_1, PREDICATE_2);
  private static final Predicate PREDICATE_4 = new OrPredicate(PREDICATE_1, PREDICATE_2);
  private static final Predicate PREDICATE_5 = new PredicateBuilder().property(PROPERTY_C).equals("Racer").toPredicate();
  private static final Predicate PREDICATE_6 = new OrPredicate(PREDICATE_5, PREDICATE_4);
  private static final Predicate PREDICATE_7 = new PredicateBuilder().property(PROPERTY_C).equals("Power").toPredicate();
  private static final Predicate PREDICATE_8 = new OrPredicate(PREDICATE_6, PREDICATE_7);
  private static final Predicate PREDICATE_9 = new AndPredicate(PREDICATE_1, PREDICATE_8);
  private static final Predicate PREDICATE_10 = new OrPredicate(PREDICATE_3, PREDICATE_5);
  private static final Predicate PREDICATE_11 = new AndPredicate(PREDICATE_4, PREDICATE_10);
  private static final Predicate PREDICATE_12 = new PredicateBuilder().property(PROPERTY_D).equals("Installer").toPredicate();
  private static final Predicate PREDICATE_13 = new AndPredicate(PREDICATE_1, PREDICATE_12);
  private static final Predicate PREDICATE_14 = new PredicateBuilder().property(PROPERTY_D).greaterThan(12).toPredicate();
  private static final Predicate PREDICATE_15 = new AndPredicate(PREDICATE_1, PREDICATE_14);
  private static final Predicate PREDICATE_16 = new CategoryIsEmptyPredicate("cat1");

  @Test
  public void testVisit() {

    ResourceProvider provider = createStrictMock(ResourceProvider.class);
    Capture<Set<String>> propertiesCapture = EasyMock.newCapture();

    SimplifyingPredicateVisitor visitor = new SimplifyingPredicateVisitor(provider);

    //expectations

    expect(provider.checkPropertyIds(capture(propertiesCapture))).andReturn(Collections.emptySet()).anyTimes();

    replay(provider);

    PredicateHelper.visit(PREDICATE_1, visitor);

    List<Predicate> simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));
    Set<String> setProps = propertiesCapture.getValue();
    assertEquals(1, setProps.size());
    assertEquals(PROPERTY_A, setProps.iterator().next());
    // ---
    PredicateHelper.visit(PREDICATE_3, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_3, simplifiedPredicates.get(0));

    // ---
    PredicateHelper.visit(PREDICATE_4, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(2, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));
    Assert.assertEquals(PREDICATE_2, simplifiedPredicates.get(1));

    // ---
    PredicateHelper.visit(PREDICATE_6, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(3, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_5, simplifiedPredicates.get(0));
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(1));
    Assert.assertEquals(PREDICATE_2, simplifiedPredicates.get(2));

    // ---
    PredicateHelper.visit(PREDICATE_8, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(4, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_5, simplifiedPredicates.get(0));
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(1));
    Assert.assertEquals(PREDICATE_2, simplifiedPredicates.get(2));
    Assert.assertEquals(PREDICATE_7, simplifiedPredicates.get(3));

    // ---
    PredicateHelper.visit(PREDICATE_9, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(4, simplifiedPredicates.size());
//    Assert.assertEquals(???, simplifiedPredicates.get(0));

    // ---
    PredicateHelper.visit(PREDICATE_11, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(4, simplifiedPredicates.size());
//    Assert.assertEquals(???, simplifiedPredicates.get(0));

    // ---
    PredicateHelper.visit(PREDICATE_16, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_16, simplifiedPredicates.get(0));

    //reset assertions.  For property D, indicate that it is not supported.
    verify(provider);
    reset(provider);
    expect(provider.checkPropertyIds(capture(propertiesCapture))).andReturn(Collections.emptySet());
    expect(provider.checkPropertyIds(capture(propertiesCapture))).andReturn(Collections.singleton(PROPERTY_D));
    replay(provider);

    // ---
    PredicateHelper.visit(PREDICATE_13, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));

    verify(provider);
    reset(provider);
    expect(provider.checkPropertyIds(capture(propertiesCapture))).andReturn(Collections.emptySet()).anyTimes();
    replay(provider);

    // ---
    PredicateHelper.visit(PREDICATE_15, visitor);

    simplifiedPredicates = visitor.getSimplifiedPredicates();

    Assert.assertEquals(1, simplifiedPredicates.size());
    Assert.assertEquals(PREDICATE_1, simplifiedPredicates.get(0));

    verify(provider);
  }
}
