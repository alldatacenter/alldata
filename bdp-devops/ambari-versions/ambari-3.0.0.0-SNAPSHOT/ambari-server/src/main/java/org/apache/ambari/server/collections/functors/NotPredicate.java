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

import java.util.Map;

import org.apache.ambari.server.collections.PredicateUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.PredicateDecorator;

/**
 * {@link NotPredicate} wraps {@link org.apache.commons.collections.functors.NotPredicate} to
 * provide additional functionality like serializing to and from a Map and JSON formatted data.
 * <p>
 * See {@link DelegatedSinglePredicateContainer}
 */
public class NotPredicate extends DelegatedSinglePredicateContainer {

  /**
   * The name of this {@link org.apache.ambari.server.collections.Predicate} implementation
   */
  public static final String NAME = "not";

  /**
   * Creates a new {@link NotPredicate} using the given {@link Map} of data.
   * <p>
   * It is expected that the map contains a single {@link java.util.Map.Entry} where the key name
   * is "not" and the value is a {@link Map} representing the contained predicate.
   *
   * @return a new {@link NotPredicate}
   */
  public static NotPredicate fromMap(Map<String, Object> map) {
    Object data = (map == null) ? null : map.get(NAME);

    if (data == null) {
      throw new IllegalArgumentException("Missing data for '" + NAME + "' operation");
    } else if (data instanceof Map) {
      return new NotPredicate(PredicateUtils.fromMap((Map) data));
    } else {
      throw new IllegalArgumentException("Missing data for '" + NAME + "' operation");
    }
  }

  /**
   * Constructor.
   *
   * @param predicate the predicate to negate
   */
  public NotPredicate(Predicate predicate) {
    super(NAME,
        (PredicateDecorator) org.apache.commons.collections.functors.NotPredicate.getInstance(predicate));
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
