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
package org.apache.drill.common;

/**
 * Provides functionality comparable to Guava's Closeables for AutoCloseables.
 */
public class DrillAutoCloseables {
  /**
   * Constructor. Prevents construction for class of static utilities.
   */
  private DrillAutoCloseables() {
  }

  /**
   * close() an {@link java.lang.AutoCloseable} without throwing a (checked)
   * {@link java.lang.Exception}. This wraps the close() call with a
   * try-catch that will rethrow an Exception wrapped with a
   * {@link java.lang.RuntimeException}, providing a way to call close()
   * without having to do the try-catch everywhere or propagate the Exception.
   *
   * @param autoCloseable the AutoCloseable to close; may be null
   * @throws RuntimeException if an Exception occurs; the Exception is
   *   wrapped by the RuntimeException
   */
  public static void closeNoChecked(final AutoCloseable autoCloseable) {
    if (autoCloseable != null) {
      try {
        autoCloseable.close();
      } catch(final Exception e) {
        throw new RuntimeException("Exception while closing", e);
      }
    }
  }
}
