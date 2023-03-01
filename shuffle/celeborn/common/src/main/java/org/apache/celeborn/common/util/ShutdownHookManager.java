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

package org.apache.celeborn.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;

/**
 * The ShutdownHookManager enables running shutdownHook in a deterministic order, higher priority
 * first.
 *
 * <p>The JVM runs ShutdownHooks in a non-deterministic order or in parallel. This class registers a
 * single JVM shutdownHook and run all the shutdownHooks registered to it (to this class) in order
 * based on their priority.
 *
 * <p>Unless a hook was registered with a shutdown explicitly set through {@link
 * #addShutdownHook(Runnable, int, long, TimeUnit)}, the shutdown time allocated to it is set by the
 * configuration option `celeborn.worker.graceful.shutdown.timeout`.
 *
 * <p>Note: code refer to Hadoop's ShutdownHookManager.
 */
public final class ShutdownHookManager {

  private static final ShutdownHookManager MGR = new ShutdownHookManager();

  private static final Logger LOG = LoggerFactory.getLogger(ShutdownHookManager.class);

  /** Minimum shutdown timeout: {@value} millisecond(s). */
  public static final long TIMEOUT_MINIMUM = 1000;

  /** The default time unit used: seconds. */
  public static final TimeUnit TIME_UNIT_DEFAULT = TimeUnit.MILLISECONDS;

  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("shutdown-hook-%01d").build());

  static {
    try {
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread() {
                @Override
                public void run() {
                  if (MGR.shutdownInProgress.getAndSet(true)) {
                    LOG.info("Shutdown process invoked a second time: ignoring");
                    return;
                  }
                  long started = System.currentTimeMillis();
                  int timeoutCount = executeShutdown();
                  long ended = System.currentTimeMillis();
                  LOG.debug(
                      String.format(
                          "Completed shutdown in %.3f seconds; Timeouts: %d",
                          (ended - started) / 1000.0, timeoutCount));
                  // each of the hooks have executed; now shut down the
                  // executor itself.
                  shutdownExecutor(new CelebornConf());
                }
              });
    } catch (IllegalStateException ex) {
      // JVM is being shut down. Ignore
      LOG.warn("Failed to add the ShutdownHook", ex);
    }
  }

  /**
   * Execute the shutdown. This is exposed purely for testing: do not invoke it.
   *
   * @return the number of shutdown hooks which timed out.
   */
  static int executeShutdown() {
    int timeouts = 0;
    for (HookEntry entry : MGR.getShutdownHooksInOrder()) {
      Future<?> future = EXECUTOR.submit(entry.getHook());
      try {
        future.get(entry.getTimeout(), entry.getTimeUnit());
      } catch (TimeoutException ex) {
        timeouts++;
        future.cancel(true);
        LOG.warn(
            "ShutdownHook '"
                + entry.getHook().getClass().getSimpleName()
                + "' timeout, "
                + ex.toString(),
            ex);
      } catch (Throwable ex) {
        LOG.warn(
            "ShutdownHook '"
                + entry.getHook().getClass().getSimpleName()
                + "' failed, "
                + ex.toString(),
            ex);
      }
    }
    return timeouts;
  }

  /**
   * Shutdown the executor thread itself.
   *
   * @param conf the configuration containing the shutdown timeout setting.
   */
  private static void shutdownExecutor(final CelebornConf conf) {
    try {
      EXECUTOR.shutdown();
      long shutdownTimeout = getShutdownTimeout(conf);
      if (!EXECUTOR.awaitTermination(shutdownTimeout, TIME_UNIT_DEFAULT)) {
        // timeout waiting for the
        LOG.error("ShutdownHookManger shutdown forcefully after" + " {} seconds.", shutdownTimeout);
        EXECUTOR.shutdownNow();
      }
      LOG.debug("ShutdownHookManger completed shutdown.");
    } catch (InterruptedException ex) {
      // interrupted.
      LOG.error("ShutdownHookManger interrupted while waiting for " + "termination.", ex);
      EXECUTOR.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public static ShutdownHookManager get() {
    return MGR;
  }

  static long getShutdownTimeout(CelebornConf conf) {
    long duration = conf.shutdownTimeoutMs();
    if (duration < TIMEOUT_MINIMUM) {
      duration = TIMEOUT_MINIMUM;
    }
    return duration;
  }

  static class HookEntry {
    private final Runnable hook;
    private final int priority;
    private final long timeout;
    private final TimeUnit unit;

    HookEntry(Runnable hook, int priority) {
      this(hook, priority, getShutdownTimeout(new CelebornConf()), TIME_UNIT_DEFAULT);
    }

    HookEntry(Runnable hook, int priority, long timeout, TimeUnit unit) {
      this.hook = hook;
      this.priority = priority;
      this.timeout = timeout;
      this.unit = unit;
    }

    @Override
    public int hashCode() {
      return hook.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      boolean eq = false;
      if (obj != null) {
        if (obj instanceof HookEntry) {
          eq = (hook == ((HookEntry) obj).hook);
        }
      }
      return eq;
    }

    Runnable getHook() {
      return hook;
    }

    int getPriority() {
      return priority;
    }

    long getTimeout() {
      return timeout;
    }

    TimeUnit getTimeUnit() {
      return unit;
    }
  }

  private final Set<HookEntry> hooks = Collections.synchronizedSet(new HashSet<HookEntry>());

  private AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

  // private to constructor to ensure singularity
  private ShutdownHookManager() {}

  /**
   * Returns the list of shutdownHooks in order of execution, Highest priority first.
   *
   * @return the list of shutdownHooks in order of execution.
   */
  List<HookEntry> getShutdownHooksInOrder() {
    List<HookEntry> list;
    synchronized (MGR.hooks) {
      list = new ArrayList<>(MGR.hooks);
    }
    Collections.sort(
        list,
        new Comparator<HookEntry>() {

          // reversing comparison so highest priority hooks are first
          @Override
          public int compare(HookEntry o1, HookEntry o2) {
            return o2.priority - o1.priority;
          }
        });
    return list;
  }

  /**
   * Adds a shutdownHook with a priority, the higher the priority the earlier will run.
   * ShutdownHooks with same priority run in a non-deterministic order.
   *
   * @param shutdownHook shutdownHook <code>Runnable</code>
   * @param priority priority of the shutdownHook.
   */
  public void addShutdownHook(Runnable shutdownHook, int priority) {
    if (shutdownHook == null) {
      throw new IllegalArgumentException("shutdownHook cannot be NULL");
    }
    if (shutdownInProgress.get()) {
      throw new IllegalStateException("Shutdown in progress, cannot add a " + "shutdownHook");
    }
    hooks.add(new HookEntry(shutdownHook, priority));
  }

  /**
   * Adds a shutdownHook with a priority and timeout the higher the priority the earlier will run.
   * ShutdownHooks with same priority run in a non-deterministic order. The shutdown hook will be
   * terminated if it has not been finished in the specified period of time.
   *
   * @param shutdownHook shutdownHook <code>Runnable</code>
   * @param priority priority of the shutdownHook
   * @param timeout timeout of the shutdownHook
   * @param unit unit of the timeout <code>TimeUnit</code>
   */
  public void addShutdownHook(Runnable shutdownHook, int priority, long timeout, TimeUnit unit) {
    if (shutdownHook == null) {
      throw new IllegalArgumentException("shutdownHook cannot be NULL");
    }
    if (shutdownInProgress.get()) {
      throw new IllegalStateException("Shutdown in progress, cannot add a " + "shutdownHook");
    }
    hooks.add(new HookEntry(shutdownHook, priority, timeout, unit));
  }

  /**
   * Removes a shutdownHook.
   *
   * @param shutdownHook shutdownHook to remove.
   * @return TRUE if the shutdownHook was registered and removed, FALSE otherwise.
   */
  public boolean removeShutdownHook(Runnable shutdownHook) {
    if (shutdownInProgress.get()) {
      throw new IllegalStateException("Shutdown in progress, cannot remove a " + "shutdownHook");
    }
    return hooks.remove(new HookEntry(shutdownHook, 0));
  }

  /**
   * Indicates if a shutdownHook is registered or not.
   *
   * @param shutdownHook shutdownHook to check if registered.
   * @return TRUE/FALSE depending if the shutdownHook is is registered.
   */
  public boolean hasShutdownHook(Runnable shutdownHook) {
    return hooks.contains(new HookEntry(shutdownHook, 0));
  }

  /**
   * Indicates if shutdown is in progress or not.
   *
   * @return TRUE if the shutdown is in progress, otherwise FALSE.
   */
  public boolean isShutdownInProgress() {
    return shutdownInProgress.get();
  }

  /** clear all registered shutdownHooks. */
  public void clearShutdownHooks() {
    hooks.clear();
  }
}
