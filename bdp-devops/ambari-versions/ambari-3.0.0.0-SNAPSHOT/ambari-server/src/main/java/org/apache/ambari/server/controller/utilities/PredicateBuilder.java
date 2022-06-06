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

import java.util.LinkedList;
import java.util.List;

import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.GreaterEqualsPredicate;
import org.apache.ambari.server.controller.predicate.GreaterPredicate;
import org.apache.ambari.server.controller.predicate.LessEqualsPredicate;
import org.apache.ambari.server.controller.predicate.LessPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;

/**
 * Builder for predicates.
 */
public class PredicateBuilder {

  private String propertyId;
  private List<Predicate> predicates = new LinkedList<>();
  private Operator operator = null;
  private final PredicateBuilder outer;
  private boolean done = false;
  private boolean not = false;

  public PredicateBuilder() {
    this.outer = null;
  }

  private PredicateBuilder(PredicateBuilder outer) {
    this.outer = outer;
  }

  /**
   * Allow for chaining of already constructed predicates
   */
  public PredicateBuilder(Predicate predicate) {
    this.outer = null;
    this.addPredicate(predicate);
  }

  private enum Operator {
    And,
    Or
  }

  public PredicateBuilderWithProperty property(String id) {
    checkDone();
    propertyId = id;
    return new PredicateBuilderWithProperty();
  }

  public PredicateBuilder not() {
    not = true;
    return this;
  }

  public PredicateBuilder begin() {
    checkDone();
    return new PredicateBuilder(this);
  }

  public Predicate toPredicate() {
    return getPredicate();
  }

  private void checkDone() {
    if (done) {
      throw new IllegalStateException("Can't reuse a predicate builder.");
    }
  }

  private PredicateBuilderWithPredicate getPredicateBuilderWithPredicate() {
    return new PredicateBuilderWithPredicate();
  }

  private void addPredicate(Predicate predicate) {
    predicates.add(predicate);
  }

  private void handleComparator() {
    if (operator == null) {
      return;
    }

    if (predicates.size() == 0) {
      throw new IllegalStateException("No left operand.");
    }
    Predicate predicate;

    switch (operator) {
      case And:
        predicate = new AndPredicate(predicates.toArray(new Predicate[predicates.size()]));
        break;
      case Or:
        predicate = new OrPredicate(predicates.toArray(new Predicate[predicates.size()]));
        break;
      default:
        throw new IllegalStateException("Unknown operator " + this.operator);
    }
    predicates.clear();
    addPredicate(predicate);
  }

  private Predicate getPredicate() {
    handleComparator();

    if (predicates.size() == 1) {
      Predicate predicate = predicates.get(0);
      if (not) {
        predicate = new NotPredicate(predicate);
        not = false;
      }
      return predicate;
    }
    throw new IllegalStateException("Can't return a predicate.");
  }

  public class PredicateBuilderWithProperty {

    // ----- Equals -----
    public <T>PredicateBuilderWithPredicate equals(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new EqualsPredicate<>(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    // ----- Greater than -----
    public <T>PredicateBuilderWithPredicate greaterThan(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new GreaterPredicate<>(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    // ----- Greater than equal to -----
    public <T>PredicateBuilderWithPredicate greaterThanEqualTo(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new GreaterEqualsPredicate<>(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    // ----- Less than -----
    public <T>PredicateBuilderWithPredicate lessThan(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new LessPredicate<>(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }

    // ----- Less than equal to -----
    public <T>PredicateBuilderWithPredicate lessThanEqualTo(Comparable<T> value) {
      if (propertyId == null) {
        throw new IllegalStateException("No property.");
      }
      addPredicate(new LessEqualsPredicate<>(propertyId, value));

      return new PredicateBuilderWithPredicate();
    }
  }

  public class PredicateBuilderWithPredicate {
    public PredicateBuilder and() {

      if (operator != Operator.And) {
        handleComparator();
        operator = Operator.And;
      }
      return PredicateBuilder.this;
    }

    public PredicateBuilder or() {

      if (operator != Operator.Or) {
        handleComparator();
        operator = Operator.Or;
      }
      return PredicateBuilder.this;
    }

    public Predicate toPredicate() {
      if (outer != null) {
        throw new IllegalStateException("Unbalanced block - missing end.");
      }
      done = true;
      return getPredicate();
    }

    public PredicateBuilderWithPredicate end() {
      if (outer == null) {
        throw new IllegalStateException("Unbalanced block - missing begin.");
      }
      outer.addPredicate(getPredicate());
      return outer.getPredicateBuilderWithPredicate();
    }
  }
}
