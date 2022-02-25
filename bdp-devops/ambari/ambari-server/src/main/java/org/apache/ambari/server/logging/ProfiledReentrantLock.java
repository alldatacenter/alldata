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
package org.apache.ambari.server.logging;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Ticker;

/**
 * Implements profiling for a ReentrantLock.
 */
final class ProfiledReentrantLock implements ProfiledLock {

  private final ReentrantLock delegate;
  private final LockProfileDelegate helper;

  /**
   * @param delegate the lock to profile
   * @param ticker is the source of time information, replaceable for testing purpose
   * @param label is included in log messages for easier identification of locks (optional, may be empty or null)
   */
  ProfiledReentrantLock(ReentrantLock delegate, Ticker ticker, String label) {
    this.delegate = delegate;
    helper = new LockProfileDelegate(ticker, label, this);
  }

  @Override
  public void lock() {
    boolean alreadyOwned = helper.logRequest();
    delegate.lock();
    helper.logRequestCompleted(alreadyOwned, true);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    boolean alreadyOwned = helper.logRequest();
    delegate.lockInterruptibly();
    helper.logRequestCompleted(alreadyOwned, true);
  }

  @Override
  public boolean tryLock() {
    boolean alreadyOwned = helper.logRequest();
    boolean result = delegate.tryLock();
    helper.logRequestCompleted(alreadyOwned, result);
    return result;
  }

  @Override
  public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    boolean alreadyOwned = helper.logRequest();
    boolean result = delegate.tryLock(timeout, unit);
    helper.logRequestCompleted(alreadyOwned, result);
    return result;
  }

  @Override
  public void unlock() {
    delegate.unlock();
    helper.logUnlock();
  }

  @Override
  public Condition newCondition() {
    return delegate.newCondition();
  }

  @Override
  public boolean isHeldByCurrentThread() {
    return delegate.isHeldByCurrentThread();
  }

  @Override
  public Map<String, Long> getTimeSpentWaitingForLock() {
    return helper.getTimeSpentWaitingForLock();
  }

  @Override
  public Map<String, Long> getTimeSpentLocked() {
    return helper.getTimeSpentLocked();
  }

  @Override
  public Map<String, Integer> getLockCount() {
    return helper.getLockCount();
  }

  @Override
  public String getLabel() {
    return helper.getLabel();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
