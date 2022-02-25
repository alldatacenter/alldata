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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.collections.Predicate;

/**
 * {@link ContainsPredicate} is a predicate implementation testing whether a string value exists
 * within some set of strings.
 * <p>
 * It it expected that the data supplied to the {@link #evaluate(Object)} method is as {@link Set}
 * of {@link String}s retrieved from a context (a {@link Map} of keys to values, where each value
 * can be an explicit value or a {@link Map} of values creating a tree structure.
 */
public class ContainsPredicate extends OperationPredicate {

  /**
   * The name of this {@link Predicate} implementation
   */
  public static final String NAME = "contains";

  /**
   * The value to test for
   */
  private final String value;

  /**
   * Creates a new {@link ContainsPredicate} using the given {@link Map} of data.
   * <p>
   * It is expected that the map contains a single {@link java.util.Map.Entry} where the key name
   * is "contains" and the value is a {@link Collection} of 2 {@link String}s. The first value
   * should be the key to use when retrieving data from the context, which should resolve to a
   * {@link Set} of {@link String}s (or null). The second value should be the value to check for
   * existing in the relevant set.
   *
   * @return a new {@link ContainsPredicate}
   */
  public static ContainsPredicate fromMap(Map<String, Object> map) {

    Object data = (map == null) ? null : map.get(NAME);

    if (data == null) {
      throw new IllegalArgumentException("Missing data for '" + NAME + "' operation");
    } else if (data instanceof Collection) {
      Collection<?> collection = (Collection) data;
      if (collection.size() == 2) {
        Iterator<?> iterator = collection.iterator();
        Object d1 = iterator.next();
        Object d2 = iterator.next();

        if ((d1 instanceof String) && (d2 instanceof String)) {
          return new ContainsPredicate(new ContextTransformer((String) d1), (String) d2);
        } else {
          throw new IllegalArgumentException(String.format("Unexpected data types: %s and %s", d1.getClass().getName(), d2.getClass().getName()));
        }
      } else {
        throw new IllegalArgumentException(String.format("Missing data for '" + NAME + "' operation - 2 predicates are needed, %d found", collection.size()));
      }
    } else {
      throw new IllegalArgumentException(String.format("Unexpected data type for '" + NAME + "' operation - %s", data.getClass().getName()));
    }
  }

  /**
   * Constructor
   *
   * @param transformer the {@link ContextTransformer} configured with the context key to find
   * @param value       the value to test
   */
  public ContainsPredicate(ContextTransformer transformer, String value) {
    super(NAME, transformer);
    this.value = value;
  }

  /**
   * Gets the test value
   *
   * @return a string
   */
  public String getValue() {
    return value;
  }

  @Override
  protected boolean evaluateTransformedData(Object data) {
    return (this.value != null) && (data instanceof Set) && ((Set<?>) data).contains(this.value);
  }

  @Override
  public Map<String, Object> toMap() {
    return Collections.singletonMap(NAME,
      new ArrayList<>(Arrays.asList(getContextKey(), value)));
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        (37 * ((this.value == null) ? 0 : this.value.hashCode()));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (super.equals(obj) && (obj instanceof ContainsPredicate) && (hashCode() == obj.hashCode())) {
      ContainsPredicate p = (ContainsPredicate) obj;
      return (value == null) ? (p.value == null) : value.equals(p.value);
    } else {
      return false;
    }
  }
}
