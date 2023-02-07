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

import java.io.PrintStream;


public class CatastrophicFailure {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CatastrophicFailure.class);

  private CatastrophicFailure() {
  }

  /**
   * Exit the VM as we hit a catastrophic failure.
   * @param e
   *          The Throwable that occurred
   * @param message
   *          A descriptive message
   * @param code
   *          An error code to exit the JVM with.
   */
  public static void exit(Throwable e, String message, int code) {
    logger.error("Catastrophic Failure Occurred, exiting. Information message: {}", message, e);

    final PrintStream out = ("true".equals(System.getProperty("drill.catastrophic_to_standard_out", "true"))) ? System.out
        : System.err;
    out.println("Catastrophic failure occurred. Exiting. Information follows: " + message);
    e.printStackTrace(out);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e2) {
    }
    System.exit(code);
  }
}
