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
package org.apache.ambari.server.orm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.annotations.TransactionalLock.LockArea;
import org.apache.ambari.server.configuration.Configuration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link TransactionalLocks} class is used to manage the locks associated
 * with each {@link LockArea}. It's a singlegon that shoudl always be injected.
 */
@Singleton
public class TransactionalLocks {

  /**
   * Used to lookup whether {@link LockArea}s are enabled.
   */
  private final Configuration m_configuration;

  /**
   * Manages the locks for each class which uses the {@link Transactional}
   * annotation.
   */
  private final ConcurrentHashMap<LockArea, ReadWriteLock> m_locks;


  /**
   * Constructor.
   *
   */
  @Inject
  private TransactionalLocks(Configuration configuration) {
    m_configuration = configuration;
    m_locks = new ConcurrentHashMap<>();

    for (LockArea lockArea : LockArea.values()) {
      final ReadWriteLock lock;
      if (lockArea.isEnabled(m_configuration)) {
        lock = new ReentrantReadWriteLock(true);
      } else {
        lock = new NoOperationReadWriteLock();
      }

      m_locks.put(lockArea, lock);
    }
  }

  /**
   * Gets a lock for the specified lock area. There is a 1:1 relationship
   * between a lock area and a lock.
   * <p/>
   * If the {@link LockArea} is not enabled, then this will return an empty
   * {@link Lock} implementation which doesn't actually lock anything.
   *
   * @param lockArea
   *          the lock area to get the lock for (not {@code null}).
   * @return the lock to use for the specified lock area (never {@code null}).
   */
  public ReadWriteLock getLock(LockArea lockArea) {
    return m_locks.get(lockArea);
  }

  /**
   * A dummy implementation of a {@link ReadWriteLock} that returns locks which
   * only NOOP. This is used for cases where dependant code doesn't want to
   * {@code if/else} all over the place.
   */
  private final static class NoOperationReadWriteLock implements ReadWriteLock {

    private final Lock m_readLock = new NoOperationLock();
    private final Lock m_writeLock = new NoOperationLock();

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock readLock() {
      return m_readLock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock writeLock() {
      return m_writeLock;
    }
  }

  /**
   * A dummy implementation of a {@link Lock} that only NOOPs. This is used for
   * cases where dependant code doesn't want to {@code if/else} all over the
   * place.
   */
  private final static class NoOperationLock implements Lock {

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void lock() {
    }

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
    }

    /**
     * NOOP, returns {@code true} always.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock() {
      return true;
    }

    /**
     * NOOP, returns {@code true} always.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return true;
    }

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void unlock() {
    }

    /**
     * NOOP
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public Condition newCondition() {
      return null;
    }
  }
}
