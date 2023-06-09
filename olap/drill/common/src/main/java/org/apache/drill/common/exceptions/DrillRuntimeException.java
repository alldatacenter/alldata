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
package org.apache.drill.common.exceptions;

public class DrillRuntimeException extends RuntimeException {
  private static final long serialVersionUID = -3796081521525479249L;

  public DrillRuntimeException() {
    super();
  }

  public DrillRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public DrillRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public DrillRuntimeException(String message) {
    super(message);
  }

  public DrillRuntimeException(Throwable cause) {
    super(cause);
  }

  public static DrillRuntimeException create(String format, Object...args) {
    return create(null, format, args);
  }

  public static DrillRuntimeException create(Throwable cause, String format, Object...args) {
    return new DrillRuntimeException(String.format(format, args), cause);
  }

  /**
   * This method can be called within loops to check whether the current thread has been
   * interrupted; it ensures that operator implementation can respond to query cancellation
   * in a timely manner.
   *
   * <p>Calling this method will result in the following behavior:
   * <ul>
   * <li>Throws a runtime exception if current thread interrupt flag has been set
   * <li>Clears current thread interrupt flag
   * </ul>
   */
  public static void checkInterrupted() {
    if (Thread.interrupted()) {
      // This exception will ensure the control layer will immediately get back control
      throw new DrillRuntimeException("Interrupt received; aborting current operation");
    }
  }
}
