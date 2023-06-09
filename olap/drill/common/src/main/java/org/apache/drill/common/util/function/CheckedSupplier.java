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

import java.util.function.Supplier;

import static org.apache.drill.common.exceptions.ErrorHelper.sneakyThrow;

/**
 * The java standard library does not provide a lambda function interface for functions that take no arguments,
 * but that throw an exception. So, we have to define our own here.
 * @param <T> The return type of the lambda function.
 * @param <E> The type of exception thrown by the lambda function.
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception> extends Supplier<T> {

  @Override
  default T get() {
    try {
      return getAndThrow();
    } catch (Throwable e) {
      sneakyThrow(e);
    }
    // should never happen
    throw new RuntimeException();
  }

  /**
   * Gets a result.
   *
   * @return a result
   * @throws E exception in case of errors
   */
  T getAndThrow() throws E;
}
