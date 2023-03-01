/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.service.deploy.worker.congestcontrol;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A time sliding list that group different {@link TimeSlidingNode} with corresponding timestamp by
 * exact interval 1 second. Internally hold a {@link sumNode} to get the sum of the nodes in the
 * list.
 *
 * <p>This list is thread-safe, but {@link TimeSlidingNode} returned by the method {@link sum}
 * should only be readable, do not try to update it.
 */
public abstract class TimeSlidingHub<N extends TimeSlidingHub.TimeSlidingNode> {

  /**
   * This class internally used by {@link TimeSlidingHub} to identify each node value.
   *
   * <p>The implementation should make all methods thread-safe as it could be accessed by different
   * threads.
   */
  public interface TimeSlidingNode extends Cloneable {

    /** Merge new node with this. */
    void combineNode(TimeSlidingNode node);

    /** Minus the value from the {@param node}. */
    void separateNode(TimeSlidingNode node);

    TimeSlidingNode clone();
  }

  // 1 second.
  private final int intervalPerBucketInMills = 1000;
  private final int maxQueueSize;
  private N sumNode;

  private final LinkedBlockingDeque<Pair<Long, N>> _deque;

  public TimeSlidingHub(int timeWindowsInSecs) {
    this._deque = new LinkedBlockingDeque<>();
    this.maxQueueSize = timeWindowsInSecs * 1000 / intervalPerBucketInMills;
    this.sumNode = newEmptyNode();
  }

  public N sum() {
    return sumNode;
  }

  public void add(N newNode) {
    long currentTimestamp = currentTimeMillis();
    add(currentTimestamp, newNode);
  }

  public synchronized void add(long currentTimestamp, N newNode) {
    if (_deque.size() == 0) {
      _deque.add(Pair.of(currentTimestamp, (N) newNode.clone()));
      sumNode = (N) newNode.clone();
      return;
    }

    Pair<Long, N> lastNode = _deque.getLast();

    long timeDiff = currentTimestamp - lastNode.getLeft();

    if (timeDiff >= intervalPerBucketInMills) {
      // The node doesn't belong to the lastNode, there might be 2 different scenarios
      // 1. All existing nodes are out of date, should be removed
      // 2. some nodes are out of date, should be removed
      long nodesToAdd = timeDiff / intervalPerBucketInMills;
      if (nodesToAdd >= maxQueueSize) {
        // The new node exceed existing sliding list, need to clear all old nodes
        // and create a new sliding list
        _deque.clear();
        _deque.add(Pair.of(currentTimestamp, (N) newNode.clone()));
        sumNode = (N) newNode.clone();
        return;
      }

      // Add new node at the end of the list, and deprecate nodes out of timeInterval
      for (long i = 1; i < nodesToAdd; i++) {
        N toAdd = newEmptyNode();
        lastNode = Pair.of(lastNode.getLeft() + intervalPerBucketInMills, toAdd);
        _deque.add(lastNode);
      }

      _deque.add(Pair.of(lastNode.getLeft() + intervalPerBucketInMills, (N) newNode.clone()));
      sumNode.combineNode(newNode);

      while (_deque.size() > maxQueueSize) {
        Pair<Long, N> removed = _deque.removeFirst();
        sumNode.separateNode(removed.getRight());
      }
      return;
    }

    if (timeDiff < 0) {
      // Belong to one existing node
      Iterator<Pair<Long, N>> iter = _deque.descendingIterator();
      while (iter.hasNext()) {
        Pair<Long, N> curNode = iter.next();
        if (currentTimestamp - curNode.getLeft() >= 0) {
          curNode.getRight().combineNode(newNode);
          sumNode.combineNode(newNode);
          return;
        }
      }

      // Out of the time window, ignore this value
      return;
    }

    // Belong to last node
    lastNode.getRight().combineNode(newNode);
    sumNode.combineNode(newNode);
  }

  public void clear() {
    synchronized (_deque) {
      _deque.clear();
      sumNode = newEmptyNode();
    }
  }

  protected abstract N newEmptyNode();

  protected int getCurrentTimeWindowsInMills() {
    return _deque.size() * intervalPerBucketInMills;
  }

  @VisibleForTesting
  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
