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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Ticker;

/**
 * A helper object for Lock implementations that need logging/profiling.
 * <p>
 * It collects the time spent waiting for the lock, the time spent holding the lock,
 * and the number of times the lock was taken.  Lock requests, acquisitions and releases
 * are also logged at debug level.  The log message for requests contains a filtered stack
 * trace, only including methods from org.apache.ambari and subpackages to eliminate the noise.
 */
final class LockProfileDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(LockProfileDelegate.class);

  private final ThreadLocal<Long> lockRequestTime = new ThreadLocal<>();
  private final ThreadLocal<Long> lockAcquireTime = new ThreadLocal<>();
  private final ConcurrentMap<String, Long> timeSpentWaitingForLock = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Long> timeSpentLocked = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Integer> lockCount = new ConcurrentHashMap<>();
  private final String label;
  private final ProfiledLock lock;
  private final Ticker ticker;

  /**
   * @param ticker is the source of time information, replaceable for testing purpose
   * @param label is included in log messages (optional, may be empty or null)
   * @param lock the lock to profile
   */
  LockProfileDelegate(Ticker ticker, String label, ProfiledLock lock) {
    this.label = addSpacePostfixIfNeeded(label);
    this.lock = lock;
    this.ticker = ticker;
  }

  /**
   * @return the label for this lock (including an extra space at the end, if the label is otherwise not empty)
   */
  String getLabel() {
    return label;
  }

  /**
   * @return time spent waiting for the lock (in milliseconds) by thread name
   */
  Map<String, Long> getTimeSpentWaitingForLock() {
    return new TreeMap<>(timeSpentWaitingForLock);
  }

  /**
   * @return time spent holding the lock (in milliseconds) by thread name
   */
  Map<String, Long> getTimeSpentLocked() {
    return new TreeMap<>(timeSpentLocked);
  }

  /**
   * @return the number of times the lock was taken by thread name
   */
  Map<String, Integer> getLockCount() {
    return new TreeMap<>(lockCount);
  }

  /**
   * Should be called by the lock to indicate that the lock was requested.
   *
   * @return whether the lock was already owned by the current thread
   */
  boolean logRequest() {
    boolean alreadyOwned = lock.isHeldByCurrentThread();
    if (!alreadyOwned) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("{}request {} from {}", label, lock, getFilteredStackTrace());
      }
      lockRequestTime.set(ticker.read());
    }
    return alreadyOwned;
  }

  /**
   * Should be called by the lock to indicate that the request for the lock was completed,
   * either successfully (acquired) or unsuccessfully (not acquired due to timeout, etc.).
   *
   * @param alreadyOwned whether the current thread already owned the lock before the request (if true, the call is ignored)
   * @param acquired whether the lock was acquired by the requestor
   */
  void logRequestCompleted(boolean alreadyOwned, boolean acquired) {
    if (!alreadyOwned) {
      if (acquired) {
        long elapsed = storeElapsedTime(lockRequestTime, timeSpentWaitingForLock);
        LOG.debug("{}acquired {} after {} ms", label, lock, elapsed);
        increment(lockCount);
        lockAcquireTime.set(ticker.read());
      } else {
        LOG.debug("{}failed to acquire {}", label, lock);
      }
    }
  }

  /**
   * Should be called by the lock to indicate that the lock was unlocked.  For reentrant locks this may or may not
   * mean the lock was actually released.
   */
  void logUnlock() {
    boolean released = !lock.isHeldByCurrentThread();
    if (released) {
      long elapsed = storeElapsedTime(lockAcquireTime, timeSpentLocked);
      if (LOG.isDebugEnabled()) {
        LOG.debug("{}released {} after {} ms", label, lock, elapsed);
      }
    }
  }

  private long storeElapsedTime(ThreadLocal<Long> startHolder, ConcurrentMap<String, Long> map) {
    long end = ticker.read();
    long elapsed = Long.MIN_VALUE;
    Long start = startHolder.get();
    if (start != null && start <= end) {
      elapsed = TimeUnit.NANOSECONDS.toMillis(end - start);
      String name = Thread.currentThread().getName();
      map.putIfAbsent(name, 0L);
      if (elapsed > 0) {
        map.put(name, map.get(name) + elapsed);
      }
    }
    startHolder.remove();
    return elapsed;
  }

  private static void increment(ConcurrentMap<String, Integer> map) {
    String name = Thread.currentThread().getName();
    map.putIfAbsent(name, 0);
    map.put(name, map.get(name) + 1);
  }

  private static String addSpacePostfixIfNeeded(String label) {
    if (label == null) {
      return "";
    }
    label = label.trim();
    return label.length() > 0 ? label + " " : label;
  }

  private static String getFilteredStackTrace() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    StringBuilder sb = new StringBuilder();
    for (StackTraceElement element : stackTrace) {
      String className = element.getClassName();
      if (className.startsWith("org.apache.ambari") && !className.startsWith("org.apache.ambari.server.logging")) {
        sb.append(className)
          .append("#").append(element.getMethodName())
          .append("(").append(element.getFileName())
          .append(":").append(element.getLineNumber())
          .append(");\t");
      }
    }
    return sb.toString();
  }

}
