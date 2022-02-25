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

import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Ticker;

public class ProfiledReentrantReadWriteLockTest extends EasyMockSupport {

  private static final String LABEL = "label";

  @Test
  public void lockingReadLockOnlyLocksReadLock() {
    ReentrantReadWriteLock delegate = new ReentrantReadWriteLock();
    ProfiledReentrantReadWriteLock testSubject = new ProfiledReentrantReadWriteLock(delegate, Ticker.systemTicker(), LABEL);

    testSubject.readLock().lock();

    Assert.assertEquals(1, delegate.getReadHoldCount());
    Assert.assertEquals(0, delegate.getWriteHoldCount());
  }

  @Test
  public void lockingWriteLockOnlyLocksWriteLock() {
    ReentrantReadWriteLock delegate = new ReentrantReadWriteLock();
    ProfiledReentrantReadWriteLock testSubject = new ProfiledReentrantReadWriteLock(delegate, Ticker.systemTicker(), LABEL);

    testSubject.writeLock().lock();

    Assert.assertEquals(0, delegate.getReadHoldCount());
    Assert.assertEquals(1, delegate.getWriteHoldCount());
  }

  @Test
  public void timeWaitingForReadLockIsRecorded() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantReadWriteLock(new ReentrantReadWriteLock(), ticker, LABEL).readLock();
    timeWaitingForLockIsRecorded(testSubject, ticker);
  }

  @Test
  public void timeWaitingForWriteLockIsRecorded() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantReadWriteLock(new ReentrantReadWriteLock(), ticker, LABEL).writeLock();
    timeWaitingForLockIsRecorded(testSubject, ticker);
  }

  private void timeWaitingForLockIsRecorded(ProfiledLock testSubject, Ticker ticker) {
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(1L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(4L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(5L));
    replayAll();

    testSubject.lock();

    Assert.assertEquals(Collections.singletonMap(Thread.currentThread().getName(), 4L - 1L), testSubject.getTimeSpentWaitingForLock());
    verifyAll();
  }

  @Test
  public void timeReadLockSpentLockedIsRecorded() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantReadWriteLock(new ReentrantReadWriteLock(), ticker, LABEL).readLock();
    timeSpentLockedIsRecorded(ticker, testSubject);
  }

  @Test
  public void timeWriteLockSpentLockedIsRecorded() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantReadWriteLock(new ReentrantReadWriteLock(), ticker, LABEL).writeLock();
    timeSpentLockedIsRecorded(ticker, testSubject);
  }

  @Test
  public void timeLockSpentLockedIsRecorded() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantLock(new ReentrantLock(), ticker, LABEL);
    timeSpentLockedIsRecorded(ticker, testSubject);
  }

  private void timeSpentLockedIsRecorded(Ticker ticker, ProfiledLock testSubject) {
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(0L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(0L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(6L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(13L));
    replayAll();

    testSubject.lock();
    testSubject.unlock();

    Assert.assertEquals(Collections.singletonMap(Thread.currentThread().getName(), 13L - 6L), testSubject.getTimeSpentLocked());
    verifyAll();
  }

  @Test
  public void onlyOutermostLockUnlockIsProfiledForReadLock() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantReadWriteLock(new ReentrantReadWriteLock(), ticker, LABEL).readLock();
    onlyOutermostLockUnlockIsProfiled(testSubject, ticker);
  }

  @Test
  public void onlyOutermostLockUnlockIsProfiledForWriteLock() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantReadWriteLock(new ReentrantReadWriteLock(), ticker, LABEL).readLock();
    onlyOutermostLockUnlockIsProfiled(testSubject, ticker);
  }

  @Test
  public void onlyOutermostLockUnlockIsProfiled() {
    Ticker ticker = createMock(Ticker.class);
    ProfiledLock testSubject = new ProfiledReentrantLock(new ReentrantLock(), ticker, LABEL);
    onlyOutermostLockUnlockIsProfiled(testSubject, ticker);
  }

  private void onlyOutermostLockUnlockIsProfiled(ProfiledLock testSubject, Ticker ticker) {
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(0L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(0L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(5L));
    expect(ticker.read()).andReturn(TimeUnit.MILLISECONDS.toNanos(19L));
    replayAll();

    testSubject.lock();
    testSubject.lock();
    testSubject.unlock();
    testSubject.unlock();

    Assert.assertEquals(Collections.singletonMap(Thread.currentThread().getName(), 19L - 5L), testSubject.getTimeSpentLocked());
    Assert.assertEquals(Collections.singletonMap(Thread.currentThread().getName(), 1), testSubject.getLockCount());
    verifyAll();
  }

}
