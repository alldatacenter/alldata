/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import org.apache.iceberg.io.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class CloseableIteratorWrapper<T> implements CloseableIterator<T> {

  private static final Logger LOG = LoggerFactory.getLogger(CloseableIteratorWrapper.class);

  private Closeable[] closeables;
  private CloseableIterator<T> closeableIterator;

  public CloseableIteratorWrapper(CloseableIterator<T> closeableIterator, Closeable... closeables) {
    this.closeableIterator = closeableIterator;
    this.closeables = closeables;
  }

  @Override
  public void close() throws IOException {
    boolean closeFailure = false;
    if (closeables != null) {
      for (Closeable closeable: closeables) {
        if (closeable != null) {
          try {
            closeable.close();
          } catch (Throwable t) {
            closeFailure = true;
            LOG.error("Exception suppressed when attempting to close resources", t);
          }
        }
      }
    }
    closeableIterator.close();
    if (closeFailure) {
      throw new IOException("Some error encounter when close these Closeable. Please see details in error log");
    }
  }

  @Override
  public boolean hasNext() {
    return closeableIterator.hasNext();
  }

  @Override
  public T next() {
    return closeableIterator.next();
  }
}
