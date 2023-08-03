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
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.service.Service;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(7)
public class TaskManagement implements Service, ActiveStateChangeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TaskManagement.class);

    private       TaskExecutor              taskExecutor;
    private final Configuration             configuration;
    private final TaskRegistry              registry;
    private final Statistics                statistics;
    private final Map<String, TaskFactory>  taskTypeFactoryMap;
    private       boolean                   hasStarted;

    @Inject
    public TaskManagement(Configuration configuration, TaskRegistry taskRegistry) {
        this.configuration      = configuration;
        this.registry           = taskRegistry;
        this.statistics         = new Statistics();
        this.taskTypeFactoryMap = new HashMap<>();
    }

    @VisibleForTesting
    TaskManagement(Configuration configuration, TaskRegistry taskRegistry, TaskFactory taskFactory) {
        this.configuration      = configuration;
        this.registry           = taskRegistry;
        this.statistics         = new Statistics();
        this.taskTypeFactoryMap = new HashMap<>();

        createTaskTypeFactoryMap(taskTypeFactoryMap, taskFactory);
    }

    @Override
    public void start() throws AtlasException {
        if (configuration == null || !HAConfiguration.isHAEnabled(configuration)) {
            startInternal();
        } else {
            LOG.info("TaskManagement.start(): deferring until instance activation");
        }

        this.hasStarted = true;
    }

    public boolean hasStarted() {
        return this.hasStarted;
    }

    @Override
    public void stop() throws AtlasException {
        LOG.info("TaskManagement: Stopped!");
    }

    @Override
    public void instanceIsActive() throws AtlasException {
        LOG.info("==> TaskManagement.instanceIsActive()");

        startInternal();

        LOG.info("<== TaskManagement.instanceIsActive()");
    }

    @Override
    public void instanceIsPassive() throws AtlasException {
        LOG.info("TaskManagement.instanceIsPassive(): no action needed");
    }

    @Override
    public int getHandlerOrder() {
        return HandlerOrder.TASK_MANAGEMENT.getOrder();
    }

    public void addFactory(TaskFactory taskFactory) {
        createTaskTypeFactoryMap(this.taskTypeFactoryMap, taskFactory);
    }

    public AtlasTask createTask(String taskType, String createdBy, Map<String, Object> parameters) {
        return this.registry.createVertex(taskType, createdBy, parameters);
    }

    public List<AtlasTask> getPendingTasks() {
        return this.registry.getPendingTasks();
    }

    public List<AtlasTask> getAll() {
        return this.registry.getAll();
    }

    public void addAll(List<AtlasTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        dispatchTasks(tasks);
    }

    public AtlasTask getByGuid(String guid) throws AtlasBaseException {
        try {
            return this.registry.getById(guid);
        } catch (Exception exception) {
            LOG.error("Error: getByGuid: {}", guid);

            throw new AtlasBaseException(exception);
        }
    }

    public List<AtlasTask> getByGuids(List<String> guids) throws AtlasBaseException {
        List<AtlasTask> ret = new ArrayList<>();

        for (String guid : guids) {
            AtlasTask task = getByGuid(guid);

            if (task != null) {
                ret.add(task);
            }
        }

        return ret;
    }

    public void deleteByGuid(String guid) throws AtlasBaseException {
        try {
            this.registry.deleteByGuid(guid);
        } catch (Exception exception) {
            throw new AtlasBaseException(exception);
        }
    }

    public void deleteByGuids(List<String> guids) throws AtlasBaseException {
        if (CollectionUtils.isEmpty(guids)) {
            return;
        }

        for (String guid : guids) {
            this.registry.deleteByGuid(guid);
        }
    }

    private synchronized void dispatchTasks(List<AtlasTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        if (this.taskExecutor == null) {
            this.taskExecutor = new TaskExecutor(registry, taskTypeFactoryMap, statistics);
        }

        this.taskExecutor.addAll(tasks);

        this.statistics.print();
    }

    private void startInternal() {
        if (AtlasConfiguration.TASKS_USE_ENABLED.getBoolean() == false) {
            return;
        }

        LOG.info("TaskManagement: Started!");
        if (this.taskTypeFactoryMap.size() == 0) {
            LOG.warn("Not factories registered! Pending tasks will be queued once factories are registered!");
            return;
        }

        queuePendingTasks();
    }

    private void queuePendingTasks() {
        if (AtlasConfiguration.TASKS_USE_ENABLED.getBoolean() == false) {
            return;
        }

        List<AtlasTask> pendingTasks = this.registry.getPendingTasks();

        LOG.info("TaskManagement: Found: {}: Tasks in pending state.", pendingTasks.size());

        addAll(pendingTasks);
    }

    @VisibleForTesting
    static Map<String, TaskFactory> createTaskTypeFactoryMap(Map<String, TaskFactory> taskTypeFactoryMap, TaskFactory factory) {
        List<String> supportedTypes = factory.getSupportedTypes();

        if (CollectionUtils.isEmpty(supportedTypes)) {
            LOG.warn("{}: Supported types returned empty!", factory.getClass());

            return taskTypeFactoryMap;
        }

        for (String type : supportedTypes) {
            taskTypeFactoryMap.put(type, factory);
        }

        return taskTypeFactoryMap;
    }

    static class Statistics {
        private static final TaskExecutor.TaskLogger logger = TaskExecutor.TaskLogger.getLogger();
        private static final long REPORT_FREQUENCY = 30000L;

        private final AtomicInteger total               = new AtomicInteger(0);
        private final AtomicInteger countSinceLastCheck = new AtomicInteger(0);
        private final AtomicInteger totalWithErrors     = new AtomicInteger(0);
        private final AtomicInteger totalSucceed        = new AtomicInteger(0);
        private       long          lastCheckTime       = System.currentTimeMillis();

        public void error() {
            this.countSinceLastCheck.incrementAndGet();
            this.totalWithErrors.incrementAndGet();
        }

        public void success() {
            this.countSinceLastCheck.incrementAndGet();
            this.totalSucceed.incrementAndGet();
        }

        public void increment() {
            increment(1);
        }

        public void increment(int delta) {
            this.total.addAndGet(delta);
            this.countSinceLastCheck.addAndGet(delta);
        }

        public void print() {
            long now = System.currentTimeMillis();
            long diff = now - this.lastCheckTime;

            if (diff < REPORT_FREQUENCY) {
                return;
            }

            logger.info(String.format("TaskManagement: Processing stats: total=%d, sinceLastStatsReport=%d completedWithErrors=%d, succeded=%d",
                                       this.total.get(), this.countSinceLastCheck.getAndSet(0),
                                       this.totalWithErrors.get(), this.totalSucceed.get()));
            this.lastCheckTime = now;
        }

        public void successPrint() {
            success();
            print();
        }

        @VisibleForTesting
        int getTotal() {
            return this.total.get();
        }

        @VisibleForTesting
        int getTotalSuccess() {
            return this.totalSucceed.get();
        }

        @VisibleForTesting
        int getTotalError() {
            return this.totalWithErrors.get();
        }
    }
}