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

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for closing @{link Closeable}s.
 *
 * @see com.google.common.io.Closeables
 */
public final class Closeables {

  private static final Logger LOGGER = LoggerFactory.getLogger(Closeables.class);

  /**
   * Closes <code>c</code> ignoring any <code>IOException</code>s.
   */
  public static void closeSilently(Closeable c) {
    close(c, null);
  }

  /**
   * Closes <code>c</code> logging any <code>IOException</code> as warning via this class' logger.
   */
  public static void closeLoggingExceptions(Closeable c) {
    closeLoggingExceptions(c, LOGGER);
  }

  /**
   * Closes <code>c</code> logging any <code>IOException</code> via the specified <code>logger</code>.
   */
  public static void closeLoggingExceptions(Closeable c, Logger logger) {
    close(c, logger);
  }

  private static void close(Closeable c, Logger logger) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        if (logger != null) {
          logger.warn("IOException while closing Closeable", e);
        }
      }
    }
  }

  private Closeables() {
    throw new UnsupportedOperationException("No instances");
  }

}
