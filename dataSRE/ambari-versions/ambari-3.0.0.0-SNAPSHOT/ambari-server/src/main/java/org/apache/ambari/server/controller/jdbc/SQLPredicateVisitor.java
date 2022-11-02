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

package org.apache.ambari.server.controller.jdbc;

import org.apache.ambari.server.controller.predicate.AlwaysPredicate;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.CategoryPredicate;
import org.apache.ambari.server.controller.predicate.ComparisonPredicate;
import org.apache.ambari.server.controller.predicate.PredicateVisitor;
import org.apache.ambari.server.controller.predicate.UnaryPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Predicate visitor used to generate a SQL where clause from a predicate graph.
 */
public class SQLPredicateVisitor implements PredicateVisitor {

  /**
   * The string builder.
   */
  private final StringBuilder stringBuilder = new StringBuilder();


  // ----- PredicateVisitor --------------------------------------------------

  @Override
  public void acceptComparisonPredicate(ComparisonPredicate predicate) {
    String propertyId = predicate.getPropertyId();

    String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);
    if (propertyCategory != null) {
      stringBuilder.append(propertyCategory).append(".");
    }
    stringBuilder.append(PropertyHelper.getPropertyName(propertyId));

    stringBuilder.append(" ").append(predicate.getOperator()).append(" \"");
    stringBuilder.append(predicate.getValue());
    stringBuilder.append("\"");

  }

  @Override
  public void acceptArrayPredicate(ArrayPredicate predicate) {
    Predicate[] predicates = predicate.getPredicates();
    if (predicates.length > 0) {

      stringBuilder.append("(");
      for (int i = 0; i < predicates.length; i++) {
        if (i > 0) {
          stringBuilder.append(" ").append(predicate.getOperator()).append(" ");
        }
        PredicateHelper.visit(predicates[i], this);
      }
      stringBuilder.append(")");
    }
  }

  @Override
  public void acceptUnaryPredicate(UnaryPredicate predicate) {
    stringBuilder.append(predicate.getOperator()).append("(");
    PredicateHelper.visit(predicate.getPredicate(), this);
    stringBuilder.append(")");
  }

  @Override
  public void acceptAlwaysPredicate(AlwaysPredicate predicate) {
    stringBuilder.append("TRUE");
  }

  @Override
  public void acceptCategoryPredicate(CategoryPredicate predicate) {
    // Do nothing
  }


  // ----- SQLPredicateVisitor -----------------------------------------------

  public String getSQL() {
    return stringBuilder.toString();
  }
}
