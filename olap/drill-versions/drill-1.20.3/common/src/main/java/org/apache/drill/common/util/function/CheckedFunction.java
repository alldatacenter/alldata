/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.util.function;

import java.util.function.Function;

import static org.apache.drill.common.exceptions.ErrorHelper.sneakyThrow;

/**
 * Extension of {@link Function} that allows to throw checked exception.
 *
 * @param <T> function argument type
 * @param <R> function result type
 * @param <E> exception type
 */
@FunctionalInterface
public interface CheckedFunction<T, R, E extends Throwable> extends Function<T, R> {

  /**
   * Overrides {@link Function#apply(Object)} method to allow calling functions that throw checked exceptions.
   * Is useful when used in methods that accept {@link Function}.
   * For example: {@link java.util.Map#computeIfAbsent(Object, Function)}.
   *
   * @param t the function argument
   * @return the function result
   */
  @Override
  default R apply(T t) {
    try {
      return applyAndThrow(t);
    } catch (Throwable e) {
      sneakyThrow(e);
    }
    // should never happen
    throw new RuntimeException();
  }

  /**
   * Applies function to the given argument.
   *
   * @param t the function argument
   * @return the function result
   * @throws E exception in case of errors
   */
  R applyAndThrow(T t) throws E;

}

