/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.stomp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.events.listeners.tasks.TaskStatusListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class NamedTasksSubscriptions {
  private static Logger LOG = LoggerFactory.getLogger(NamedTasksSubscriptions.class);

  private ConcurrentHashMap<String, List<SubscriptionId>> taskIds = new ConcurrentHashMap<>();
  private final String subscriptionPrefix = "/events/tasks/";
  private final Lock taskIdsLock = new ReentrantLock();

  private Provider<TaskStatusListener> taskStatusListenerProvider;

  @Inject
  public NamedTasksSubscriptions(Provider<TaskStatusListener> taskStatusListenerProvider) {
    this.taskStatusListenerProvider = taskStatusListenerProvider;
  }

  public void addTaskId(String sessionId, Long taskId, String id) {
    try {
      taskIdsLock.lock();
      taskIds.compute(sessionId, (sid, ids) -> {
        if (ids == null) {
          ids = new ArrayList<>();
        }
        AtomicBoolean completed = new AtomicBoolean(false);
        taskStatusListenerProvider.get().getActiveTasksMap().computeIfPresent(taskId, (tid, task) -> {
          if (task.getStatus().isCompletedState()) {
            completed.set(true);
          }
          return task;
        });
        if (!completed.get()) {
          ids.add(new SubscriptionId(taskId, id));
        }
        return ids;
      });
      LOG.info(String.format("Task subscription was added for sessionId = %s, taskId = %s, id = %s",
          sessionId, taskId, id));
    } finally {
      taskIdsLock.unlock();
    }
  }

  public void removeId(String sessionId, String id) {
    taskIds.computeIfPresent(sessionId, (sid, tasks) -> {
      Iterator<SubscriptionId> iterator = tasks.iterator();
      while (iterator.hasNext()) {
        if (iterator.next().getId().equals(id)) {
          iterator.remove();
          LOG.info(String.format("Task subscription was removed for sessionId = %s, id = %s", sessionId, id));
        }
      }
      return tasks;
    });
  }

  public void removeTaskId(Long taskId) {
    try {
      taskIdsLock.lock();
      for (String sessionId : taskIds.keySet()) {
        taskIds.computeIfPresent(sessionId, (id, tasks) -> {
          Iterator<SubscriptionId> iterator = tasks.iterator();
          while (iterator.hasNext()) {
            if (iterator.next().getTaskId().equals(taskId)) {
              iterator.remove();
              LOG.info(String.format("Task subscription was removed for sessionId = %s and taskId = %s",
                  sessionId, taskId));
            }
          }
          return tasks;
        });
      }
    } finally {
      taskIdsLock.unlock();
    }
  }

  public void removeSession(String sessionId) {
    try {
      taskIdsLock.lock();
      taskIds.remove(sessionId);
      LOG.info(String.format("Task subscriptions were removed for sessionId = %s", sessionId));
    } finally {
      taskIdsLock.unlock();
    }
  }

  public Optional<Long> matchDestination(String destination) {
    Optional<Long> taskIdOpt = Optional.of(StringUtils.substringAfter(destination, subscriptionPrefix))
        .filter(StringUtils::isNotEmpty)
        .filter(StringUtils::isNumeric)
        .map(Long::parseLong);
    return taskIdOpt;
  }

  public void addDestination(String sessionId, String destination, String id) {
    Optional<Long> taskIdOpt = matchDestination(destination);
    if (taskIdOpt.isPresent()) {
      addTaskId(sessionId, taskIdOpt.get(), id);
    }
  }

  public boolean checkTaskId(Long taskId) {
    for (List<SubscriptionId> ids: taskIds.values()) {
      for (SubscriptionId subscriptionId : ids) {
        if (subscriptionId.getTaskId().equals(taskId)) {
          return true;
        }
      }
    }
    return false;
  }

  public class SubscriptionId {
    private final Long taskId;
    private final String id;

    public SubscriptionId(Long taskId, String id) {
      this.taskId = taskId;
      this.id = id;
    }

    public Long getTaskId() {
      return taskId;
    }

    public String getId() {
      return id;
    }
  }
}
