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

package com.netease.arctic.optimizer.util;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * not thread-safe
 * Fix capacity, Reverse order traversal, Circular Array.
 */
public class CircularArray<E> implements Iterable<E>, Serializable {
  private final Object[] elementData;
  private int currentIndex;

  public CircularArray(int capacity) {
    Preconditions.checkArgument(capacity > 0);
    this.elementData = new Object[capacity];
    this.currentIndex = 0;
  }

  /**
   * not thread-safe
   *
   * @param element -
   */
  public void add(E element) {
    elementData[currentIndex] = element;
    currentIndex++;
    if (currentIndex == elementData.length) {
      currentIndex = 0;
    }
  }

  public void clear() {
    Arrays.fill(elementData, null);
    currentIndex = 0;
  }
  
  @Override
  public Iterator<E> iterator() {
    return new ListIterator<>(currentIndex);
  }

  @Override
  public void forEach(Consumer<? super E> action) {
    Iterable.super.forEach(action);
  }

  @Override
  public Spliterator<E> spliterator() {
    return Iterable.super.spliterator();
  }

  private class ListIterator<E> implements Iterator<E> {
    private final int startIndex;
    private int nowIndex;
    private boolean first = true;

    public ListIterator(int startIndex) {
      this.startIndex = startIndex;
      this.nowIndex = startIndex;
    }

    @Override
    public boolean hasNext() {
      if (first) {
        return true;
      }
      return nowIndex != startIndex;
    }

    private int getNextIndex() {
      int next = nowIndex - 1;
      if (next == -1) {
        next = elementData.length - 1;
      }
      return next;
    }

    @Override
    public E next() {
      first = false;
      nowIndex = getNextIndex();
      return (E) elementData[nowIndex];
    }
  }
}
