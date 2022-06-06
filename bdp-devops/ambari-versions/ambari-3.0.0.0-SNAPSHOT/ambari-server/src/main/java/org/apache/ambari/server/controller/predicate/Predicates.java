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

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.apache.ambari.server.controller.spi.Predicate;

public class Predicates {

  /**
   * Creates an {@link OrPredicate} of the given predicates if any.
   *
   * @param predicates collection of predicates to be OR-ed
   * @return {@link Optional} of the {@code OrPredicate} if any predicates are given,
   *   otherwise an empty {@code Optional}
   */
  public static Optional<Predicate> anyOf(Collection<? extends Predicate> predicates) {
    return predicates != null && !predicates.isEmpty() ? Optional.of(OrPredicate.of(predicates)) : Optional.empty();
  }

  /**
   * Creates a {@link Function} which, when called, creates an {@link AndPredicate} of
   * its input and the {@code presetPredicate}.  The function can then be used to transform
   * {@code Optional}s or streams.
   *
   * @param presetPredicate this predicate will be AND-ed with the input to the {@code Function}
   * @return the {@code Function}
   */
  public static Function<Predicate, Predicate> and(Predicate presetPredicate) {
    return predicate -> new AndPredicate(presetPredicate, predicate);
  }
}
