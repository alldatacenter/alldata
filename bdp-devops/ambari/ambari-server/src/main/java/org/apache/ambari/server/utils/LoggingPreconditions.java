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
package org.apache.ambari.server.utils;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;

public class LoggingPreconditions {

  private final Logger logger;

  public LoggingPreconditions(Logger logger) {
    this.logger = logger;
  }

  public void checkNotNull(Object o, String errorMessage, Object... messageParams) {
    if (o == null) {
      logAndThrow(NullPointerException::new, errorMessage, messageParams);
    }
  }

  public void checkArgument(boolean expression, String errorMessage, Object... messageParams) {
    if (!expression) {
      logAndThrow(IllegalArgumentException::new, errorMessage, messageParams);
    }
  }

  public void checkState(boolean expression, String errorMessage, Object... messageParams) {
    if (!expression) {
      logAndThrow(IllegalStateException::new, errorMessage, messageParams);
    }
  }

  /**
   * Wraps {@code exception} in an unchecked exception created by {@code uncheckedWrapper}, and always throws the unchecked exception.
   * @return null to make the compiler happy
   */
  public <T> T wrapInUnchecked(Exception exception, BiFunction<String, Exception, RuntimeException> uncheckedWrapper, String errorMessage, Object... messageParams) {
    logAndThrow(msg -> uncheckedWrapper.apply(msg, exception), errorMessage, messageParams);
    return null; // unreachable
  }

  /**
   * Formats an error message with parameters using {@code String.format}, logs it using {@code logger},
   * and throws an unchecked exception with the same message created by {@code exceptionCreator}.
   */
  public void logAndThrow(Function<String, RuntimeException> exceptionCreator, String errorMessage, Object... messageParams) {
    String msg = String.format(errorMessage, messageParams);
    logger.error(msg);
    throw exceptionCreator.apply(msg);
  }

}
