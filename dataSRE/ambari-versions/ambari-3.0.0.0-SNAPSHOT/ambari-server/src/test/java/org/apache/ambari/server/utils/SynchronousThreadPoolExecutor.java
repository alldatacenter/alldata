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

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An {@link Executor} which will run every command on the current thread.
 */
public class SynchronousThreadPoolExecutor extends ThreadPoolExecutor {

  /**
   * Constructor.
   *
   */
  public SynchronousThreadPoolExecutor() {
    super(1, 1, 0L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void shutdown() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Runnable> shutdownNow() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isShutdown() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTerminated() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(Runnable command) {
    command.run();
  }
}
