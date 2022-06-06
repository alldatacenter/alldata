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

import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

/**
 *  Tests for predicate visitors.
 */
public class PredicateVisitorTest {

  @Test
  public void testVisitor() {

    String propertyId = PropertyHelper.getPropertyId("category1", "foo");
    EqualsPredicate equalsPredicate = new EqualsPredicate<>(propertyId, "bar");

    TestPredicateVisitor visitor = new TestPredicateVisitor();
    equalsPredicate.accept(visitor);

    Assert.assertSame(equalsPredicate, visitor.visitedComparisonPredicate);
    Assert.assertNull(visitor.visitedArrayPredicate);
    Assert.assertNull(visitor.visitedUnaryPredicate);

    AndPredicate andPredicate = new AndPredicate(equalsPredicate);

    visitor = new TestPredicateVisitor();
    andPredicate.accept(visitor);

    Assert.assertNull(visitor.visitedComparisonPredicate);
    Assert.assertSame(andPredicate, visitor.visitedArrayPredicate);
    Assert.assertNull(visitor.visitedUnaryPredicate);

    NotPredicate notPredicate = new NotPredicate(andPredicate);

    visitor = new TestPredicateVisitor();
    notPredicate.accept(visitor);

    Assert.assertNull(visitor.visitedComparisonPredicate);
    Assert.assertNull(visitor.visitedArrayPredicate);
    Assert.assertSame(notPredicate, visitor.visitedUnaryPredicate);


    CategoryPredicate categoryPredicate = new CategoryIsEmptyPredicate("cat1");

    visitor = new TestPredicateVisitor();
    categoryPredicate.accept(visitor);

    Assert.assertNull(visitor.visitedComparisonPredicate);
    Assert.assertNull(visitor.visitedArrayPredicate);
    Assert.assertNull(visitor.visitedUnaryPredicate);
    Assert.assertSame(categoryPredicate, visitor.visitedCategoryPredicate);
  }

  public static class TestPredicateVisitor implements PredicateVisitor {

    ComparisonPredicate visitedComparisonPredicate = null;
    ArrayPredicate visitedArrayPredicate = null;
    UnaryPredicate visitedUnaryPredicate = null;
    AlwaysPredicate visitedAlwaysPredicate = null;
    CategoryPredicate visitedCategoryPredicate = null;

    @Override
    public void acceptComparisonPredicate(ComparisonPredicate predicate) {
      visitedComparisonPredicate = predicate;
    }

    @Override
    public void acceptArrayPredicate(ArrayPredicate predicate) {
      visitedArrayPredicate = predicate;
    }

    @Override
    public void acceptUnaryPredicate(UnaryPredicate predicate) {
      visitedUnaryPredicate = predicate;
    }

    @Override
    public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
      visitedAlwaysPredicate = predicate;
    }

    @Override
    public void acceptCategoryPredicate(CategoryPredicate predicate) {
      visitedCategoryPredicate = predicate;
    }
  }
}
