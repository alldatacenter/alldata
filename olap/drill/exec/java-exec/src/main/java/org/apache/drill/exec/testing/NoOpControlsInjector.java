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

/**
 * An injector that does not inject any controls, useful when not testing (i.e. assertions are not enabled).
 * See {@link ControlsInjector} for documentation.
 */
public final class NoOpControlsInjector implements ControlsInjector {

  private final Class<?> clazz;

  /**
   * Constructor. Classes should use the static {@link ControlsInjectorFactory#getInjector} method to obtain their
   * injector.
   *
   * @param clazz the owning class
   */
  protected NoOpControlsInjector(final Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public Class<?> getSiteClass() {
    return clazz;
  }

  @Override
  public void injectUnchecked(final ExecutionControls executionControls, final String desc) {
  }

  @Override
  public <T extends Throwable> void injectChecked(
    final ExecutionControls executionControls, final String desc, final Class<T> exceptionClass) throws T {
  }

  @Override
  public void injectPause(final ExecutionControls executionControls, final String desc, final Logger logger) {
  }

  @Override
  public void injectInterruptiblePause(final ExecutionControls executionControls, final String desc,
                                       final Logger logger) throws InterruptedException {
  }

  /**
   * When assertions are not enabled, this count down latch that does nothing is injected.
   */
  public static final CountDownLatchInjection LATCH = new CountDownLatchInjection() {
    @Override
    public void initialize(int count) {
    }

    @Override
    public void await() {
    }

    @Override
    public void awaitUninterruptibly() {
    }

    @Override
    public void countDown() {
    }

    @Override
    public void close() {
    }
  };

  @Override
  public CountDownLatchInjection getLatch(final ExecutionControls executionControls, final String desc) {
    return LATCH;
  }
}
