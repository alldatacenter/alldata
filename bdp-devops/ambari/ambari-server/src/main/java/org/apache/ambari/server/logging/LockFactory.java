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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Ticker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Factory to create locks depending on configuration.  If lock profiling is enabled,
 * it creates instrumented locks that collect statistics and log requests.  If profiling is
 * disabled, it creates regular reentrant locks.
 *
 * @see Configuration#isServerLocksProfilingEnabled()
 */
@Singleton
public class LockFactory {

  private static final Logger LOG = LoggerFactory.getLogger(LockFactory.class);

  private final boolean profiling;
  private final Set<ProfiledLock> profiledLocks;

  @Inject
  public LockFactory(Configuration config) {
    profiling = config.isServerLocksProfilingEnabled();
    profiledLocks = profiling ? new CopyOnWriteArraySet<>() : null;
    LOG.info("Lock profiling is {}", profiling ? "enabled" : "disabled");
  }

  /**
   * @return a new Lock instance (implementation depends on configuration setting) without any special label to identify it in log messages
   */
  public Lock newLock() {
    return newLock(getDefaultPrefix());
  }

  /**
   * @return a new Lock instance (implementation depends on configuration setting) with <code>label</code> to identify it in log messages
   */
  public Lock newLock(String label) {
    ReentrantLock baseLock = new ReentrantLock();
    if (profiling) {
      ProfiledReentrantLock profiledLock = new ProfiledReentrantLock(baseLock, Ticker.systemTicker(), label);
      profiledLocks.add(profiledLock);
      return profiledLock;
    }
    return baseLock;
  }

  /**
   * @return a new ReadWriteLock instance (implementation depends on configuration setting) without any special label to identify it in log messages
   */
  public ReadWriteLock newReadWriteLock() {
    return newReadWriteLock(getDefaultPrefix());
  }

  /**
   * @return a new ReadWriteLock instance (implementation depends on configuration setting) with <code>label</code> to identify it in log messages
   */
  public ReadWriteLock newReadWriteLock(String label) {
    ReentrantReadWriteLock baseLock = new ReentrantReadWriteLock();
    if (profiling) {
      ProfiledReentrantReadWriteLock profiledLock = new ProfiledReentrantReadWriteLock(baseLock, Ticker.systemTicker(), label);
      profiledLocks.add(profiledLock.readLock());
      profiledLocks.add(profiledLock.writeLock());
      return profiledLock;
    }
    return baseLock;
  }

  /**
   * If lock profiling is enabled, append summary statistics about lock usage to <code>sb</code>
   * @param sb the buffer to append the statistics to
   */
  public void debugDump(StringBuilder sb) {
    if (profiling) {
      sb.append("\n\t\tLocks: [");
      for (ProfiledLock lock : profiledLocks) {
        sb.append("\n\t\t\t").append(lock.getLabel())
          .append(lock)
          .append(" waited: ").append(lock.getTimeSpentWaitingForLock())
          .append(" held: ").append(lock.getTimeSpentLocked())
          .append(" times locked: ").append(lock.getLockCount());
      }
      if (!profiledLocks.isEmpty()) {
        sb.append("\n");
      }
      sb.append("]");
    }
  }

  private static String getDefaultPrefix() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    // 0: getStackTrace()
    // 1: getDefaultPrefix()
    // 2: newLock()
    // 3: caller of newLock()
    return stackTrace.length > 3
      ? stackTrace[3].getFileName() + ":" + stackTrace[3].getLineNumber()
      : "";
  }
}
