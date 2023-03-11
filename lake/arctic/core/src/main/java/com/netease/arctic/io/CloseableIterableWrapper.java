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

import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;

import java.io.Closeable;
import java.io.IOException;

public class CloseableIterableWrapper<T> implements CloseableIterable<T> {

  private Closeable[] closeables;
  private CloseableIterable<T> inner;

  public CloseableIterableWrapper(CloseableIterable<T> inner, Closeable... closeables) {
    this.inner = inner;
    this.closeables = closeables;
  }

  @Override
  public CloseableIterator<T> iterator() {
    CloseableIterator<T> closeableIterator = inner.iterator();
    return new CloseableIteratorWrapper<>(closeableIterator, closeables);
  }

  @Override
  public void close() throws IOException {
    inner.close();
  }
}
