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
package org.apache.drill.common.concurrent;

import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ForwardingListenableFuture;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A delegating wrapper around a {@link ListenableFuture} that adds support for the {@link
 * #checkedGet()} and {@link #checkedGet(long, TimeUnit)} methods.
 *
 * This class is moved from Guava, since there it was deleted.
 */
public abstract class AbstractCheckedFuture<T, E extends Exception>
    extends ForwardingListenableFuture.SimpleForwardingListenableFuture<T> implements CheckedFuture<T, E> {

  protected AbstractCheckedFuture(ListenableFuture<T> delegate) {
    super(delegate);
  }

  /**
   * Translates from an {@link InterruptedException},
   * {@link CancellationException} or {@link ExecutionException} thrown by
   * {@code get} to an exception of type {@code X} to be thrown by
   * {@code checkedGet}. Subclasses must implement this method.
   *
   * <p>If {@code e} is an {@code InterruptedException}, the calling
   * {@code checkedGet} method has already restored the interrupt after catching
   * the exception. If an implementation of {@link #mapException(Exception)}
   * wishes to swallow the interrupt, it can do so by calling
   * {@link Thread#interrupted()}.
   *
   * <p>Subclasses may choose to throw, rather than return, a subclass of
   * {@code RuntimeException} to allow creating a CheckedFuture that throws
   * both checked and unchecked exceptions.
   */
  protected abstract E mapException(Exception e);

  /**
   * Exception checking version of {@link Future#get()} that will translate
   * {@link InterruptedException}, {@link CancellationException} and
   * {@link ExecutionException} into application-specific exceptions.
   *
   * @return the result of executing the future.
   * @throws E on interruption, cancellation or execution exceptions.
   */
  public T checkedGet() throws E {
    try {
      return get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw mapException(e);
    } catch (CancellationException | ExecutionException e) {
      throw mapException(e);
    }
  }

  /**
   * Exception checking version of {@link Future#get(long, TimeUnit)} that will
   * translate {@link InterruptedException}, {@link CancellationException} and
   * {@link ExecutionException} into application-specific exceptions.  On
   * timeout this method throws a normal {@link TimeoutException}.
   *
   * @return the result of executing the future.
   * @throws TimeoutException if retrieving the result timed out.
   * @throws E on interruption, cancellation or execution exceptions.
   */
  public T checkedGet(long timeout, TimeUnit unit) throws TimeoutException, E {
    try {
      return get(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw mapException(e);
    } catch (CancellationException | ExecutionException e) {
      throw mapException(e);
    }
  }
}
