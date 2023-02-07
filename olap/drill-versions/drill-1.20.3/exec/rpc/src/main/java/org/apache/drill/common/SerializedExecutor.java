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

import java.util.LinkedList;
import java.util.concurrent.Executor;

/**
 * Serializes execution of multiple submissions to a single target, while still
 * using a thread pool to execute those submissions. Provides an implicit
 * queueing capability for a single target that requires any commands that
 * execute against it to be serialized.
 */
public abstract class SerializedExecutor implements Executor {

  private boolean isProcessing = false;
  private final LinkedList<Runnable> queuedRunnables = new LinkedList<>();
  private final Executor underlyingExecutor;
  private final String name;

  /**
   * Constructor.
   *
   * @param underlyingExecutor
   *          underlying executor to use to execute commands submitted to this
   *          SerializedExecutor
   */
  public SerializedExecutor(String name, Executor underlyingExecutor) {
    this.underlyingExecutor = underlyingExecutor;
    this.name = name;
  }

  /**
   * An exception occurred in the last command executed; this reports that to
   * the subclass of SerializedExecutor.
   *
   * <p>
   * The default implementation of this method throws an exception, which is
   * considered an error (see below). Implementors have two alternatives:
   * Arrange not to throw from your commands' run(), or if they do, provide an
   * override of this method that handles any exception that is thrown.
   * </p>
   *
   * <p>
   * It is an error for this to throw an exception, and doing so will terminate
   * the thread with an IllegalStateException. Derivers must handle any reported
   * exceptions in other ways.
   * </p>
   *
   * @param command
   *          the command that caused the exception
   * @param t
   *          the exception
   */
  protected abstract void runException(Runnable command, Throwable t);

  private class RunnableProcessor implements Runnable {

    private Runnable command;

    public RunnableProcessor(Runnable command) {
      this.command = command;
    }

    @Override
    public void run() {
      final Thread currentThread = Thread.currentThread();
      final String originalThreadName = currentThread.getName();
      currentThread.setName(name);

      try {
        while (true) {
          try {
            command.run();
          } catch (Exception | AssertionError e) {
            try {
              runException(command, e);
            } catch (Exception | AssertionError ee) {
              throw new IllegalStateException("Exception handler threw an exception", ee);
            }
          }
          synchronized (queuedRunnables) {
            if (queuedRunnables.isEmpty()) {
              isProcessing = false;
              break;
            }

            command = queuedRunnables.removeFirst();
          }
        }
      } finally {
        currentThread.setName(originalThreadName);
      }
    }
  }

  @Override
  public void execute(Runnable command) {
    synchronized (queuedRunnables) {
      if (isProcessing) {
        queuedRunnables.addLast(command);
        return;
      }

      isProcessing = true;
    }

    underlyingExecutor.execute(new RunnableProcessor(command));
  }
}
