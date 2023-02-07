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
package org.apache.drill.exec.rpc;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ThreadFactory} for {@link java.util.concurrent.ExecutorService}s that names threads sequentially.
 * Creates Threads named with the prefix specified at construction time. Created threads
 * have the daemon bit set and priority Thread.MAX_PRIORITY.
 *
 * <p>An instance creates names with an instance-specific prefix suffixed with sequential
 * integers.</p>
 *
 * <p>Concurrency: See {@link #newThread}.</p>
 */
public class NamedThreadFactory implements ThreadFactory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NamedThreadFactory.class);
  private final AtomicInteger nextId = new AtomicInteger(); // used to generate unique ids
  private final String prefix;

  /**
   * Constructor.
   *
   * @param prefix the string prefix that will be used to name threads created by this factory
   */
  public NamedThreadFactory(final String prefix) {
    this.prefix = prefix;
  }

 /**
  * Creates a sequentially named thread running a given Runnable.
  * <p>
  *   The thread's name will be this instance's prefix concatenated with
  *   this instance's next<sup><a href="#fn-1">*</a></sup> sequential integer.
  * </p>
  * <p>
  *  Concurrency:  Thread-safe.
  * </p>
  * <p>
  * (Concurrent calls get different numbers.
  *  Calls started after other calls complete get later/higher numbers than
  *  those other calls.
  * </p>
  * <p>
  *  <a name="fn-1" />*However, for concurrent calls, the order of numbers
  *  is not defined.)
  */
  @Override
  public Thread newThread(final Runnable runnable) {
    final Thread thread = new Thread(runnable, prefix + nextId.incrementAndGet());
    thread.setDaemon(true);

    try {
      if (thread.getPriority() != Thread.MAX_PRIORITY) {
        thread.setPriority(Thread.MAX_PRIORITY);
      }
    } catch (Exception ignored) {
      // Doesn't matter even if failed to set.
      logger.info("ignored exception " + ignored);
    }
    return thread;
  }
}
