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
package org.apache.drill.exec.server;

import io.netty.util.internal.OutOfDirectMemoryError;
import org.apache.drill.common.CatastrophicFailure;
import org.apache.drill.exec.exception.OutOfMemoryException;

public final class FailureUtils {
  private static volatile boolean unrecoverableFailure;

  public static final int EXIT_CODE_HEAP_OOM = -1;
  public static final int EXIT_CODE_JAVA_ERROR = -2;

  /**
   * This message is used to distinguish between direct memory and heap memory {@link OutOfMemoryError}s.
   */
  public static final String DIRECT_MEMORY_OOM_MESSAGE = "Direct buffer memory";

  private FailureUtils() {
    // Don't instantiate
  }

  public static boolean isDirectMemoryOOM(Throwable e) {
    if (e instanceof OutOfDirectMemoryError || e instanceof OutOfMemoryException) {
      // These are always direct memory errors
      return true;
    }

    return (e instanceof OutOfMemoryError) && DIRECT_MEMORY_OOM_MESSAGE.equals(e.getMessage());
  }

  public static boolean isHeapOOM(Throwable e) {
    return (e instanceof OutOfMemoryError) && !DIRECT_MEMORY_OOM_MESSAGE.equals(e.getMessage());
  }

  public static void unrecoverableFailure(Throwable e, String message, int exitCode) {
    unrecoverableFailure = true;
    CatastrophicFailure.exit(e, message, exitCode);
  }

  public static boolean hadUnrecoverableFailure() {
    return unrecoverableFailure;
  }
}
