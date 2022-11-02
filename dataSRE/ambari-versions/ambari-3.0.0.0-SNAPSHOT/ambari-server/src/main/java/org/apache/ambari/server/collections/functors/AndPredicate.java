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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.ambari.server.collections.PredicateUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.PredicateDecorator;

/**
 * {@link AndPredicate} wraps {@link org.apache.commons.collections.functors.AndPredicate} to
 * provide additional functionality like serializing to and from a Map and JSON formatted data.
 * <p>
 * See {@link DelegatedMultiplePredicateContainer}
 */
public class AndPredicate extends DelegatedMultiplePredicateContainer {
  /**
   * The name of this {@link org.apache.ambari.server.collections.Predicate} implementation
   */
  public static final String NAME = "and";

  /**
   * Creates a new {@link AndPredicate} using the given {@link Map} of data.
   * <p>
   * It is expected that the map contains a single {@link java.util.Map.Entry} where the key name
   * is "and" and the value is a {@link Collection} of {@link Map}s representing the contained
   * predicates.
   *
   * @return a new {@link AndPredicate}
   */
  public static AndPredicate fromMap(Map<String, Object> map) {
    Object data = (map == null) ? null : map.get(NAME);

    if (data == null) {
      throw new IllegalArgumentException("Missing data for '" + NAME + "' operation");
    } else if (data instanceof Collection) {
      Collection<?> collection = (Collection) data;
      if (collection.size() == 2) {
        Iterator<?> iterator = collection.iterator();
        Object d1 = iterator.next();
        Object d2 = iterator.next();

        if ((d1 instanceof Map) && (d2 instanceof Map)) {
          return new AndPredicate(PredicateUtils.fromMap((Map) d1), PredicateUtils.fromMap((Map) d2));
        } else {
          throw new IllegalArgumentException(String.format("Unexpected data types for predicates: %s and %s", d1.getClass().getName(), d2.getClass().getName()));
        }
      } else {
        throw new IllegalArgumentException(String.format("Missing data for '" + NAME + "' operation - 2 predicates are needed, %d found", collection.size()));
      }
    } else {
      throw new IllegalArgumentException(String.format("Unexpected data type for '" + NAME + "' operation - %s", data.getClass().getName()));
    }
  }

  /**
   * Constructor.
   *
   * @param predicate1 the first predicate to process
   * @param predicate2 the second predicate to process (if the first one yields <code>true</code>
   */
  public AndPredicate(Predicate predicate1, Predicate predicate2) {
    super(NAME,
        (PredicateDecorator) org.apache.commons.collections.functors.AndPredicate.getInstance(predicate1, predicate2));
  }

  @Override
  public int hashCode() {
    return super.hashCode() + this.getClass().getName().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj == this) ||
        ((obj != null) && (super.equals(obj) && this.getClass().isInstance(obj) && (hashCode() == obj.hashCode())));
  }
}
