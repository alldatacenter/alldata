/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskExecutor {
    private static final Logger     LOG              = LoggerFactory.getLogger(TaskExecutor.class);
    private static final TaskLogger TASK_LOG         = TaskLogger.getLogger();
    private static final String     TASK_NAME_FORMAT = "atlas-task-%d-";

    private final TaskRegistry              registry;
    private final Map<String, TaskFactory>  taskTypeFactoryMap;
    private final TaskManagement.Statistics statistics;
    private final ExecutorService           executorService;

    public TaskExecutor(TaskRegistry registry, Map<String, TaskFactory> taskTypeFactoryMap, TaskManagement.Statistics statistics) {
        this.registry           = registry;
        this.taskTypeFactoryMap = taskTypeFactoryMap;
        this.statistics         = statistics;
        this.executorService    = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                    .setDaemon(true)
                                                                    .setNameFormat(TASK_NAME_FORMAT + Thread.currentThread().getName())
                                                                    .build());
    }

    public void addAll(List<AtlasTask> tasks) {
        for (AtlasTask task : tasks) {
            if (task == null) {
                continue;
            }

            TASK_LOG.log(task);

            this.executorService.submit(new TaskConsumer(task, this.registry, this.taskTypeFactoryMap, this.statistics));
        }
    }

    @VisibleForTesting
    void waitUntilDone() throws InterruptedException {
        Thread.sleep(5000);
    }

    static class TaskConsumer implements Runnable {
        private static final int MAX_ATTEMPT_COUNT = 3;

        private final Map<String, TaskFactory>  taskTypeFactoryMap;
        private final TaskRegistry              registry;
        private final TaskManagement.Statistics statistics;
        private final AtlasTask                 task;

        public TaskConsumer(AtlasTask task, TaskRegistry registry, Map<String, TaskFactory> taskTypeFactoryMap, TaskManagement.Statistics statistics) {
            this.task               = task;
            this.registry           = registry;
            this.taskTypeFactoryMap = taskTypeFactoryMap;
            this.statistics         = statistics;
        }

        @Override
        public void run() {
            AtlasVertex taskVertex = null;
            int         attemptCount;

            try {
                taskVertex = registry.getVertex(task.getGuid());

                if (task == null || taskVertex == null || task.getStatus() == AtlasTask.Status.COMPLETE) {
                    TASK_LOG.warn("Task not scheduled as it was not found or status was COMPLETE!", task);

                    return;
                }

                statistics.increment(1);

                attemptCount = task.getAttemptCount();

                if (attemptCount >= MAX_ATTEMPT_COUNT) {
                    TASK_LOG.warn("Max retry count for task exceeded! Skipping!", task);

                    return;
                }

                performTask(taskVertex, task);
            } catch (InterruptedException exception) {
                if (task != null) {
                    registry.updateStatus(taskVertex, task);

                    TASK_LOG.error("{}: {}: Interrupted!", task, exception);
                } else {
                    LOG.error("Interrupted!", exception);
                }

                statistics.error();
            } catch (Exception exception) {
                if (task != null) {
                    task.updateStatusFromAttemptCount();

                    registry.updateStatus(taskVertex, task);

                    TASK_LOG.error("Error executing task. Please perform the operation again!", task, exception);
                } else {
                    LOG.error("Error executing. Please perform the operation again!", exception);
                }

                statistics.error();
            } finally {
                if (task != null) {
                    this.registry.commit();

                    TASK_LOG.log(task);
                }
            }
        }

        private void performTask(AtlasVertex taskVertex, AtlasTask task) throws Exception {
            TaskFactory  factory      = taskTypeFactoryMap.get(task.getType());
            if (factory == null) {
                LOG.error("taskTypeFactoryMap does not contain task of type: {}", task.getType());
                return;
            }

            AbstractTask runnableTask = factory.create(task);

            runnableTask.run();

            registry.deleteComplete(taskVertex, task);

            statistics.successPrint();
        }
    }

    static class TaskLogger {
        private static final Logger LOG = LoggerFactory.getLogger("TASKS");

        public static TaskLogger getLogger() {
            return new TaskLogger();
        }

        public void info(String message) {
            LOG.info(message);
        }

        public void log(AtlasTask task) {
            LOG.info(AtlasType.toJson(task));
        }

        public void warn(String message, AtlasTask task) {
            LOG.warn(message, AtlasType.toJson(task));
        }

        public void error(String s, AtlasTask task, Exception exception) {
            LOG.error(s, AtlasType.toJson(task), exception);
        }
    }
}