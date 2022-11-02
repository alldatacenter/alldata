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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.collections.Predicate;
import org.apache.commons.collections.functors.PredicateDecorator;

/**
 * DelegatedMultiplePredicateContainer is an abstract class providing functionality related to
 * managing a delegate used to hold multiple {@link Predicate}s. For example <code>and</code> and
 * <code>or</code> operators.
 */
abstract class DelegatedMultiplePredicateContainer extends Predicate implements PredicateDecorator {

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
  DelegatedMultiplePredicateContainer(String name, PredicateDecorator delegate) {
    super(name);
    this.delegate = delegate;
  }

  @Override
  public Map<String, Object> toMap() {
    return Collections.singletonMap(getName(), containedPredicatesToMaps());
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
    } else if (super.equals(obj) && (obj instanceof DelegatedMultiplePredicateContainer) && (hashCode() == obj.hashCode())) {
      DelegatedMultiplePredicateContainer p = (DelegatedMultiplePredicateContainer) obj;
      return (delegate == null) ? (p.delegate == null) : delegate.equals(p.delegate);
    } else {
      return false;
    }
  }

  /**
   * Processes the contained predicates into a {@link List} of {@link Map}s.
   * <p>
   * This is used to serialize this {@link DelegatedMultiplePredicateContainer} into a {@link Map}
   *
   * @return a list of maps representing the contained predicates
   */
  private List<Map<String, Object>> containedPredicatesToMaps() {

    List<Map<String, Object>> list = new ArrayList<>();

    if (delegate != null) {
      org.apache.commons.collections.Predicate[] predicates = delegate.getPredicates();

      if (predicates != null) {
        for (org.apache.commons.collections.Predicate p : predicates) {
          if (p instanceof Predicate) {
            list.add(((Predicate) p).toMap());
          } else {
            throw new UnsupportedOperationException(String.format("Cannot convert a %s to a Map", p.getClass().getName()));
          }
        }
      }
    }

    return list;
  }

}
