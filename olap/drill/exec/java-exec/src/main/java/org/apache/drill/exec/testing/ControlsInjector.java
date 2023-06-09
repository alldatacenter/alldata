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
package org.apache.drill.exec.testing;

import org.slf4j.Logger;

public interface ControlsInjector {

  /**
   * Get the injector's owning class.
   *
   * @return the injector's owning class
   */
  Class<?> getSiteClass();

  /**
   * Inject (throw) an unchecked exception at this point, if an injection is specified, and it is time
   * for it to be thrown.
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate an exception
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site descriptor
   *                          throws the exception specified by the injection, if it is time
   */
  void injectUnchecked(ExecutionControls executionControls, String desc);

  /**
   * Inject (throw) a checked exception at this point, if an injection is specified, and it is time
   * for it to be thrown.
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate an exception
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site descriptor
   * @param exceptionClass    the expected class of the exception (or a super class of it)
   * @throws T the exception specified by the injection, if it is time
   */
  <T extends Throwable> void injectChecked(ExecutionControls executionControls, String desc, Class<T> exceptionClass)
    throws T;

  /**
   * Pauses at this point, if such an injection is specified (i.e. matches the site description).
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate a pause
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site descriptor
   * @param logger            logger of the class containing the injection site
   */
  void injectPause(ExecutionControls executionControls, String desc, Logger logger);

  /**
   * Insert a pause that can be interrupted using {@link Thread#interrupt()} at the given site point, if such an
   * injection is specified (i.e. matches the site description).
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate a interruptible pause
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site descriptor
   * @param logger            logger of the class containing the injection site
   * @throws InterruptedException if interrupted using {@link Thread#interrupt()}
   */
  void injectInterruptiblePause(ExecutionControls executionControls, String desc, Logger logger)
    throws InterruptedException;

  /**
   * Used to insert a latch in site class. See {@link CountDownLatchInjection} and
   * {@link org.apache.drill.exec.testing.TestCountDownLatchInjection} for usage.
   * @param executionControls the controls in the current context
   * @param desc              the site descriptor
   */
  CountDownLatchInjection getLatch(final ExecutionControls executionControls, final String desc);

}
