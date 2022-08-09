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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

/**
 * Predicate which evaluates an array of predicates.
 */
public abstract class ArrayPredicate implements BasePredicate {
  private final Predicate[] predicates;
  private final Set<String> propertyIds = new HashSet<>();

  // ----- Constructors -----------------------------------------------------

  /**
   * Constructor.
   *
   * @param predicates  the predicates
   */
  public ArrayPredicate(Predicate... predicates) {
    this.predicates = predicates;
    for (Predicate predicate : predicates) {
      propertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }
  }


  // ----- BasePredicate ----------------------------------------------------

  @Override
  public Set<String> getPropertyIds() {
    return propertyIds;
  }


  // ----- PredicateVisitorAcceptor -----------------------------------------

  @Override
  public void accept(PredicateVisitor visitor) {
    visitor.acceptArrayPredicate(this);
  }


  // ----- ArrayPredicate ---------------------------------------------------

  public abstract String getOperator();

  /**
   * Factory method.
   *
   * @param predicates  the predicate array
   *
   * @return a new ArrayPredicate
   */
  public abstract Predicate create(Predicate... predicates);


  // ----- accessors --------------------------------------------------------

  /**
   * Get the predicates.
   *
   * @return the predicates
   */
  public Predicate[] getPredicates() {
    return predicates;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArrayPredicate)) return false;

    ArrayPredicate that = (ArrayPredicate) o;

    if (propertyIds != null ? !propertyIds.equals(that.propertyIds) : that.propertyIds != null) return false;

    // don't care about array order
    Set<Predicate> setThisPredicates = new HashSet<>(Arrays.asList(predicates));
    Set<Predicate> setThatPredicates = new HashSet<>(Arrays.asList(that.predicates));
    return setThisPredicates.equals(setThatPredicates);
  }

  @Override
  public int hashCode() {
    // don't care about array order
    int result = predicates != null ? new HashSet<>(Arrays.asList(predicates)).hashCode() : 0;
    result = 31 * result + (propertyIds != null ? propertyIds.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (Predicate predicate : predicates) {

      boolean arrayPredicate = predicate instanceof ArrayPredicate;

      if (sb.length() > 0) {
        sb.append(" ").append(getOperator()).append(" ");
      }

      if (arrayPredicate) {
        sb.append("(").append(predicate).append(")");
      } else {
        sb.append(predicate);
      }
    }
    return sb.toString();
  }
}
