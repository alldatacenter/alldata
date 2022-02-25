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

import org.apache.ambari.server.collections.Predicate;
import org.apache.commons.collections.functors.EqualPredicate;

/**
 * {@link EqualsPredicate} wraps {@link org.apache.commons.collections.functors.EqualPredicate} to
 * provide additional functionality like serializing to and from a Map and JSON formatted data as well
 * as obtaining data using a {@link ContextTransformer}
 */
public class EqualsPredicate extends OperationPredicate {

  /**
   * The name of this {@link Predicate} implementation
   */
  public static final String NAME = "equals";

  /**
   * The {@link org.apache.commons.collections.functors.EqualPredicate} to delegate operations to
   */
  private final org.apache.commons.collections.functors.EqualPredicate delegate;

  /**
   * Creates a new {@link EqualsPredicate} using the given {@link Map} of data.
   * <p>
   * It is expected that the map contains a single {@link java.util.Map.Entry} where the key name
   * is "equals" and the value is a {@link Collection} of 2 {@link String}s. The first value
   * should be the key to use when retrieving data from the context, which should resolve to a
   * {@link String} (or null). The second value should be the value to use when checking for equality.
   *
   * @return a new {@link EqualsPredicate}
   */
  public static EqualsPredicate fromMap(Map<String, Object> map) {
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
          return new EqualsPredicate(new ContextTransformer((String) d1), (String) d2);
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
   * Constructor.
   *
   * @param transformer the {@link ContextTransformer}
   * @param value       the value to test
   */
  public EqualsPredicate(ContextTransformer transformer, String value) {
    super(NAME, transformer);
    delegate = new EqualPredicate(value);
  }

  /**
   * Gets the test value
   *
   * @return a string
   */
  public String getValue() {
    Object o = (delegate == null) ? null : delegate.getValue();
    return (o == null) ? null : o.toString();
  }

  @Override
  public Map<String, Object> toMap() {
    return Collections.singletonMap(NAME,
      new ArrayList<>(Arrays.asList(getContextKey(), delegate.getValue().toString())));
  }

  @Override
  public boolean evaluateTransformedData(Object data) {
    return delegate.evaluate(data);
  }
}

