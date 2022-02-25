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
package org.apache.ambari.server.controller.logging;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

/**
 * Utility class to hold static convenience methods for
 * the LogSearch integration layer
 *
 */
public class Utils {

  private static int WAIT_COUNT_MAX = 1000;

  // dis-allow creating separate instances of this class, since
  // only static methods will be allowed on this convenience class
  private Utils() {}

  static void logErrorMessageWithThrowableWithCounter(Logger logger, AtomicInteger atomicInteger, String errorMessage, Throwable throwable) {
    logErrorMessageWithThrowableWithCounter(logger, atomicInteger, errorMessage, throwable, WAIT_COUNT_MAX);
  }


  static void logErrorMessageWithThrowableWithCounter(Logger logger, AtomicInteger atomicInteger, String errorMessage, Throwable throwable, int maxCount) {
    if (atomicInteger.getAndIncrement() == 0) {
      // only log the message once every maxCount attempts
      logger.error(errorMessage, throwable);
    } else {
      // if we've hit maxCount attempts, reset the counter
      atomicInteger.compareAndSet(maxCount, 0);
    }
  }

  static void logErrorMessageWithCounter(Logger logger, AtomicInteger atomicInteger, String errorMessage) {
    logErrorMessageWithCounter(logger, atomicInteger, errorMessage, WAIT_COUNT_MAX);
  }

  static void logErrorMessageWithCounter(Logger logger, AtomicInteger atomicInteger, String errorMessage, int maxCount) {
    if (atomicInteger.getAndIncrement() == 0) {
      // only log the message once every maxCount attempts
      logger.error(errorMessage);
    } else {
      // if we've hit maxCount attempts, reset the counter
      atomicInteger.compareAndSet(maxCount, 0);
    }
  }

  static void logDebugMessageWithCounter(Logger logger, AtomicInteger atomicInteger, String errorMessage) {
    logDebugMessageWithCounter(logger, atomicInteger, errorMessage, WAIT_COUNT_MAX);
  }

  static void logDebugMessageWithCounter(Logger logger, AtomicInteger atomicInteger, String debugMessage, int maxCount) {
    if (atomicInteger.getAndIncrement() == 0) {
      // only log the message once every maxCount attempts
      logger.debug(debugMessage);
    } else {
      // if we've hit maxCount attempts, reset the counter
      atomicInteger.compareAndSet(maxCount, 0);
    }
  }
}
