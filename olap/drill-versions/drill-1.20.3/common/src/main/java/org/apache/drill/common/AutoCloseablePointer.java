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

/**
 * A class similar to Pointer<>, but with features unique to holding
 * AutoCloseable pointers. The AutoCloseablePointer<> must be closed
 * when it will no longer be used.
 *
 * <p>If you're familiar with C++/Boost's shared_ptr<>, you might recognize
 * some of the features here.</p>
 *
 * @param <T> type of the pointer
 */
public final class AutoCloseablePointer<T extends AutoCloseable> implements AutoCloseable {
  private T pointer;

  /**
   * Constructor for a null-valued pointer.
   */
  public AutoCloseablePointer() {
    pointer = null;
  }

  /**
   * Constructor for a pointer value.
   *
   * @param pointer the initial pointer value
   */
  public AutoCloseablePointer(final T pointer) {
    this.pointer = pointer;
  }

  @Override
  public void close() throws Exception {
    assign(null);
  }

  /**
   * Get the raw pointer out for use.
   *
   * @return the raw pointer
   */
  public T get() {
    return pointer;
  }

  /**
   * The caller adopts the pointer; the holder is set to
   * null, and will no longer be responsible for close()ing this pointer.
   *
   * @return the pointer being adopted; may be null
   */
  public T adopt() {
    final T p = pointer;
    pointer = null;
    return p;
  }

  /**
   * Assign a new pointer to this holder. Any currently held pointer
   * will first be closed. If closing the currently held pointer throws
   * an exception, the new pointer is still assigned, and the holder must still
   * be closed to close that.
   *
   * <p>This makes it convenient to assign a new pointer without having to check
   * for a previous value and worry about cleaning it up; this does all that.</p>
   *
   * @param newP the new pointer to hold
   * @throws Exception any exception thrown by closing the currently held
   *   pointer
   */
  public void assign(final T newP) throws Exception {
    try {
      if (pointer != null) {
        pointer.close();
      }
    } finally {
      pointer = newP;
    }
  }

  /**
   * Like {@link #assign(AutoCloseable)}, except that any exception thrown
   * by closing the previously held pointer is wrapped with (an unchecked)
   * {@link RuntimeException}.
   *
   * @param newP the new pointer to hold
   */
  public void assignNoChecked(final T newP) {
    try {
      assign(newP);
    } catch(final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
