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

import java.util.concurrent.TimeUnit;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;

/**
 * Injects exceptions and pauses at execution time for testing. Any class that wants to simulate exceptions
 * or inject pauses for testing should have it's own private static instance of an injector (similar to the use
 * of loggers). Injection site either use {@link FragmentContextImpl} or
 * {@link org.apache.drill.exec.ops.QueryContext}.
 * See {@link ControlsInjector} for documentation.
 */
public class ExecutionControlsInjector implements ControlsInjector {

  private final Class<?> clazz; // the class that owns this injector

  /**
   * Constructor. Classes should use the static {@link ControlsInjectorFactory#getInjector} method to obtain their
   * injector.
   *
   * @param clazz the owning class
   */
  protected ExecutionControlsInjector(final Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public Class<?> getSiteClass() {
    return clazz;
  }

  @Override
  public void injectUnchecked(final ExecutionControls executionControls, final String desc) {
    Preconditions.checkNotNull(executionControls);
    final ExceptionInjection exceptionInjection = executionControls.lookupExceptionInjection(this, desc);
    if (exceptionInjection != null) {
      exceptionInjection.throwUnchecked();
    }
  }

  @Override
  public <T extends Throwable> void injectChecked(final ExecutionControls executionControls, final String desc,
                                                  final Class<T> exceptionClass) throws T {
    Preconditions.checkNotNull(executionControls);
    final ExceptionInjection exceptionInjection = executionControls.lookupExceptionInjection(this, desc);
    if (exceptionInjection != null) {
      exceptionInjection.throwChecked(exceptionClass);
    }
  }

  @Override
  public void injectPause(final ExecutionControls executionControls, final String desc, final Logger logger) {
    Preconditions.checkNotNull(executionControls);
    final PauseInjection pauseInjection =
      executionControls.lookupPauseInjection(this, desc);

    if (pauseInjection != null) {
      long pauseDuration = pauseInjection.getMsPause();
      if ( pauseDuration > 0L) {
        logger.debug("Pausing at {} for {} sec", desc, TimeUnit.MILLISECONDS.toSeconds(pauseDuration) );
      } else {
        logger.debug("Pausing at {}", desc);
      }
      pauseInjection.pause();
      logger.debug("Resuming at {}", desc);
    }
  }

  @Override
  public void injectInterruptiblePause(final ExecutionControls executionControls, final String desc,
      final Logger logger) throws InterruptedException {
    Preconditions.checkNotNull(executionControls);
    final PauseInjection pauseInjection = executionControls.lookupPauseInjection(this, desc);

    if (pauseInjection != null) {
      logger.debug("Interruptible pausing at {}", desc);
      try {
        pauseInjection.interruptiblePause();
      } catch (final InterruptedException e) {
        logger.debug("Pause interrupted at {}", desc);
        throw e;
      }
      logger.debug("Interruptible pause resuming at {}", desc);
    }
  }

  @Override
  public CountDownLatchInjection getLatch(final ExecutionControls executionControls, final String desc) {
    Preconditions.checkNotNull(executionControls);
    return executionControls.lookupCountDownLatchInjection(this, desc);
  }
}
