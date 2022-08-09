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

package org.apache.ambari.server.collections.functors;

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.collections.Predicate;
import org.apache.commons.collections.functors.PredicateDecorator;

/**
 * DelegatedSinglePredicateContainer is an abstract class providing functionality to managing a
 * delegate used to hold a since {@link Predicate}s. For example <code>not</code>.
 */
abstract class DelegatedSinglePredicateContainer extends Predicate implements PredicateDecorator {

  /**
   * The delegate {@link PredicateDecorator} used to handle the internal logic for the container
   */
  private final PredicateDecorator delegate;

  /**
   * Constructor.
   *
   * @param name     the name of this predicate
   * @param delegate the delegate used to handle the internal logic for the container
   */
  DelegatedSinglePredicateContainer(String name, PredicateDecorator delegate) {
    super(name);
    this.delegate = delegate;
  }

  @Override
  public Map<String, Object> toMap() {
    return Collections.singletonMap(getName(), containedPredicateToMap());
  }

  @Override
  public boolean evaluate(Object o) {
    return delegate.evaluate(o);
  }

  @Override
  public org.apache.commons.collections.Predicate[] getPredicates() {
    return delegate.getPredicates();
  }

  @Override
  public int hashCode() {
    return super.hashCode() + ((delegate == null) ? 0 : delegate.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (super.equals(obj) && (obj instanceof DelegatedSinglePredicateContainer) && (hashCode() == obj.hashCode())) {
      DelegatedSinglePredicateContainer p = (DelegatedSinglePredicateContainer) obj;
      return (delegate == null) ? (p.delegate == null) : delegate.equals(p.delegate);
    } else {
      return false;
    }
  }

  /**
   * Processes the contained predicate into a Map.
   * <p>
   * This is used to serialize this {@link DelegatedSinglePredicateContainer} into a {@link Map}
   *
   * @return a map representing the contained predicate
   */
  private Map<String, Object> containedPredicateToMap() {

    Map<String, Object> map = null;

    if (delegate != null) {
      org.apache.commons.collections.Predicate[] predicates = delegate.getPredicates();

      if ((predicates != null) && (predicates.length > 0)) {
        // Only process the 1st predicate.
        org.apache.commons.collections.Predicate p = predicates[0];
        if (p instanceof Predicate) {
          map = ((Predicate) p).toMap();
        } else {
          throw new UnsupportedOperationException(String.format("Cannot convert a %s to a Map", p.getClass().getName()));
        }
      }
    }

    return map;
  }
}
