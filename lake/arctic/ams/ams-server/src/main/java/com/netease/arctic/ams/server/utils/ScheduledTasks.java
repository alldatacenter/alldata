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

package com.netease.arctic.ams.server.utils;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The manage class of scheduled tasks.
 */
public class ScheduledTasks<K, T extends ScheduledTasks.Task> {
  private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasks.class);

  // A map for keeping scheduled tasks
  private final Map<K, TaskWrapper<T>> taskMap = new HashMap<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final String name;

  public ScheduledTasks(ThreadPool.Type type) {
    this.scheduledExecutor = ThreadPool.getPool(type);
    this.name = "Tasks-" + type.name();
  }

  /**
   * Schedule Task to be executed with fixed delay
   *
   * @param id    task id
   * @param task  task to be executed
   * @param delay the delay in milliseconds
   */
  public void addTaskToSchedulerWithFixedDelay(K id, T task, long delay) {
    if (!taskScheduled(id)) {
      TaskWrapper<T> taskWrapper = new TaskWrapper<>(task, delay);
      taskWrapper.future = getScheduledExecutor()
          .scheduleWithFixedDelay(taskWrapper, 0, delay, TimeUnit.MILLISECONDS);
      taskMap.put(id, taskWrapper);
    }
  }

  /**
   * Schedule Task to be executed with fixed rate
   *
   * @param id        task id
   * @param task      task to be executed
   * @param startTime time to start
   * @param period    the period in milliseconds
   */
  public void addTaskToSchedulerAtFixedRate(K id, T task, Date startTime, long period) {
    if (!taskScheduled(id)) {
      long initialDelay = startTime.getTime() - System.currentTimeMillis();
      TaskWrapper<T> taskWrapper = new TaskWrapper<>(task, period);
      taskWrapper.future = getScheduledExecutor()
          .scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MILLISECONDS);
      taskMap.put(id, taskWrapper);
    }
  }

  /**
   * Schedule Task to be executed with fixed rate
   *
   * @param id     task id
   * @param task   task to be executed
   * @param period the period in milliseconds
   */
  public void addTaskToSchedulerAtFixedRate(K id, T task, long period) {
    if (!taskScheduled(id)) {
      TaskWrapper<T> taskWrapper = new TaskWrapper<>(task, period);
      taskWrapper.future = getScheduledExecutor()
          .scheduleAtFixedRate(task, 0, period, TimeUnit.MILLISECONDS);
      taskMap.put(id, taskWrapper);
    }
  }

  /**
   * Remove scheduled task
   *
   * @param id task id
   */
  public void removeTaskFromScheduler(K id, boolean mayInterruptIfRunning) {
    ScheduledFuture<?> scheduledTask = taskMap.get(id).future;
    if (scheduledTask != null) {
      scheduledTask.cancel(mayInterruptIfRunning);
    }
    taskMap.remove(id);
  }

  /**
   * If id does not exist, then remove scheduled task. example id was deleted
   *
   * @param ids task ids
   * @return ids removed
   */
  public Set<K> removeLegacyTaskFromScheduler(Set<K> ids, boolean mayInterruptIfRunning) {

    Set<K> removed = new HashSet<>();
    Iterator<Map.Entry<K, TaskWrapper<T>>> iterator = taskMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<K, TaskWrapper<T>> entry = iterator.next();
      if (!ids.contains(entry.getKey())) {
        ScheduledFuture<?> scheduledTask = entry.getValue().future;
        if (scheduledTask != null) {
          scheduledTask.cancel(mayInterruptIfRunning);
        }
        iterator.remove();
        removed.add(entry.getKey());
      }
    }
    return removed;
  }

  /**
   * Whether task was scheduled or not
   *
   * @param id task id
   * @return true if scheduled, else false
   */
  public boolean taskScheduled(K id) {
    return taskMap.containsKey(id);
  }

  /**
   * get task
   *
   * @param id task id
   * @return null if task is not scheduled
   */
  public T getTask(K id) {
    TaskWrapper<T> task = taskMap.get(id);
    if (task != null) {
      return task.getTask();
    } else {
      return null;
    }
  }

  public static class TaskWrapper<T extends Runnable> implements Runnable {
    private final T task;
    private final long interval;
    private volatile long lastExecuteTime;
    private ScheduledFuture<?> future;

    public TaskWrapper(T task, long interval) {
      this.task = task;
      this.interval = interval;
    }

    public T getTask() {
      return task;
    }

    public long getInterval() {
      return interval;
    }

    public long getLastExecuteTime() {
      return lastExecuteTime;
    }

    @Override
    public void run() {
      this.lastExecuteTime = System.currentTimeMillis();
      task.run();
    }
  }

  public interface Task extends Runnable {

    default void updateTask(Supplier<Task> newTask) {
    }

  }

  private ScheduledExecutorService getScheduledExecutor() {
    return scheduledExecutor;
  }

  /**
   * Check running tasks, cancel invalid tasks, add new tasks, change task's interval.
   *
   * @param validKeys             - tasks to keep
   * @param intervalSupplier      - generate interval
   * @param taskFactory           - build task
   * @param mayInterruptIfRunning - interrupt task when cancel
   */
  public synchronized void checkRunningTask(Set<K> validKeys,
                                            Function<K, Long> intervalSupplier,
                                            Function<K, T> taskFactory,
                                            boolean mayInterruptIfRunning) {
    Set<K> removedKeys = removeLegacyTaskFromScheduler(validKeys, false);
    if (!CollectionUtils.isEmpty(removedKeys)) {
      LOG.info("{} cancel {} task(s), {}", name, removedKeys.size(), removedKeys);
    }

    for (K key : validKeys) {
      try {
        Long interval = intervalSupplier.apply(key);
        boolean needSchedulerTask = true;
        long lastExecuteTime = 0;
        if (taskScheduled(key)) {
          needSchedulerTask = false;
          TaskWrapper<T> taskWrapper = taskMap.get(key);
          if (interval == null) {
            removeTaskFromScheduler(key, mayInterruptIfRunning);
          } else if (taskWrapper.getInterval() != interval) {
            removeTaskFromScheduler(key, mayInterruptIfRunning);
            lastExecuteTime = taskWrapper.getLastExecuteTime();
            LOG.info("{} task {} commit interval change from {}ms to {}ms",
                name, key, taskWrapper.getInterval(), interval);
            needSchedulerTask = true;
          } else {
            taskWrapper.getTask().updateTask(() -> taskFactory.apply(key));
          }
        }
        if (needSchedulerTask) {
          T task = taskFactory.apply(key);
          if (task != null) {
            if (lastExecuteTime == 0) {
              // next commitTime = now
              addTaskToSchedulerAtFixedRate(key, task, interval);
            } else {
              // next commitTime = lastExecuteTime + interval
              addTaskToSchedulerAtFixedRate(key, task, new Date(lastExecuteTime + interval), interval);
            }
            LOG.info("{} schedule {} with interval {}ms", name, key, interval);
          }
        }
      } catch (Throwable t) {
        LOG.error("{} ==== unexpected error while check running tasks, continue", name, t);
      }
    }
  }
}
