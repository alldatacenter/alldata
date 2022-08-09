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

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests for bas category predicate.
 */
public class CategoryPredicateTest {
  @Test
  public void testAccept() {
    String propertyId = PropertyHelper.getPropertyId("category1", "foo");
    TestCategoryPredicate predicate = new TestCategoryPredicate(propertyId);

    TestPredicateVisitor visitor = new TestPredicateVisitor();
    predicate.accept(visitor);

    Assert.assertSame(predicate, visitor.visitedCategoryPredicate);
  }

  public static class TestCategoryPredicate extends CategoryPredicate {

    public TestCategoryPredicate(String propertyId) {
      super(propertyId);
    }

    @Override
    public boolean evaluate(Resource resource) {
      return false;
    }
  }

  public static class TestPredicateVisitor implements PredicateVisitor {

    CategoryPredicate visitedCategoryPredicate = null;

    @Override
    public void acceptComparisonPredicate(ComparisonPredicate predicate) {
    }

    @Override
    public void acceptArrayPredicate(ArrayPredicate predicate) {
    }

    @Override
    public void acceptUnaryPredicate(UnaryPredicate predicate) {
    }

    @Override
    public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
    }

    @Override
    public void acceptCategoryPredicate(CategoryPredicate predicate) {
      visitedCategoryPredicate = predicate;
    }
  }
}
