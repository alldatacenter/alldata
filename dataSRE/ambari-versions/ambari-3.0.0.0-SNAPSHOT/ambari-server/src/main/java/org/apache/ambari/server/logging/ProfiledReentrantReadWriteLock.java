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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Ticker;

/**
 * Implements profiling for a ReentrantReadWriteLock.
 */
final class ProfiledReentrantReadWriteLock implements ReadWriteLock {

  private final ProfiledReadLock readLock;
  private final ProfiledWriteLock writeLock;

  /**
   * @param delegate the lock to profile
   * @param ticker is the source of time information, replaceable for testing purpose
   * @param label is included in log messages for easier identification of locks (optional, may be empty or null)
   */
  ProfiledReentrantReadWriteLock(ReentrantReadWriteLock delegate, Ticker ticker, String label) {
    readLock = new ProfiledReadLock(delegate, ticker, label);
    writeLock = new ProfiledWriteLock(delegate, ticker, label);
  }

  @Override
  public ProfiledLock readLock() {
    return readLock;
  }

  @Override
  public ProfiledLock writeLock() {
    return writeLock;
  }

  private static class ProfiledReadLock extends ReentrantReadWriteLock.ReadLock implements ProfiledLock {

    private final LockProfileDelegate helper;
    private final ReentrantReadWriteLock delegate;

    ProfiledReadLock(ReentrantReadWriteLock delegate, Ticker ticker, String label) {
      super(delegate);
      this.delegate = delegate;
      helper = new LockProfileDelegate(ticker, label, this);
    }

    @Override
    public void lock() {
      boolean alreadyOwned = helper.logRequest();
      super.lock();
      helper.logRequestCompleted(alreadyOwned, true);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      boolean alreadyOwned = helper.logRequest();
      super.lockInterruptibly();
      helper.logRequestCompleted(alreadyOwned, true);
    }

    @Override
    public boolean tryLock() {
      boolean alreadyOwned = helper.logRequest();
      boolean result = super.tryLock();
      helper.logRequestCompleted(alreadyOwned, result);
      return result;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      boolean alreadyOwned = helper.logRequest();
      boolean result = super.tryLock(timeout, unit);
      helper.logRequestCompleted(alreadyOwned, result);
      return result;
    }

    @Override
    public void unlock() {
      super.unlock();
      helper.logUnlock();
    }

    /**
     * @return true if any thread holds this lock (note that the read lock may be shared between threads, hence the different semantics)
     */
    @Override
    public boolean isHeldByCurrentThread() {
      return delegate.getReadHoldCount() > 0;
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
    public String toString() {
      return delegate.readLock().toString();
    }

    @Override
    public String getLabel() {
      return helper.getLabel();
    }
  }

  private static class ProfiledWriteLock extends ReentrantReadWriteLock.WriteLock implements ProfiledLock {

    private final LockProfileDelegate helper;
    private final ReentrantReadWriteLock delegate;

    ProfiledWriteLock(ReentrantReadWriteLock delegate, Ticker ticker, String label) {
      super(delegate);
      this.delegate = delegate;
      helper = new LockProfileDelegate(ticker, label, this);
    }

    @Override
    public void lock() {
      boolean alreadyOwned = helper.logRequest();
      super.lock();
      helper.logRequestCompleted(alreadyOwned, true);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      boolean alreadyOwned = helper.logRequest();
      super.lockInterruptibly();
      helper.logRequestCompleted(alreadyOwned, true);
    }

    @Override
    public boolean tryLock() {
      boolean alreadyOwned = helper.logRequest();
      boolean result = super.tryLock();
      helper.logRequestCompleted(alreadyOwned, result);
      return result;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      boolean alreadyOwned = helper.logRequest();
      boolean result = super.tryLock(timeout, unit);
      helper.logRequestCompleted(alreadyOwned, result);
      return result;
    }

    @Override
    public void unlock() {
      super.unlock();
      helper.logUnlock();
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
    public String toString() {
      return delegate.writeLock().toString();
    }

    @Override
    public String getLabel() {
      return helper.getLabel();
    }
  }
}
