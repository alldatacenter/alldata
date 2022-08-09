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
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.collections.Predicate;

/**
 * {@link PredicateClassFactory} is a factory class used to derive a {@link Predicate} implementation
 * class from its name.
 */
public class PredicateClassFactory {
  /**
   * A static map of names to {@link Class}s.
   */
  private static final Map<String, Class<? extends Predicate>> NAME_TO_CLASS;

  static {
    Map<String, Class<? extends Predicate>> map = new HashMap<>();

    map.put(AndPredicate.NAME, AndPredicate.class);
    map.put(OrPredicate.NAME, OrPredicate.class);
    map.put(NotPredicate.NAME, NotPredicate.class);
    map.put(ContainsPredicate.NAME, ContainsPredicate.class);
    map.put(EqualsPredicate.NAME, EqualsPredicate.class);

    NAME_TO_CLASS = Collections.unmodifiableMap(map);
  }

  /**
   * Return a {@link Predicate} implementation class give its name
   *
   * @param name the name of a {@link Predicate} implementation
   * @return a {@link Predicate} implementation class give its name or <code>null</code> is not found
   */
  public static Class<? extends Predicate> getPredicateClass(String name) {
    return (name == null) ? null : NAME_TO_CLASS.get(name);
  }
}
