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
package org.apache.drill.test;

import ch.qos.logback.classic.Level;

import org.apache.drill.common.util.function.CheckedSupplier;
import org.apache.drill.exec.client.LoggingResultsListener;
import org.apache.drill.exec.util.VectorUtil;

import java.util.function.Supplier;

/**
 * <p>
 *   This class contains utility methods to run lambda functions with the necessary {@link org.apache.drill.test.LogFixture}
 *   boilerplate to print results to stdout for debugging purposes.
 * </p>
 *
 * <p>
 *   If you need to enable printing for more classes, simply add them to the {@link org.apache.drill.test.LogFixture}
 *   constructed in {@link #printAndThrow(CheckedSupplier)}.
 * </p>
 */
public final class PrintingUtils {

  /**
   * Enables printing to stdout for lambda functions that do not throw exceptions.
   * @param supplier Lambda function to execute.
   * @param <T> The return type of the lambda function.
   * @return Data produced by the lambda function.
   */
  public static <T> T print(Supplier<T> supplier) {
    return printAndThrow(supplier::get);
  }

  /**
   * Enables printing to stdout for lambda functions that throw an exception.
   * @param supplier Lambda function to execute.
   * @param <T> Return type of the lambda function.
   * @param <E> Type of exception thrown.
   * @return Data produced by the lambda function.
   * @throws E An exception.
   */
  public static <T, E extends Exception> T printAndThrow(CheckedSupplier<T, E> supplier) throws E {
    try(LogFixture logFixture = new LogFixture.LogFixtureBuilder()
      .rootLogger(Level.OFF)
      // For some reason rootLogger(Level.OFF) is not sufficient.
      .logger("org.apache.drill", Level.OFF) // Disable logging for Drill class we don't want
      .logger(VectorUtil.class, Level.INFO)
      .logger(LoggingResultsListener.class, Level.INFO)
      .toConsole() // This redirects output to stdout
      .build()) {
      return supplier.get();
    }
  }
}
